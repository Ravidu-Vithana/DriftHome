package com.ryvk.drifthome;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
    private List<HistoryCard> historyList;
    private Context context;
    private OnFeedbackSubmittedListener feedbackListener;

    private static final String TAG = "AddressAdapter";

    public HistoryAdapter(List<HistoryCard> historyList, Context context, OnFeedbackSubmittedListener feedbackListener) {
        this.historyList = historyList;
        this.context = context;
        this.feedbackListener = feedbackListener;
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

        if(item.getProfilePictureUrl() != null && !item.getProfilePictureUrl().isBlank()){
            Utils.loadImageUrlToView(context,holder.profileImageView,item.getProfilePictureUrl());
        }

        holder.nameView.setText(item.getNameText());
        holder.vehicleView.setText(item.getVehicleText());
        if(item.isFeedbackGiven()){
            int newColor = ContextCompat.getColor(context, R.color.d_gray3);
            ColorStateList newColorStateList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.d_gray3));

            holder.buttonFeedback.setText(R.string.d_historyCardFragment_btn_given);
            holder.buttonFeedback.setStrokeColor(newColorStateList);
            holder.buttonFeedback.setTextColor(newColor);
            holder.buttonFeedback.setEnabled(false);
        }else{
            holder.buttonFeedback.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showFeedbackModal(view.getContext(),item.getTripId());
                }
            });
        }

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
                        .update("feedback_of_drinker", feedbackData)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(context, "Feedback submitted successfully!", Toast.LENGTH_SHORT).show();
                            bottomSheetDialog.dismiss();
                            if (feedbackListener != null) {
                                feedbackListener.onFeedbackSubmitted();
                            }
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
        ImageView profileImageView;
        TextView nameView;
        TextView vehicleView;
        MaterialButton buttonFeedback;

        public ViewHolder(View itemView) {
            super(itemView);
            dateView = itemView.findViewById(R.id.textView17);
            profileImageView = itemView.findViewById(R.id.imageView2);
            nameView = itemView.findViewById(R.id.textView18);
            vehicleView = itemView.findViewById(R.id.textView50);
            buttonFeedback = itemView.findViewById(R.id.button3);
        }
    }

    public interface OnFeedbackSubmittedListener {
        void onFeedbackSubmitted();
    }
}
