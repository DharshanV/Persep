package com.perception.persep.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.splashscreen.SplashScreen;

import com.example.persep.BuildConfig;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.perception.persep.constants.UserConstants;

import timber.log.Timber;

public class SplashScreenActivity extends Activity
{
    private static final String TAG = "SplashScreen";

    private static final int SPLASH_SCREEN_ANIMATION_TIME = 1000;
    private Class<?> m_LaunchActivityClass = null;
    private boolean m_LaunchedActivity = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        if (BuildConfig.DEBUG)
        {
            Timber.plant(new Timber.DebugTree() {
                @Nullable
                @Override
                protected String createStackElementTag(@NonNull StackTraceElement element)
                {
                    return "Persep-" + super.createStackElementTag(element) + "[" + element.getLineNumber() + "]";
                }
            });
        }

        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);

        final View content = findViewById(android.R.id.content);
        content.getViewTreeObserver().addOnPreDrawListener(() -> {
            if (m_LaunchActivityClass != null && !m_LaunchedActivity)
            {
                m_LaunchedActivity = true;
                startActivity(new Intent(SplashScreenActivity.this, m_LaunchActivityClass));
                finish();
                return true;
            }
            return false;
        });

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null)
        {
            long currTime = SystemClock.elapsedRealtime();
            FirebaseFirestore.getInstance().collection(UserConstants.COLLECTION_NAME).document(user.getUid()).get().addOnSuccessListener(documentSnapshot -> {
                long delayTime = Math.max(SPLASH_SCREEN_ANIMATION_TIME - (SystemClock.elapsedRealtime() - currTime), 0);
                new Handler().postDelayed(() -> m_LaunchActivityClass = HomeActivity.class, delayTime);
            }).addOnFailureListener(e -> {
                long delayTime = Math.max(SPLASH_SCREEN_ANIMATION_TIME - (SystemClock.elapsedRealtime() - currTime), 0);
                new Handler().postDelayed(() -> m_LaunchActivityClass = LoginActivity.class, delayTime);
            });
        } else
        {
            new Handler().postDelayed(() -> m_LaunchActivityClass = LoginActivity.class, SPLASH_SCREEN_ANIMATION_TIME);
        }
    }

}