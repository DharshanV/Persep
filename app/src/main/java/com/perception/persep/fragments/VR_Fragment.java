package com.perception.persep.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.example.persep.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.perception.persep.camera.CameraView;
import com.perception.persep.data_gatherer.DataGathererManager;
import com.perception.persep.render.RenderView;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.io.IOException;

public class VR_Fragment extends Fragment
{
    private static final String TAG = "VR_Fragment";

    public DataGathererManager m_DataGathererManager;

    private FrameLayout m_VRLayout;
    private RenderView m_RenderView;
    private CameraView m_CameraView;
    private ActivityResultLauncher<Intent> m_UCropActivityResultLauncher = null;

    // widgets
    private FloatingActionButton m_TakePhotoButton = null;

    GestureDetector m_GestureDetector;
    ViewPager2 m_ViewPager;
    private boolean m_EnableViewPager = true;

    public VR_Fragment(ViewPager2 viewPager)
    {
        m_ViewPager = viewPager;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        Log.d(TAG, "Created");
        super.onCreate(savedInstanceState);

        m_UCropActivityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getData() == null) return;
                    if (result.getResultCode() == Activity.RESULT_OK)
                    {
                        Uri croppedImageUri = UCrop.getOutput(result.getData());
                        if (croppedImageUri != null)
                        {
                            FirebaseUser loggedInUser = FirebaseAuth.getInstance().getCurrentUser();
                            if (loggedInUser != null)
                            {
                                m_DataGathererManager.m_AlbumManger.AddPost(loggedInUser, croppedImageUri);
                            } else
                            {
                                Log.e(TAG, "Unable to find logged in user!");
                            }
                        } else
                        {
                            Log.e(TAG, "Cropped image uri is null!");
                        }
                    } else
                    {
                        Log.e(TAG, UCrop.getError(result.getData()).toString());
                    }
                });
    }

    private void TryInitializeFragment()
    {
        String[] manifestPermissions = GetAllPermission();
        if (HasAllPermissions(manifestPermissions))
        {
            InitializeFragment();
            return;
        }
        ActivityResultLauncher<String[]> multiplePermissionResultLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), isGranted -> {
            Log.d(TAG, "Permission launcher result: " + isGranted.toString());
            if (isGranted.containsValue(false))
            {
                Log.d(TAG, "At least one of the permissions was not granted");
            } else
            {
                Log.d(TAG, "All permissions granted...");
                InitializeFragment();
            }
        });

        Log.d(TAG, "Launching multiple contract permission launcher for ALL required permissions");
        multiplePermissionResultLauncher.launch(manifestPermissions);
    }

    private void InitializeFragment()
    {
        if (m_VRLayout == null)
        {
            Log.e(TAG, "Null VR layout");
            return;
        }

        m_CameraView = new CameraView(getActivity());
        m_VRLayout.addView(m_CameraView);

        m_RenderView = new RenderView(getActivity());
        m_VRLayout.addView(m_RenderView);
        m_RenderView.setOpaque(false);

        m_DataGathererManager = new DataGathererManager(getActivity());
        m_DataGathererManager.AddGyroscopeListener(m_RenderView);
        m_DataGathererManager.AddGPSListener(m_RenderView);
        m_DataGathererManager.AddGPSListener(m_DataGathererManager.m_AlbumManger);
        m_DataGathererManager.AddAlbumListener(m_RenderView);
    }

    @Override
    public void onStart()
    {
        Log.d(TAG, "Started");
        super.onStart();

        if (m_RenderView != null) m_RenderView.onStart();
        if (m_CameraView != null) m_CameraView.onStart();
        if (m_DataGathererManager != null) m_DataGathererManager.onStart();
    }

    @Override
    public void onStop()
    {
        Log.d(TAG, "Stopped");
        super.onStop();

        if (m_RenderView != null) m_RenderView.onStop();
        if (m_CameraView != null) m_CameraView.onStop();
        if (m_DataGathererManager != null) m_DataGathererManager.onStop();
    }

    @Override
    public void onPause()
    {
        Log.d(TAG, "Paused");
        super.onPause();

        if (m_RenderView != null) m_RenderView.onPause();
        if (m_CameraView != null) m_CameraView.onPause();
        if (m_DataGathererManager != null) m_DataGathererManager.onPause();
    }

    @Override
    public void onResume()
    {
        Log.d(TAG, "Resumed");
        super.onResume();

        if (m_RenderView != null) m_RenderView.onResume();
        if (m_CameraView != null) m_CameraView.onResume();
        if (m_DataGathererManager != null) m_DataGathererManager.onResume();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.vr_fragment, container, false);
        m_VRLayout = view.findViewById(R.id.vr_root_layout);

        m_GestureDetector = new GestureDetector(getActivity(), new MyGestureListener());
        m_VRLayout.setOnTouchListener((v, event) -> m_GestureDetector.onTouchEvent(event));

        InitWidgets(view);
        TryInitializeFragment();
        return view;
    }

    void InitWidgets(View view)
    {
        m_TakePhotoButton = view.findViewById(R.id.vr_button_take_photo);
        m_TakePhotoButton.setOnClickListener(v -> {
            if (m_CameraView != null) TakePhoto();
        });
    }

    void TakePhoto()
    {
        m_CameraView.TakeAndSavePreview(file -> {
            try
            {
                File previewCroppedFile = File.createTempFile("cameraPreview-cropped", ".jpg");
                Uri savedPreviewUri = Uri.fromFile(file);
                Uri croppedPreviewUri = Uri.fromFile(previewCroppedFile);
                Intent UCropIntent = UCrop.of(savedPreviewUri, croppedPreviewUri).withAspectRatio(16, 9).getIntent(getActivity());
                m_UCropActivityResultLauncher.launch(UCropIntent);
            } catch (IOException e)
            {
                Log.e(TAG, e.toString());
            }
        });
    }

    private boolean HasAllPermissions(String[] permissions)
    {
        if (permissions != null)
        {
            for (String permission : permissions)
            {
                if (ActivityCompat.checkSelfPermission(getActivity(), permission) != PackageManager.PERMISSION_GRANTED)
                {
                    Log.d(TAG, "Permission is not granted: " + permission);
                    return false;
                }
                Log.d(TAG, "Permission already granted: " + permission);
            }
        }
        return true;
    }

    private String[] GetAllPermission()
    {
        try
        {
            Context context = getActivity();
            return context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), PackageManager.GET_PERMISSIONS)
                    .requestedPermissions;
        } catch (PackageManager.NameNotFoundException e)
        {
            throw new RuntimeException("Unable to get manifest permission", e);
        }
    }

    class MyGestureListener extends GestureDetector.SimpleOnGestureListener
    {
        @Override
        public boolean onDown(MotionEvent event) {
//            Log.d(TAG,"onDown: ");
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e)
        {
            Log.d(TAG, "onDoubleTap: ");
            m_EnableViewPager = !m_EnableViewPager;
            m_ViewPager.setUserInputEnabled(m_EnableViewPager);
            return true;
        }
//
//        @Override
//        public boolean onScroll(MotionEvent e1, MotionEvent e2,
//                                float distanceX, float distanceY) {
//            Log.d(TAG, "onScroll: ");
//            return true;
//        }
//
//        @Override
//        public boolean onFling(MotionEvent event1, MotionEvent event2,
//                               float velocityX, float velocityY) {
//            Log.d(TAG, "onFling: ");
//            return true;
//        }
    }
}