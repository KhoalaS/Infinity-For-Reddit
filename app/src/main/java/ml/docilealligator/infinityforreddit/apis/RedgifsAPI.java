package ml.docilealligator.infinityforreddit.apis;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.HEAD;
import retrofit2.http.HeaderMap;
import retrofit2.http.Path;

public interface RedgifsAPI {
    @GET("/v2/gifs/{id}")
    Call<String> getRedgifsData(@HeaderMap Map<String, String> headers, @Path("id") String id);

    @HEAD("v2/gifs/{id}/hd.m3u8")
    Call<Void> testHLS(@HeaderMap Map<String, String> headers, @Path("id") String id);

    @GET("/v2/auth/temporary")
    Call<String> getRedgifsAccessToken();
}
