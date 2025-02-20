package com.ryvk.drifthome.ui.home;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.gson.Gson;
import com.ryvk.drifthome.BookingActivity;
import com.ryvk.drifthome.Drinker;
import com.ryvk.drifthome.R;
import com.ryvk.drifthome.databinding.FragmentHomeBinding;

public class HomeFragment extends Fragment {

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

        loggedDrinker = Drinker.getSPDrinker(getContext());

        mainButton = binding.button9;
        mainText = binding.textView13;
        tokensButton = binding.button8;

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshData();
    }

    private void refreshData(){
        tokensButton.setText(String.valueOf(loggedDrinker.getTokens()));

        int minToken = getActivity().getResources().getInteger(R.integer.minimum_token_amount);

        if(!loggedDrinker.isProfileComplete()){
            int newColor = ContextCompat.getColor(getContext(), R.color.d_red1);
            mainButton.setText(R.string.d_homeFragment_btn1_completeProfile);
            mainButton.setBackgroundColor(newColor);
            mainText.setText(R.string.d_homeFragment_text1_completeProfile);

            mainButton.setEnabled(false);

        }else if(loggedDrinker.getTokens() <= minToken) {
            int newColor = ContextCompat.getColor(getContext(), R.color.d_orange);
            mainButton.setText(R.string.d_homeFragment_btn1_insufficientCredit);
            mainButton.setBackgroundColor(newColor);
            mainText.setText(R.string.d_homeFragment_text1_insufficientCredit);

            mainButton.setEnabled(false);

        }else{
            mainButton.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    Intent i = new Intent(getContext(), BookingActivity.class);
                    startActivity(i);
                    return true;
                }
            });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}