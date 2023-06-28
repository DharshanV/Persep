package com.perception.persep.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

import nu.aaro.gustav.passwordstrengthmeter.PasswordStrengthCalculator;
import nu.aaro.gustav.passwordstrengthmeter.PasswordStrengthLevel;
import nu.aaro.gustav.passwordstrengthmeter.PasswordStrengthMeter;

public class RegisterActivity extends AppCompatActivity
{
    private static final String TAG = "RegisterActivity";
    private static final int HOME_ACTIVITY_DELAY_MS = 1000;

    // Widgets
    private TextInputLayout m_EmailLayout, m_UsernameLayout;
    private EditText m_Email, m_Username, m_Password;
    private Button m_RegisterButton;
    private PasswordStrengthMeter m_PasswordStrengthMeter;
    private boolean m_AcceptPassword = false;

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
        m_PasswordStrengthMeter = findViewById(R.id.password_input_meter);

        m_UsernameLayout = findViewById(R.id.register_usernameTextInputLayout);
        m_EmailLayout = findViewById(R.id.register_emailTextInputLayout);

        m_RegisterButton.setOnClickListener(v -> {
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
            if (inputMethodManager.isAcceptingText())
            {
                inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
            }

            String username = m_Username.getText().toString();
            String email = m_Email.getText().toString();
            String password = m_Password.getText().toString();

            if (CheckInputs(username, email) && m_AcceptPassword)
            {
                m_RegisterButton.setEnabled(false);
                m_RegisterButton.setText(R.string.button_loading_text);
                FirebaseUserAuthentication.getInstance().RegisterNewUser(username, email, password, (successful, message) -> {
                    if (successful)
                    {
                        SharedPreferences sharedPreferences = getPreferences(Context.MODE_PRIVATE);
                        boolean showTutorial = sharedPreferences.getBoolean("show_tutorial", true);
                        startActivity(showTutorial ? TutorialActivity.class : HomeActivity.class);
                    } else
                    {
                        m_RegisterButton.setEnabled(true);
                        m_RegisterButton.setText(R.string.register_button_register_text);
                        Toast.makeText(getApplicationContext(), "Error registering. Please try again...", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        Button backButton = findViewById(R.id.button_back_register);
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

        m_PasswordStrengthMeter.setEditText(m_Password);
        m_PasswordStrengthMeter.setPasswordStrengthCalculator(new PasswordStrengthCalculator()
        {
            @Override
            public int calculatePasswordSecurityLevel(String password)
            {
                int level = 0;
                int minLength = getMinimumLength();

                // Checks if password meets minimum length
                if (password.length() < minLength) {
                    return 0;
                } else {
                    ++level;
                }

                // Give level for extra long password
                if (password.length() >= (minLength * (3.0 / 2))) ++level;

                boolean hasUppercase = !password.equals(password.toLowerCase());
                boolean hasLowercase = !password.equals(password.toUpperCase());

                // Give level for password containing both upper and lower case letters
                if (hasLowercase && hasUppercase) ++level;

                // Give level for password containing digits
                boolean hasNumber = password.matches(".*\\d.*");
                if (hasNumber) ++level;

                // Give level for password containing special characters
                boolean hasSpecial = password.matches(".*[!@#€£©§|≈$%^&*].*");
                if (hasSpecial) ++level;

                m_AcceptPassword = false;

                return level;
            }

            @Override
            public int getMinimumLength()
            {
                return 6;
            }

            @Override
            public boolean passwordAccepted(int level)
            {
                return level >= 3;
            }

            @Override
            public void onPasswordAccepted(String password)
            {
                m_AcceptPassword = true;
            }
        });
        m_PasswordStrengthMeter.setStrengthLevels(new PasswordStrengthLevel[]{
                new PasswordStrengthLevel("Too short", android.R.color.darker_gray), // level 0
                new PasswordStrengthLevel("Weak", android.R.color.holo_red_dark), // level 1
                new PasswordStrengthLevel("Fair", android.R.color.holo_orange_dark), // level 2
                new PasswordStrengthLevel("Good", android.R.color.holo_orange_light), // level 3
                new PasswordStrengthLevel("Strong", android.R.color.holo_green_dark), // level 4
                new PasswordStrengthLevel("Very strong", android.R.color.holo_green_light)} // level 5)
        );
    }

    private boolean CheckInputs(String username, String email)
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

        return isUsernameValid && isEmailValid;
    }

    private void startActivity(Class<?> activity)
    {
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(RegisterActivity.this, activity);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        }, HOME_ACTIVITY_DELAY_MS);
    }
}
