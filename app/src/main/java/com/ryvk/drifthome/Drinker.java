package com.ryvk.drifthome;

import com.google.firebase.firestore.GeoPoint;

import java.util.Date;
import java.util.HashMap;

public class Drinker {
    private String email;
    private String name;
    private String mobile;
    private String dob;
    private String gender;
    private int tokens;
    private int trip_count;
    private GeoPoint home_address;
    private GeoPoint address1;
    private GeoPoint address2;
    private GeoPoint address3;
    private GeoPoint address4;
    private String created_at;
    private String updated_at;

    public Drinker(){

    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getDob() {
        return dob;
    }

    public void setDob(String dob) {
        this.dob = dob;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public int getTokens() {
        return tokens;
    }

    public void setTokens(int tokens) {
        this.tokens = tokens;
    }

    public int getTrip_count() {
        return trip_count;
    }

    public void setTrip_count(int trip_count) {
        this.trip_count = trip_count;
    }

    public GeoPoint getHome_address() {
        return home_address;
    }

    public void setHome_address(GeoPoint home_address) {
        this.home_address = home_address;
    }

    public GeoPoint getAddress1() {
        return address1;
    }

    public void setAddress1(GeoPoint address1) {
        this.address1 = address1;
    }

    public GeoPoint getAddress2() {
        return address2;
    }

    public void setAddress2(GeoPoint address2) {
        this.address2 = address2;
    }

    public GeoPoint getAddress3() {
        return address3;
    }

    public void setAddress3(GeoPoint address3) {
        this.address3 = address3;
    }

    public GeoPoint getAddress4() {
        return address4;
    }

    public void setAddress4(GeoPoint address4) {
        this.address4 = address4;
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

    public boolean isProfileComplete() {
        return email != null && name != null && mobile != null && dob != null && gender != null &&
                home_address != null && address1 != null && address2 != null && address3 != null && address4 != null;
    }

    public HashMap<String, Object> updateFields(String email, String name, String mobile, String gender, String dob) {
        if (email != null) {
            this.email = email;
        }
        if (name != null) {
            this.name = name;
        }
        if (mobile != null) {
            this.mobile = mobile;
        }
        if (gender != null) {
            this.gender = gender;
        }
        if (dob != null) {
            this.dob = dob;
        }

        HashMap<String, Object> drinker = new HashMap<>();
        drinker.put("name", this.getName());
        drinker.put("email", this.getEmail());
        drinker.put("mobile", this.getMobile());
        drinker.put("gender", this.getGender());
        drinker.put("dob", this.getDob());
        drinker.put("updated_at", Validation.todayDateTime());

        return drinker;

    }

}
