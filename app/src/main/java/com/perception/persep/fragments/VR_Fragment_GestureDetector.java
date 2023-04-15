package com.perception.persep.fragments;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;

public class VR_Fragment_GestureDetector extends GestureDetector
{
    public interface Listener
    {
        void onUp(MotionEvent ev);
    }

    Listener m_VRFragmentGestureListener;

    public VR_Fragment_GestureDetector(Context context, OnGestureListener listener)
    {
        super(context, listener);
        init(listener);
    }

    void init(OnGestureListener listener)
    {
        if (listener instanceof Listener)
        {
            m_VRFragmentGestureListener = (Listener) listener;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev)
    {
        if (ev.getAction() == MotionEvent.ACTION_UP && m_VRFragmentGestureListener != null)
        {
            m_VRFragmentGestureListener.onUp(ev);
        }
        return super.onTouchEvent(ev);
    }
}