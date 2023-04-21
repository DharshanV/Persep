package com.perception.persep.activities;

import android.content.Context;
import android.os.Bundle;
import android.view.Display;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.persep.R;
import com.perception.persep.adapters.SlidePagerAdapter;

import java.lang.reflect.Field;

public class HomeActivity extends AppCompatActivity
{
    private static final String TAG = "HomeActivity";

    public static HomeActivity m_HomeActivityInstance;

    ViewPager2 m_ViewPager;
    SlidePagerAdapter m_SlidePagerAdapter;

    public static float DEFAULT_DISPLAY_REFRESH_RATE = 0.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        m_HomeActivityInstance = this;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        DEFAULT_DISPLAY_REFRESH_RATE = display.getRefreshRate();

        m_ViewPager = findViewById(R.id.ViewPager);
        m_ViewPager.setOffscreenPageLimit(SlidePagerAdapter.NUM_PAGES);

        FragmentManager fragmentManager = getSupportFragmentManager();
        m_SlidePagerAdapter = new SlidePagerAdapter(m_ViewPager, fragmentManager, getLifecycle());
        m_ViewPager.setAdapter(m_SlidePagerAdapter);
        m_ViewPager.setCurrentItem(SlidePagerAdapter.VR_FRAGMENT_INDEX, false);

        reduceDragSensitivity();
    }

    private void reduceDragSensitivity()
    {
        try
        {
            Field ff = ViewPager2.class.getDeclaredField("mRecyclerView");
            ff.setAccessible(true);
            RecyclerView recyclerView = (RecyclerView) ff.get(m_ViewPager);
            Field touchSlopField = RecyclerView.class.getDeclaredField("mTouchSlop");
            touchSlopField.setAccessible(true);
            Object touchSopObj = touchSlopField.get(recyclerView);
            if (touchSopObj != null)
            {
                int touchSlop = (int) touchSopObj;
                touchSlopField.set(recyclerView, touchSlop * 3);
            }
        } catch (NoSuchFieldException | IllegalAccessException e)
        {
            e.printStackTrace();
        }
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

    public static Context getHomeActivityContext()
    {
        return m_HomeActivityInstance.getApplicationContext();
    }
}