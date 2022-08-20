package com.perception.persep.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import com.example.persep.R;
import com.perception.persep.firebase_helpers.FirebaseUserAuthentication;

import java.util.regex.Pattern;


public class RegisterActivity extends AppCompatActivity implements FirebaseUserAuthentication.UserAuthListener
{
    private static final String TAG = "RegisterActivity";
    private static final int HOME_ACTIVITY_DELAY_MS = 1000;

    private Context m_Context;

    // Widgets
    private EditText m_Email, m_Username, m_Password;
    private TextView m_RegisterTextView;
    private Button m_RegisterButton;

    // Firebase
    private FirebaseUserAuthentication m_UserAuth;

    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        m_Context = RegisterActivity.this;
        m_UserAuth = new FirebaseUserAuthentication(m_Context);
        m_UserAuth.SetAuthListener(this);

        InitWidgets();
        Init();
    }

    @Override
    public void onStart()
    {
        super.onStart();
        m_UserAuth.onStart();
    }

    @Override
    public void onStop()
    {
        super.onStop();
        m_UserAuth.onStop();
    }

    private void InitWidgets()
    {
        Log.d(TAG, "Initializing Widgets.");
        m_Username = (EditText) findViewById(R.id.register_input_username);
        m_Email = (EditText) findViewById(R.id.register_input_email);
        m_Password = (EditText) findViewById(R.id.register_input_password);
        m_RegisterButton = (Button) findViewById(R.id.register_button);
        m_RegisterTextView = (TextView) findViewById(R.id.register_message);

        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {
                if (m_RegisterTextView.getVisibility() != View.GONE)
                    m_RegisterTextView.setVisibility(View.GONE);
            }

            @Override
            public void afterTextChanged(Editable s) { }
        };
        m_Username.addTextChangedListener(watcher);
        m_Email.addTextChangedListener(watcher);
        m_Password.addTextChangedListener(watcher);
    }

    private void Init()
    {
        m_RegisterButton.setOnClickListener(v -> {
            String username = m_Username.getText().toString();
            String email = m_Email.getText().toString();
            String password = m_Password.getText().toString();

            DisplayMessage("Loading...");
            m_RegisterButton.setEnabled(false);
            if (CheckInputs(username, email, password))
            {
                m_UserAuth.RegisterNewUser(username, email, password);
            }
            else
            {
                m_RegisterButton.setEnabled(true);
            }
        });
    }

    private void DisplayMessage(String message)
    {
        m_RegisterTextView.setVisibility(View.VISIBLE);
        m_RegisterTextView.setText(message);
    }

    private boolean CheckInputs(String username, String email, String password)
    {
        // Username
        boolean isUsernameValid = false;
        if (!username.isEmpty())
        {
            final String regexPattern = "^(?=.{5,20}$)(?![_.])(?!.*[_.]{2})[a-zA-Z0-9._]+(?<![_.])$";
            isUsernameValid = Pattern.compile(regexPattern).matcher(username).matches();
        }
        if (!isUsernameValid) {  DisplayMessage("Invalid username or already exists"); return false;  }

        // Email
        boolean isEmailValid = false;
        if (!email.isEmpty())
        {
            final String regexPattern = "^(?=.{1,64}@)[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*@"
                    + "[^-][A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$";
            isEmailValid = Pattern.compile(regexPattern).matcher(email).matches();
        }
        if (!isEmailValid) { DisplayMessage("Invalid email or already exists"); return false; }

        // Password
        boolean isPasswordValid = !password.isEmpty() && password.length() >= 5;
        if (!isPasswordValid) { DisplayMessage("Invalid password"); return false; }

        return true;
    }

    @Override
    public void WasAuthSuccessful(boolean successful, String message)
    {
        if (successful)
        {
            DisplayMessage("Successful! Logging in...");
//            StartHomeActivity();
        }
        else
        {
            DisplayMessage("Error: " + message);
            m_RegisterButton.setEnabled(true);
        }
    }

    private void StartHomeActivity()
    {
        Log.d(TAG, "Starting home activity.");
        new Handler().postDelayed(() -> {
            startActivity(new Intent(RegisterActivity.this, HomeActivity.class));
            finish();
        }, HOME_ACTIVITY_DELAY_MS);
    }
}
