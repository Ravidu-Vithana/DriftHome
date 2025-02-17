package com.ryvk.drifthome;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;

import okhttp3.OkHttpClient;

public class EmailPasswordAuthentication extends AppCompatActivity {

    private static final String TAG = "EmailPasswordAuthentication";
    private FirebaseAuth mAuth;

    private String name;
    private String email;
    private String mobile;
    private String password;
    int state;

    public static final int SIGNUPSTATE = 1;
    public static final int SIGNINSTATE = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_email_password_authentication);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if(InternetChecker.checkInternet(EmailPasswordAuthentication.this)){
            mAuth = FirebaseAuth.getInstance();
            Intent i = getIntent();

            email = i.getStringExtra("email");
            password = i.getStringExtra("password");
            state = i.getIntExtra("state",SIGNINSTATE);

            if(state == SIGNUPSTATE){
                name = i.getStringExtra("name");
                mobile = i.getStringExtra("mobile");

                createAccount(email,password);
            }else{
                signIn(email,password);
            }
        }
    }

    private void createAccount(String email, String password) {
        // [START create_user_with_email]
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "createUserWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            storeDetails(user);
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                            Toast.makeText(EmailPasswordAuthentication.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
        // [END create_user_with_email]
    }

    private void signIn(String email, String password) {
        // [START sign_in_with_email]
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            Intent intent = new Intent();
                            setResult(RESULT_OK, intent);
                            finish();
                        } else {
                            Exception exception = task.getException();
                            String errorMessage;

                            if (exception instanceof FirebaseAuthInvalidCredentialsException) {
                                errorMessage = "Invalid email or password. Please try again.";
                            } else if (exception instanceof FirebaseAuthInvalidUserException) {
                                errorMessage = "No account found with this email.";
                            } else {
                                errorMessage = "Authentication failed. Please try again later.";
                            }

                            Log.w(TAG, "signInWithEmail: failure", exception);

                            // Send error message back to MainActivity
                            Intent intent = new Intent();
                            intent.putExtra("AUTH_ERROR", errorMessage);
                            setResult(RESULT_CANCELED, intent);
                            finish();

                        }
                    }
                });
        // [END sign_in_with_email]
    }

    private void sendEmailVerification() {
        // Send verification email
        // [START send_email_verification]
        final FirebaseUser user = mAuth.getCurrentUser();
        if(user != null){
            user.sendEmailVerification()
                    .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            // Email sent
                        }
                    });
            // [END send_email_verification]
        }
    }

    private void storeDetails(FirebaseUser loggedUser) {
        if(loggedUser != null){
            loggedUser.getIdToken(true)
                    .addOnCompleteListener(new OnCompleteListener<GetTokenResult>() {
                        @Override
                        public void onComplete(@NonNull Task<GetTokenResult> task) {

                            if(task.isSuccessful()){
                                String firebaseToken = task.getResult().getToken();

                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        OkHttpClient okHttpClient = new OkHttpClient();

                                    }
                                }).start();

                            }else{
                                Toast.makeText(EmailPasswordAuthentication.this,"Failed to retrieve the Client Token",Toast.LENGTH_LONG).show();
                            }

                        }
                    });

        }
    }
}