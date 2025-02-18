package com.ryvk.drifthome.ui.profile;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.ryvk.drifthome.Drinker;
import com.ryvk.drifthome.MainActivity;
import com.ryvk.drifthome.R;
import com.ryvk.drifthome.databinding.FragmentProfileBinding;

public class ProfileFragment extends Fragment {
    private static final String TAG = "ProfileFragment";
    private FragmentProfileBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        ProfileViewModel profileViewModel =
                new ViewModelProvider(this).get(ProfileViewModel.class);

        binding = FragmentProfileBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        Drinker loggedDrinker = MainActivity.loggedDrinker;

        if (loggedDrinker.getName() != null) binding.editTextText8.setText(loggedDrinker.getName());
        if (loggedDrinker.getDob() != null) binding.editTextText9.setText(loggedDrinker.getDob());
        if (loggedDrinker.getMobile() != null) binding.editTextText10.setText(loggedDrinker.getMobile());
        if (loggedDrinker.getGender() != null){
            String[] genderArray = getResources().getStringArray(R.array.d_profileFragment_genderSpinner);
            int position = getGenderPosition(loggedDrinker.getGender(), genderArray);
            binding.spinner.setSelection(position);
        }
        if (loggedDrinker.getEmail() != null) binding.editTextText12.setText(loggedDrinker.getEmail());

        return root;
    }

    private int getGenderPosition(String gender, String[] genderArray) {
        for (int i = 0; i < genderArray.length; i++) {
            if (genderArray[i].equalsIgnoreCase(gender)) {
                return i;
            }
        }
        return 0;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}