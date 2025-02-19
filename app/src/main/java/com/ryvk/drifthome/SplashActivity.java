package com.ryvk.drifthome;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;
import android.widget.VideoView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
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
                .load(R.drawable.splash_video) // Place GIF in res/drawable
                .into(gifImageView);

        if (InternetChecker.checkInternet(this)) {
            checkCurrentUser();
        } else {
            navigateToLogin();
        }
    }
    private void checkCurrentUser() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("drinker")
                    .document(user.getEmail())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Drinker drinker = documentSnapshot.toObject(Drinker.class);

                            Gson gson = new Gson();
                            String drinkerJSON = gson.toJson(drinker);

                            SharedPreferences sharedPreferences = getSharedPreferences("com.ryvk.drifthome.data",MODE_PRIVATE);
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putString("user",drinkerJSON);
                            editor.apply();

                            navigateToHome();
                        } else {
                            showErrorAndLogin();
                        }
                    })
                    .addOnFailureListener(e -> {
                        showErrorAndLogin();
                    });
        } else {
            navigateToLogin();
        }
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