package com.perception.persep.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;

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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        if (BuildConfig.DEBUG)
        {
            Timber.plant(new Timber.DebugTree()
            {
                @Nullable
                @Override
                protected String createStackElementTag(@NonNull StackTraceElement element)
                {
                    return "Persep-" + super.createStackElementTag(element) + "[" + element.getLineNumber() + "]";
                }
            });
        }

        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);

        splashScreen.setKeepOnScreenCondition(() -> true);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null)
        {
            long currTime = SystemClock.elapsedRealtime();
            FirebaseFirestore.getInstance().collection(UserConstants.COLLECTION_NAME).document(user.getUid()).get().addOnSuccessListener(documentSnapshot -> {
                long delayTime = Math.max(SPLASH_SCREEN_ANIMATION_TIME - (SystemClock.elapsedRealtime() - currTime), 0);
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
//                boolean showTutorial = sharedPreferences.getBoolean("show_tutorial", true);
                boolean showTutorial = false;
                startActivity(showTutorial ? TutorialActivity.class : HomeActivity.class, delayTime);
            }).addOnFailureListener(e -> {
                long delayTime = Math.max(SPLASH_SCREEN_ANIMATION_TIME - (SystemClock.elapsedRealtime() - currTime), 0);
                startActivity(LoginActivity.class, delayTime);
            });
        } else
        {
            startActivity(LoginActivity.class, SPLASH_SCREEN_ANIMATION_TIME);
        }
    }

    private void startActivity(Class<?> activity, long delay)
    {
        new Handler().postDelayed(() -> {
            startActivity(new Intent(SplashScreenActivity.this, activity));
            finish();
        }, delay);
    }

}