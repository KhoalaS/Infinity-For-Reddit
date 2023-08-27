package ml.docilealligator.infinityforreddit;

import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import ml.docilealligator.infinityforreddit.apis.RedditAPI;
import ml.docilealligator.infinityforreddit.utils.APIUtils;
import ml.docilealligator.infinityforreddit.utils.SharedPreferencesUtils;
import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Call;
import retrofit2.Retrofit;

public class AnonymousAccessTokenInterceptor implements Interceptor {
    private Retrofit mRetrofit;
    private SharedPreferences mAnonymousAccountSharedPreferences;

    public AnonymousAccessTokenInterceptor(Retrofit retrofit, SharedPreferences anonymousAccountSharedPreferences) {
        mRetrofit = retrofit;
        mAnonymousAccountSharedPreferences = anonymousAccountSharedPreferences;
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        String accessToken = mAnonymousAccountSharedPreferences.getString(SharedPreferencesUtils.ACCESS_TOKEN, "");
        if ("".equals(accessToken)) {
            accessToken = refreshAccessToken();
        }

        Response response = chain.proceed(newRequest(chain.request(), accessToken));

        if (response.code() != 401) {
            return response;
        }

        final String accessTokenFromDatabase = accessToken;
        String accessTokenHeader = response.request().header(APIUtils.AUTHORIZATION_KEY);
        if (accessTokenHeader == null) {
            return response;
        }

        accessToken = accessTokenHeader.substring(APIUtils.AUTHORIZATION_BASE.length());
        synchronized (this) {
            if (accessToken.equals(accessTokenFromDatabase)) {
                final String newAccessToken = refreshAccessToken();
                if ("".equals(newAccessToken)) {
                    return response;
                } else {
                    response.close();
                    return chain.proceed(newRequest(response.request(), newAccessToken));
                }
            } else {
                return response;
            }
        }
    }

    private Request newRequest(Request request, String accessToken) {
        return request.newBuilder()
                .headers(Headers.of(APIUtils.getOAuthHeader(accessToken)))
                .build();
    }

    private String refreshAccessToken() {
        RedditAPI api = mRetrofit.create(RedditAPI.class);

        Map<String, String> accessTokenFields = new HashMap<>();
        accessTokenFields.put(APIUtils.GRANT_TYPE_KEY, APIUtils.GRANT_TYPE_INSTALLED_CLIENT);
        accessTokenFields.put(APIUtils.DEVICE_ID_KEY, mAnonymousAccountSharedPreferences.getString(SharedPreferencesUtils.DEVICE_ID, ""));

        Call<String> accessTokenCall = api.getAccessToken(APIUtils.getHttpBasicAuthHeader(), accessTokenFields);
        try {
            retrofit2.Response<String> response = accessTokenCall.execute();
            if (response.isSuccessful() && response.body() != null) {
                JSONObject jsonObject = new JSONObject(response.body());
                String newAccessToken = jsonObject.getString(APIUtils.ACCESS_TOKEN_KEY);

                mAnonymousAccountSharedPreferences.edit()
                        .putString(SharedPreferencesUtils.ACCESS_TOKEN, newAccessToken).apply();

                return newAccessToken;
            }
            return "";
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }

        return "";
    }
}
