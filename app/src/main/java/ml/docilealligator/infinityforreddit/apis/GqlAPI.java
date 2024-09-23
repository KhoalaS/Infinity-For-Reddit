package ml.docilealligator.infinityforreddit.apis;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Map;

import ml.docilealligator.infinityforreddit.SortType;
import ml.docilealligator.infinityforreddit.activities.ChatOverviewActivity;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.HeaderMap;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface GqlAPI {

    @POST("/")
    Call<String> getRequests(@HeaderMap Map<String, String> headers, @Body String body);

    @POST("/")
    Call<String> getJoined(@HeaderMap Map<String, String> headers, @Body RequestBody body);

    @POST("/")
    ListenableFuture<Response<String>> getSubredditBestPostsOauthListenableFuture(@HeaderMap Map<String, String> headers, @Body RequestBody body);

    @POST("/")
    ListenableFuture<Response<String>> getUserPostsOauthListenableFuture(@HeaderMap Map<String, String> headers, @Body RequestBody body);

    @POST("/")
    ListenableFuture<Response<String>> getBestPostsListenableFuture(@HeaderMap Map<String, String> headers, @Body RequestBody body);

    @POST("/")
    ListenableFuture<Response<String>> searchPostsOauthListenableFuture(@HeaderMap Map<String, String> headers, @Body RequestBody body);

    @POST("/")
    Call<String> getPostComments(@HeaderMap Map<String, String> headers, @Body RequestBody body);

    @POST("/")
    Call<String> subredditAutocomplete(@HeaderMap Map<String, String> headers, @Body RequestBody body);

    @POST("/")
    Call<String> getSubredditData(@HeaderMap Map<String, String> headers, @Body RequestBody body);

    @POST("/")
    Call<String> subredditSubscription(@HeaderMap Map<String, String> headers, @Body RequestBody body);

    @POST("/")
    Call<String> updatePostVoteState(@HeaderMap Map<String, String> headers, @Body RequestBody body);

    @POST("/")
    Call<String> updateCommentVoteState(@HeaderMap Map<String, String> headers, @Body RequestBody body);

    @POST("/")
    Call<String> savePost(@HeaderMap Map<String, String> headers, @Body RequestBody body);

    @POST("/")
    Call<String> saveComment(@HeaderMap Map<String, String> headers, @Body RequestBody body);

    @POST("/")
    Call<String> searchUsers(@HeaderMap Map<String, String> headers, @Body RequestBody body);

    @POST("/")
    Call<String> searchSubreddit(@HeaderMap Map<String, String> headers, @Body RequestBody body);
}
