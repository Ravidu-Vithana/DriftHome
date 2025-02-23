package com.ryvk.drifthome;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class DriftMessagingService extends FirebaseMessagingService {
    private static final String TAG = "DriftMessagingService";
    private FusedLocationProviderClient fusedLocationClient;
    private RemoteMessage remoteMessage;
    private String API_KEY;

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        this.remoteMessage = remoteMessage;

        if (remoteMessage.getNotification() != null) {
            String title = remoteMessage.getNotification().getTitle();
            if(title.equals("rideAccepted")){
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        rideRequestAccepted();
                    }
                }).start();
            }else if(title.equals("rideCancelled")){
                rideCancelled();
            }
        }
    }

    private void rideRequestAccepted(){
        String body = remoteMessage.getNotification().getBody();
        Log.d(TAG, "rideRequestAccepted: ride is accepted -> "+body);

        Intent intent = new Intent("com.ryvk.drifthome.RIDE_ACCEPTED");
        intent.putExtra("rideData", body);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

    }

    private void rideCancelled(){
        String body = remoteMessage.getNotification().getBody();
        Log.d(TAG, "rideCancelled: ride is cancelled -> "+body);

        Intent intent = new Intent("com.ryvk.drifthome.RIDE_CANCELLED");
        intent.putExtra("rideData", body);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void showNotification(String title, String body) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "ride_requests_channel";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId,
                    "Ride Requests", NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        Notification notification = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build();

        notificationManager.notify(0, notification);
    }

    @Override
    public void onNewToken(String token) {
        Log.d(TAG, "New token generated: " + token);
        Drinker drinker = Drinker.getSPDrinker(this);
        FirebaseFirestore.getInstance().collection("drinker").document(drinker.getEmail())
                .update("fcmToken", token);
        SplashActivity.fcmToken = token;
    }

}
