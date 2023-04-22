package com.perception.persep.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.persep.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SettingsActivity extends AppCompatActivity
{
    private boolean m_UserPressedSignOut = false;

    private Button m_SignOutButton;

    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        m_SignOutButton = findViewById(R.id.button_sign_out);
        m_SignOutButton.setOnClickListener(v -> {
            m_UserPressedSignOut = true;

            FirebaseUser loggedInUser = FirebaseAuth.getInstance().getCurrentUser();
            if (loggedInUser != null) {
                m_SignOutButton.setEnabled(false);
                m_SignOutButton.setText("Signing out...");
                FirebaseAuth.getInstance().signOut();
            }
        });

        ImageButton backButton = findViewById(R.id.button_back_settings);
        backButton.setOnClickListener(v -> finish());

        FirebaseAuth.getInstance().addAuthStateListener(firebaseAuth -> {
            if (firebaseAuth.getCurrentUser() == null) {
                Toast.makeText(getApplicationContext(), "Signed out successfully", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });
    }

    @Override
    public void onBackPressed()
    {
        if (!m_UserPressedSignOut) {
            super.onBackPressed();
        }
    }
}
