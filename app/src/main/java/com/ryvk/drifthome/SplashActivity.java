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
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.gson.Gson;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";

    public static String fcmToken;

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
                                if(isBlocked(drinker)){
                                    GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                            .requestIdToken(getString(R.string.web_server_client_id))
                                            .requestEmail()
                                            .build();

                                    GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(SplashActivity.this, gso);
                                    FirebaseAuth mAuth = FirebaseAuth.getInstance();
                                    mAuth.signOut();
                                    mGoogleSignInClient.signOut();
                                    Log.i(TAG, "onClick: Logout, user logged out------------------------");

                                    drinker.removeSPDrinker(SplashActivity.this);
                                    runOnUiThread(this::navigateToLogin);
                                }else{
                                    drinker.updateSPDrinker(SplashActivity.this, drinker);

                                    FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
                                        if (task.isSuccessful()) {
                                            String token = task.getResult();
                                            FirebaseFirestore.getInstance().collection("drinker").document(user.getEmail())
                                                    .update("fcmToken", token);
                                            fcmToken = token;
                                            Log.d(TAG, "checkCurrentUser: fcm token "+ fcmToken );
                                        }
                                    });

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
                                }
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

    public static boolean isBlocked(Drinker drinker){
        return drinker != null && drinker.isBlocked();
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