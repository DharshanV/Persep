package com.perception.persep.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.persep.R;
import com.google.android.material.textfield.TextInputLayout;
import com.perception.persep.firebase_helpers.FirebaseUserAuthentication;

import java.util.regex.Pattern;


public class LoginActivity extends AppCompatActivity
{
    private static final String TAG = "LoginActivity";

    private static final int HOME_ACTIVITY_DELAY_MS = 1000;

    // Widgets
    private TextInputLayout m_EmailLayout, m_PasswordLayout;
    private EditText m_Email, m_Password;
    private Button m_LoginButton;

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
        m_EmailLayout = findViewById(R.id.login_emailTextInputLayout);
        m_PasswordLayout = findViewById(R.id.login_passwordTextInputLayout);

        m_LoginButton = findViewById(R.id.button_login);
        m_LoginButton.setOnClickListener(v -> {
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
            if (inputMethodManager.isAcceptingText())
            {
                inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
            }

            String email = m_Email.getText().toString();
            String password = m_Password.getText().toString();
            if (CheckInputs(email, password))
            {
                m_LoginButton.setEnabled(false);
                m_LoginButton.setText(R.string.button_loading_text);
                hideKeyboard();

                FirebaseUserAuthentication.getInstance().LoginUser(email, password, (successful, user, message) -> {
                    if (successful)
                    {
                        StartHomeActivity();
                    } else
                    {
                        m_LoginButton.setEnabled(true);
                        m_LoginButton.setText(R.string.login_button_login_text);
                        Toast.makeText(getApplicationContext(), "Error logging in. Please try again...", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        TextView signUpTextView = findViewById(R.id.textview_signup);
        signUpTextView.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            hideKeyboard();
        });

        m_Email.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after)
            {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {
                m_EmailLayout.setError(null);
                m_EmailLayout.setErrorEnabled(false);
            }

            @Override
            public void afterTextChanged(Editable s)
            {
            }
        });
        m_Password.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after)
            {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {
                m_PasswordLayout.setError(null);
                m_PasswordLayout.setErrorEnabled(false);
            }

            @Override
            public void afterTextChanged(Editable s)
            {
            }
        });
    }

    private boolean CheckInputs(String email, String password)
    {
        // Email
        boolean isEmailValid = false;
        if (!email.isEmpty())
        {
            final String regexPattern = "^(?=.{1,64}@)[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*@"
                    + "[^-][A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$";
            isEmailValid = Pattern.compile(regexPattern).matcher(email).matches();
        }
        if (!isEmailValid)
        {
            m_EmailLayout.setError("Invalid email");
            m_EmailLayout.setErrorEnabled(true);
        }

        // Password
        boolean isPasswordValid = !password.isEmpty();
        if (!isPasswordValid)
        {
            m_PasswordLayout.setError("Invalid password");
            m_PasswordLayout.setErrorEnabled(true);
        }

        return isEmailValid && isPasswordValid;
    }

    private void StartHomeActivity()
    {
        new Handler().postDelayed(() -> {
            startActivity(new Intent(LoginActivity.this, HomeActivity.class));
            finish();
        }, HOME_ACTIVITY_DELAY_MS);
    }

    private void hideKeyboard()
    {
    }
}
