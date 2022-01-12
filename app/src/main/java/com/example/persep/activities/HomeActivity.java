package com.example.persep.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager2.widget.ViewPager2;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.view.Display;
import android.view.WindowManager;

import com.example.persep.R;
import com.example.persep.adapters.SlidePagerAdapter;

public class HomeActivity extends AppCompatActivity
{

    ViewPager2 viewPager;
    SlidePagerAdapter slidePagerAdapter;

    public static float DEFAULT_DISPLAY_REFRESH_RATE = 0.0f;

    @SuppressLint("WrongConstant")
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        DEFAULT_DISPLAY_REFRESH_RATE = display.getRefreshRate();

        viewPager = findViewById(R.id.ViewPager);
        viewPager.setOffscreenPageLimit(SlidePagerAdapter.NUM_PAGES);

        FragmentManager fragmentManager = getSupportFragmentManager();
        slidePagerAdapter = new SlidePagerAdapter(fragmentManager, getLifecycle());
        viewPager.setAdapter(slidePagerAdapter);
        viewPager.setCurrentItem(SlidePagerAdapter.MIDDLE_PAGE_INDEX, false);
    }

    @Override
    public void onBackPressed()
    {
        int currentPageIndex = viewPager.getCurrentItem();
        if (currentPageIndex == SlidePagerAdapter.MIDDLE_PAGE_INDEX)
        {
            super.onBackPressed();
            return;
        }
        viewPager.setCurrentItem(currentPageIndex + (currentPageIndex < SlidePagerAdapter.MIDDLE_PAGE_INDEX ? 1 : -1));
    }
}