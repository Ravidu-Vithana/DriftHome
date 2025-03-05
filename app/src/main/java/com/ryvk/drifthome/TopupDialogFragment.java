package com.ryvk.drifthome;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;

public class TopupDialogFragment extends DialogFragment {
    private static final String TAG = "TopupDialogFragment";
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.fragment_topup_dialog, null);
        builder.setView(view);

        Drinker loggedDrinker = Drinker.getSPDrinker(getContext());

        EditText amountInput = view.findViewById(R.id.amountInput);
        Button btnProceed = view.findViewById(R.id.btnProceed);

        btnProceed.setOnClickListener(v -> {
            String amount = amountInput.getText().toString();

            if(!Validation.isInteger(amount)){
                AlertUtils.showAlert(getContext(),"Error","Invalid Amount");
            }else if(Integer.parseInt(amount) < 2000){
                AlertUtils.showAlert(getContext(),"Error","Minimum Top up amount is Rs.2000");
            }else{

                HashMap<String, Object> paymentHash = new HashMap<>();
                paymentHash.put("drinker_email", loggedDrinker.getEmail());
                paymentHash.put("amount", amount);
                paymentHash.put("created_at", Validation.todayDateTime());

                FirebaseFirestore db = FirebaseFirestore.getInstance();
                db.collection("payment")
                        .add(paymentHash)
                        .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                            @Override
                            public void onSuccess(DocumentReference documentReference) {
                                Log.d(TAG, "onSuccess: payment document added");
                                dismiss();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.d(TAG, "onFailure: payment document adding failed");
                                AlertUtils.showAlert(getContext(),"Error","Payment details adding failed!");
                            }
                        });
            }
        });

        return builder.create();
    }
}