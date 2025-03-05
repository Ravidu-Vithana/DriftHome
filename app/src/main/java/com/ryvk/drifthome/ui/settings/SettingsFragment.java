package com.ryvk.drifthome.ui.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.ryvk.drifthome.AlertUtils;
import com.ryvk.drifthome.Drinker;
import com.ryvk.drifthome.DrinkerConfig;
import com.ryvk.drifthome.MainActivity;
import com.ryvk.drifthome.PaymentHelper;
import com.ryvk.drifthome.R;
import com.ryvk.drifthome.TopupDialogFragment;
import com.ryvk.drifthome.Validation;
import com.ryvk.drifthome.databinding.FragmentSettingsBinding;

import java.util.HashMap;

import lk.payhere.androidsdk.PHConstants;
import lk.payhere.androidsdk.PHResponse;
import lk.payhere.androidsdk.model.StatusResponse;

public class SettingsFragment extends Fragment {
    private static final String TAG = "SettingsFragment";
    HashMap<String,Object> updatingPropertyMap = new HashMap<>();

    private FragmentSettingsBinding binding;
    private DrinkerConfig loggedDrinkerConfig;
    private Drinker loggedDrinker;
    private LogoutListener logoutListener;
    private Switch alwaysHome;
    private String amount;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        SettingsViewModel settingsViewModel =
                new ViewModelProvider(this).get(SettingsViewModel.class);

        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        loggedDrinkerConfig = DrinkerConfig.getSPDrinkerConfig(getContext());
        loggedDrinker = Drinker.getSPDrinker(getContext());

        alwaysHome = binding.switch6;
        Switch shakeToBook = binding.switch3;
        Switch autoClose = binding.switch4;
        Switch voiceNotifications = binding.switch5;
        Button editRangeButton = binding.button27;
        editRangeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showEditRangeBottomSheet();
            }
        });

        if (loggedDrinkerConfig.isAlways_home()){
            alwaysHome.setChecked(true);
            editRangeButton.setVisibility(View.VISIBLE);
            editRangeButton.setEnabled(true);
        }
        if (loggedDrinkerConfig.isShake_to_book()) shakeToBook.setChecked(true);
        if (loggedDrinkerConfig.isAuto_close()) autoClose.setChecked(true);
        if (loggedDrinkerConfig.isVoice_notifications()) voiceNotifications.setChecked(true);


        alwaysHome.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            if (isChecked) {
                BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(getContext());
                View view = LayoutInflater.from(getContext()).inflate(R.layout.bottom_sheet_home_range, null);
                bottomSheetDialog.setContentView(view);

                EditText rangeField = view.findViewById(R.id.amountInput);
                Button setRangeButton = view.findViewById(R.id.btnProceed);

                boolean[] isDismissedByButton = {false};

                bottomSheetDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        if (!isDismissedByButton[0]) {
                            alwaysHome.setChecked(false);
                        }
                    }
                });

                setRangeButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String range = rangeField.getText().toString().trim();
                        if(Validation.isInteger(range) && Integer.parseInt(range) > 0){

                            loggedDrinkerConfig.setAlways_home(true);
                            loggedDrinkerConfig.setHome_range(Integer.parseInt(range));
                            updatingPropertyMap.put("always_home",loggedDrinkerConfig.isAlways_home());
                            updatingPropertyMap.put("home_range",Integer.parseInt(range));
                            updateSettings();

                            editRangeButton.setVisibility(View.VISIBLE);
                            editRangeButton.setEnabled(true);

                            isDismissedByButton[0] = true;
                            bottomSheetDialog.dismiss();
                        }else{
                            Toast.makeText(getContext(),"Range is not valid!",Toast.LENGTH_SHORT).show();
                        }
                    }
                });

                bottomSheetDialog.show();
            } else {
                editRangeButton.setVisibility(View.INVISIBLE);
                editRangeButton.setEnabled(false);
                loggedDrinkerConfig.setAlways_home(false);
                loggedDrinkerConfig.setHome_range(0);
                updatingPropertyMap.put("always_home",loggedDrinkerConfig.isAlways_home());
                updatingPropertyMap.put("home_range",0);
                updateSettings();
            }

        });

        shakeToBook.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            if (isChecked) {
                SensorManager sensorManager = (SensorManager) ((Activity)getContext()).getSystemService(Context.SENSOR_SERVICE);
                if(sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null){
                    loggedDrinkerConfig.setShake_to_book(true);
                }else{
                    AlertUtils.showAlert(getContext(),"Not Allowed!","Your device does not support this feature.");
                    shakeToBook.setChecked(false);
                }

            } else {
                loggedDrinkerConfig.setShake_to_book(false);
            }

            updatingPropertyMap.put("shake_to_book",loggedDrinkerConfig.isShake_to_book());
            updateSettings();
        });

        autoClose.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            if (isChecked) {
                loggedDrinkerConfig.setAuto_close(true);
            } else {
                loggedDrinkerConfig.setAuto_close(false);
            }

            updatingPropertyMap.put("auto_close",loggedDrinkerConfig.isAuto_close());
            updateSettings();
        });

        voiceNotifications.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            if (isChecked) {
                loggedDrinkerConfig.setVoice_notifications(true);
            } else {
                loggedDrinkerConfig.setVoice_notifications(false);
            }

            updatingPropertyMap.put("voice_notifications",loggedDrinkerConfig.isVoice_notifications());
            updateSettings();
        });

        binding.button26.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPaymentAlertDialog();
            }
        });

        binding.button15.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                AlertUtils.showConfirmDialog(getContext(), "Confirm Action", "Are you sure you want to logout?",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int which) {
                                FirebaseUser loggedUser = MainActivity.getFirebaseUser();
                                if(loggedUser != null){

                                    GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                            .requestIdToken(getString(R.string.web_server_client_id))
                                            .requestEmail()
                                            .build();

                                    loggedDrinker.removeSPDrinker(getContext());

                                    GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(getContext(), gso);
                                    FirebaseAuth mAuth = FirebaseAuth.getInstance();
                                    mAuth.signOut();
                                    mGoogleSignInClient.signOut();

                                    Log.i(TAG, "onClick: Logout, user logged out------------------------");
                                }else{
                                    Log.i(TAG, "onClick: Logout . NO user ----------------------------------");
                                }
                                if (logoutListener != null) {
                                    logoutListener.onLogout();
                                }
                            }
                        }
                );
            }
        });

        binding.button21.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                AlertUtils.showConfirmDialog(getContext(), "Confirm Action", "Are you sure you want to delete the account?",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int which) {
                                FirebaseUser loggedUser = MainActivity.getFirebaseUser();
                                if(loggedUser != null){

                                    deleteUser();

                                    Log.i(TAG, "onClick: Delete account, account deleted------------------------");
                                }else{
                                    Log.i(TAG, "onClick: Delete account . NO user ----------------------------------");
                                }
                            }
                        }
                );
            }
        });

        return root;
    }

    private void showPaymentAlertDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.fragment_topup_dialog, null);
        builder.setView(view);

        Drinker loggedDrinker = Drinker.getSPDrinker(getContext());

        EditText amountInput = view.findViewById(R.id.amountInput);
        Button btnProceed = view.findViewById(R.id.btnProceed);
        AlertDialog dialog = builder.create();

        btnProceed.setOnClickListener(v -> {
            amount = amountInput.getText().toString();

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
                                dialog.dismiss();
                                PaymentHelper.initiatePayment(SettingsFragment.this, Double.parseDouble(amount), documentReference.getId(),
                                        "Top up account.", loggedDrinker.getEmail(), loggedDrinker.getName(), "", loggedDrinker.getMobile());
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
        dialog.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PaymentHelper.RC_PAYHERE && data != null && data.hasExtra(PHConstants.INTENT_EXTRA_RESULT)) {
            PHResponse<StatusResponse> response = (PHResponse<StatusResponse>) data.getSerializableExtra(PHConstants.INTENT_EXTRA_RESULT);
            if (resultCode == Activity.RESULT_OK) {
                String msg;
                if (response != null){
                    if (response.isSuccess()){
                        msg = "Activity result:" + response.getData().toString();
                        if(response.getStatus() == PHResponse.STATUS_SUCCESS){
                            loggedDrinker.setTokens(loggedDrinker.getTokens()+Integer.parseInt(amount));
                            loggedDrinker.updateSPDrinker(getContext(),loggedDrinker);

                            FirebaseFirestore db = FirebaseFirestore.getInstance();

                            db.collection("drinker")
                                    .document(loggedDrinker.getEmail())
                                    .update("tokens",loggedDrinker.getTokens())
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void unused) {
                                            Log.i(TAG, "update tokens: success");
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Log.i(TAG, "update tokens: failure");
                                            AlertUtils.showAlert(getContext(),"Tokens Update Failed!","Error: "+e);
                                        }
                                    });

                        }else if(response.getStatus() == PHResponse.STATUS_ERROR_CANCELED){
                            Toast.makeText(getContext(),"Payment Cancelled",Toast.LENGTH_LONG).show();
                        }else if(response.getStatus() == PHResponse.STATUS_ERROR_NETWORK){
                            Toast.makeText(getContext(),"Network error occured!",Toast.LENGTH_LONG).show();
                        }else if(response.getStatus() == PHResponse.STATUS_SESSION_TIME_OUT){
                            AlertUtils.showAlert(getContext(),"Error","Your session timed out.");
                        }else if(response.getStatus() == PHResponse.STATUS_ERROR_DATA){
                            Toast.makeText(getContext(),"Data error occurred",Toast.LENGTH_LONG).show();
                        }else if(response.getStatus() == PHResponse.STATUS_ERROR_PAYMENT){
                            Toast.makeText(getContext(),"Payment error occurred",Toast.LENGTH_LONG).show();
                        }else if(response.getStatus() == PHResponse.STATUS_ERROR_VALIDATION){
                            Toast.makeText(getContext(),"Validation Failed!",Toast.LENGTH_LONG).show();
                        }else if(response.getStatus() == PHResponse.STATUS_ERROR_UNKNOWN){
                            AlertUtils.showAlert(getContext(),"Error","An unknown error occurred. Please try again later.");
                        }else{
                            Log.d(TAG, "onActivityResult: no match for status code: "+response.getStatus());
                            AlertUtils.showAlert(getContext(),"Error","Response status is not valid. Please try again later.");
                        }
                    }else{
                        msg = "Result:" + response.toString();
                    }
                }else{
                    msg = "Result: no response";
                }
                Log.d(TAG, msg);
            } else if (resultCode == Activity.RESULT_CANCELED) {
                if (response != null){
                    Log.d(TAG, "onActivityResult: Top up failed!"+response.toString());
                    if(response.getStatus() == PHResponse.STATUS_ERROR_CANCELED){
                        Toast.makeText(getContext(),"Payment Cancelled",Toast.LENGTH_LONG).show();
                    }else if(response.getStatus() == PHResponse.STATUS_ERROR_NETWORK){
                        Toast.makeText(getContext(),"Network error occured!",Toast.LENGTH_LONG).show();
                    }else if(response.getStatus() == PHResponse.STATUS_SESSION_TIME_OUT){
                        AlertUtils.showAlert(getContext(),"Error","Your session timed out.");
                    }else if(response.getStatus() == PHResponse.STATUS_ERROR_DATA){
                        Toast.makeText(getContext(),"Data error occurred",Toast.LENGTH_LONG).show();
                    }else if(response.getStatus() == PHResponse.STATUS_ERROR_PAYMENT){
                        Toast.makeText(getContext(),"Payment error occurred",Toast.LENGTH_LONG).show();
                    }else if(response.getStatus() == PHResponse.STATUS_ERROR_VALIDATION){
                        Toast.makeText(getContext(),"Validation Failed!",Toast.LENGTH_LONG).show();
                    }else if(response.getStatus() == PHResponse.STATUS_ERROR_UNKNOWN){
                        AlertUtils.showAlert(getContext(),"Error","An unknown error occurred. Please try again later.");
                    }else{
                        Log.d(TAG, "onActivityResult: no match for status code: "+response.getStatus());
                        AlertUtils.showAlert(getContext(),"Error","Response status is not valid. Please try again later.");
                    }
                }else {
                    Toast.makeText(getContext(),"Request cancelled",Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void showEditRangeBottomSheet(){
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(getContext());
        View view = LayoutInflater.from(getContext()).inflate(R.layout.bottom_sheet_home_range, null);
        bottomSheetDialog.setContentView(view);

        int range = loggedDrinkerConfig.getHome_range();

        EditText rangeField = view.findViewById(R.id.amountInput);
        rangeField.setText(String.valueOf(range));

        Button setRangeButton = view.findViewById(R.id.btnProceed);

        setRangeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String range = rangeField.getText().toString().trim();
                if(Validation.isInteger(range) && Integer.parseInt(range) > 0){

                    loggedDrinkerConfig.setAlways_home(true);
                    loggedDrinkerConfig.setHome_range(Integer.parseInt(range));
                    updatingPropertyMap.put("always_home",loggedDrinkerConfig.isAlways_home());
                    updatingPropertyMap.put("home_range",Integer.parseInt(range));
                    updateSettings();

                    bottomSheetDialog.dismiss();
                }else{
                    Toast.makeText(getContext(),"Range is not valid!",Toast.LENGTH_SHORT).show();
                }
            }
        });

        bottomSheetDialog.show();
    }

    private void updateSettings(){
        if(loggedDrinker != null && loggedDrinkerConfig != null){
            loggedDrinkerConfig.updateSPDrinkerConfig(getContext(),loggedDrinkerConfig);

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("drinkerConfig")
                    .document(loggedDrinker.getEmail())
                    .update(updatingPropertyMap)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            Log.i(TAG, "update setting: success");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.i(TAG, "update setting: failure");
                            AlertUtils.showAlert(getContext(),"Setting Update Failed!","Error: "+e);
                        }
                    });
        }
    }

    public void deleteUser() {
        // [START delete_user]
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        user.delete()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "User account deleted.");
                        }
                    }
                });
        // [END delete_user]
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof LogoutListener) {
            logoutListener = (LogoutListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement LogoutListener");
        }
    }

    public interface LogoutListener {
        void onLogout();
    }
}