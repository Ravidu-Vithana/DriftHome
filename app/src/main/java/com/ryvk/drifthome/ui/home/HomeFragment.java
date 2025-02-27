package com.ryvk.drifthome.ui.home;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.gson.Gson;
import com.ryvk.drifthome.BaseActivity;
import com.ryvk.drifthome.BookingActivity;
import com.ryvk.drifthome.Drinker;
import com.ryvk.drifthome.DrinkerConfig;
import com.ryvk.drifthome.R;
import com.ryvk.drifthome.SplashActivity;
import com.ryvk.drifthome.databinding.FragmentHomeBinding;

public class HomeFragment extends Fragment {
    private static final String TAG = "HomeFragment";
    private FragmentHomeBinding binding;
    Button mainButton;
    TextView mainText;
    Button tokensButton;
    Drinker loggedDrinker;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        mainButton = binding.button9;
        mainText = binding.textView13;
        tokensButton = binding.button8;
        Button refreshButton = binding.button25;
        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                reloadData();
            }
        });

        refreshData();

        return root;
    }

    private void reloadData(){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("drinker")
                .document(loggedDrinker.getEmail())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {

                        Drinker drinker = documentSnapshot.toObject(Drinker.class);
                        drinker.updateSPDrinker(getContext(), drinker);

                        db.collection("drinkerConfig")
                                .document(loggedDrinker.getEmail())
                                .get()
                                .addOnSuccessListener(documentSnapshot2 -> {
                                    if (documentSnapshot2.exists()) {
                                        DrinkerConfig drinkerConfig = documentSnapshot2.toObject(DrinkerConfig.class);
                                        drinkerConfig.updateSPDrinkerConfig(getContext(), drinkerConfig);
                                        ((Activity) getContext()).runOnUiThread(()-> Toast.makeText(getContext(),"Data refresh successful!",Toast.LENGTH_SHORT).show());
                                        refreshData();
                                    } else {
                                        ((Activity) getContext()).runOnUiThread(()-> Toast.makeText(getContext(),"Data refresh failed!",Toast.LENGTH_LONG).show());
                                    }
                                })
                                .addOnFailureListener(e -> ((Activity) getContext()).runOnUiThread(()-> Toast.makeText(getContext(),"Data refresh failed!",Toast.LENGTH_LONG).show()));
                    } else {
                        ((Activity) getContext()).runOnUiThread(()-> Toast.makeText(getContext(),"Data refresh failed!",Toast.LENGTH_LONG).show());
                    }
                })
                .addOnFailureListener(e -> ((Activity) getContext()).runOnUiThread(()-> Toast.makeText(getContext(),"Data refresh failed!",Toast.LENGTH_LONG).show()));
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "onResume: Home Fragment");
        refreshData();
    }

    private void refreshData(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                loggedDrinker = Drinker.getSPDrinker(getContext());
                ((Activity) getContext()).runOnUiThread(()->tokensButton.setText(String.valueOf(loggedDrinker.getTokens())));

                int minToken = getActivity().getResources().getInteger(R.integer.minimum_token_amount);

                ((Activity) getContext()).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(!loggedDrinker.isProfileComplete()){
                            int newColor = ContextCompat.getColor(getContext(), R.color.d_red1);
                            mainButton.setText(R.string.d_homeFragment_btn1_completeProfile);
                            mainButton.setBackgroundColor(newColor);
                            mainText.setText(R.string.d_homeFragment_text1_completeProfile);

                        }else if(loggedDrinker.getTokens() <= minToken) {
                            int newColor = ContextCompat.getColor(getContext(), R.color.d_orange);
                            mainButton.setText(R.string.d_homeFragment_btn1_insufficientCredit);
                            mainButton.setBackgroundColor(newColor);
                            mainText.setText(R.string.d_homeFragment_text1_insufficientCredit);
                        }else{
                            mainButton.setOnLongClickListener(new View.OnLongClickListener() {
                                @Override
                                public boolean onLongClick(View view) {
                                    Intent i = new Intent(getContext(), BookingActivity.class);
                                    startActivity(i);
                                    return true;
                                }
                            });
                            mainButton.setEnabled(true);
                        }
                    }
                });
            }
        }).start();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.i(TAG, "onDestroyView: home fragment");
        binding = null;
    }
}