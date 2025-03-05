package com.ryvk.drifthome;

import com.google.firebase.firestore.GeoPoint;

public class HistoryCard {
    private final String tripId;
    private final String dateText;
    private final String profilePictureUrl;
    private final String nameText;
    private final String vehicleText;
    private final boolean feedbackGiven;

    public HistoryCard(String tripId, String dateText, String profilePictureUrl, String nameText, String vehicleText, boolean feedbackGiven) {
        this.tripId = tripId;
        this.dateText = dateText;
        this.profilePictureUrl = profilePictureUrl;
        this.nameText = nameText;
        this.vehicleText = vehicleText;
        this.feedbackGiven = feedbackGiven;
    }

    public String getTripId() {
        return tripId;
    }

    public String getDateText() {
        return dateText;
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public String getNameText() {
        return nameText;
    }

    public String getVehicleText() {
        return vehicleText;
    }

    public boolean isFeedbackGiven() {
        return feedbackGiven;
    }
}
