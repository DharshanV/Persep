package com.perception.persep.activities;

import android.app.Activity;
import android.os.Bundle;
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
import com.google.firebase.auth.FirebaseAuth;

import java.util.regex.Pattern;

public class SignInHelpActivity extends AppCompatActivity
{
    private TextInputLayout m_EmailLayout;
    private EditText m_Email;
    private Button m_SendResetEmailButton;

    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in_help);

        InitWidgets();
    }

    private void InitWidgets()
    {
        m_EmailLayout = findViewById(R.id.textinputlayout_sign_in_help_email);
        m_Email = findViewById(R.id.sign_in_help_input_email);
        m_SendResetEmailButton = findViewById(R.id.button_send_reset_email);

        Button backButton = findViewById(R.id.button_back_sign_in_help);
        backButton.setOnClickListener(v -> finish());

        m_SendResetEmailButton.setOnClickListener(v -> {
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
            if (inputMethodManager.isAcceptingText())
            {
                inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
            }

            String email = m_Email.getText().toString();

            if (CheckInput(email))
            {
                m_SendResetEmailButton.setEnabled(false);
                m_SendResetEmailButton.setText(R.string.button_sending_email_text);
                FirebaseAuth.getInstance().sendPasswordResetEmail(email).addOnSuccessListener(unused -> {
                    m_SendResetEmailButton.setEnabled(true);
                    m_SendResetEmailButton.setText(R.string.button_send_email_text);
                    Toast.makeText(getApplicationContext(), "Sent reset email", Toast.LENGTH_SHORT).show();
                }).addOnFailureListener(e -> {
                    m_SendResetEmailButton.setEnabled(true);
                    m_SendResetEmailButton.setText(R.string.button_send_email_text);
                    Toast.makeText(getApplicationContext(), "No user with that email", Toast.LENGTH_SHORT).show();
                });
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
    }


    private boolean CheckInput(String email)
    {
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
        return isEmailValid;
    }

}
