package com.example.persep.camera;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Handler;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class PreviewCamera
{
    private static final String TAG = "PreviewCamera";

    private Context m_Context;
    private SurfaceTexture m_SurfaceTexture;
    private Handler m_BackgroundHandler;
    private Size mPreviewSize;
    private String mCameraId;

    private Semaphore mCameraOpenCloseLock = new Semaphore(1);
    private CameraDevice mCameraDevice;

    private CaptureRequest.Builder mPreviewRequestBuilder;
    private CaptureRequest mPreviewRequest;

    private CameraCaptureSession mCaptureSession;

    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            Log.d(TAG, "Camera opened");
            mCameraOpenCloseLock.release();
            mCameraDevice = cameraDevice;
            CreateCameraPreviewSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            Log.d(TAG, "Camera disconnected");
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            Log.d(TAG, "Camera error");
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
        }
    };

    public PreviewCamera(Context context, SurfaceTexture texture, Handler backgroundHandler)
    {
        m_Context = context;
        m_SurfaceTexture = texture;
        m_BackgroundHandler = backgroundHandler;
    }

    public void SetupCamera(int width, int height)
    {
        try
        {
            CameraManager manager = (CameraManager) m_Context.getSystemService(Context.CAMERA_SERVICE);

            for (String cameraId : manager.getCameraIdList())
            {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT)
                {
                    continue;
                }

                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                mPreviewSize = GetPreferredPreviewSize(map.getOutputSizes(SurfaceTexture.class), width, height);
                mCameraId = cameraId;
                return;
            }
        } catch (CameraAccessException e) {  e.printStackTrace(); }
    }

    private Size GetPreferredPreviewSize(Size[] mapSizes, int width, int height)
    {
        List<Size> collectorSizes = new ArrayList<>();
        for(Size option : mapSizes)
        {
            if(width > height)
            {
                if(option.getWidth() > width && option.getHeight() > height)
                {
                    collectorSizes.add(option);
                }
            }
            else
            {
                if(option.getWidth() > height && option.getHeight() > width)
                {
                    collectorSizes.add(option);
                }
            }
        }

        if(collectorSizes.size() > 0)
        {
            return Collections.min(collectorSizes, (lhs, rhs) -> Long.signum((long) lhs.getWidth() * lhs.getHeight() - (long) rhs.getWidth() * rhs.getHeight()));
        }
        return mapSizes[0];
    }

    public void OpenCamera()
    {
        if (ContextCompat.checkSelfPermission(m_Context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
        {
            return;
        }

        try
        {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS))
            {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }

            CameraManager manager = (CameraManager) m_Context.getSystemService(Context.CAMERA_SERVICE);
            manager.openCamera(mCameraId, mStateCallback, m_BackgroundHandler);
        } catch (CameraAccessException | InterruptedException e) { e.printStackTrace(); }
    }

    public void CloseCamera()
    {
        try
        {
            mCameraOpenCloseLock.acquire();
            if (mCaptureSession != null)
            {
                mCaptureSession.close();
                mCaptureSession = null;
            }
            if (mCameraDevice != null)
            {
                mCameraDevice.close();
                mCameraDevice = null;
            }
        }
        catch (InterruptedException e)
        {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        }
        mCameraOpenCloseLock.release();
    }

    public void CreateCameraPreviewSession()
    {
        try
        {
            m_SurfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            Surface previewSurface = new Surface(m_SurfaceTexture);

            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(previewSurface);
            mCameraDevice.createCaptureSession(Arrays.asList(previewSurface),
                    new CameraCaptureSession.StateCallback()
                    {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession)
                        {
                            if (mCameraDevice != null)
                            {
                                try
                                {
                                    mPreviewRequest = mPreviewRequestBuilder.build();
                                    mCaptureSession = cameraCaptureSession;
                                    mCaptureSession.setRepeatingRequest(mPreviewRequest, null, m_BackgroundHandler);
                                } catch (CameraAccessException e) { e.printStackTrace(); }
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) { }
                    },
                    null
            );
        } catch (CameraAccessException e) { e.printStackTrace(); }
    }
}
