package com.perception.persep.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.splashscreen.SplashScreen;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.perception.persep.constants.UserConstants;

public class SplashScreenActivity extends Activity
{
    private static final String TAG = "SplashScreen";

    private Class<?> m_LaunchActivityClass = null;
    private boolean m_LaunchedActivity = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);

        final View content = findViewById(android.R.id.content);
        content.getViewTreeObserver().addOnPreDrawListener(() -> {
            if (m_LaunchActivityClass != null && !m_LaunchedActivity)
            {
                m_LaunchedActivity = true;
                new Handler().postDelayed(() -> {
                    startActivity(new Intent(SplashScreenActivity.this, m_LaunchActivityClass));
                    finish();
                }, 1000);
                return true;
            }
            return false;
        });

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null)
        {
            FirebaseFirestore.getInstance().collection(UserConstants.COLLECTION_NAME).document(user.getUid()).get().addOnSuccessListener(documentSnapshot -> {
                Log.d(TAG, "Has authenticated user. Starting Home activity.");
                m_LaunchActivityClass = HomeActivity.class;
            }).addOnFailureListener(e -> {
                Log.d(TAG, "No authenticated user. Staring login activity.");
                m_LaunchActivityClass = LoginActivity.class;
            });
        } else
        {
            Log.d(TAG, "No authenticated user. Staring login activity.");
            m_LaunchActivityClass = LoginActivity.class;
        }
    }
}