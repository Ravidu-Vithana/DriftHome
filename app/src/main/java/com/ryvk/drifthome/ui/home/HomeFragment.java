package com.ryvk.drifthome.ui.home;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.firestore.FirebaseFirestore;
import com.ryvk.drifthome.BookingActivity;
import com.ryvk.drifthome.Drinker;
import com.ryvk.drifthome.DrinkerConfig;
import com.ryvk.drifthome.R;
import com.ryvk.drifthome.databinding.FragmentHomeBinding;

public class HomeFragment extends Fragment {
    private static final String TAG = "HomeFragment";
    private FragmentHomeBinding binding;
    private Button mainButton;
    private TextView mainText;
    private Button tokensButton;
    private Drinker loggedDrinker;
    private boolean bookingAllowed;
    private SensorManager sensorManager;
    private SensorEventListener listener;
    private float lastX, lastY, lastZ;
    private long lastShakeTime;
    private static final int SHAKE_THRESHOLD = 15;
    private boolean isCooldown = false;
    private boolean shakeListenerRegistered = false;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        mainButton = binding.button9;
        mainText = binding.textView13;
        tokensButton = binding.button8;
        Button refreshButton = binding.button25;
        refreshButton.setOnClickListener(view -> reloadData());

        refreshData();

        return root;
    }

    private void onShakeDetected() {
        Log.d(TAG, "onShakeDetected: shake message received");
        if (bookingAllowed) {
            Intent i = new Intent(getContext(), BookingActivity.class);
            startActivity(i);
            ((Activity) getContext()).finish();
            isCooldown = true;
        }
    }

    private void reloadData() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("drinker")
                .document(loggedDrinker.getEmail())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Drinker drinker = documentSnapshot.toObject(Drinker.class);
                        drinker.updateSPDrinker(getContext(), drinker);

                        db.collection("drinkerConfig")
                                .document(loggedDrinker.getEmail())
                                .get()
                                .addOnSuccessListener(documentSnapshot2 -> {
                                    if (documentSnapshot2.exists()) {
                                        DrinkerConfig drinkerConfig = documentSnapshot2.toObject(DrinkerConfig.class);
                                        drinkerConfig.updateSPDrinkerConfig(getContext(), drinkerConfig);
                                        ((Activity) getContext()).runOnUiThread(() -> Toast.makeText(getContext(), "Data refresh successful!", Toast.LENGTH_SHORT).show());
                                        refreshData();
                                    } else {
                                        ((Activity) getContext()).runOnUiThread(() -> Toast.makeText(getContext(), "Data refresh failed!", Toast.LENGTH_LONG).show());
                                    }
                                })
                                .addOnFailureListener(e -> ((Activity) getContext()).runOnUiThread(() -> Toast.makeText(getContext(), "Data refresh failed!", Toast.LENGTH_LONG).show()));
                    } else {
                        ((Activity) getContext()).runOnUiThread(() -> Toast.makeText(getContext(), "Data refresh failed!", Toast.LENGTH_LONG).show());
                    }
                })
                .addOnFailureListener(e -> ((Activity) getContext()).runOnUiThread(() -> Toast.makeText(getContext(), "Data refresh failed!", Toast.LENGTH_LONG).show()));
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "onResume: Home Fragment");
        refreshData();

        DrinkerConfig loggedDrinkerConfig = DrinkerConfig.getSPDrinkerConfig(getContext());
        if (loggedDrinkerConfig.isShake_to_book()) {
            Log.d(TAG, "onResume: shake to book enabled");
            sensorManager = (SensorManager) ((Activity) getContext()).getSystemService(Context.SENSOR_SERVICE);
            Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if (sensor != null) {
                isCooldown = false;
                Log.d(TAG, "onResume: cooldown value: "+isCooldown);
                listener = new SensorEventListener() {
                    @Override
                    public void onSensorChanged(SensorEvent event) {
                        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER && !isCooldown) {
                            float x = event.values[0];
                            float y = event.values[1];
                            float z = event.values[2];

                            long currentTime = System.currentTimeMillis();
                            if ((currentTime - lastShakeTime) > 100) {
                                float deltaX = x - lastX;
                                float deltaY = y - lastY;
                                float deltaZ = z - lastZ;
                                float shake = Math.abs(deltaX + deltaY + deltaZ);

                                lastX = x;
                                lastY = y;
                                lastZ = z;

                                if (shake > SHAKE_THRESHOLD) {
                                    Log.d(TAG, "onSensorChanged: Shake detected!");
                                    sensorManager.unregisterListener(listener);
                                    listener = null;
                                    shakeListenerRegistered = false;
                                    lastShakeTime = currentTime;
                                    onShakeDetected();
                                }
                            }
                        }
                    }
                    @Override
                    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
                };
                if(!shakeListenerRegistered){
                    shakeListenerRegistered = true;
                    sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL);
                }
            }
        }else{
            if(shakeListenerRegistered){
                sensorManager.unregisterListener(listener);
                listener = null;
                shakeListenerRegistered = false;
            }
        }
    }

    private void refreshData() {
        new Thread(() -> {
            loggedDrinker = Drinker.getSPDrinker(getContext());
            ((Activity) getContext()).runOnUiThread(() -> tokensButton.setText(String.valueOf(loggedDrinker.getTokens())));

            int minToken = getActivity().getResources().getInteger(R.integer.minimum_token_amount);

            ((Activity) getContext()).runOnUiThread(() -> {
                if (!loggedDrinker.isProfileComplete()) {
                    bookingAllowed = false;
                    int newColor = ContextCompat.getColor(getContext(), R.color.d_red1);
                    mainButton.setText(R.string.d_homeFragment_btn1_completeProfile);
                    mainButton.setBackgroundColor(newColor);
                    mainText.setText(R.string.d_homeFragment_text1_completeProfile);
                    if (sensorManager != null) {
                        sensorManager.unregisterListener(listener);
                    }
                    listener = null;
                    shakeListenerRegistered = false;
                } else if (loggedDrinker.getTokens() <= minToken) {
                    bookingAllowed = false;
                    int newColor = ContextCompat.getColor(getContext(), R.color.d_orange);
                    mainButton.setText(R.string.d_homeFragment_btn1_insufficientCredit);
                    mainButton.setBackgroundColor(newColor);
                    mainText.setText(R.string.d_homeFragment_text1_insufficientCredit);
                    if (sensorManager != null) {
                        sensorManager.unregisterListener(listener);
                    }
                    shakeListenerRegistered = false;
                    listener = null;
                } else {
                    bookingAllowed = true;
                    mainButton.setOnLongClickListener(view -> {
                        Intent i = new Intent(getContext(), BookingActivity.class);
                        startActivity(i);
                        ((Activity) getContext()).finish();
                        return true;
                    });
                    mainButton.setEnabled(true);
                }
            });
        }).start();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.i(TAG, "onDestroyView: home fragment");
        if (sensorManager != null) {
            sensorManager.unregisterListener(listener);
            listener = null;
            shakeListenerRegistered = false;
        }
        binding = null;
    }
}