package com.perception.persep.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.persep.R;
import com.perception.persep.firebase_helpers.FirebaseUserAuthentication;


public class LoginActivity extends AppCompatActivity
{
    private static final String TAG = "LoginActivity";

    private static final int HOME_ACTIVITY_DELAY_MS = 1000;

    // Widgets
    private EditText m_Email, m_Password;

    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        InitWidgets();
    }

    private void InitWidgets()
    {
        m_Email = findViewById(R.id.login_input_email);
        m_Password = findViewById(R.id.login_input_password);

        Button loginButton = findViewById(R.id.button_login);
        loginButton.setOnClickListener(v -> {
            String email = m_Email.getText().toString();
            String password = m_Password.getText().toString();
            FirebaseUserAuthentication.getInstance().LoginUser(email, password, (successful, user, message) -> {
                if (successful)
                {
                    Toast.makeText(getApplicationContext(), "Login successful. Loading...", Toast.LENGTH_LONG).show();
                    StartHomeActivity();
                } else
                {
                    Toast.makeText(getApplicationContext(), "Error logging in. Please try again...", Toast.LENGTH_LONG).show();
                }
            });
        });

        TextView signUpTextView = findViewById(R.id.textview_signup);
        signUpTextView.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            finish();
        });
    }

    private void StartHomeActivity()
    {
        Log.d(TAG, "Starting home activity.");
        new Handler().postDelayed(() -> {
            startActivity(new Intent(LoginActivity.this, HomeActivity.class));
            finish();
        }, HOME_ACTIVITY_DELAY_MS);
    }
}
