package com.perception.persep.fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.example.persep.R;
import com.perception.persep.camera.CameraView;
import com.perception.persep.data_gatherer.DataGathererManager;
import com.perception.persep.render.RenderView;

public class VR_Fragment extends Fragment
{
    private static final String TAG = "VR_Fragment";

    public DataGathererManager m_DataGathererManager;

    private FrameLayout m_VRLayout;
    private RenderView m_RenderView;
    private CameraView m_CameraView;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        Log.d(TAG, "Created");
        super.onCreate(savedInstanceState);

        m_DataGathererManager = new DataGathererManager(getActivity());
    }

    @Override
    public void onStart()
    {
        Log.d(TAG, "Started");
        super.onStart();
        m_RenderView.onStart();
//        m_CameraView.onStart();
        m_DataGathererManager.onStart();
    }

    @Override
    public void onStop()
    {
        Log.d(TAG, "Stopped");
        super.onStop();
        m_RenderView.onStop();
//        m_CameraView.onStop();
        m_DataGathererManager.onStop();
    }

    @Override
    public void onPause()
    {
        Log.d(TAG, "Paused");
        super.onPause();
        m_RenderView.onPause();
//        m_CameraView.onPause();
        m_DataGathererManager.onPause();
    }

    @Override
    public void onResume()
    {
        Log.d(TAG, "Resumed");
        super.onResume();
        m_RenderView.onResume();
//        m_CameraView.onResume();
        m_DataGathererManager.onResume();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.vr_fragment, container, false);
        m_VRLayout = view.findViewById(R.id.vr_root_layout);

//        m_CameraView = new CameraView(getActivity());
//        m_VRLayout.addView(m_CameraView);

        m_RenderView = new RenderView(getActivity());
        m_VRLayout.addView(m_RenderView);
        m_RenderView.setOpaque(false);

        m_DataGathererManager.AddGyroscopeListener(m_RenderView);
        m_DataGathererManager.AddGPSListener(m_RenderView);
        m_DataGathererManager.AddAlbumListener(m_RenderView);

        return view;
    }
}