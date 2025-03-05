package com.ryvk.drifthome;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {
    private static final String TAG = "HistoryActivity";
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private ConstraintLayout noHistoryContainer;
    private Drinker loggedDrinker;
    private final List<HistoryCard> tripList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_history);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        loggedDrinker = Drinker.getSPDrinker(HistoryActivity.this);

        recyclerView = findViewById(R.id.recyclerView2);
        progressBar = findViewById(R.id.progressBar3);
        noHistoryContainer = findViewById(R.id.noHistoryContainer);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(RecyclerView.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);

        loadTripHistory();

    }

    public void loadTripHistory(){
        tripList.clear();
        progressBar.setVisibility(View.VISIBLE);
        Log.d(TAG, "loadTripHistory: started");
        new Thread(()->{
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            db.collection("trip")
                    .whereEqualTo("drinker_email", loggedDrinker.getEmail())
                    .orderBy("created_at", Query.Direction.DESCENDING)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                                Trip trip = document.toObject(Trip.class);

                                if (trip.getSaviour_email() != null) {
                                    db.collection("saviour")
                                            .document(trip.getSaviour_email())
                                            .get()
                                            .addOnSuccessListener(documentSnapshot -> {
                                                if (documentSnapshot.exists()) {
                                                    Saviour saviour = documentSnapshot.toObject(Saviour.class);

                                                    if(saviour != null){
                                                        boolean feedbackGiven = trip.getFeedback_of_drinker() != null && !trip.getFeedback_of_drinker().isEmpty();

                                                        HistoryCard historyCard = new HistoryCard(document.getId(), trip.getCreated_at(), saviour.getProfile_pic(), saviour.getName(), saviour.getVehicle(), feedbackGiven);
                                                        tripList.add(historyCard);
                                                        updateUI();
                                                    }

                                                } else {
                                                    Log.d("Saviour data", "No saviour found.");
                                                }
                                            })
                                            .addOnFailureListener(e -> Log.e("Saviour data", "No saviour found.", e));
                                } else {
                                    Log.d(TAG, "loadTripHistory: savior email is null");
                                }
                            }
                        }else{
                            updateUI();
                        }
                    })
                    .addOnFailureListener(e -> Log.e("TripData", "Error getting trips: ", e));
        }).start();
    }
    private void updateUI(){
        if(tripList.isEmpty()){
            noHistoryContainer.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
        }else{
            HistoryAdapter adapter = new HistoryAdapter(tripList, HistoryActivity.this, new HistoryAdapter.OnFeedbackSubmittedListener() {
                @Override
                public void onFeedbackSubmitted() {
                    loadTripHistory();
                }
            });
            recyclerView.setAdapter(adapter);
            recyclerView.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
        }
    }
}