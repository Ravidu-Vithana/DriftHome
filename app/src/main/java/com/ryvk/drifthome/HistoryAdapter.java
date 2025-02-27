package com.ryvk.drifthome;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HistoryAdapter  extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
    private List<HistoryCard> historyList;
    private Context context;

    private static final String TAG = "AddressAdapter";

    public HistoryAdapter(List<HistoryCard> historyList, Context context) {
        this.historyList = historyList;
        this.context = context;
    }

    @NonNull
    @Override
    public HistoryAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.history_card, parent, false);
        return new HistoryAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryAdapter.ViewHolder holder, int position) {
        HistoryCard item = historyList.get(position);
        holder.dateView.setText(item.getDateText());
        holder.nameView.setText(item.getNameText());
        holder.vehicleView.setText(item.getNameText());

        holder.buttonFeedback.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showFeedbackModal(view.getContext(),item.getTripId());
            }
        });
    }

    private void showFeedbackModal(Context context, String tripId) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(context);
        View view = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_feedback, null);
        bottomSheetDialog.setContentView(view);

        RatingBar ratingBar = view.findViewById(R.id.ratingBar);
        EditText feedbackInput = view.findViewById(R.id.feedbackInput);
        Button btnSubmit = view.findViewById(R.id.btnSubmit);

        feedbackInput.setMovementMethod(new ScrollingMovementMethod());
        feedbackInput.setVerticalScrollBarEnabled(true);

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        btnSubmit.setOnClickListener(v -> {
            float rating = ratingBar.getRating();
            String feedback = feedbackInput.getText().toString().trim();

            if (rating < 1) {
                rating = 1;
            }

            if (feedback.isEmpty()) {
                Toast.makeText(context, "Please enter your feedback!", Toast.LENGTH_SHORT).show();
            } else {
                Map<String, Object> feedbackData = new HashMap<>();
                feedbackData.put("rating", rating);
                feedbackData.put("feedback", feedback);
                feedbackData.put("timestamp", System.currentTimeMillis());

                db.collection("trip").document(tripId)
                        .update("feedback", feedbackData)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(context, "Feedback submitted successfully!", Toast.LENGTH_SHORT).show();
                            bottomSheetDialog.dismiss();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(context, "Failed to submit feedback: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            }
        });

        bottomSheetDialog.show();
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView dateView;
        TextView nameView;
        TextView vehicleView;
        Button buttonFeedback;

        public ViewHolder(View itemView) {
            super(itemView);
            dateView = itemView.findViewById(R.id.textView17);
            nameView = itemView.findViewById(R.id.textView18);
            vehicleView = itemView.findViewById(R.id.textView50);
            buttonFeedback = itemView.findViewById(R.id.button3);
        }
    }
}
