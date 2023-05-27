package com.perception.persep.activities;

import android.app.ProgressDialog;
import android.content.Intent;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.perception.persep.constants.UserConstants;
import com.perception.persep.data_gatherer.UserManager;

import timber.log.Timber;

public class SettingsActivity extends AppCompatActivity
{
    private Button m_SignOutButton;
    private AlertDialog m_ConfirmSignOutDialog;

    private Button m_DeleteAccountButton;
    private AlertDialog m_ConfirmDeleteAccountDialog;

    private ImageButton m_BackButton;

    private ProgressDialog m_ProgressDialog;

    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        m_ConfirmSignOutDialog = new AlertDialog.Builder(this)
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
                .setNegativeButton("Cancel", null)
                .create();
        setupDialog(m_ConfirmSignOutDialog);


        m_SignOutButton = findViewById(R.id.button_sign_out);
        m_SignOutButton.setOnClickListener(v -> {
            m_ConfirmSignOutDialog.show();
        });

        m_ConfirmDeleteAccountDialog = new AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("Do you really want to delete your account?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    createProgressDialog("Deleting account...");

                    FirebaseUser loggedInUser = FirebaseAuth.getInstance().getCurrentUser();
                    if (loggedInUser != null) {
                        DocumentReference userDocRef = FirebaseFirestore.getInstance().collection(UserConstants.COLLECTION_NAME).document(loggedInUser.getUid());
                        userDocRef.delete().addOnSuccessListener(unused -> {
                            loggedInUser.delete().addOnSuccessListener(unused2 -> {
                                Timber.d("Deleted successful");
                            }).addOnFailureListener(e -> {
                                Timber.d("Deleted failed: %s", e.toString());
                            });
                        });
                    }
                })
                .setNegativeButton("Cancel", null)
                .create();
        setupDialog(m_ConfirmDeleteAccountDialog);

        m_DeleteAccountButton = findViewById(R.id.button_delete_account);
        m_DeleteAccountButton.setOnClickListener(v -> {
            m_ConfirmDeleteAccountDialog.show();
        });

        m_BackButton = findViewById(R.id.button_back_settings);
        m_BackButton.setOnClickListener(v -> finish());

        FirebaseAuth.getInstance().addAuthStateListener(firebaseAuth -> {
            if (firebaseAuth.getCurrentUser() == null)
            {
                Toast.makeText(getApplicationContext(), "Signed out successfully", Toast.LENGTH_SHORT).show();
                onSignOutLaunchLogin();
            }
        });
    }

    private void setupDialog(AlertDialog alertDialog)
    {
        alertDialog.setOnShowListener(dialog -> {
            TextView messageTextview = alertDialog.findViewById(android.R.id.message);
            if (messageTextview != null)
            {
                messageTextview.setTypeface(ResourcesCompat.getFont(this, R.font.playfair_display_regular));
            }

            float size = getResources().getDimension(R.dimen.delete_post_button_text_size);
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
            alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
        });
    }

    private void createProgressDialog(String message)
    {
        m_ProgressDialog = new ProgressDialog(this);
        m_ProgressDialog.setMessage(message);
        m_ProgressDialog.show();
        m_ProgressDialog.setCancelable(false);
    }

    private void onSignOutLaunchLogin()
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
