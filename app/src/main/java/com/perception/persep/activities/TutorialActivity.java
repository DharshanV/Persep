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
        addSlide(AppIntroCustomLayoutFragment.newInstance(R.layout.tutorial_slide_creating_a_post));
        addSlide(AppIntroCustomLayoutFragment.newInstance(R.layout.tutorial_slide_liking_a_post));
        addSlide(AppIntroCustomLayoutFragment.newInstance(R.layout.tutorial_slide_comment_in_a_post));
        addSlide(AppIntroCustomLayoutFragment.newInstance(R.layout.tutorial_slide_other_posts_in_album));

        setIndicatorColor(getColor(R.color.tutorial_selected_indicator_color), getColor(R.color.tutorial_unselected_indicator_color));

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
