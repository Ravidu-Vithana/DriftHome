package com.ryvk.drifthome;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;

public class DrinkerConfig {
    private boolean shake_to_book;
    private boolean auto_close;
    private boolean voice_notifications;
    private boolean always_home;
    private int home_range;
    private String created_at;
    private String updated_at;

    public DrinkerConfig(){

    }

    public boolean isShake_to_book() {
        return shake_to_book;
    }

    public void setShake_to_book(boolean shake_to_book) {
        this.shake_to_book = shake_to_book;
    }

    public boolean isAuto_close() {
        return auto_close;
    }

    public void setAuto_close(boolean auto_close) {
        this.auto_close = auto_close;
    }

    public boolean isVoice_notifications() {
        return voice_notifications;
    }

    public void setVoice_notifications(boolean voice_notifications) {
        this.voice_notifications = voice_notifications;
    }

    public boolean isAlways_home() {
        return always_home;
    }

    public void setAlways_home(boolean always_home) {
        this.always_home = always_home;
    }

    public int getHome_range() {
        return home_range;
    }

    public void setHome_range(int home_range) {
        this.home_range = home_range;
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
    public static DrinkerConfig getSPDrinkerConfig(Context context){
        SharedPreferences sharedPreferences = context.getSharedPreferences("com.ryvk.drifthome.data", Context.MODE_PRIVATE);
        String drinkerConfigJSON = sharedPreferences.getString("userConfig",null);
        Gson gson = new Gson();
        return gson.fromJson(drinkerConfigJSON, DrinkerConfig.class);
    }
    public void updateSPDrinkerConfig (Context context, DrinkerConfig drinkerConfig){
        Gson gson = new Gson();
        String drinkerConfigJSON = gson.toJson(drinkerConfig);

        SharedPreferences sharedPreferences = context.getSharedPreferences("com.ryvk.drifthome.data",Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("userConfig", drinkerConfigJSON);
        editor.apply();
    }
}