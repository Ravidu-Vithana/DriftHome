package com.ryvk.drifthome;

import com.google.firebase.firestore.GeoPoint;

public class AddressCard {
    private final String addressText;
    private final GeoPoint geoPoint;

    public AddressCard(String addressText, GeoPoint geoPoint) {
        this.addressText = addressText;
        this.geoPoint = geoPoint;
    }

    public String getAddressText() {
        return addressText;
    }

    public GeoPoint getGeoPoint() {
        return geoPoint;
    }
}
