package com.ryvk.drifthome;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.Manifest;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentContainerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class BookingActivity extends AppCompatActivity {
    private static final String TAG = "BookingActivity";
    float bookRideBtnAnimate;
    float bookRideBtnHeight;
    float bookRideTextY;
    float bookRideDriverCardAnimate;
    float bookRideAddressTextAnimate;
    private GeoPoint userLocation;
    private GeoPoint dropLocation;

    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private static final OkHttpClient client = new OkHttpClient();
    private Drinker loggedDrinker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_booking);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        loggedDrinker = Drinker.getSPDrinker(BookingActivity.this);

        // Request location if permissions are granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            readyTripLocations();
        }

    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                readyTripLocations();
            }
        }
    }
    private void readyTripLocations() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (ActivityCompat.checkSelfPermission(BookingActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(BookingActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }

                fusedLocationClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful() && task.getResult() != null) {
                            Location location = task.getResult();
                            userLocation = new GeoPoint(location.getLatitude(), location.getLongitude());
                            // Use the GeoPoint as needed
                            Log.i(TAG, "onComplete: User Location: " + userLocation.getLatitude() + ", " + userLocation.getLongitude());
                            dropLocation = getClosestGeoPoint(BookingActivity.this,loggedDrinker,userLocation,loggedDrinker.getAddresses());
                            startBooking();
                        } else {
                            Log.i(TAG, "Failed to get location");
                            AlertUtils.showAlert(BookingActivity.this,"Error","Location services failed!");
                        }
                    }
                });
            }
        }).start();
    }
    public static GeoPoint getClosestGeoPoint(Context context, Drinker drinker, GeoPoint target, List<GeoPoint> geoPoints) {
        if (geoPoints == null || geoPoints.isEmpty()) {
            return null; // No points to compare
        }

        String apiKey = drinker.getApiKey(context);
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("API Key is missing or invalid!");
        }

        GeoPoint closest = null;
        int minDistance = Integer.MAX_VALUE;

        for (GeoPoint point : geoPoints) {
            int distance = getRoadDistance(apiKey, target, point);
            if (distance != -1 && distance < minDistance) {
                minDistance = distance;
                closest = point;
            }
        }

        return closest;
    }

    private static int getRoadDistance(String apiKey, GeoPoint origin, GeoPoint destination) {
        try {
            String url = "https://maps.googleapis.com/maps/api/distancematrix/json?origins=" +
                    origin.getLatitude() + "," + origin.getLongitude() +
                    "&destinations=" + destination.getLatitude() + "," + destination.getLongitude() +
                    "&key=" + apiKey;

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();

            Response response = client.newCall(request).execute();

            if (response.isSuccessful() && response.body() != null) {
                String responseData = response.body().string();
                JSONObject jsonResponse = new JSONObject(responseData);
                JSONArray rows = jsonResponse.getJSONArray("rows");

                if (rows.length() > 0) {
                    JSONObject elements = rows.getJSONObject(0).getJSONArray("elements").getJSONObject(0);
                    if (!elements.getString("status").equals("OK")) {
                        return -1; // Distance not available
                    }
                    return elements.getJSONObject("distance").getInt("value"); // Distance in meters
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1; // Default error value
    }
    private void startBooking(){

        DrinkerConfig loggedDrinkerConfig = DrinkerConfig.getSPDrinkerConfig(BookingActivity.this);
        String apiKey = loggedDrinker.getApiKey(BookingActivity.this);
        if(loggedDrinkerConfig.isAlways_home()){

            int distance = getRoadDistance(apiKey,userLocation,loggedDrinker.getHomeAddress());
            int distanceThreshold = R.integer.distance_threshold_meters;

            if(distance <= distanceThreshold){
                dropLocation = loggedDrinker.getHomeAddress();
            }
        }
        Trip trip = new Trip(loggedDrinker.getEmail(),userLocation,dropLocation);
        HashMap<String, Object> tripMap = trip.getTripHashMap();

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("trip")
                .document(loggedDrinker.getEmail())
                .update(tripMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.i(TAG, "add trip details: success");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.i(TAG, "add trip details: failure");
                        AlertUtils.showAlert(getApplicationContext(),"Booking Failed!","Error: "+e);
                    }
                });
    }
    private void confirmBooking(){
        Button bookBtn = findViewById(R.id.button10);
        Button cancelBtn = findViewById(R.id.button11);
        TextView infoText = findViewById(R.id.textView14);
        ProgressBar progressBar = findViewById(R.id.progressBar4);
        FragmentContainerView driverCardView = findViewById(R.id.fragmentContainerView2);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressBar.setVisibility(View.GONE);
                infoText.setVisibility(View.VISIBLE);
                cancelBtn.setVisibility(View.GONE);
                bookBtn.setText("");
                bookBtn.setTextSize(60f);
            }
        });

        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int x = 3; x >= 0; x--){
                    int finalX = x;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            bookBtn.setText(String.valueOf(finalX));
                        }
                    });
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        infoText.setText(R.string.d_booking_text1_driverOnTheWay);

                        // Change button background color
                        int newColor = ContextCompat.getColor(getApplicationContext(), R.color.d_red1);
                        bookBtn.setBackgroundColor(newColor);
                        bookBtn.setText(R.string.d_booking_btn1_cancel);
                        bookBtn.setTextSize(20f);

                        // Get new size from dimens.xml
                        int newSize = (int) getResources().getDimension(R.dimen.d_circle_btn_small);
                        int currentWidth = bookBtn.getWidth();

                        // Animate width and height
                        ValueAnimator sizeAnimator = ValueAnimator.ofInt(currentWidth, newSize);
                        sizeAnimator.setDuration(300);
                        sizeAnimator.addUpdateListener(animation -> {
                            int animatedValue = (int) animation.getAnimatedValue();
                            ViewGroup.LayoutParams params = bookBtn.getLayoutParams();
                            params.width = animatedValue;
                            params.height = animatedValue;
                            bookBtn.setLayoutParams(params);
                        });
                        sizeAnimator.start();

                        bookRideBtnAnimate = getResources().getDimension(R.dimen.d_bookRideBtn_animate);
                        ObjectAnimator animator = ObjectAnimator.ofFloat(bookBtn, "y", bookRideBtnAnimate);
                        animator.setDuration(500);

                        bookRideBtnHeight = getResources().getDimension(R.dimen.d_circle_btn_small);
                        bookRideTextY = bookRideBtnAnimate + bookRideBtnHeight + 50f;
                        ObjectAnimator animator2 = ObjectAnimator.ofFloat(infoText, "y", bookRideTextY);
                        animator2.setDuration(500);
                        animator2.addListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        driverCardView.setVisibility(View.VISIBLE);
                                        bookBtn.setClickable(true);
                                    }
                                });
                                try {
                                    Thread.sleep(3000);
                                    startTrip();
                                } catch (InterruptedException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        });

                        bookRideDriverCardAnimate = getResources().getDimension(R.dimen.d_bookRideDriverCard_animate);
                        ObjectAnimator animator3 = ObjectAnimator.ofFloat(driverCardView, "y", bookRideTextY+bookRideDriverCardAnimate);
                        animator3.setDuration(100);

                        AnimatorSet animatorSet = new AnimatorSet();
                        animatorSet.playTogether(animator, animator2, animator3);
                        animatorSet.start();
                    }
                });
            }
        }).start();
    }
    private void startTrip(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Intent i = new Intent(BookingActivity.this, TripActivity.class);
                startActivity(i);
            }
        });
    }
}