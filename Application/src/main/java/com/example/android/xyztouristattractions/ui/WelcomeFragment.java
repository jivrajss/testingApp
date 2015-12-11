package com.example.android.xyztouristattractions.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.android.xyztouristattractions.DataModel;
import com.example.android.xyztouristattractions.R;
import com.example.android.xyztouristattractions.common.Attraction;
import com.example.android.xyztouristattractions.common.Utils;
import com.google.gson.Gson;

import java.util.List;

/**
 * Created by jivraj.singh on 10-12-2015.
 */
public class WelcomeFragment extends Fragment{

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.welcome_screen, container, false);
        List<Attraction> attractions = AttractionListFragment.loadAttractionsFromLocation(Utils.getLocation(getActivity()));
        DataModel model= new DataModel();
        model.setAttractions(attractions);
        String obj=new Gson().toJson(model);
        Log.d(WelcomeFragment.class.getSimpleName(),obj);
        TextView mTextView= (TextView) view.findViewById(R.id.WelcomeTextView);
        mTextView.setText(obj);
        return view;
    }
}
