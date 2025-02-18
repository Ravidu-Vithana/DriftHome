package com.ryvk.drifthome;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.VideoView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class SplashActivity extends AppCompatActivity {

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

        VideoView videoView = findViewById(R.id.videoView);
        String videoPath = "android.resource://" + getPackageName() + "/" + R.raw.splash_video;
        Uri uri = Uri.parse(videoPath);
        videoView.setVideoURI(uri);
        videoView.start();

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
                            MainActivity.loggedDrinker = drinker;
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
        AlertUtils.showAlert(this, "Login Error", "Data retrieval failed! Please restart the application.");
        navigateToLogin();
    }
}