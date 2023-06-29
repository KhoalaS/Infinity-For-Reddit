package ml.docilealligator.infinityforreddit.apis;

import java.util.Map;

import ml.docilealligator.infinityforreddit.activities.ChatOverviewActivity;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.HeaderMap;
import retrofit2.http.POST;

public interface GqlAPI {

    @POST("/")
    Call<String> getRequests(@HeaderMap Map<String, String> headers, @Body String body);

    @POST("/")
    Call<String> getJoined(@HeaderMap Map<String, String> headers, @Body RequestBody body);

}
