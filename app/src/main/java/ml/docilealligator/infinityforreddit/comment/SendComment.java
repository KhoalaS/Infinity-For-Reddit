package ml.docilealligator.infinityforreddit.comment;

import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ml.docilealligator.infinityforreddit.account.Account;
import ml.docilealligator.infinityforreddit.apis.RedditAPI;
import ml.docilealligator.infinityforreddit.utils.APIUtils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class SendComment {
    public static void sendComment(Executor executor, Handler handler, String commentMarkdown,
                                   String thingFullname, int parentDepth,
                                   Retrofit newAuthenticatorOauthRetrofit, Account account,
                                   SendCommentListener sendCommentListener) {
        Map<String, String> headers = APIUtils.getOAuthHeader(account.getAccessToken());

        RedditAPI api = newAuthenticatorOauthRetrofit.create(RedditAPI.class);
        Map<String, String> params = new HashMap<>();

        Pattern gifPattern = Pattern.compile("!\\[gif]\\(giphy\\|\\w+\\)");
        Matcher matcher = gifPattern.matcher(commentMarkdown);
        boolean containsMedia = matcher.find();

        if (containsMedia){
            api.convertRichTextToJson(commentMarkdown, "rtjson", headers).enqueue(new Callback<String>() {
                @Override
                public void onResponse(Call<String> call, Response<String> response) {
                    if (response.isSuccessful()) {
                        try{
                        JSONObject responseJSON = new JSONObject(response.body());
                        JSONObject data = responseJSON.getJSONObject("output");
                        String stringData = data.toString();

                        params.put(APIUtils.API_TYPE_KEY, "json");
                        params.put(APIUtils.RETURN_RTJSON_KEY, "true");
                        params.put(APIUtils.RICHTEXT_JSON_KEY, stringData);
                        params.put(APIUtils.THING_ID_KEY, thingFullname);
                        api.sendCommentOrReplyToMessage(headers, params).enqueue(new Callback<String>() {
                                @Override
                                public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                                    if (response.isSuccessful()) {
                                        ParseComment.parseSentComment(executor, handler, response.body(), parentDepth, new ParseComment.ParseSentCommentListener() {
                                            @Override
                                            public void onParseSentCommentSuccess(Comment comment) {
                                                sendCommentListener.sendCommentSuccess(comment);
                                            }

                                            @Override
                                            public void onParseSentCommentFailed(@Nullable String errorMessage) {
                                                sendCommentListener.sendCommentFailed(errorMessage);
                                            }
                                        });
                                    } else {
                                        sendCommentListener.sendCommentFailed(response.message());
                                    }
                                }

                                @Override
                                public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                                    sendCommentListener.sendCommentFailed(t.getMessage());
                                }
                            });

                        } catch (JSONException e){

                        }

                    }
                }

                @Override
                public void onFailure(Call<String> call, Throwable t) {
                    sendCommentListener.sendCommentFailed(t.getMessage());
                }
            });
        }

        if (containsMedia) return;

        params.put(APIUtils.API_TYPE_KEY, "json");
        params.put(APIUtils.RETURN_RTJSON_KEY, "true");
        params.put(APIUtils.TEXT_KEY, commentMarkdown);
        params.put(APIUtils.THING_ID_KEY, thingFullname);

        api.sendCommentOrReplyToMessage(headers, params).enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                if (response.isSuccessful()) {
                    ParseComment.parseSentComment(executor, handler, response.body(), parentDepth, new ParseComment.ParseSentCommentListener() {
                        @Override
                        public void onParseSentCommentSuccess(Comment comment) {
                            sendCommentListener.sendCommentSuccess(comment);
                        }

                        @Override
                        public void onParseSentCommentFailed(@Nullable String errorMessage) {
                            sendCommentListener.sendCommentFailed(errorMessage);
                        }
                    });
                } else {
                    sendCommentListener.sendCommentFailed(response.message());
                }
            }

            @Override
            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                sendCommentListener.sendCommentFailed(t.getMessage());
            }
        });
    }

    public interface SendCommentListener {
        void sendCommentSuccess(Comment comment);

        void sendCommentFailed(String errorMessage);
    }
}
