package com.junctionx.pathfinder;

import com.google.gson.JsonObject;
import com.junctionx.pathfinder.model.Mobilities;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface JsonPlaceHolderApi {

    @GET("api/v1/mobilities")
    Call<Mobilities> getMobilities(@Query("northeast_lat") double northeast_lat, @Query("northeast_lng") double northeast_lng, @Query("southwest_lat") double southwest_lat, @Query("southwest_lng") double southwest_lng);

    @GET("api/v1/mobilities/obstacles_base_map_guide")
    Call<Mobilities> getObstacles(@Query("lat_and_lng") Object positionList);

    @GET("api/v1/mobilities/from_my_position")
    Call<Mobilities> getObstaclesPosition(@Query("lat") double lat, @Query("lng") double lng, @Query("ride_type") String ride_type);
}
