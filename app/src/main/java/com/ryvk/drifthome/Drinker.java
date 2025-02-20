package com.ryvk.drifthome;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import com.google.firebase.firestore.GeoPoint;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class Drinker {

    @Expose
    public static final int ADDRESSLIMIT = 5;
    @Expose
    private String email;
    @Expose
    private String name;
    @Expose
    private String mobile;
    @Expose
    private String dob;
    @Expose
    private String gender;
    @Expose
    private int tokens;
    @Expose
    private int trip_count;
    @Expose
    private List<GeoPoint> addresses = new ArrayList<>();
    @Expose
    private String created_at;
    @Expose
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

    public List<GeoPoint> getAddresses() {
        return addresses;
    }

    public void setAddresses(List<GeoPoint> addresses) {
        this.addresses = addresses;
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
                addresses != null && addresses.size() == ADDRESSLIMIT && !addresses.contains(null);
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

    public boolean addAddress(GeoPoint address, Context context,AddressCallback callback) {
        if (address != null && addresses.size() < ADDRESSLIMIT) {
            addresses.add(address);
            getAddressCardList(context,callback);
            return true;
        } else {
            return false;
        }
    }

    public boolean removeAddress(int index, Context context,AddressCallback callback) {
        if (index < 0 || index >= addresses.size()) {
            return false;
        }else{
            addresses.remove(index);
            getAddressCardList(context,callback);
            return true;
        }
    }

    public GeoPoint getHomeAddress() {
        return addresses.isEmpty() ? null : addresses.get(0);
    }

    public interface AddressCallback {
        void onAddressesReady(List<AddressCard> addressCards);
        void onError(String error);
    }

    private String getApiKey(Context context) {
        try {
            ApplicationInfo ai = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            return ai.metaData.getString("com.google.android.geo.API_KEY");
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void getAddressCardList(Context context, AddressCallback callback) {
        new Thread(() -> {
            List<AddressCard> addressCards = new ArrayList<>();
            try {
                for (GeoPoint geoPoint : addresses) {
                    String address = getAddressFromGeoPoint(geoPoint, context);
                    if (address != null) {
                        addressCards.add(new AddressCard(address, geoPoint));
                    } else {
                        addressCards.add(new AddressCard("Unknown Address", geoPoint));
                    }
                }
                callback.onAddressesReady(addressCards);
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }

    private String getAddressFromGeoPoint(GeoPoint geoPoint, Context context) {

        OkHttpClient client = new OkHttpClient();
        try {
            String apiKey = getApiKey(context);
            if (apiKey == null) {
                Log.e("Geocode Error", "API Key is missing");
                return null;
            }

            String url = "https://maps.googleapis.com/maps/api/geocode/json?latlng=" +
                    geoPoint.getLatitude() + "," + geoPoint.getLongitude() + "&key=" + apiKey;

            Request request = new Request.Builder().url(url).build();
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    JSONObject jsonResponse = new JSONObject(responseBody);

                    if ("OK".equals(jsonResponse.getString("status"))) {
                        JSONArray results = jsonResponse.getJSONArray("results");
                        if (results.length() > 0) {
                            return results.getJSONObject(0).getString("formatted_address");
                        }
                    }
                }
            }
        } catch (IOException | org.json.JSONException e) {
            Log.e("Geocode Error", e.getMessage());
        }
        return null;
    }

    public static Drinker getSPDrinker(Context context){
        SharedPreferences sharedPreferences = context.getSharedPreferences("com.ryvk.drifthome.data", Context.MODE_PRIVATE);
        String drinkerJSON = sharedPreferences.getString("user",null);
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(GeoPoint.class, new GeoPointAdapter())
                .excludeFieldsWithoutExposeAnnotation()
                .create();

        return gson.fromJson(drinkerJSON, Drinker.class);
    }

    public void updateSPDrinker (Context context,Drinker drinker){
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(GeoPoint.class, new GeoPointAdapter())
                .create();
        String drinkerJSON = gson.toJson(drinker);

        SharedPreferences sharedPreferences = context.getSharedPreferences("com.ryvk.drifthome.data",Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("user",drinkerJSON);
        editor.apply();
    }

}
