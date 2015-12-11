/*
 * Copyright 2015 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.xyztouristattractions.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.android.xyztouristattractions.DataModel;
import com.example.android.xyztouristattractions.PlaceJSONParser;
import com.example.android.xyztouristattractions.R;
import com.example.android.xyztouristattractions.common.Attraction;
import com.example.android.xyztouristattractions.common.Constants;
import com.example.android.xyztouristattractions.common.Utils;
import com.example.android.xyztouristattractions.provider.TouristAttractions;
import com.example.android.xyztouristattractions.restinterface.RestClient;
import com.example.android.xyztouristattractions.service.UtilityService;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.maps.android.SphericalUtil;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

import static com.example.android.xyztouristattractions.provider.TouristAttractions.ATTRACTIONS;

/**
 * The main tourist attraction fragment which contains a list of attractions
 * sorted by distance (contained inside
 * {@link com.example.android.xyztouristattractions.ui.AttractionListActivity}).
 */
public class AttractionListFragment extends Fragment implements Callback<JsonElement> {

    private AttractionAdapter mAdapter;
    private LatLng mLatestLocation;
    private int mImageSize;
    private boolean mItemClicked;
    private AttractionsRecyclerView recyclerView;

    public AttractionListFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Load a larger size image to make the activity transition to the detail screen smooth
        mImageSize = getResources().getDimensionPixelSize(R.dimen.image_size)
                * Constants.IMAGE_ANIM_MULTIPLIER;

        mLatestLocation = Utils.getLocation(getActivity());
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        RestClient.getRestClient().getResponse(this);
        List<Attraction> attractions = loadAttractionsFromLocation(mLatestLocation);
                mAdapter = new AttractionAdapter(getActivity(), new ArrayList<Attraction>());
        recyclerView =
                (AttractionsRecyclerView) view.findViewById(android.R.id.list);
        recyclerView.setEmptyView(view.findViewById(android.R.id.empty));
        recyclerView.setHasFixedSize(true);
//        recyclerView.setAdapter(mAdapter);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        mItemClicked = false;
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                mBroadcastReceiver, UtilityService.getLocationUpdatedIntentFilter());
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mBroadcastReceiver);
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Location location =
                    intent.getParcelableExtra(FusedLocationProviderApi.KEY_LOCATION_CHANGED);
            if (location != null) {
                mLatestLocation = new LatLng(location.getLatitude(), location.getLongitude());
                mAdapter.mAttractionList = loadAttractionsFromLocation(mLatestLocation);
                mAdapter.notifyDataSetChanged();
//                Toast.makeText(getActivity(), "Calling from BroadCastReciever()--", Toast.LENGTH_SHORT).show();
//                RestClient.getRestClient().getResponse(AttractionListFragment.this);
            }
        }
    };

    public static List<Attraction> loadAttractionsFromLocation(final LatLng curLatLng) {
        String closestCity = TouristAttractions.getClosestCity(curLatLng);
        if (closestCity != null) {
            List<Attraction> attractions = ATTRACTIONS.get(closestCity);
            if (curLatLng != null) {
                Collections.sort(attractions,
                        new Comparator<Attraction>() {
                            @Override
                            public int compare(Attraction lhs, Attraction rhs) {
                                double lhsDistance = SphericalUtil.computeDistanceBetween(
                                        lhs.location, curLatLng);
                                double rhsDistance = SphericalUtil.computeDistanceBetween(
                                        rhs.location, curLatLng);
                                return (int) (lhsDistance - rhsDistance);
                            }
                        }
                );
            }
            return attractions;
        }
        return null;
    }

    @Override
    public void success(JsonElement string, Response response) {
        JsonObject mJsonObject = null;
        List<Attraction> attractionsList = new ArrayList<Attraction>();
        if (string.isJsonObject()) {
//            Toast.makeText(getActivity(), "OnSuccess--Object" + string.toString(), Toast.LENGTH_LONG).show();
            mJsonObject = string.getAsJsonObject();
            JsonElement mElement = mJsonObject.get("attractions");
            if (mElement.isJsonArray()) {
                JsonArray mArray = mElement.getAsJsonArray();
                HashMap<String, List<Attraction>> ATTRACTIONS =
                        new HashMap<String, List<Attraction>>();

                String city="";
                for (JsonElement element : mArray) {
                    JsonObject attractions = null;
                    if (element.isJsonObject()) {
                        attractions = element.getAsJsonObject();
                        JsonObject imageURl = attractions.get("imageUrl").getAsJsonObject();
                        JsonObject secImageURl = attractions.get("secondaryImageUrl").getAsJsonObject();
                        JsonObject location = attractions.get("location").getAsJsonObject();
                        city=attractions.get("city").toString();
//                        Toast.makeText(getActivity(), "OnSuccess--" + attractions.get("name") + imageURl.get("uriString")
//                                + attractions.get("city") + attractions.get("longDescription")
//                                + attractions.get("description"), Toast.LENGTH_LONG).show();
//                        String name, String description, String longDescription, Uri imageUrl,
//                                Uri secondaryImageUrl, LatLng location, String city
                        attractionsList.add(new Attraction(attractions.get("name").toString(),
                                attractions.get("description").toString(), attractions.get("longDescription").toString(),
                                Uri.parse(imageURl.get("uriString").toString()), Uri.parse(secImageURl.get("uriString").toString()),
                                new LatLng(Float.parseFloat(location.get("latitude").toString()), Float.parseFloat(location.get("longitude").toString())),
                                attractions.get("city").toString()));
                        Log.d(AttractionListFragment.class.getSimpleName(),imageURl.get("uriString").toString());
                        Log.d(AttractionListFragment.class.getSimpleName(),secImageURl.get("uriString").toString());

                    } else
                        Toast.makeText(getActivity(), "OnSuccess--Object--Array--Not Object", Toast.LENGTH_LONG).show();
                }
                ATTRACTIONS.put(city,attractionsList);

            } else
                Toast.makeText(getActivity(), "OnSuccess--Object--Not Array", Toast.LENGTH_LONG).show();
        } else
            Toast.makeText(getActivity(), "OnSuccess--Not Object", Toast.LENGTH_LONG).show();


//        DataModel model = new Gson().fromJson(string.toString(), DataModel.class);
//        List<Attraction> attractions = model.getAttractions();
        mAdapter = new AttractionAdapter(getActivity(), attractionsList);
        recyclerView.setAdapter(mAdapter);
    }

    @Override
    public void failure(RetrofitError error) {
        error.printStackTrace();
        Toast.makeText(getActivity(), "OnFailure--" + error.getMessage(), Toast.LENGTH_LONG).show();
    }

    private class AttractionAdapter extends RecyclerView.Adapter<ViewHolder>
            implements ItemClickListener {

        public List<Attraction> mAttractionList;
        private Context mContext;

        public AttractionAdapter(Context context, List<Attraction> attractions) {
            super();
            mContext = context;
            mAttractionList = attractions;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            View view = inflater.inflate(R.layout.list_row, parent, false);
            return new ViewHolder(view, this);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Attraction attraction = mAttractionList.get(position);

            holder.mTitleTextView.setText(attraction.name);
            holder.mDescriptionTextView.setText(attraction.description);
            Glide.with(mContext)
                    .load(attraction.imageUrl)
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .placeholder(R.drawable.empty_photo)
                    .override(mImageSize, mImageSize)
                    .into(holder.mImageView);

            String distance =
                    Utils.formatDistanceBetween(mLatestLocation, attraction.location);
            if (TextUtils.isEmpty(distance)) {
                holder.mOverlayTextView.setVisibility(View.GONE);
            } else {
                holder.mOverlayTextView.setVisibility(View.VISIBLE);
                holder.mOverlayTextView.setText(distance);
            }
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getItemCount() {
            return mAttractionList == null ? 0 : mAttractionList.size();
        }

        @Override
        public void onItemClick(View view, int position) {
            if (!mItemClicked) {
                mItemClicked = true;
                View heroView = view.findViewById(android.R.id.icon);
                DetailActivity.launch(
                        getActivity(), mAdapter.mAttractionList.get(position).name, heroView);
            }
        }
    }

    private static class ViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {

        TextView mTitleTextView;
        TextView mDescriptionTextView;
        TextView mOverlayTextView;
        ImageView mImageView;
        ItemClickListener mItemClickListener;

        public ViewHolder(View view, ItemClickListener itemClickListener) {
            super(view);
            mTitleTextView = (TextView) view.findViewById(android.R.id.text1);
            mDescriptionTextView = (TextView) view.findViewById(android.R.id.text2);
            mOverlayTextView = (TextView) view.findViewById(R.id.overlaytext);
            mImageView = (ImageView) view.findViewById(android.R.id.icon);
            mItemClickListener = itemClickListener;
            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            mItemClickListener.onItemClick(v, getAdapterPosition());
        }
    }

    interface ItemClickListener {
        void onItemClick(View view, int position);
    }



}
