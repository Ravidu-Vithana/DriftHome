package com.ryvk.drifthome.ui.settings;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.ryvk.drifthome.AlertUtils;
import com.ryvk.drifthome.MainActivity;
import com.ryvk.drifthome.R;
import com.ryvk.drifthome.databinding.FragmentSettingsBinding;

public class SettingsFragment extends Fragment {

    private static final String TAG = "SettingsFragment";

    private FragmentSettingsBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        SettingsViewModel settingsViewModel =
                new ViewModelProvider(this).get(SettingsViewModel.class);

        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        Switch additionalCharges = binding.switch1;
        Switch shakeToBook = binding.switch3;
        Switch autoClose = binding.switch4;
        Switch voiceNotifications = binding.switch5;
        Switch alwaysHome = binding.switch6;

//        if (loggedDrinker.getName() != null) namefield.setText(loggedDrinker.getName());

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
}