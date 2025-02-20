package com.ryvk.drifthome;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddressListActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_MAP = 1003;
    private static final String TAG = "AddressListActivity";
    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_address_list);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        recyclerView = findViewById(R.id.recyclerView);
        ProgressBar progressBar = findViewById(R.id.progressBar3);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(RecyclerView.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);

        new Thread(() -> {
            Drinker loggedDrinker = Drinker.getSPDrinker(AddressListActivity.this);
            loggedDrinker.getAddressCardList(AddressListActivity.this, new Drinker.AddressCallback() {
                @Override
                public void onAddressesReady(List<AddressCard> addressCards) {
                    runOnUiThread(() -> {
                        AddressAdapter adapter = new AddressAdapter(addressCards, AddressListActivity.this);
                        recyclerView.setAdapter(adapter);
                        recyclerView.setVisibility(View.VISIBLE);
                        progressBar.setVisibility(View.GONE);
                    });
                }

                @Override
                public void onError(String error) {
                    Log.e("Address Fetch", "Error: " + error);
                }
            });
        }).start();

        Button addAddressButton = findViewById(R.id.button20);
        addAddressButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Drinker loggedDrinker = Drinker.getSPDrinker(AddressListActivity.this);
                if(loggedDrinker.getAddresses().size() < Drinker.ADDRESSLIMIT){
                    Intent intent = new Intent(AddressListActivity.this, MapActivity.class);
                    startActivityForResult(intent, REQUEST_CODE_MAP);
                }else{
                    AlertUtils.showAlert(AddressListActivity.this,"Limit Reached!","You can only add up to 5 addresses.");
                }
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_MAP && resultCode == RESULT_OK) {
            double latitude = data.getDoubleExtra("latitude", 0);
            double longitude = data.getDoubleExtra("longitude", 0);
            GeoPoint selectedGeoPoint = new GeoPoint(latitude, longitude);

            Drinker loggedDrinker = Drinker.getSPDrinker(AddressListActivity.this);
            loggedDrinker.addAddress(selectedGeoPoint, getApplicationContext(), new Drinker.AddressCallback() {
                @Override
                public void onAddressesReady(List<AddressCard> addressCards) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            AddressAdapter adapter = new AddressAdapter(addressCards, AddressListActivity.this);
                            recyclerView.setAdapter(adapter);

                            FirebaseFirestore db = FirebaseFirestore.getInstance();
                            Map<String, Object> drinkerAddressData = new HashMap<>();
                            drinkerAddressData.put("addresses", loggedDrinker.getAddresses());

                            db.collection("drinker")
                                    .document(loggedDrinker.getEmail())
                                    .update(drinkerAddressData)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void unused) {
                                            Log.i(TAG, "update address details: success");
                                            loggedDrinker.updateSPDrinker(AddressListActivity.this,loggedDrinker);

                                            Toast.makeText(getApplicationContext(), "Address added successfully!", Toast.LENGTH_LONG).show();
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Log.i(TAG, "update address details: failure");
                                            AlertUtils.showAlert(getApplicationContext(),"Address adding Failed!","Error: "+e);
                                        }
                                    });

                        }
                    });
                }

                @Override
                public void onError(String error) {
                    Log.i(TAG, "Address adding: failure " + error);
                }
            });
        }
    }
}