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
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.persep.R;
import com.google.android.material.textfield.TextInputLayout;
import com.perception.persep.firebase_helpers.FirebaseUserAuthentication;

import java.util.regex.Pattern;


public class RegisterActivity extends AppCompatActivity
{
    private static final String TAG = "RegisterActivity";
    private static final int HOME_ACTIVITY_DELAY_MS = 1000;

    // Widgets
    private TextInputLayout m_EmailLayout, m_UsernameLayout, m_PasswordLayout;
    private EditText m_Email, m_Username, m_Password;
    private Button m_RegisterButton;

    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        InitWidgets();
    }

    private void InitWidgets()
    {
        m_Username = findViewById(R.id.register_input_username);
        m_Email = findViewById(R.id.register_input_email);
        m_Password = findViewById(R.id.register_input_password);
        m_RegisterButton = findViewById(R.id.register_button);

        m_UsernameLayout = findViewById(R.id.register_usernameTextInputLayout);
        m_EmailLayout = findViewById(R.id.register_emailTextInputLayout);
        m_PasswordLayout = findViewById(R.id.register_passwordTextInputLayout);

        m_RegisterButton.setOnClickListener(v -> {
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
            if (inputMethodManager.isAcceptingText())
            {
                inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
            }

            String username = m_Username.getText().toString();
            String email = m_Email.getText().toString();
            String password = m_Password.getText().toString();

            if (CheckInputs(username, email, password))
            {
                m_RegisterButton.setEnabled(false);
                m_RegisterButton.setText(R.string.button_loading_text);
                hideKeyboard();
                FirebaseUserAuthentication.getInstance().RegisterNewUser(username, email, password, (successful, message) -> {
                    if (successful)
                    {
                        StartHomeActivity();
                    } else
                    {
                        m_RegisterButton.setEnabled(true);
                        m_RegisterButton.setText(R.string.register_button_register_text);
                        Toast.makeText(getApplicationContext(), "Error registering. Please try again...", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        ImageButton backButton = findViewById(R.id.button_back_register);
        backButton.setOnClickListener(v -> finish());

        m_Username.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after)
            {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {
                m_UsernameLayout.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s)
            {

            }
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
            }

            @Override
            public void afterTextChanged(Editable s)
            {

            }
        });
    }

    private boolean CheckInputs(String username, String email, String password)
    {
        // Username
        boolean isUsernameValid = false;
        if (!username.isEmpty())
        {
            final String regexPattern = "^(?=.{3,15}$)(?![_.])(?!.*[_.]{2})[a-zA-Z0-9._]+(?<![_.])$";
            isUsernameValid = Pattern.compile(regexPattern).matcher(username).matches();
        }
        if (!isUsernameValid)
        {
            m_UsernameLayout.setError("Invalid username. Must be between 3-15 characters.");
        }

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
        }

        // Password
        boolean isPasswordValid = !password.isEmpty() && password.length() >= 4;
        if (!isPasswordValid)
        {
            m_PasswordLayout.setError("Invalid password. Must be more than 4.");
        }

        return isUsernameValid && isEmailValid && isPasswordValid;
    }

    private void StartHomeActivity()
    {
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(RegisterActivity.this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        }, HOME_ACTIVITY_DELAY_MS);
    }

    private void hideKeyboard()
    {
    }
}
