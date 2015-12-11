package com.example.android.xyztouristattractions.restinterface;

import android.net.Uri;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.squareup.okhttp.OkHttpClient;

import java.lang.reflect.Type;

import retrofit.RestAdapter;
import retrofit.client.OkClient;
import retrofit.converter.GsonConverter;

/**
 * Created by jivraj.singh on 10-12-2015.
 */
public class RestClient {

    private static MyApi REST_CLIENT;

    static {
        setupRestClient();
    }

    private RestClient() {
    }

    public static MyApi getRestClient() {
        return REST_CLIENT;
    }

    private static void setupRestClient() {
//        Gson gson = new GsonBuilder()
//                .registerTypeAdapter(Uri.class, new UriDeserializer())
//                .create();
        RestAdapter.Builder builder = new RestAdapter.Builder()
                .setEndpoint(MyApi.BASE_URL).setConverter(new GsonConverter(new Gson()))
                /*.setClient(new OkClient(new OkHttpClient()))*/;

        RestAdapter restAdapter = builder.build();
        REST_CLIENT = restAdapter.create(MyApi.class);
    }
    private static class UriDeserializer implements JsonDeserializer<Uri> {
        @Override
        public Uri deserialize(final JsonElement src, final Type srcType,
                               final JsonDeserializationContext context) throws JsonParseException {
            return Uri.parse(src.getAsString());
        }
    }
}