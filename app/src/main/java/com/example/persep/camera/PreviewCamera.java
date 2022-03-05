package com.example.persep.camera;

import android.annotation.SuppressLint;
import android.content.Context;
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
    private Size m_PreviewSize;
    private String m_CameraId;

    private Semaphore m_CameraOpenCloseLock = new Semaphore(1);
    private CameraDevice m_CameraDevice;

    private CaptureRequest.Builder m_PreviewRequestBuilder;
    private CaptureRequest m_PreviewRequest;

    private CameraCaptureSession m_CaptureSession;

    private final CameraDevice.StateCallback m_StateCallback = new CameraDevice.StateCallback()
    {

        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice)
        {
            Log.d(TAG, "Camera opened");
            m_CameraOpenCloseLock.release();
            m_CameraDevice = cameraDevice;
            CreateCameraPreviewSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice)
        {
            Log.d(TAG, "Camera disconnected");
            m_CameraOpenCloseLock.release();
            cameraDevice.close();
            m_CameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error)
        {
            Log.d(TAG, "Camera error");
            m_CameraOpenCloseLock.release();
            cameraDevice.close();
            m_CameraDevice = null;
        }
    };

    public PreviewCamera(Context context, SurfaceTexture texture)
    {
        m_Context = context;
        m_SurfaceTexture = texture;
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
                m_PreviewSize = GetPreferredPreviewSize(map.getOutputSizes(SurfaceTexture.class), width, height);
                m_CameraId = cameraId;
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

    @SuppressLint("MissingPermission")
    public void OpenCamera(Handler backgroundHandler)
    {
        m_BackgroundHandler = backgroundHandler;
        try
        {
            if (!m_CameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS))
            {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }

            CameraManager manager = (CameraManager) m_Context.getSystemService(Context.CAMERA_SERVICE);
            manager.openCamera(m_CameraId, m_StateCallback, m_BackgroundHandler);
        } catch (CameraAccessException | InterruptedException e) { e.printStackTrace(); }
    }

    public void CloseCamera()
    {
        try
        {
            m_CameraOpenCloseLock.acquire();
            if (m_CaptureSession != null)
            {
                m_CaptureSession.close();
                m_CaptureSession = null;
            }
            if (m_CameraDevice != null)
            {
                m_CameraDevice.close();
                m_CameraDevice = null;
            }
        }
        catch (InterruptedException e)
        {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        }
        m_CameraOpenCloseLock.release();
    }

    public void CreateCameraPreviewSession()
    {
        try
        {
            m_SurfaceTexture.setDefaultBufferSize(m_PreviewSize.getWidth(), m_PreviewSize.getHeight());
            Surface previewSurface = new Surface(m_SurfaceTexture);

            m_PreviewRequestBuilder = m_CameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            m_PreviewRequestBuilder.addTarget(previewSurface);
            m_CameraDevice.createCaptureSession(Arrays.asList(previewSurface),
                    new CameraCaptureSession.StateCallback()
                    {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession)
                        {
                            if (m_CameraDevice != null)
                            {
                                try
                                {
                                    m_PreviewRequest = m_PreviewRequestBuilder.build();
                                    m_CaptureSession = cameraCaptureSession;
                                    m_CaptureSession.setRepeatingRequest(m_PreviewRequest, null, m_BackgroundHandler);
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
