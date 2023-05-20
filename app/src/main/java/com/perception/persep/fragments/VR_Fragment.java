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
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.example.persep.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraOptions;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.PictureResult;
import com.perception.persep.album_data.Album;
import com.perception.persep.album_data.Post;
import com.perception.persep.data_gatherer.AlbumManager;
import com.perception.persep.data_gatherer.DataGathererManager;
import com.perception.persep.data_gatherer.PostManager;
import com.perception.persep.firebase_helpers.FirebaseUpdates;
import com.perception.persep.render.RenderView;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.io.IOException;

public class VR_Fragment extends Fragment
{
    private static final String TAG = "VR_Fragment";

    public DataGathererManager m_DataGathererManager;

    private RelativeLayout m_VRLayout;
    private RenderView m_VRRenderView;
    private boolean m_IsVRInitialized = false;
    private boolean m_PrevIsVREnabled = false;
    private boolean m_IsVREnabled = false;

    private CameraView m_CameraView;
    private ActivityResultLauncher<Intent> m_UCropActivityResultLauncher = null;

    // widgets
    private ImageButton m_TakePhotoButton = null;
    private ImageButton m_ToggleVRButton = null;
    private ImageButton m_ToggleCameraButton = null;
    private boolean m_IsBackCamera = true;

    GestureDetector m_GestureDetector;
    ViewPager2 m_ViewPager;
    private boolean m_IsInteractingWithPost = false;
    private String m_InteractingWithAlbum = null;
    private int m_ViewWidth = 0;
    private int m_ViewHeight = 0;

    public VR_Fragment(ViewPager2 viewPager)
    {
        m_ViewPager = viewPager;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        Log.d(TAG, "Created");
        super.onCreate(savedInstanceState);
    }

    private void TryInitializeFragment()
    {
        String[] manifestPermissions = GetAllPermission();
        if (HasAllPermissions(manifestPermissions))
        {
            InitializeFragment(false);
            return;
        }

        ActivityResultLauncher<String[]> multiplePermissionResultLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), isGranted -> {
            Log.d(TAG, "Permission launcher result: " + isGranted.toString());
            if (isGranted.containsValue(false))
            {
                Log.d(TAG, "At least one of the permissions was not granted");
                Toast.makeText(getActivity(), "Please enable all required permission for the AR feature.", Toast.LENGTH_LONG).show();
            } else
            {
                Log.d(TAG, "All permissions granted...");
                InitializeFragment(true);
            }
        });

        Log.d(TAG, "Launching multiple contract permission launcher for ALL required permissions");
        multiplePermissionResultLauncher.launch(manifestPermissions);
    }

    private void InitializeFragment(boolean forceStart)
    {
        if (m_VRLayout == null)
        {
            Log.e(TAG, "Null VR layout");
            return;
        }

        m_IsVRInitialized = true;
        m_IsVREnabled = true;
        m_PrevIsVREnabled = true;
        m_ToggleVRButton.setEnabled(true);
        m_ToggleVRButton.setImageResource(R.drawable.ic_ar_enabled_button);

        m_DataGathererManager = new DataGathererManager(getActivity());
        m_VRRenderView = new RenderView(getActivity());

        m_VRLayout.addView(m_VRRenderView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        m_VRRenderView.setOpaque(false);

        m_DataGathererManager.SetGyroscopeListener(m_VRRenderView);
        m_DataGathererManager.AddGPSListener(m_VRRenderView);
        m_DataGathererManager.AddGPSListener(AlbumManager.getInstance());
//        m_DataGathererManager.SetAlbumListener(m_VRRenderView);

        if (forceStart)
        {
            if (m_VRRenderView != null) m_VRRenderView.onStart();
            if (m_DataGathererManager != null) m_DataGathererManager.onStart();
        }
    }

    @Override
    public void onStart()
    {
        Log.d(TAG, "Started");
        super.onStart();

        if (m_VRRenderView != null) m_VRRenderView.onStart();
        if (m_DataGathererManager != null) m_DataGathererManager.onStart();
    }

    @Override
    public void onStop()
    {
        Log.d(TAG, "Stopped");
        super.onStop();

        if (m_VRRenderView != null) m_VRRenderView.onStop();
        if (m_DataGathererManager != null) m_DataGathererManager.onStop();
    }

    @Override
    public void onPause()
    {
        Log.d(TAG, "Paused");
        super.onPause();

        if (m_VRRenderView != null) m_VRRenderView.onPause();
        if (m_DataGathererManager != null) m_DataGathererManager.onPause();
    }

    @Override
    public void onResume()
    {
        Log.d(TAG, "Resumed");
        super.onResume();

        if (m_VRRenderView != null) m_VRRenderView.onResume(m_IsVREnabled);
        if (m_DataGathererManager != null) m_DataGathererManager.onResume();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.vr_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        m_VRLayout = view.findViewById(R.id.vr_root_layout);

        m_GestureDetector = new VR_Fragment_GestureDetector(getActivity(), new VR_Fragment_GestureListener());

        InitWidgets(view);
        TryInitializeFragment();
    }

    @SuppressLint("ClickableViewAccessibility")
    void InitWidgets(View view)
    {
        m_CameraView = view.findViewById(R.id.otalia_camera_view);
        m_CameraView.setLifecycleOwner(getViewLifecycleOwner());
        m_CameraView.setOnTouchListener((v, event) -> {
            m_ViewWidth = v.getWidth();
            m_ViewHeight = v.getHeight();
            if (m_IsVREnabled)
            {
                return m_GestureDetector.onTouchEvent(event);
            }
            return true;
        });


        m_CameraView.addCameraListener(new CameraListener()
        {
            @Override
            public void onCameraOpened(@NonNull CameraOptions options)
            {
                m_ToggleCameraButton.setEnabled(true);
            }

            @Override
            public void onPictureTaken(@NonNull PictureResult result)
            {
                try
                {
                    File previewCroppedFile = File.createTempFile("cameraPreview-cropped", ".jpg");
                    result.toFile(previewCroppedFile, file -> {
                        Uri savedPreviewUri = Uri.fromFile(file);
                        Uri croppedPreviewUri = Uri.fromFile(previewCroppedFile);
                        Intent UCropIntent = UCrop.of(savedPreviewUri, croppedPreviewUri).withAspectRatio(16, 9).getIntent(getActivity());
                        m_UCropActivityResultLauncher.launch(UCropIntent);
                    });
                } catch (IOException e)
                {
                    Log.e(TAG, e.toString());
                }
            }
        });

        m_TakePhotoButton = view.findViewById(R.id.vr_button_take_photo);
        m_TakePhotoButton.setOnClickListener(v -> {
            if (m_CameraView != null && m_CameraView.isOpened() && !m_CameraView.isTakingPicture())
            {
                TakePhoto();
            }
        });

        m_ToggleVRButton = view.findViewById(R.id.toggle_vr_button);
        m_ToggleVRButton.setEnabled(false);
        m_ToggleVRButton.setOnClickListener(v -> ToggleVREnabled());

        m_ToggleCameraButton = view.findViewById(R.id.toggle_camera_button);
        m_ToggleCameraButton.setEnabled(false);
        m_ToggleCameraButton.setOnClickListener(v -> ToggleCamera());

        m_UCropActivityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    m_TakePhotoButton.setEnabled(true);
                    if (result.getData() == null) return;
                    if (result.getResultCode() == Activity.RESULT_OK)
                    {
                        Uri croppedImageUri = UCrop.getOutput(result.getData());
                        if (croppedImageUri != null)
                        {
                            FirebaseUser loggedInUser = FirebaseAuth.getInstance().getCurrentUser();
                            if (loggedInUser != null && m_DataGathererManager != null)
                            {
                                AlbumManager.getInstance().tryAddPost(croppedImageUri);
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

    private void ToggleVREnabled()
    {
        if (m_IsVRInitialized) {
            m_IsVREnabled = !m_IsVREnabled;
            m_ToggleVRButton.setImageResource(m_IsVREnabled ? R.drawable.ic_ar_enabled_button : R.drawable.ic_ar_disabled_button);
            if (m_VRRenderView != null) m_VRRenderView.ShouldRender(m_IsVREnabled);
            m_PrevIsVREnabled = m_IsVREnabled;
        }
    }

    private void ToggleCamera()
    {
        if (m_CameraView != null && m_CameraView.isOpened()) {
            m_IsBackCamera = !m_IsBackCamera;
            m_ToggleCameraButton.setImageResource(m_IsBackCamera ? R.drawable.ic_baseline_camera_rear_24 : R.drawable.ic_baseline_camera_front_24);
            m_CameraView.toggleFacing();
        }

        if (m_IsVRInitialized) {
            if (m_IsBackCamera) {
                m_ToggleVRButton.setEnabled(true);
                m_ToggleVRButton.setImageResource(m_PrevIsVREnabled ? R.drawable.ic_ar_enabled_button : R.drawable.ic_ar_disabled_button);
                if (m_VRRenderView != null) m_VRRenderView.ShouldRender(m_PrevIsVREnabled);
            }
            else {
                m_ToggleVRButton.setEnabled(false);
                m_ToggleVRButton.setImageResource(R.drawable.ic_ar_disabled_button);
                if (m_VRRenderView != null) m_VRRenderView.ShouldRender(false);
            }
        }
    }

    void TakePhoto()
    {
        m_TakePhotoButton.setEnabled(false);
        m_CameraView.takePicture();
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
                Log.d(TAG, "Permission granted: " + permission);
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

    private String rayCast(MotionEvent event)
    {
        if (m_VRRenderView != null)
        {
            float x = 2f * (event.getX() / m_ViewWidth) - 1f;
            float y = -2f * (event.getY() / m_ViewHeight) + 1f;
            return m_VRRenderView.rayCast(x, y);
        }
        return null;
    }

    class VR_Fragment_GestureListener extends GestureDetector.SimpleOnGestureListener implements VR_Fragment_GestureDetector.Listener
    {
        private double m_SwipeDistanceCovered = 0;
        private long m_SwipeTimeSpent = 0;

        @Override
        public boolean onDown(MotionEvent event)
        {
            m_InteractingWithAlbum = rayCast(event);
            if (m_InteractingWithAlbum != null)
            {
                m_ViewPager.setUserInputEnabled(false);
                m_IsInteractingWithPost = true;
            }
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent event)
        {
            m_InteractingWithAlbum = rayCast(event);
            if (m_InteractingWithAlbum != null)
            {
                Album album = AlbumManager.getInstance().getAlbum(m_InteractingWithAlbum);
                if (album != null) {
                    Post post = PostManager.getInstance().getPost(album.getCurrentPostID());
                    if (post != null) {
                        post.like(successful -> {
                            Toast.makeText(getContext(), "Liked post!", Toast.LENGTH_LONG).show();
                        });
                    }
                }
            }
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY)
        {
            if (m_IsInteractingWithPost)
            {
                m_SwipeDistanceCovered += distanceX;
                m_SwipeTimeSpent += e2.getEventTime() - e1.getEventTime();
            }
            return true;
        }

        @Override
        public void onUp(MotionEvent ev)
        {
            if (m_IsInteractingWithPost)
            {
                if (m_InteractingWithAlbum != null && m_SwipeTimeSpent != 0)
                {
                    double velocity = m_SwipeDistanceCovered / m_SwipeTimeSpent;
                    Album album = AlbumManager.getInstance().getAlbum(m_InteractingWithAlbum);
                    if (album != null)
                    {
                        album.updatePostIndex(velocity > 0);
                    }
                    m_SwipeDistanceCovered = 0;
                    m_SwipeTimeSpent = 0;
                }
                m_ViewPager.setUserInputEnabled(true);
                m_IsInteractingWithPost = false;
            }
        }
    }
}