package com.perception.persep.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import androidx.fragment.app.Fragment;

import com.example.persep.R;
import com.github.appintro.AppIntro2;
import com.github.appintro.AppIntroCustomLayoutFragment;
import com.github.appintro.AppIntroPageTransformerType;

import timber.log.Timber;

public class TutorialActivity extends AppIntro2
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        addSlide(AppIntroCustomLayoutFragment.newInstance(R.layout.tutorial_slide_welcome));
        addSlide(AppIntroCustomLayoutFragment.newInstance(R.layout.tutorial_slide_creating_a_post));
        addSlide(AppIntroCustomLayoutFragment.newInstance(R.layout.tutorial_slide_liking_a_post));
        addSlide(AppIntroCustomLayoutFragment.newInstance(R.layout.tutorial_slide_comment_in_a_post));
        addSlide(AppIntroCustomLayoutFragment.newInstance(R.layout.tutorial_slide_other_posts_in_album));

        setIndicatorColor(getColor(R.color.tutorial_selected_indicator_color), getColor(R.color.tutorial_unselected_indicator_color));

        setTransformer(AppIntroPageTransformerType.Depth.INSTANCE);
        showStatusBar(true);
        setScrollDurationFactor(2);
        setSystemBackButtonLocked(true);
        setWizardMode(true);
        setImmersive(true);
        setIndicatorEnabled(true);
        setButtonsEnabled(true);
    }

    @Override
    protected void onSkipPressed(Fragment currentFragment) {
        super.onSkipPressed(currentFragment);
        finish();
    }

    @Override
    protected void onDonePressed(Fragment currentFragment) {
        super.onDonePressed(currentFragment);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("show_tutorial", false);
        editor.apply();

        Intent intent = new Intent(TutorialActivity.this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
