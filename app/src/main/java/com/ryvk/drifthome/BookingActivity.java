package com.ryvk.drifthome;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentContainerView;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class BookingActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final String TAG = "BookingActivity";
    float bookRideBtnAnimate;
    float bookRideBtnHeight;
    float bookRideTextY;
    float bookRideDriverCardAnimate;
    float bookRideAddressTextAnimate;
    private String rideId;
    private GeoPoint userLocation;
    private GeoPoint saviourLocation;
    private GeoPoint dropLocation;
    private Trip tripData;
    private String saviour_email;
    private static Saviour bookedSaviour;
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private boolean isMapReady;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private static final OkHttpClient client = new OkHttpClient();
    private Thread standby;
    private Thread cancelButtonThread;
    private Drinker loggedDrinker;
    private OnBackPressedCallback callback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_booking);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainBookingActivity), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        Button cancelBtn = findViewById(R.id.button11);
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                endRideRequest();
            }
        });
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment);
        getSupportFragmentManager().beginTransaction().hide(mapFragment).commit();

        callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                requestRideCancellation();
            }
        };

        getOnBackPressedDispatcher().addCallback(this, callback);

        ProgressBar timerBar = findViewById(R.id.searchRideProgressBar);
        standby = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    for (int x = 0; x <= 40; x++) {
                        if (Thread.currentThread().isInterrupted()) {
                            return; // Stop the thread if interrupted
                        }
                        int progress = x;
                        runOnUiThread(() -> timerBar.setProgress(progress));
                        Thread.sleep(1000);
                    }
                    endRideRequest();
                } catch (InterruptedException e) {
                    Log.d(TAG, "Standby thread interrupted");
                }
            }
        });
        standby.start();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        loggedDrinker = Drinker.getSPDrinker(BookingActivity.this);
        Log.d(TAG, "onCreate: booking activity fcm token variable "+SplashActivity.fcmToken);

        LocalBroadcastManager.getInstance(this).registerReceiver(rideAcceptedReceiver,
                new IntentFilter("com.ryvk.drifthome.RIDE_ACCEPTED"));
        LocalBroadcastManager.getInstance(this).registerReceiver(rideCancelledReceiver,
                new IntentFilter("com.ryvk.drifthome.RIDE_CANCELLED"));

        checkLocationPermission();

    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
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

                if (ActivityCompat.checkSelfPermission(BookingActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
                    fusedLocationClient.getLastLocation()
                            .addOnSuccessListener(BookingActivity.this, new OnSuccessListener<Location>() {
                                @Override
                                public void onSuccess(Location location) {
                                    if (location != null) {
                                        userLocation = new GeoPoint(location.getLatitude(), location.getLongitude());
                                        // Use the GeoPoint as needed
                                        Log.i(TAG, "onComplete: User Location: " + userLocation.getLatitude() + ", " + userLocation.getLongitude());

                                        new Thread(new Runnable() {
                                            @Override
                                            public void run() {
                                                dropLocation = getClosestGeoPoint(BookingActivity.this,loggedDrinker,userLocation,loggedDrinker.getAddresses());
                                                startBooking();
                                            }
                                        }).start();
                                    }
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.i(TAG, "Failed to get location");
                                    AlertUtils.showAlert(BookingActivity.this,"Error","Location services failed!");
                                }
                            });
                }
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
                .add(tripMap)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.i(TAG, "add trip details: success, ID: " + documentReference.getId());

                        rideId = documentReference.getId();

                        JsonObject json = new JsonObject();
                        Gson gson = new GsonBuilder()
                                .registerTypeAdapter(GeoPoint.class, new GeoPointAdapter())
                                .create();
                        try {
                            json.addProperty("rideId", documentReference.getId());
                            json.addProperty("customerName", loggedDrinker.getName());
                            json.addProperty("fcmToken", SplashActivity.fcmToken);
                            JsonObject locationJson = gson.toJsonTree(userLocation).getAsJsonObject();
                            json.add("location", locationJson);
                        } catch (Exception e) {
                            e.printStackTrace();
                            return;
                        }

                        String BASE_URL = getResources().getString(R.string.base_url);

                        // Build request body
                        RequestBody requestBody = RequestBody.create(json.toString(), MediaType.get("application/json; charset=utf-8"));

                        // Create request
                        Request request = new Request.Builder()
                                .url(BASE_URL + "/send-ride-request")
                                .post(requestBody)
                                .build();

                        // Execute request asynchronously
                        client.newCall(request).enqueue(new Callback() {
                            @Override
                            public void onFailure(Call call, IOException e) {
                                e.printStackTrace();
                                System.out.println("Request Failed: " + e.getMessage());
                            }

                            @Override
                            public void onResponse(Call call, Response response) throws IOException {
                                if (response.isSuccessful()) {
                                    System.out.println("Notification Sent: " + response.body().string());
                                } else {
                                    System.out.println("Error: " + response.code());
                                }
                            }
                        });

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.i(TAG, "add trip details: failure", e);
                    }
                });
    }
    private void confirmBooking(){

        if (standby != null && standby.isAlive()) {
            standby.interrupt();
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("saviour")
                .document(saviour_email)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot.exists()) {
                            BookingActivity.bookedSaviour = documentSnapshot.toObject(Saviour.class);

                            updateUI();

                        } else {
                            runOnUiThread(() -> AlertUtils.showAlert(getApplicationContext(),"Login Error","Data retrieval failed! Please restart the application."));
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.i(TAG, "Error fetching saviour: " + e);
                        runOnUiThread(() -> AlertUtils.showAlert(getApplicationContext(),"Login Error","Data retrieval failed! Please restart the application."));
                    }
                });
    }
    private void updateUI(){
        Button bookBtn = findViewById(R.id.button10);
        Button cancelBtn = findViewById(R.id.button11);
        TextView infoText = findViewById(R.id.textView14);
        TextView saviourNameText = findViewById(R.id.textView17);
        TextView saviourVehicleText = findViewById(R.id.textView18);
        ProgressBar progressBar = findViewById(R.id.progressBar4);
        ProgressBar timerBar = findViewById(R.id.searchRideProgressBar);
        FragmentContainerView driverCardView = findViewById(R.id.fragmentContainerView2);
        SupportMapFragment mapView = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment);

        bookBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //request to cancel the ride
                requestRideCancellation();
            }
        });

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressBar.setVisibility(View.GONE);
                timerBar.setVisibility(View.GONE);
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
                                        saviourNameText.setText(bookedSaviour.getName());
                                        saviourVehicleText.setText(bookedSaviour.getVehicle());
                                        driverCardView.setVisibility(View.VISIBLE);
                                        bookBtn.setClickable(true);
                                    }
                                });
                            }
                        });

                        bookRideDriverCardAnimate = getResources().getDimension(R.dimen.d_bookRideDriverCard_animate);
                        ObjectAnimator animator3 = ObjectAnimator.ofFloat(driverCardView, "y", bookRideTextY+bookRideDriverCardAnimate);
                        animator3.setDuration(100);
                        animator3.addListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {

                                        getSupportFragmentManager().beginTransaction().show(mapView).commit();
                                        loadTripData();
                                    }
                                });
                            }
                        });

                        AnimatorSet animatorSet = new AnimatorSet();
                        animatorSet.playTogether(animator, animator2, animator3);
                        animatorSet.start();
                    }
                });
            }
        }).start();
    }

    private void requestRideCancellation(){
        Button bookBtn = findViewById(R.id.button10);
        runOnUiThread(()->{
            bookBtn.setText(R.string.d_booking_btn1_requesting);
            bookBtn.setEnabled(false);
        });

        cancelButtonThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(5000);
                }catch (Exception e){
                    Log.e(TAG, "run: cancel button thread",e);
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        bookBtn.setText(R.string.d_booking_btn1_cancel);
                        bookBtn.setEnabled(true);
                    }
                });
            }
        });
        cancelButtonThread.start();

        JsonObject json = new JsonObject();
        try {
            json.addProperty("rideId", rideId);
            json.addProperty("fcmToken", bookedSaviour.getFcmToken());
            Log.d(TAG, "onClick: cancel booking request -> fcmToken: "+bookedSaviour.getFcmToken());
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        String BASE_URL = getResources().getString(R.string.base_url);
        RequestBody requestBody = RequestBody.create(json.toString(), MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(BASE_URL + "/request-ride-cancel")
                .post(requestBody)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                System.out.println("Request Failed: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    System.out.println("Notification Sent: " + response.body().string());
                } else {
                    System.out.println("Error: " + response.code());
                }
            }
        });
    }

    private void loadTripData(){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("trip").document(rideId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        tripData = documentSnapshot.toObject(Trip.class);
                        loadMap();
                    } else {
                        Log.d("Firestore", "No such document exists");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error fetching document", e);
                });
    }

    private void loadMap(){
        isMapReady = true;
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        if(isMapReady){
            mMap = googleMap;

            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mMap.setMyLocationEnabled(true);

                fusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            userLocation = new GeoPoint(location.getLatitude(), location.getLongitude());
                            LatLng userLatLng = new LatLng(userLocation.getLatitude(), userLocation.getLongitude());
                            LatLng pickupLatLng = new LatLng(tripData.getCurrent_saviour_location().getLatitude(), tripData.getCurrent_saviour_location().getLongitude());

                            // Add marker for the user's location
                            mMap.addMarker(new MarkerOptions().position(userLatLng).title("Your Location"));

                            // Add marker for the pickup location
                            mMap.addMarker(new MarkerOptions().position(pickupLatLng).title("Pickup Location"));
                            getRoute();

                            // Move camera to show both locations
                            LatLngBounds.Builder builder = new LatLngBounds.Builder();
                            builder.include(userLatLng);
                            builder.include(pickupLatLng);
                            LatLngBounds bounds = builder.build();
                            int padding = 50; // padding around the map
                            mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));

                        }else{
                            Toast.makeText(BookingActivity.this,"Location is null",Toast.LENGTH_LONG).show();
                        }
                    }
                });

            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            }
        }
    }

    private void getRoute() {
        String origin = userLocation.getLatitude() + "," + userLocation.getLongitude();
        String destination = tripData.getCurrent_saviour_location().getLatitude() + "," + tripData.getCurrent_saviour_location().getLongitude();

        String apiKey = loggedDrinker.getApiKey(BookingActivity.this);
        String directionsApiUrl = "https://maps.googleapis.com/maps/api/directions/json?origin=" + origin + "&destination=" + destination + "&key=" + apiKey;

        Request directionsApiRequest = new Request.Builder().url(directionsApiUrl).build();

        client.newCall(directionsApiRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to get directions", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    try {
                        // Parse the Directions API response
                        JsonObject jsonResponse = new Gson().fromJson(responseBody, JsonObject.class);
                        JsonArray routes = jsonResponse.getAsJsonArray("routes");
                        if (routes.size() > 0) {
                            JsonObject route = routes.get(0).getAsJsonObject();
                            JsonArray legs = route.getAsJsonArray("legs");
                            if (legs.size() > 0) {
                                JsonObject leg = legs.get(0).getAsJsonObject();
                                JsonArray steps = leg.getAsJsonArray("steps");

                                List<LatLng> polylinePoints = new ArrayList<>();
                                for (JsonElement step : steps) {
                                    JsonObject stepObject = step.getAsJsonObject();
                                    String polyline = stepObject.getAsJsonObject("polyline").get("points").getAsString();
                                    polylinePoints.addAll(Utils.decodePolyline(polyline));
                                }

                                // Add the route polyline to the map
                                drawRouteOnMap(polylinePoints);

                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing directions response", e);
                    }
                }
            }
        });
    }

    private void drawRouteOnMap(List<LatLng> polylinePoints) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mMap != null && !polylinePoints.isEmpty()) {
                    Polyline polyline = mMap.addPolyline(new PolylineOptions().addAll(polylinePoints).color(getResources().getColor(R.color.d_blue))); // Replace with your desired color
                    polyline.setWidth(10); // You can change the width of the polyline
                }
            }
        });
    }

    private BroadcastReceiver rideAcceptedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.ryvk.drifthome.RIDE_ACCEPTED".equals(intent.getAction())) {
                Toast.makeText(BookingActivity.this,"Ride Accepted!",Toast.LENGTH_LONG).show();
                String rideIntentData = intent.getStringExtra("rideData");

                Gson gson = new GsonBuilder()
                        .registerTypeAdapter(GeoPoint.class, new GeoPointAdapter())
                        .create();
                JsonObject rideData = gson.fromJson(rideIntentData, JsonObject.class);

                saviour_email = rideData.get("saviour_email").getAsString();

                // Call the confirmBooking method
                confirmBooking();
            }
        }
    };

    private BroadcastReceiver rideCancelledReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.ryvk.drifthome.RIDE_CANCELLED".equals(intent.getAction())) {
                Log.d(TAG, "onReceive: Ride cancelled");
                endRideRequest();
            }
        }
    };
    private void startTrip(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Intent i = new Intent(BookingActivity.this, TripActivity.class);
                startActivity(i);
            }
        });
    }

    private void endRideRequest(){

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("trip").document(rideId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Document successfully deleted!");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting document", e);
                });

        finish();
    }

    @Override
    public void finish() {
        if (standby != null && standby.isAlive()) {
            standby.interrupt();
        }
        if (cancelButtonThread != null && cancelButtonThread.isAlive()) {
            cancelButtonThread.interrupt();
        }
        super.finish();
    }

    @Override
    protected void onDestroy() {
        if (cancelButtonThread != null && cancelButtonThread.isAlive()) {
            cancelButtonThread.interrupt();
        }
        LocalBroadcastManager.getInstance(this).unregisterReceiver(rideAcceptedReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(rideCancelledReceiver);
        super.onDestroy();
    }
}