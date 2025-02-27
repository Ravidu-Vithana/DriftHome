package com.ryvk.drifthome;

import com.google.firebase.firestore.GeoPoint;

public class HistoryCard {
    private final String tripId;
    private final String dateText;
    private final String nameText;
    private final String vehicleText;

    public HistoryCard(String tripId, String dateText, String nameText, String vehicleText) {
        this.tripId = tripId;
        this.dateText = dateText;
        this.nameText = nameText;
        this.vehicleText = vehicleText;
    }

    public String getTripId() {
        return tripId;
    }

    public String getDateText() {
        return dateText;
    }

    public String getNameText() {
        return nameText;
    }

    public String getVehicleText() {
        return vehicleText;
    }
}
