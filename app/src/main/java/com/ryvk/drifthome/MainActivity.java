package com.ryvk.drifthome;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int RC_EPSIGNIN = 1001;

    public static Drinker loggedDrinker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button signInButton = findViewById(R.id.button);
        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText emailEditText = findViewById(R.id.editTextText2);
                EditText passwordEditText = findViewById(R.id.editTextText14);

                String email = emailEditText.getText().toString().trim();
                String password = passwordEditText.getText().toString().trim();

                Log.i(TAG, "email: "+email);
                Log.i(TAG, "password: "+password);

                Intent i = new Intent(MainActivity.this, EmailPasswordAuthentication.class);
                i.putExtra("email",email);
                i.putExtra("password",password);
                i.putExtra("state",EmailPasswordAuthentication.SIGNINSTATE);
                startActivityForResult(i,RC_EPSIGNIN);
            }
        });

        Button goToSignUpButton = findViewById(R.id.button5);
        goToSignUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(MainActivity.this, SignUpActivity.class);
                startActivity(i);
            }
        });

        ImageButton googleSigninButton = findViewById(R.id.imageButton);
        googleSigninButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(MainActivity.this,GoogleAuthentication.class);
                startActivityForResult(i,RC_EPSIGNIN);
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == RC_EPSIGNIN){
            if (resultCode == RESULT_CANCELED && data != null) {
                String errorMessage = data.getStringExtra("AUTH_ERROR");
                if (errorMessage != null) {
                    AlertUtils.showAlert(this,"Login Error",errorMessage);
                }
            }
            if(resultCode == RESULT_OK){
                checkCurrentUser();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(InternetChecker.checkInternet(MainActivity.this)){
            checkCurrentUser();
        }
    }

    public void checkCurrentUser() {
        // [START check_current_user]
        FirebaseUser user = getFirebaseUser();
        if (user != null) {
            //already signed in, ready to intent Home

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("drinker")
                    .document(user.getEmail())
                    .get()
                    .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                            if (documentSnapshot.exists()) {
                                Log.i(TAG, "Drinker Data: " + documentSnapshot.getData());
                                Drinker drinker = documentSnapshot.toObject(Drinker.class);
                                Log.i(TAG, "Drinker Data Object: " + drinker.getEmail() + drinker.getName());

                                MainActivity.loggedDrinker = drinker;

                                Intent i = new Intent(MainActivity.this, BaseActivity.class);
                                startActivity(i);
                            } else {
                                AlertUtils.showAlert(getApplicationContext(),"Login Error","Data retrieval failed! Please restart the application.");
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.i(TAG, "Error fetching drinker: " + e);
                            AlertUtils.showAlert(getApplicationContext(),"Login Error","Data retrieval failed! Please restart the application.");
                        }
                    });

        }
    }

    public static FirebaseUser getFirebaseUser (){
        return FirebaseAuth.getInstance().getCurrentUser();
    }
}