package com.ryvk.drifthome;

import com.google.firebase.firestore.GeoPoint;

import java.util.HashMap;

public class Trip {
    public static final int TRIP_REQUESTED = 0;
    public static final int TRIP_ACCEPTED = 1;
    public static final int TRIP_STARTED = 2;
    public static final int TRIP_ENDED = 3;
    private String drinker_email;
    private String saviour_email;
    private GeoPoint pickup;
    private GeoPoint drop;
    private GeoPoint current_saviour_location;
    private int state;
    private String created_at;
    private String updated_at;

    public Trip(){

    }

    public Trip(String drinker_email, GeoPoint pickup, GeoPoint drop){
        this.drinker_email = drinker_email;
        this.pickup = pickup;
        this.drop = drop;
        this.state = TRIP_REQUESTED;
        this.created_at = Validation.todayDateTime();
        this.updated_at = Validation.todayDateTime();
    }

    public String getDrinker_email() {
        return drinker_email;
    }

    public void setDrinker_email(String drinker_email) {
        this.drinker_email = drinker_email;
    }

    public String getSaviour_email() {
        return saviour_email;
    }

    public void setSaviour_email(String saviour_email) {
        this.saviour_email = saviour_email;
    }

    public GeoPoint getPickup() {
        return pickup;
    }

    public void setPickup(GeoPoint pickup) {
        this.pickup = pickup;
    }

    public GeoPoint getDrop() {
        return drop;
    }

    public void setDrop(GeoPoint drop) {
        this.drop = drop;
    }

    public GeoPoint getCurrent_saviour_location() {
        return current_saviour_location;
    }

    public void setCurrent_saviour_location(GeoPoint current_saviour_location) {
        this.current_saviour_location = current_saviour_location;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public String getCreated_at() {
        return created_at;
    }

    public void setCreated_at(String created_at) {
        this.created_at = created_at;
    }

    public String getUpdated_at() {
        return updated_at;
    }

    public void setUpdated_at(String updated_at) {
        this.updated_at = updated_at;
    }

    public HashMap<String, Object> getTripHashMap(){

        HashMap<String, Object> trip = new HashMap<>();
        trip.put("drinker_email",this.drinker_email);
        trip.put("saviour_email",this.saviour_email);
        trip.put("pickup",this.pickup);
        trip.put("drop",this.drop);
        trip.put("current_saviour_location",this.current_saviour_location);
        trip.put("state",this.state);
        trip.put("created_at",this.created_at);
        trip.put("updated_at",this.updated_at);

        return trip;
    }
}
