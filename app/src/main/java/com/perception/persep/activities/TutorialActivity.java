package com.perception.persep.activities;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import com.example.persep.R;
import com.github.appintro.AppIntro2;
import com.github.appintro.AppIntroCustomLayoutFragment;
import com.github.appintro.AppIntroPageTransformerType;

public class TutorialActivity extends AppIntro2
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        addSlide(AppIntroCustomLayoutFragment.newInstance(R.layout.tutorial_slide_welcome));
        addSlide(AppIntroCustomLayoutFragment.newInstance(R.layout.tutorial_slide_liking_a_post));

        setIndicatorColor(getColor(R.color.persep_color), getColor(R.color.white));

        setVibrate(true);
        setVibrateDuration(50);
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
        finish();
    }
}
