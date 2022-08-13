package com.perception.persep.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import com.example.persep.R;


public class LoginActivity extends AppCompatActivity
{
    private static final String TAG = "LoginActivity";

    private static final int HOME_ACTIVITY_DELAY_MS = 1000;

    private Button m_LoginButton;
    private TextView m_SignUpTextView;

    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        m_LoginButton = (Button) findViewById(R.id.button_login);
        m_LoginButton.setOnClickListener(v -> {
            StartHomeActivity();
        });

        m_SignUpTextView = (TextView) findViewById(R.id.textview_signup);
        m_SignUpTextView.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
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
