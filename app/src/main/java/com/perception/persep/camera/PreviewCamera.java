package com.perception.persep.camera;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.ImageFormat;
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
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class PreviewCamera
{
    private static final String TAG = "PreviewCamera";

    private static final int MAX_PREVIEW_WIDTH = 1920;
    private static final int MAX_PREVIEW_HEIGHT = 1080;

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

    public Size GetPreviewSize()
    {
        return m_PreviewSize;
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
                Size largest = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)), new CompareSizesByArea());
                m_PreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), width, height, largest);
                m_CameraId = cameraId;
                return;
            }
        } catch (CameraAccessException e)
        {
            e.printStackTrace();
        }
    }

    static class CompareSizesByArea implements Comparator<Size>
    {
        @Override
        public int compare(Size lhs, Size rhs)
        {
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }
    }

    private static Size chooseOptimalSize(Size[] choices, int textureViewWidth, int textureViewHeight, Size aspectRatio)
    {
        List<Size> bigEnough = new ArrayList<>();
        List<Size> notBigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices)
        {
            if (option.getWidth() <= PreviewCamera.MAX_PREVIEW_WIDTH && option.getHeight() <= PreviewCamera.MAX_PREVIEW_HEIGHT &&
                    option.getHeight() == option.getWidth() * h / w)
            {
                if (option.getWidth() >= textureViewWidth &&
                        option.getHeight() >= textureViewHeight)
                {
                    bigEnough.add(option);
                } else
                {
                    notBigEnough.add(option);
                }
            }
        }

        if (bigEnough.size() > 0)
        {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else if (notBigEnough.size() > 0)
        {
            return Collections.max(notBigEnough, new CompareSizesByArea());
        } else
        {
            return choices[0];
        }
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
        } catch (CameraAccessException | InterruptedException e)
        {
            e.printStackTrace();
        }
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
        } catch (InterruptedException e)
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
                                } catch (CameraAccessException e)
                                {
                                    e.printStackTrace();
                                }
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession)
                        {
                        }
                    },
                    null
            );
        } catch (CameraAccessException e)
        {
            e.printStackTrace();
        }
    }
}
