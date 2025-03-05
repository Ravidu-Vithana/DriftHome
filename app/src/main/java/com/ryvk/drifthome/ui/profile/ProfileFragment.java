package com.ryvk.drifthome.ui.profile;

import static android.app.Activity.RESULT_OK;

import android.Manifest;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.ryvk.drifthome.AddressListActivity;
import com.ryvk.drifthome.AlertUtils;
import com.ryvk.drifthome.Drinker;
import com.ryvk.drifthome.HistoryActivity;
import com.ryvk.drifthome.R;
import com.ryvk.drifthome.Utils;
import com.ryvk.drifthome.databinding.FragmentProfileBinding;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.util.Calendar;
import java.util.HashMap;

public class ProfileFragment extends Fragment {
    private static final String TAG = "ProfileFragment";
    private FragmentProfileBinding binding;
    private Button historyButton;
    private Uri profileImageUri;
    private ImageView profileImageView;
    private ActivityResultLauncher<Intent> cropImageLauncher;
    private static final int PICK_IMAGE_REQUEST = 10;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ProfileViewModel profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Get the history button from root view
        historyButton =((Activity) getContext()).findViewById(R.id.historyButton);
        if(historyButton != null){
            historyButton.setVisibility(View.VISIBLE);
            historyButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent i = new Intent(getContext(), HistoryActivity.class);
                    startActivity(i);
                }
            });
        }

        profileImageView = binding.imageView9;
        ImageButton profileImagePickerButton = binding.imagePickerButton;
        EditText nameField = binding.editTextText8;
        EditText dobField = binding.editTextText9;
        EditText mobileField = binding.editTextText10;
        Spinner genderField = binding.spinner;
        EditText emailField = binding.editTextText12;

        // Load Drinker details
        Drinker loggedDrinker = Drinker.getSPDrinker(getContext());
        if (loggedDrinker.getProfile_pic() != null) Utils.loadImageUrlToView(getContext(),profileImageView,loggedDrinker.getProfile_pic());
        if (loggedDrinker.getName() != null) nameField.setText(loggedDrinker.getName());
        if (loggedDrinker.getDob() != null) dobField.setText(loggedDrinker.getDob());
        if (loggedDrinker.getMobile() != null) mobileField.setText(loggedDrinker.getMobile());
        if (loggedDrinker.getGender() != null) {
            String[] genderArray = getResources().getStringArray(R.array.d_profileFragment_genderSpinner);
            genderField.setSelection(getGenderPosition(loggedDrinker.getGender(), genderArray));
        }
        if (loggedDrinker.getEmail() != null) emailField.setText(loggedDrinker.getEmail());

        // Initialize crop image launcher
        cropImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        profileImageUri = UCrop.getOutput(result.getData());
                        if (profileImageUri != null) {
                            updateImageView(profileImageUri);
                            uploadToFirebaseStorage();
                        }
                    }
                });

        // Image picker button
        profileImagePickerButton.setOnClickListener(view -> {
            openGallery();
        });

        // Address button
        binding.button18.setOnClickListener(view -> {
            Utils.hideKeyboard(requireActivity());
            startActivity(new Intent(getContext(), AddressListActivity.class));
        });

        // Update button
        binding.button19.setOnClickListener(view -> {
            Utils.hideKeyboard(requireActivity());

            HashMap<String, Object> drinker = loggedDrinker.updateFields(
                    emailField.getText().toString().trim(),
                    nameField.getText().toString().trim(),
                    mobileField.getText().toString().trim(),
                    genderField.getSelectedItem().toString().trim(),
                    dobField.getText().toString().trim()
            );

            loggedDrinker.updateSPDrinker(getContext(), loggedDrinker);
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("drinker")
                    .document(loggedDrinker.getEmail())
                    .update(drinker)
                    .addOnSuccessListener(unused -> AlertUtils.showAlert(getContext(), "Success!", "Profile updated successfully!"))
                    .addOnFailureListener(e -> AlertUtils.showAlert(getContext(), "Profile Update Failed!", "Error: " + e));
        });

        // Date picker
        dobField.setOnClickListener(view -> showDatePickerDialog());

        return root;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri selectedImageUri = data.getData();
            Log.d(TAG, "onActivityResult: gallery image is selected: "+selectedImageUri);
            startCrop(selectedImageUri);
        } else {
            Log.d(TAG, "onActivityResult: some result received");
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    private void startCrop(Uri sourceUri) {
        Uri destinationUri = Uri.fromFile(new File(requireContext().getCacheDir(), "cropped_image.jpg"));
        Intent cropIntent = UCrop.of(sourceUri, destinationUri)
                .withAspectRatio(1, 1)
                .getIntent(requireContext());
        cropImageLauncher.launch(cropIntent);
    }

    private void updateImageView(Uri imageUri) {
        profileImageView.setImageBitmap(Utils.getRoundedImageBitmap(imageUri));
    }

    private void uploadToFirebaseStorage(){
        Drinker loggedDrinker = Drinker.getSPDrinker(getContext());

        if(profileImageUri != null){
            FirebaseStorage.getInstance().getReference().child("drinker_profile_pic").child(loggedDrinker.getEmail())
                    .putFile(profileImageUri)
                    .addOnSuccessListener(taskSnapshot ->
                            taskSnapshot.getStorage().getDownloadUrl()
                                    .addOnSuccessListener(uri -> {
                                        String profileImageUrl = uri.toString();
                                        loggedDrinker.setProfile_pic(profileImageUrl);
                                        loggedDrinker.updateSPDrinker(getContext(), loggedDrinker);
                                        FirebaseFirestore db = FirebaseFirestore.getInstance();
                                        db.collection("drinker")
                                                .document(loggedDrinker.getEmail())
                                                .update("profile_pic", profileImageUrl)
                                                .addOnSuccessListener(unused -> Toast.makeText(getContext(),"Profile Image Updated!",Toast.LENGTH_LONG).show())
                                                .addOnFailureListener(e -> AlertUtils.showAlert(getContext(), "Profile with image Update Failed!", "Error: " + e));
                                    })
                                    .addOnFailureListener(e -> Log.e("Storage", "Failed to get download URL", e))
                    )
                    .addOnFailureListener(e -> Log.e("Storage", "Image upload failed", e));
        }
    }

    private void showDatePickerDialog() {
        final Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                getContext(),
                (DatePicker view, int selectedYear, int selectedMonth, int selectedDay) -> {
                    String selectedDate = String.format("%02d/%02d/%04d", selectedDay, (selectedMonth + 1), selectedYear);
                    binding.editTextText9.setText(selectedDate);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
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
        if (historyButton != null) {
            historyButton.setVisibility(View.INVISIBLE);
        }
        super.onDestroyView();
        binding = null;
    }
}