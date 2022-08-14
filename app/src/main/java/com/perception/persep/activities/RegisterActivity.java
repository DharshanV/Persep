package com.perception.persep.activities;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import com.example.persep.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.perception.persep.firebase_helpers.FirebaseUserAuthentication;

import java.util.regex.Pattern;


public class RegisterActivity extends AppCompatActivity
{
    private static final String TAG = "RegisterActivity";

    private Context m_Context;

    // Widgets
    private EditText m_Email, m_Username, m_Password;
    private TextView m_RegisterTextView;
    private Button m_RegisterButton;

    // Firebase
    private FirebaseAuth m_FirebaseAuth;
    private FirebaseAuth.AuthStateListener m_FirebaseAuthListener;
    private FirebaseUserAuthentication m_UserAuth;

    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        m_Context = RegisterActivity.this;
        m_UserAuth = new FirebaseUserAuthentication(m_Context);

        InitWidgets();
//        SetupFirebaseAuth();
        Init();
    }

    @Override
    public void onStart()
    {
        super.onStart();
        m_FirebaseAuth.addAuthStateListener(m_FirebaseAuthListener);
    }

    @Override
    public void onStop()
    {
        super.onStop();
        if (m_FirebaseAuthListener != null)
        {
            m_FirebaseAuth.removeAuthStateListener(m_FirebaseAuthListener);
        }
    }

    private void InitWidgets()
    {
        Log.d(TAG, "Initializing Widgets.");
        m_Username = (EditText) findViewById(R.id.register_input_username);
        m_Email = (EditText) findViewById(R.id.register_input_email);
        m_Password = (EditText) findViewById(R.id.register_input_password);
        m_RegisterButton = (Button) findViewById(R.id.register_button);
        m_RegisterTextView = (TextView) findViewById(R.id.register_message);

        View.OnKeyListener editTextOnKeyListener = (v, keyCode, event) -> {
            m_RegisterTextView.setVisibility(View.GONE);
            return false;
        };
        m_Username.setOnKeyListener(editTextOnKeyListener);
        m_Email.setOnKeyListener(editTextOnKeyListener);
    }

    private void SetupFirebaseAuth()
    {
        Log.d(TAG, "Setting up Firebase auth.");
        m_FirebaseAuth = FirebaseAuth.getInstance();
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        DatabaseReference databaseRef = firebaseDatabase.getReference();

        m_FirebaseAuthListener = firebaseAuth -> {
            FirebaseUser currentUser = firebaseAuth.getCurrentUser();
            if (currentUser != null)
            {
                Log.d(TAG, "onAuthStateChanged: User signed in.");
            }
            else
            {
                Log.d(TAG, "onAuthStateChanged: User signed out.");
            }
        };
    }

    private void Init()
    {
        m_RegisterButton.setOnClickListener(v -> {
            String username = m_Username.getText().toString();
            String email = m_Email.getText().toString();
            String password = m_Password.getText().toString();

            DisplayMessage("Loading...");
            if (CheckInputs(username, email, password))
            {
                if (m_UserAuth.RegisterNewUser(username, email, password)) {
                    DisplayMessage("Successful! Logging in...");
                }
            }
        });
    }

    private void DisplayError(String message)
    {
        m_RegisterTextView.setText(message);
    }

    private void DisplayMessage(String message)
    {
        m_RegisterTextView.setText(message);
    }

    private boolean CheckInputs(String username, String email, String password)
    {
        // Username
        boolean isUsernameValid = false;
        if (!username.isEmpty())
        {
            final String regexPattern = "^(?=.{5,20}$)(?![_.])(?!.*[_.]{2})[a-zA-Z0-9._]+(?<![_.])$";
            isUsernameValid = Pattern.compile(regexPattern).matcher(email).matches();
        }
        if (!isUsernameValid) {  DisplayError("Invalid username or already exists."); return false;  }

        // Email
        boolean isEmailValid = false;
        if (!email.isEmpty())
        {
            final String regexPattern = "^(?=.{1,64}@)[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*@"
                    + "[^-][A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$";
            isEmailValid = Pattern.compile(regexPattern).matcher(email).matches();
        }
        if (!isEmailValid) { DisplayError("Invalid email or already exists."); return false; }

        // Password
        boolean isPasswordValid = !password.isEmpty();
        if (!isPasswordValid) { DisplayError("Invalid password."); return false; }

        return true;
    }

}
