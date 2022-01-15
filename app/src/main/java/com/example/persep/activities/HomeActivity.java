package com.example.persep.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager2.widget.ViewPager2;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.view.Display;
import android.view.WindowManager;

import com.example.persep.R;
import com.example.persep.adapters.SlidePagerAdapter;

public class HomeActivity extends AppCompatActivity
{

    ViewPager2 m_ViewPager;
    SlidePagerAdapter m_SlidePagerAdapter;

    public static float DEFAULT_DISPLAY_REFRESH_RATE = 0.0f;
    private static final int REQUEST_CAMERA_PERMISSION = 1;

    @SuppressLint("WrongConstant")
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);

        Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        DEFAULT_DISPLAY_REFRESH_RATE = display.getRefreshRate();

        m_ViewPager = findViewById(R.id.ViewPager);
        m_ViewPager.setOffscreenPageLimit(SlidePagerAdapter.NUM_PAGES);

        FragmentManager fragmentManager = getSupportFragmentManager();
        m_SlidePagerAdapter = new SlidePagerAdapter(fragmentManager, getLifecycle());
        m_ViewPager.setAdapter(m_SlidePagerAdapter);
        m_ViewPager.setCurrentItem(SlidePagerAdapter.MIDDLE_PAGE_INDEX, false);
    }

    @Override
    public void onBackPressed()
    {
        int currentPageIndex = m_ViewPager.getCurrentItem();
        if (currentPageIndex == SlidePagerAdapter.MIDDLE_PAGE_INDEX)
        {
            super.onBackPressed();
            return;
        }
        m_ViewPager.setCurrentItem(currentPageIndex + (currentPageIndex < SlidePagerAdapter.MIDDLE_PAGE_INDEX ? 1 : -1));
    }
}