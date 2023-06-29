package ml.docilealligator.infinityforreddit.apis;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.HeaderMap;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface MatrixAPI {
    @GET("/_matrix/media/custom/stickers/")
    Call<String> getStickers(@HeaderMap Map<String, String> headers);

    @POST("/_matrix/client/r0/login")
    Call<String> login(@HeaderMap Map<String, String> headers, @Body String body);

    @GET("/_matrix/client/r0/sync")
    Call<String> sync(@HeaderMap Map<String, String> headers, @Query("filter") String filter, @Query("set_presence") String presence, @Query("timeout") int timeout, @Query("since") String since);

}
