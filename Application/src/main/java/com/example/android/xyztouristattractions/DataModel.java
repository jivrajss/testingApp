package com.example.android.xyztouristattractions;

import com.example.android.xyztouristattractions.common.Attraction;

import java.io.Serializable;
import java.util.List;

/**
 * Created by jivraj.singh on 10-12-2015.
 */
public class DataModel implements Serializable {

    public List<Attraction> getAttractions() {
        return attractions;
    }

    public void setAttractions(List<Attraction> attractions) {
        this.attractions = attractions;
    }


    private List<Attraction> attractions;


}
