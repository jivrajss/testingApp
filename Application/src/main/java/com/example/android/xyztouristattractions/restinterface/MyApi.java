package com.example.android.xyztouristattractions.restinterface;

import com.example.android.xyztouristattractions.DataModel;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;


import retrofit.Callback;
import retrofit.http.GET;

/**
 * Created by jivraj.singh on 10-12-2015.
 */
public interface MyApi {
    public static final String BASE_URL="https://dl.dropboxusercontent.com/u/100695038";

    @GET("/response.json.txt")
    public void getResponse(Callback<JsonElement> callback);
}
