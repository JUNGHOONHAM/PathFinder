package com.junctionx.pathfinder.model;

import com.google.gson.annotations.SerializedName;

public class Mobilities {
    @SerializedName("response")
    Object response;

    int id;
    String ride_type;
    String uid;
    float impulse;
    Double lat;
    Double lng;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getRide_type() {
        return ride_type;
    }

    public void setRide_type(String ride_type) {
        this.ride_type = ride_type;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public float getImpulse() {
        return impulse;
    }

    public void setImpulse(float impulse) {
        this.impulse = impulse;
    }

    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public Double getLng() {
        return lng;
    }

    public void setLng(Double lng) {
        this.lng = lng;
    }
}
