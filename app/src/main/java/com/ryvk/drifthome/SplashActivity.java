package com.ryvk.drifthome;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ImageView gifImageView = findViewById(R.id.gifImageView);

        // Load GIF
        Glide.with(this)
                .asGif()
                .load(R.drawable.splash_video)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(gifImageView);

        if (InternetChecker.checkInternet(this)) {
            checkCurrentUser();
        } else {
            navigateToLogin();
        }
    }
    private void checkCurrentUser() {
        new Thread(() -> {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                db.collection("drinker")
                        .document(user.getEmail())
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                Drinker drinker = documentSnapshot.toObject(Drinker.class);

                                drinker.updateSPDrinker(SplashActivity.this, drinker);

                                db.collection("drinkerConfig")
                                        .document(user.getEmail())
                                        .get()
                                        .addOnSuccessListener(documentSnapshot2 -> {
                                            if (documentSnapshot2.exists()) {
                                                DrinkerConfig drinkerConfig = documentSnapshot2.toObject(DrinkerConfig.class);

                                                drinkerConfig.updateSPDrinkerConfig(SplashActivity.this, drinkerConfig);
                                                runOnUiThread(this::navigateToHome);
                                            } else {
                                                runOnUiThread(this::showErrorAndLogin);
                                            }
                                        })
                                        .addOnFailureListener(e -> runOnUiThread(this::showErrorAndLogin));
                            } else {
                                runOnUiThread(this::showErrorAndLogin);
                            }
                        })
                        .addOnFailureListener(e -> runOnUiThread(this::showErrorAndLogin));
            } else {
                runOnUiThread(this::navigateToLogin);
            }
        }).start();
    }

    private void navigateToHome() {
        startActivity(new Intent(SplashActivity.this, BaseActivity.class));
        finish();
    }

    private void navigateToLogin() {
        startActivity(new Intent(SplashActivity.this, MainActivity.class));
        finish();
    }

    private void showErrorAndLogin() {
//        AlertUtils.showAlert(getApplicationContext(), "Login Error", "Data retrieval failed! Please restart the application.");
        navigateToLogin();
    }

    public void deleteUser() {
        // [START delete_user]
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        user.delete()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "User account deleted.");
                        }
                    }
                });
        // [END delete_user]
    }
}