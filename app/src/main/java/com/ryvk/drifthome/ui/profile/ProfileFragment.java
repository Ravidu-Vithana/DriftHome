package com.ryvk.drifthome.ui.profile;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.ryvk.drifthome.AddCreditCardFragment;
import com.ryvk.drifthome.AddressListActivity;
import com.ryvk.drifthome.AlertUtils;
import com.ryvk.drifthome.Drinker;
import com.ryvk.drifthome.MainActivity;
import com.ryvk.drifthome.R;
import com.ryvk.drifthome.Validation;
import com.ryvk.drifthome.databinding.FragmentProfileBinding;

import java.util.Calendar;
import java.util.HashMap;

public class ProfileFragment extends Fragment {
    private static final String TAG = "ProfileFragment";
    private FragmentProfileBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        ProfileViewModel profileViewModel =
                new ViewModelProvider(this).get(ProfileViewModel.class);

        binding = FragmentProfileBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("com.ryvk.drifthome.data", Context.MODE_PRIVATE);
        String drinkerJSON = sharedPreferences.getString("user",null);

        Gson gson = new Gson();
        Drinker loggedDrinker = gson.fromJson(drinkerJSON, Drinker.class);

        EditText namefield = binding.editTextText8;
        EditText dobField = binding.editTextText9;
        EditText mobileField = binding.editTextText10;
        Spinner genderField = binding.spinner;
        EditText emailField = binding.editTextText12;

        if (loggedDrinker.getName() != null) namefield.setText(loggedDrinker.getName());
        if (loggedDrinker.getDob() != null) dobField.setText(loggedDrinker.getDob());
        if (loggedDrinker.getMobile() != null) mobileField.setText(loggedDrinker.getMobile());
        if (loggedDrinker.getGender() != null){
            String[] genderArray = getResources().getStringArray(R.array.d_profileFragment_genderSpinner);
            int position = getGenderPosition(loggedDrinker.getGender(), genderArray);
            genderField.setSelection(position);
        }
        if (loggedDrinker.getEmail() != null) emailField.setText(loggedDrinker.getEmail());

        binding.button18.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getContext(), AddressListActivity.class);
                startActivity(i);
            }
        });

        binding.button19.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HashMap<String, Object> drinker = loggedDrinker.updateFields(
                        emailField.getText().toString().trim(),
                        namefield.getText().toString().trim(),
                        mobileField.getText().toString().trim(),
                        genderField.getSelectedItem().toString().trim(),
                        dobField.getText().toString().trim()
                        );

                Log.i(TAG, "onClick: "+loggedDrinker.getDob()+loggedDrinker.getGender()+loggedDrinker.getMobile());

                Gson gson = new Gson();
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("user",gson.toJson(loggedDrinker));
                editor.apply();

                FirebaseFirestore db = FirebaseFirestore.getInstance();

                db.collection("drinker")
                        .document(loggedDrinker.getEmail())
                        .update(drinker)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unused) {
                                Log.i(TAG, "update details: success");
                                AlertUtils.showAlert(getContext(),"Success!","Profile updated successfully!");
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.i(TAG, "update details: failure");
                                AlertUtils.showAlert(getContext(),"Profile Update Failed!","Error: "+e);
                            }
                        });

            }
        });

        dobField.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDatePickerDialog();
            }
        });

        return root;
    }

    private void showDatePickerDialog() {
        // Get current date
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        // Create DatePickerDialog
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                getContext(),
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int selectedYear, int selectedMonth, int selectedDay) {
                        // Format date as DD/MM/YYYY
                        String selectedDate = String.format("%02d/%02d/%04d", selectedDay, (selectedMonth + 1), selectedYear);
                        binding.editTextText9.setText(selectedDate);
                    }
                },
                year, month, day
        );

        datePickerDialog.show();
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