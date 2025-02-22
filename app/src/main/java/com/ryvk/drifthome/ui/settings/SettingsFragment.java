package com.ryvk.drifthome.ui.settings;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.ryvk.drifthome.AlertUtils;
import com.ryvk.drifthome.Drinker;
import com.ryvk.drifthome.DrinkerConfig;
import com.ryvk.drifthome.MainActivity;
import com.ryvk.drifthome.R;
import com.ryvk.drifthome.databinding.FragmentSettingsBinding;

import java.util.HashMap;

public class SettingsFragment extends Fragment {

    private static final String TAG = "SettingsFragment";
    HashMap<String,Object> updatingPropertyMap = new HashMap<>();

    private FragmentSettingsBinding binding;
    private DrinkerConfig loggedDrinkerConfig;
    private Drinker loggedDrinker;
    private LogoutListener logoutListener;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        SettingsViewModel settingsViewModel =
                new ViewModelProvider(this).get(SettingsViewModel.class);

        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        loggedDrinkerConfig = DrinkerConfig.getSPDrinkerConfig(getContext());
        loggedDrinker = Drinker.getSPDrinker(getContext());

        Switch additionalCharges = binding.switch1;
        Switch shakeToBook = binding.switch3;
        Switch autoClose = binding.switch4;
        Switch voiceNotifications = binding.switch5;
        Switch alwaysHome = binding.switch6;

        if (loggedDrinkerConfig.isAdditional_charges()) additionalCharges.setChecked(true);
        if (loggedDrinkerConfig.isShake_to_book()) shakeToBook.setChecked(true);
        if (loggedDrinkerConfig.isAuto_close()) autoClose.setChecked(true);
        if (loggedDrinkerConfig.isVoice_notifications()) voiceNotifications.setChecked(true);
        if (loggedDrinkerConfig.isAlways_home()) alwaysHome.setChecked(true);

        additionalCharges.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            if (isChecked) {
                loggedDrinkerConfig.setAdditional_charges(true);
            } else {
                loggedDrinkerConfig.setAdditional_charges(false);
            }

            updatingPropertyMap.put("additional_charges",loggedDrinkerConfig.isAdditional_charges());
            updateSettings();
        });

        shakeToBook.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            if (isChecked) {
                loggedDrinkerConfig.setShake_to_book(true);
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

        alwaysHome.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            if (isChecked) {
                loggedDrinkerConfig.setAlways_home(true);
            } else {
                loggedDrinkerConfig.setAlways_home(false);
            }

            updatingPropertyMap.put("always_home",loggedDrinkerConfig.isAlways_home());
            updateSettings();

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
                                Intent i = new Intent(getContext(), MainActivity.class);
                                startActivity(i);
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