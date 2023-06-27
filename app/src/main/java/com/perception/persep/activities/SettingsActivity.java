package com.perception.persep.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import com.example.persep.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.perception.persep.constants.UserConstants;
import com.perception.persep.data_gatherer.UserManager;

import timber.log.Timber;

public class SettingsActivity extends AppCompatActivity
{
    private AlertDialog m_ConfirmSignOutDialog;
    private AlertDialog m_ConfirmDeleteAccountDialog;
    private ProgressDialog m_ProgressDialog;

    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        m_ConfirmSignOutDialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Sign Out")
                .setMessage("Do you really want to sign out?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    createProgressDialog("Signing out...");
                    FirebaseUser loggedInUser = FirebaseAuth.getInstance().getCurrentUser();
                    if (loggedInUser != null)
                    {
                        FirebaseAuth.getInstance().signOut();
                    }
                })
                .setNegativeButton("Cancel", null).create();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            TextView usernameTextview= findViewById(R.id.account_settings_username);
            usernameTextview.setText(user.getDisplayName());

            TextView emailTextview= findViewById(R.id.account_settings_email);
            emailTextview.setText(user.getEmail());
        }

        Button signOutButton = findViewById(R.id.account_settings_sign_out_button);
        signOutButton.setOnClickListener(v -> {
            m_ConfirmSignOutDialog.show();
        });

        m_ConfirmDeleteAccountDialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Account")
                .setMessage("Do you really want to delete your account?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    createProgressDialog("Deleting account...");
                    UserManager.getInstance().deleteUser(successful -> {
                        if (!successful) {
                            Toast.makeText(getApplicationContext(), "Failed to delete account. Try again later.", Toast.LENGTH_SHORT).show();
                            if (m_ProgressDialog != null) {
                                m_ProgressDialog.dismiss();
                                m_ProgressDialog = null;
                            }
                        }
                    });
                })
                .setNegativeButton("Cancel", null).create();

        Button deleteAccountButton = findViewById(R.id.account_settings_delete_account_button);
        deleteAccountButton.setOnClickListener(v -> {
            m_ConfirmDeleteAccountDialog.show();
        });

        Button backButton = findViewById(R.id.button_back_settings);
        backButton.setOnClickListener(v -> finish());

        Button privacyPolityButton = findViewById(R.id.account_settings_privacy_policy_button);
        privacyPolityButton.setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.persep.app/privacy-policy"));
            startActivity(browserIntent);
        });

        Button termsOfServiceButton = findViewById(R.id.account_settings_terms_of_service_button);
        termsOfServiceButton.setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.persep.app/privacy-policy/terms-of-service"));
            startActivity(browserIntent);
        });

        FirebaseAuth.getInstance().addAuthStateListener(firebaseAuth -> {
            if (firebaseAuth.getCurrentUser() == null)
            {
                onInvalidCurrentUser();
            }
        });
    }

    private void createProgressDialog(String message)
    {
        m_ProgressDialog = new ProgressDialog(this);
        m_ProgressDialog.setMessage(message);
        m_ProgressDialog.show();
        m_ProgressDialog.setCancelable(false);
    }

    private void onInvalidCurrentUser()
    {
        if (m_ProgressDialog != null) {
            m_ProgressDialog.dismiss();
            m_ProgressDialog = null;
        }
        Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
}
