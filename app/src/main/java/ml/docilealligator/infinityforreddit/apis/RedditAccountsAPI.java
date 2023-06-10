package ml.docilealligator.infinityforreddit.apis;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.HeaderMap;
import retrofit2.http.POST;

public interface RedditAccountsAPI {

    @POST("/api/login")
    Call<String> login(@HeaderMap Map<String, String> headers, @Body String body);

    @POST("/api/access_token")
    Call<String> getAccessToken(@HeaderMap Map<String, String> headers, @Body String body);

}
