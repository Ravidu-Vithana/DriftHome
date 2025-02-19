package com.ryvk.drifthome;

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
        return email != null && name != null && mobile != null && dob != null && gender != null;
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
