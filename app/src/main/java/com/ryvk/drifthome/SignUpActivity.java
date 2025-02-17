package com.ryvk.drifthome;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseUser;

public class SignUpActivity extends AppCompatActivity {

    private static final String TAG = "SignUpActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_up);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ImageButton googleSigninButton = findViewById(R.id.imageButton3);
        googleSigninButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(SignUpActivity.this,GoogleAuthentication.class);
                startActivity(i);
            }
        });

        Button signInButton = findViewById(R.id.button4);
        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                EditText nameEditText = findViewById(R.id.editTextText3);
                EditText emailEditText = findViewById(R.id.editTextText4);
                EditText mobileEditText = findViewById(R.id.editTextText5);
                EditText passwordEditText = findViewById(R.id.editTextText13);

                String name = nameEditText.getText().toString().trim();
                String email = emailEditText.getText().toString().trim();
                String mobile = mobileEditText.getText().toString().trim();
                String password = passwordEditText.getText().toString().trim();

                Log.i(TAG, "name: "+name);
                Log.i(TAG, "email: "+email);
                Log.i(TAG, "mobile: "+mobile);
                Log.i(TAG, "password: "+password);

                Intent i = new Intent(SignUpActivity.this, EmailPasswordAuthentication.class);
                i.putExtra("name",name);
                i.putExtra("email",email);
                i.putExtra("mobile",mobile);
                i.putExtra("password",password);
                i.putExtra("state",EmailPasswordAuthentication.SIGNUPSTATE);
                startActivity(i);
            }
        });

        Button goToSignInButton = findViewById(R.id.button6);
        goToSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(SignUpActivity.this,MainActivity.class);
                startActivity(i);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(InternetChecker.checkInternet(SignUpActivity.this)){
            checkCurrentUser();
        }
    }

    public void checkCurrentUser() {
        // [START check_current_user]
        FirebaseUser user = MainActivity.getFirebaseUser();
        if (user != null) {
            //already signed in, ready to intent Home
            Intent i = new Intent(SignUpActivity.this, BaseActivity.class);
            startActivity(i);
        }
    }
}