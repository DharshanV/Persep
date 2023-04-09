package com.perception.persep.activities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager2.widget.ViewPager2;

import com.example.persep.R;
import com.perception.persep.adapters.SlidePagerAdapter;

public class HomeActivity extends AppCompatActivity
{
    private static final String TAG = "HomeActivity";

    ViewPager2 m_ViewPager;
    SlidePagerAdapter m_SlidePagerAdapter;
    GestureDetector m_GestureDetector;
    boolean m_SlideEnable = true;

    public static float DEFAULT_DISPLAY_REFRESH_RATE = 0.0f;

    @SuppressLint({"WrongConstant", "ClickableViewAccessibility"})
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        DEFAULT_DISPLAY_REFRESH_RATE = display.getRefreshRate();

        m_ViewPager = findViewById(R.id.ViewPager);
        m_ViewPager.setOffscreenPageLimit(SlidePagerAdapter.NUM_PAGES);

        FragmentManager fragmentManager = getSupportFragmentManager();
        m_SlidePagerAdapter = new SlidePagerAdapter(fragmentManager, getLifecycle());
        m_ViewPager.setAdapter(m_SlidePagerAdapter);
        m_ViewPager.setCurrentItem(SlidePagerAdapter.MIDDLE_PAGE_INDEX, false);
//        m_ViewPager.setUserInputEnabled(false);
//        m_ViewPager.setNestedScrollingEnabled(false);

//        m_GestureDetector = new GestureDetector(this, new MyGestureListener());
//        m_ViewPager.getChildAt(0).setOnTouchListener((v, event) -> {
//            int action  = event.getActionMasked();
//            if (action == MotionEvent.ACTION_UP) {
//                m_ViewPager.endFakeDrag();
//                return true;
//            }
//            return m_GestureDetector.onTouchEvent(event);
//        });
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

    class MyGestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDown(MotionEvent event) {
            Log.d(TAG,"onDown: ");
//            lastValue = event.getX();
            if (!m_ViewPager.isFakeDragging() && m_SlideEnable) {
                m_ViewPager.beginFakeDrag();
            }
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            Log.d(TAG, "onDoubleTap: ");
            m_SlideEnable = !m_SlideEnable;
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2,
                                float distanceX, float distanceY) {
            Log.d(TAG, "onScroll: ");
            if (m_SlideEnable)
            m_ViewPager.fakeDragBy(-distanceX);
            return true;
        }

        @Override
        public boolean onFling(MotionEvent event1, MotionEvent event2,
                               float velocityX, float velocityY) {
            Log.d(TAG, "onFling: ");
            return true;
        }
    }
}