package com.perception.persep.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.persep.R;
import com.google.firebase.auth.FirebaseAuth;

public class SplashScreenActivity extends Activity
{
    private static final String TAG = "SplashScreen";

    private static final int START_ACTIVITY_DELAY = 2000;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        if (IsUserAuthenticated())
        {
            Log.d(TAG, "Has authenticated user. Starting Home activity.");
            StartActivity(HomeActivity.class);
        } else
        {
            Log.d(TAG, "No authenticated user. Staring login activity.");
            StartActivity(LoginActivity.class);
        }
    }

    private void StartActivity(Class<?> cls)
    {
        new Handler().postDelayed(() -> {
            startActivity(new Intent(SplashScreenActivity.this, cls));
            finish();
        }, START_ACTIVITY_DELAY);
    }

    private boolean IsUserAuthenticated()
    {
        return FirebaseAuth.getInstance().getCurrentUser() != null;
    }
}