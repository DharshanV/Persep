package com.perception.persep.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import com.example.persep.R;
import com.perception.persep.firebase_helpers.FirebaseUserAuthentication;
import com.perception.persep.models.User;


public class LoginActivity extends AppCompatActivity implements FirebaseUserAuthentication.UserLoginAuthListener
{
    private static final String TAG = "LoginActivity";

    private static final int HOME_ACTIVITY_DELAY_MS = 1000;

    // Widgets
    private EditText m_Email, m_Password;
    private Button m_LoginButton;
    private TextView m_SignUpTextView;

    // Firebase
    private FirebaseUserAuthentication m_UserAuth;

    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        m_UserAuth = FirebaseUserAuthentication.getInstance();
        m_UserAuth.m_UserLoginAuthListener = this;

        InitWidgets();
    }

    private void InitWidgets()
    {
        m_Email = findViewById(R.id.login_input_email);
        m_Password = findViewById(R.id.login_input_password);

        m_LoginButton = findViewById(R.id.button_login);
        m_LoginButton.setOnClickListener(v -> {
            String email = m_Email.getText().toString();
            String password = m_Password.getText().toString();
            m_UserAuth.LoginUser(email, password);
        });

        m_SignUpTextView = findViewById(R.id.textview_signup);
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

    @Override
    public void WasLoginSuccessful(boolean successful, User user, String message)
    {
        if (successful) {
            Log.d(TAG, "Login successfully: " + user.toString());
            StartHomeActivity();
        }
    }
}
