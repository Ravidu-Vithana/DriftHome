package com.ryvk.drifthome;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
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
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class TripActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final String TAG = "TripActivity";
    private Drinker loggedDrinker;
    private DrinkerConfig loggedDrinkerConfig;
    private String rideId;
    private GeoPoint userLocation;
    private String dropLocation;
    private int totalFare;
    private static final OkHttpClient client = new OkHttpClient();
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private OnBackPressedCallback callback;
    private Thread autoCloseThread;
    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_trip);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        autoCloseThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(5000);
                }catch (Exception e){
                    Log.e(TAG, "run: auto closing button",e);
                }
                Button tripStateButton = findViewById(R.id.button12);
                int newColor = ContextCompat.getColor(getApplicationContext(), R.color.d_lightBlue);
                runOnUiThread(()->tripStateButton.setBackgroundColor(newColor));

                for(int x = 5; x >= 0; x--){
                    int seconds = x;
                    runOnUiThread(()->tripStateButton.setText("Auto closing in "+seconds+"..."));
                    try {
                        Thread.sleep(1000);
                    }catch (Exception e){
                        Log.e(TAG, "run: auto closing button",e);
                    }
                }
                finishAffinity();
                System.exit(0);
            }
        });

        Intent intent = getIntent();

        if (intent != null) {
            rideId = intent.getStringExtra("rideId");
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment2);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Toast.makeText(TripActivity.this,"Not Allowed!",Toast.LENGTH_SHORT).show();
            }
        };

        Button backToHome = findViewById(R.id.button13);
        backToHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        getOnBackPressedDispatcher().addCallback(this, callback);

        loggedDrinker = Drinker.getSPDrinker(TripActivity.this);
        loggedDrinkerConfig = DrinkerConfig.getSPDrinkerConfig(TripActivity.this);
        loadData();

        checkLocationPermission();

        if(loggedDrinkerConfig.isVoice_notifications()){
            mediaPlayer = MediaPlayer.create(this, R.raw.trip_started);
            mediaPlayer.start();
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(endTripReceiver,
                new IntentFilter("com.ryvk.drifthome.END_TRIP"));
    }

    private void loadData(){

        String apiKey = loggedDrinker.getApiKey(TripActivity.this);

        String geocodingApiUrl = "https://maps.googleapis.com/maps/api/geocode/json?latlng=" + BookingActivity.tripData.getDrop().getLatitude() + "," + BookingActivity.tripData.getDrop().getLongitude() + "&key=" + apiKey;

        Request geocodeApiRequest = new Request.Builder().url(geocodingApiUrl).build();
        client.newCall(geocodeApiRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to get geocode data", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    try {
                        // Parse the Geocoding API response
                        Gson gson = new Gson();
                        JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
                        JsonArray results = jsonResponse.getAsJsonArray("results");

                        if (results != null && results.size() > 0) {
                            JsonObject firstResult = results.get(0).getAsJsonObject();
                            dropLocation = firstResult.get("formatted_address").getAsString();
                            Log.d(TAG, "Address: " + dropLocation);
                            updateUI(true);
                        } else {
                            Log.d(TAG, "No address found");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing geocode response", e);
                    }
                }
            }
        });
    }
    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
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
                if (ActivityCompat.checkSelfPermission(TripActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(TripActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }

                if (ActivityCompat.checkSelfPermission(TripActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
                    fusedLocationClient.getLastLocation()
                            .addOnSuccessListener(TripActivity.this, new OnSuccessListener<Location>() {
                                @Override
                                public void onSuccess(Location location) {
                                    if (location != null) {
                                        userLocation = new GeoPoint(location.getLatitude(), location.getLongitude());
                                        // Use the GeoPoint as needed
                                        Log.i(TAG, "onComplete: User Location: " + userLocation.getLatitude() + ", " + userLocation.getLongitude());
                                    }else{
                                        AlertUtils.showAlert(TripActivity.this,"No Location Detected","Location services are off. Please turn them on and try again.");
                                    }
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.i(TAG, "Failed to get location");
                                    AlertUtils.showAlert(TripActivity.this,"Error","Location services failed!");
                                }
                            });
                }
            }
        }).start();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);

            fusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        userLocation = new GeoPoint(location.getLatitude(), location.getLongitude());
                        LatLng pickupLatLng = new LatLng(BookingActivity.tripData.getPickup().getLatitude(), BookingActivity.tripData.getPickup().getLongitude());
                        LatLng dropLatLng = new LatLng(BookingActivity.tripData.getDrop().getLatitude(), BookingActivity.tripData.getDrop().getLongitude());

                        // Add marker for the user's location
                        mMap.addMarker(new MarkerOptions().position(pickupLatLng).title("Your Location"));

                        // Add marker for the pickup location
                        mMap.addMarker(new MarkerOptions().position(dropLatLng).title("Pickup Location"));
                        getRoute();

                        // Move camera to show both locations
                        LatLngBounds.Builder builder = new LatLngBounds.Builder();
                        builder.include(pickupLatLng);
                        builder.include(dropLatLng);
                        LatLngBounds bounds = builder.build();
                        int padding = 50; // padding around the map
                        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));

                    }else{
                        Toast.makeText(TripActivity.this,"Location is null",Toast.LENGTH_LONG).show();
                    }
                }
            });

        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void getRoute() {
        String origin = BookingActivity.tripData.getPickup().getLatitude() + "," + BookingActivity.tripData.getPickup().getLongitude();
        String destination = BookingActivity.tripData.getDrop().getLatitude() + "," + BookingActivity.tripData.getDrop().getLongitude();

        String apiKey = loggedDrinker.getApiKey(TripActivity.this);
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
                    polyline.setWidth(10);
                }
            }
        });
    }
    private void updateUI(boolean isStart){
        if(isStart){
            TextView locationText = findViewById(R.id.textView20);
            TextView saviourNameText = findViewById(R.id.textView17);
            TextView saviourVehicleText = findViewById(R.id.textView18);

            runOnUiThread(()->{
                locationText.setText(dropLocation);
                saviourNameText.setText(BookingActivity.bookedSaviour.getName());
                saviourVehicleText.setText(BookingActivity.bookedSaviour.getVehicle());
            });
            Log.d(TAG, "updateUI: value of saviour vehicle :"+BookingActivity.bookedSaviour.getVehicle());
        }else{
            if(loggedDrinkerConfig.isVoice_notifications()){
                mediaPlayer = MediaPlayer.create(this, R.raw.trip_ended);
                mediaPlayer.start();
            }

            TextView soulSavedText = findViewById(R.id.textView19);
            Button tripStateButton = findViewById(R.id.button12);
            TextView destinationText = findViewById(R.id.textView21);
            TextView totalFareText = findViewById(R.id.textView23);
            TextView balanceText = findViewById(R.id.textView25);
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment2);
            ConstraintLayout tripFareCardContainer = findViewById(R.id.tripFareCardContainer);

            runOnUiThread(()->{
                getSupportFragmentManager().beginTransaction().hide(mapFragment).commit();
                soulSavedText.setVisibility(View.VISIBLE);
                tripStateButton.setText(R.string.d_trip_btn1_tripEnded);
                destinationText.setText(R.string.d_trip_text1_arrivedAt);
                tripFareCardContainer.setVisibility(View.VISIBLE);
                totalFareText.setText(String.valueOf(totalFare));
                balanceText.setText(String.valueOf(loggedDrinker.getTokens()));
            });

            DrinkerConfig drinkerConfig = DrinkerConfig.getSPDrinkerConfig(TripActivity.this);
            if(drinkerConfig.isAuto_close()){
                autoCloseThread.start();
            }
        }
    }
    private void endTrip(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                String api_key = loggedDrinker.getApiKey(TripActivity.this);

                int tripDistance = Utils.getRoadDistance(api_key,BookingActivity.tripData.getPickup(),BookingActivity.tripData.getDrop());

                if(tripDistance != -1){
                    tripDistance = tripDistance / 1000;  //convert to KM

                    int feePerKM = getResources().getInteger(R.integer.fee_per_km);
                    totalFare = tripDistance * feePerKM;

                    int currentTokens = loggedDrinker.getTokens();
                    int remainingTokens = currentTokens - totalFare;
                    loggedDrinker.setTokens(remainingTokens);
                    loggedDrinker.updateSPDrinker(TripActivity.this,loggedDrinker);

                    HashMap<String,Object> drinker = new HashMap<>();
                    drinker.put("tokens",loggedDrinker.getTokens());

                    FirebaseFirestore db = FirebaseFirestore.getInstance();

                    db.collection("drinker")
                            .document(loggedDrinker.getEmail())
                            .update(drinker)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void unused) {
                                    Log.i(TAG, "update details: success");
                                    updateUI(false);
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.i(TAG, "update details: failure");
                                    runOnUiThread(()->AlertUtils.showAlert(TripActivity.this,"Tokens Update Failed!","Error: "+e));
                                }
                            });
                }else{
                    AlertUtils.showAlert(TripActivity.this,"Error","Error calculating the distance");
                }
            }
        }).start();
    }
    private BroadcastReceiver endTripReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.ryvk.drifthome.END_TRIP".equals(intent.getAction())) {
                Log.d(TAG, "onReceive: end trip");

                BookingActivity.tripData.setState(Trip.TRIP_ENDED);

                FirebaseFirestore db = FirebaseFirestore.getInstance();

                db.collection("trip")
                        .document(rideId)
                        .update("state",Trip.TRIP_ENDED)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unused) {
                                Log.i(TAG, "update trip state: success");
                                endTrip();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.i(TAG, "update trip state: failure");
                                AlertUtils.showAlert(TripActivity.this,"Trip Update Failed!","Error: "+e);
                            }
                        });
            }
        }
    };

    @Override
    public void finish() {
        if (autoCloseThread != null && autoCloseThread.isAlive()) {
            autoCloseThread.interrupt();
        }
        if(mediaPlayer != null && mediaPlayer.isPlaying()){
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    TripActivity.super.finish();
                }
            });
        }else{
            super.finish();
        }
    }

    @Override
    protected void onDestroy() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        LocalBroadcastManager.getInstance(this).unregisterReceiver(endTripReceiver);
        super.onDestroy();
    }
}