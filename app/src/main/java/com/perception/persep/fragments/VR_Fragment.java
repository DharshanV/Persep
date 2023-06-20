package com.perception.persep.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
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
import com.otaliastudios.cameraview.controls.Facing;
import com.perception.persep.album_data.Album;
import com.perception.persep.album_data.Post;
import com.perception.persep.data_gatherer.AlbumManager;
import com.perception.persep.data_gatherer.BearingSensor;
import com.perception.persep.data_gatherer.DataGathererManager;
import com.perception.persep.data_gatherer.PostManager;
import com.perception.persep.render.RenderView;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.io.IOException;

import timber.log.Timber;

public class VR_Fragment extends Fragment implements BearingSensor.BearingSensorListener
{
    private static final String TAG = "VR_Fragment";

    public DataGathererManager m_DataGathererManager;
    GestureDetector m_GestureDetector;
    ViewPager2 m_ViewPager;
    private RelativeLayout m_VRLayout;
    private SlidingUpPanelLayout m_SlidingUpPanel;
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
    private boolean m_IsInteractingWithPost = false;
    private String m_InteractingWithAlbum = null;
    private int m_ViewWidth = 0;
    private int m_ViewHeight = 0;

    private int m_DeviceBearing = 0;
    private int m_PostBearing = 0;

    public VR_Fragment(ViewPager2 viewPager)
    {
        m_ViewPager = viewPager;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
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
            if (isGranted.containsValue(false))
            {
                Toast.makeText(getActivity(), "Please enable all required permission for the AR feature.", Toast.LENGTH_SHORT).show();
            } else
            {
                InitializeFragment(true);
            }
        });

        multiplePermissionResultLauncher.launch(manifestPermissions);
    }

    private void InitializeFragment(boolean forceStart)
    {
        if (m_VRLayout == null)
        {
            return;
        }

//        m_IsVRInitialized = true;
//        m_IsVREnabled = true;
//        m_PrevIsVREnabled = false;
//        m_ToggleVRButton.setEnabled(true);
//        m_ToggleVRButton.setImageResource(R.drawable.ic_ar_enabled_button);
//
//        m_DataGathererManager = new DataGathererManager(getActivity());
//        m_VRRenderView = new RenderView(getActivity());
//
//        m_VRLayout.addView(m_VRRenderView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
//        m_VRRenderView.setOpaque(false);
//
//        m_DataGathererManager.SetGyroscopeListener(m_VRRenderView);
//        m_DataGathererManager.AddGPSListener(m_VRRenderView);
//        m_DataGathererManager.AddGPSListener(AlbumManager.getInstance());
//        m_DataGathererManager.AddBearingListener(this);
////        m_DataGathererManager.SetAlbumListener(m_VRRenderView);
//
//        if (forceStart)
//        {
//            if (m_VRRenderView != null) m_VRRenderView.onStart();
//            if (m_DataGathererManager != null) m_DataGathererManager.onStart();
//        }
    }

    @Override
    public void onStart()
    {
        super.onStart();

        if (m_VRRenderView != null) m_VRRenderView.onStart();
        if (m_DataGathererManager != null) m_DataGathererManager.onStart();
    }

    @Override
    public void onStop()
    {
        super.onStop();

        if (m_VRRenderView != null) m_VRRenderView.onStop();
        if (m_DataGathererManager != null) m_DataGathererManager.onStop();
    }

    @Override
    public void onPause()
    {
        super.onPause();

        if (m_VRRenderView != null) m_VRRenderView.onPause();
        if (m_DataGathererManager != null) m_DataGathererManager.onPause();
    }

    @Override
    public void onResume()
    {
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
        m_SlidingUpPanel = view.findViewById(R.id.vr_fragment_slide_up_panel_layout);
        m_SlidingUpPanel.setFadeOnClickListener(v -> m_SlidingUpPanel.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED));
        m_SlidingUpPanel.setDragView(R.id.popup_drag_view);
        m_SlidingUpPanel.addPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener()
        {
            @Override
            public void onPanelSlide(View panel, float slideOffset)
            {

            }

            @Override
            public void onPanelStateChanged(View panel, SlidingUpPanelLayout.PanelState previousState, SlidingUpPanelLayout.PanelState newState)
            {
                if (newState == SlidingUpPanelLayout.PanelState.COLLAPSED) {
                    m_ViewPager.setUserInputEnabled(true);
                }
            }
        });

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
                    m_PostBearing = m_DeviceBearing;
                    File previewCroppedFile = File.createTempFile("cameraPreview-cropped", ".jpg");
                    result.toFile(previewCroppedFile, file -> {
                        Uri savedPreviewUri = Uri.fromFile(file);
                        Uri croppedPreviewUri = Uri.fromFile(previewCroppedFile);
                        Intent UCropIntent = UCrop.of(savedPreviewUri, croppedPreviewUri).withAspectRatio(16, 9).getIntent(getActivity());
                        m_UCropActivityResultLauncher.launch(UCropIntent);
                    });
                } catch (IOException e)
                {
                }
            }
        });

        m_TakePhotoButton = view.findViewById(R.id.vr_button_take_photo);
        m_TakePhotoButton.setOnClickListener(v -> {
//            if (m_CameraView != null && m_CameraView.isOpened() && !m_CameraView.isTakingPicture())
//            {
//                TakePhoto();
//            }
            m_ViewPager.setUserInputEnabled(false);
            m_SlidingUpPanel.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
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
                                AlbumManager.getInstance().tryAddPost(croppedImageUri, m_PostBearing);
                            }
                        }
                    }
                });
    }

    private void ToggleVREnabled()
    {
        if (m_IsVRInitialized)
        {
            m_PrevIsVREnabled = m_IsVREnabled;
            m_IsVREnabled = !m_IsVREnabled;
            m_ToggleVRButton.setImageResource(m_IsVREnabled ? R.drawable.ic_ar_enabled_button : R.drawable.ic_ar_disabled_button);
            if (m_VRRenderView != null) m_VRRenderView.ShouldRender(m_IsVREnabled);
        }
    }

    private void ToggleCamera()
    {
        if (m_CameraView != null && m_CameraView.isOpened())
        {
            Facing cameraFacing = m_CameraView.toggleFacing();
            if (cameraFacing == Facing.BACK) {
                m_IsVREnabled = m_PrevIsVREnabled;
                m_ToggleCameraButton.setImageResource(R.drawable.ic_baseline_camera_rear_24);
                if (m_IsVRInitialized) {
                    m_ToggleVRButton.setEnabled(true);
                    m_ToggleVRButton.setImageResource(m_IsVREnabled ? R.drawable.ic_ar_enabled_button : R.drawable.ic_ar_disabled_button);
                    if (m_VRRenderView != null) m_VRRenderView.ShouldRender(m_IsVREnabled);
                }
            }
            else if (cameraFacing == Facing.FRONT) {
                m_PrevIsVREnabled = m_IsVREnabled;
                m_IsVREnabled = false;
                m_ToggleCameraButton.setImageResource(R.drawable.ic_baseline_camera_front_24);
                if (m_IsVRInitialized) {
                    m_ToggleVRButton.setEnabled(false);
                    m_ToggleVRButton.setImageResource(R.drawable.ic_ar_disabled_button);
                    if (m_VRRenderView != null) m_VRRenderView.ShouldRender(false);
                }
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
                    return false;
                }
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

    @Override
    public void onBearingSensorAngleChanged(int azimuth)
    {
        m_DeviceBearing = azimuth;
    }

    class VR_Fragment_GestureListener extends GestureDetector.SimpleOnGestureListener implements VR_Fragment_GestureDetector.Listener
    {
        private double m_SwipeDistanceCovered = 0;
        private long m_SwipeTimeSpent = 0;
        private long m_TouchDownSpent = 0;

        @Override
        public boolean onDown(MotionEvent event)
        {
            m_InteractingWithAlbum = rayCast(event);
            if (m_InteractingWithAlbum != null)
            {
                m_ViewPager.setUserInputEnabled(false);
                m_IsInteractingWithPost = true;
                m_TouchDownSpent = event.getEventTime();
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
                if (album != null)
                {
                    Post post = PostManager.getInstance().getPost(album.getCurrentPostID());
                    if (post != null)
                    {
                        if (post.isLikedByCurrentUser()) {
                            post.unlike(successful -> album.updateUIOnPostChanged());
                        }
                        else {
                            post.like(successful -> album.updateUIOnPostChanged());
                        }
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
                if (m_InteractingWithAlbum != null)
                {
                    if (m_SwipeTimeSpent != 0 && Math.abs(m_SwipeDistanceCovered) > 100) {
                        double velocity = m_SwipeDistanceCovered / m_SwipeTimeSpent;
                        Album album = AlbumManager.getInstance().getAlbum(m_InteractingWithAlbum);
                        if (album != null)
                        {
                            album.userInteractUpdatePostIndex(velocity > 0);
                            album.updateUIOnPostChanged();
                        }
                    }
                    else {
                        long timeSpentDown = ev.getEventTime() - m_TouchDownSpent;
                        if (timeSpentDown > 500) {
                        }
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