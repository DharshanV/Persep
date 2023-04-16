package com.perception.persep.camera;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class PreviewCamera
{
    private static final String TAG = "PreviewCamera";

    public interface TakePictureListener
    {
        void OnPictureTaken(File file);
    }

    private static final int MAX_PREVIEW_WIDTH = 1920;
    private static final int MAX_PREVIEW_HEIGHT = 1080;

    private final Context m_Context;
    private final SurfaceTexture m_SurfaceTexture;
    private Handler m_BackgroundHandler;
    private Size m_PreviewSize;
    private String m_CameraId;

    private final Semaphore m_CameraOpenCloseLock = new Semaphore(1);
    private CameraDevice m_CameraDevice;

    private CaptureRequest.Builder m_PreviewRequestBuilder;

    private CaptureRequest.Builder m_TakePictureRequestBuilder;
    private CaptureRequest m_TakePictureRequest;

    private CameraCaptureSession m_CaptureSession;

    private final CameraDevice.StateCallback m_StateCallback = new CameraDevice.StateCallback()
    {

        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice)
        {
            Log.d(TAG, "Camera opened");
            m_CameraDevice = cameraDevice;
            CreateCameraPreviewSession();
            m_CameraOpenCloseLock.release();
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
                m_PreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), height, width);
                m_CameraId = cameraId;
                return;
            }
        } catch (CameraAccessException e)
        {
            e.printStackTrace();
        }
    }

    private static Size chooseOptimalSize(Size[] sizes, int w, int h)
    {
        // Use a very small tolerance because we want an exact match.
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) w / h;
        if (sizes == null)
            return null;

        Size optimalSize = null;/*  w  w  w  .ja va  2 s .c  om*/

        // Start with max value and refine as we iterate over available preview sizes. This is the
        // minimum difference between view and camera height.
        double minDiff = Double.MAX_VALUE;

        // Try to find a preview size that matches aspect ratio and the target view size.
        // Iterate over all available sizes and pick the largest size that can fit in the view and
        // still maintain the aspect ratio.
        for (Size size : sizes)
        {
            double ratio = (double) size.getWidth() / size.getHeight();
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
                continue;
            if (Math.abs(size.getHeight() - h) < minDiff)
            {
                optimalSize = size;
                minDiff = Math.abs(size.getHeight() - h);
            }
        }

        // Cannot find preview size that matches the aspect ratio, ignore the requirement
        if (optimalSize == null)
        {
            minDiff = Double.MAX_VALUE;
            for (Size size : sizes)
            {
                if (Math.abs(size.getHeight() - h) < minDiff)
                {
                    optimalSize = size;
                    minDiff = Math.abs(size.getHeight() - h);
                }
            }
        }

        return optimalSize;
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
            ClosePreviewSession();
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
            ClosePreviewSession();
            m_SurfaceTexture.setDefaultBufferSize(m_PreviewSize.getWidth(), m_PreviewSize.getHeight());
            Surface previewSurface = new Surface(m_SurfaceTexture);

            m_PreviewRequestBuilder = m_CameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            m_PreviewRequestBuilder.addTarget(previewSurface);
            m_CameraDevice.createCaptureSession(Arrays.asList(previewSurface), new CameraCaptureSession.StateCallback()
            {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession)
                {
                    if (m_CameraDevice != null)
                    {
                        try
                        {
                            m_CaptureSession = cameraCaptureSession;
                            m_CaptureSession.setRepeatingRequest(m_PreviewRequestBuilder.build(), null, m_BackgroundHandler);
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
            }, null);
        } catch (CameraAccessException e)
        {
            e.printStackTrace();
        }
    }

    private void ClosePreviewSession() {
        if (m_CaptureSession != null) {
            m_CaptureSession.close();
            m_CaptureSession = null;
        }
    }

    public void TakeAndSavePreview(TakePictureListener listener)
    {
        try
        {
            ImageReader imageReader = ImageReader.newInstance(m_PreviewSize.getWidth(), m_PreviewSize.getHeight(), ImageFormat.JPEG, 1);
            m_SurfaceTexture.setDefaultBufferSize(m_PreviewSize.getWidth(), m_PreviewSize.getHeight());
            Surface previewSurface = new Surface(m_SurfaceTexture);

            List<Surface> outputSurfaces = new ArrayList<>(2);
            outputSurfaces.add(imageReader.getSurface());
            outputSurfaces.add(previewSurface);

            m_TakePictureRequestBuilder = m_CameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            m_TakePictureRequestBuilder.addTarget(imageReader.getSurface());
            m_TakePictureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

            ImageReader.OnImageAvailableListener imageAvailableListener = reader -> {
                Image image = reader.acquireLatestImage();
                try
                {
                    File previewFile = File.createTempFile("cameraPreview", ".jpg");
                    OutputStream fileOutputSteam = new FileOutputStream(previewFile);

                    ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                    byte[] bytes = new byte[buffer.capacity()];
                    buffer.get(bytes);
                    Bitmap bitmapImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);
                    bitmapImage = RotateBitmap(bitmapImage, 90);
                    bitmapImage.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputSteam);

                    fileOutputSteam.flush();
                    fileOutputSteam.close();

                    listener.OnPictureTaken(previewFile);
                } catch (IOException e)
                {
                    e.printStackTrace();
                }
                image.close();
            };
            imageReader.setOnImageAvailableListener(imageAvailableListener, m_BackgroundHandler);

            CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback()
            {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result)
                {
                    super.onCaptureCompleted(session, request, result);
                    CreateCameraPreviewSession();
                }
            };
            m_CameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback()
            {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session)
                {
                    if (m_CameraDevice != null)
                    {
                        try
                        {
                            m_TakePictureRequest = m_TakePictureRequestBuilder.build();
                            session.capture(m_TakePictureRequest, captureCallback, m_BackgroundHandler);
                        } catch (CameraAccessException e)
                        {
                            e.printStackTrace();
                        }
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session)
                {
                }
            }, m_BackgroundHandler);
        } catch (CameraAccessException e)
        {
            e.printStackTrace();
        }
    }

    public static Bitmap RotateBitmap(Bitmap source, float angle)
    {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }
}
