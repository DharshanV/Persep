package com.perception.persep.render;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES30;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.os.Trace;
import android.util.Log;

import com.perception.persep.album_data.Album;
import com.perception.persep.math_helpers.GeoLocationHelper;
import com.perception.persep.shapes.AlbumModel;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class OpenGLRenderer
{
    private static final String TAG = "Renderer";

    private Context m_Context;
    private int m_Width;
    private int m_Height;

    private final float[] m_ModelMatrix = new float[16];
    private final float[] m_ProjectionMatrix = new float[16];
    private final float[] m_ViewMatrix = new float[16];

    private float[] m_OrientationMatrix = new float[16];
    static private final float[] DEVICE_TO_OPENGL = {-1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f};

    private AlbumModel m_AlbumModel;
    static private final String[] m_AlbumTexturePaths = {"sample_image.jpg", "sample_image_2.jpg"};
    private int m_TextureIndex = 0;

    private HashSet<Album> m_RenderAlbums = new HashSet<>();

    private final ReadWriteLock m_DataUpdateReadWriteLock = new ReentrantReadWriteLock();
    private final Lock m_DataUpdateWriteLock = m_DataUpdateReadWriteLock.writeLock();
    private final Lock m_DataUpdateReadLock = m_DataUpdateReadWriteLock.writeLock();
    private double m_DeviceLat = 0;
    private double m_DeviceLon = 0;
    private boolean m_DeviceCoordInit = false;

    // N(+z), W(+x), E(-x), S(-z)
    private final Vector3D[] albumOffsets = {new Vector3D(0,0,5), new Vector3D(5,0,0), new Vector3D(-5,0,0), new Vector3D(0,0,-5)};
    private final float[] albumRotation = {90, 180, 0, -90, 0, 0, 0};

    private final Vector3D[] albumCoords = {
            new Vector3D(-33.92414741995048, 151.23380290528448, 0),
            new Vector3D(-33.92418636856642, 151.23368689975817, 0),
            new Vector3D(-33.9242036172335, 151.23387733657594, 0),
            new Vector3D(-33.92425091194791, 151.23378480037576, 0),

            new Vector3D(-33.89315131618332, 151.2287090320646, 0),
            new Vector3D(-33.8932495300973, 151.2285675947477, 0),
            new Vector3D(-33.89429456102966, 151.22743769199667, 0)
    };

    OpenGLRenderer(Context context)
    {
        m_Context = context;

//        m_AlbumCoord = GeoLocationHelper.ConvertToCartesian(ALBUM_LAT, ALBUM_LON);

        Matrix.setIdentityM(m_ModelMatrix, 0);
        Matrix.setIdentityM(m_ProjectionMatrix, 0);
        Matrix.setLookAtM(m_ViewMatrix, 0, 0f, 0f, 2f, 0f, 0f, 0f, 0f, 1.0f, 0.0f);
        Matrix.setIdentityM(m_OrientationMatrix, 0);
    }

    public void Initialize()
    {
        Log.d(TAG, "Initialized renderer");
        InitGLSettings();

        m_AlbumModel = AlbumModel.LoadModel(m_Context);
        m_AlbumModel.BuildShaders(m_Context);
        m_AlbumModel.LoadTextures(m_Context, m_AlbumTexturePaths);

        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                m_TextureIndex = (m_TextureIndex + 1) % m_AlbumTexturePaths.length;
            }
        }, 1000, 1000);

    }

    private void InitGLSettings()
    {
        GLES30.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);
        GLES30.glEnable(GLES30.GL_CULL_FACE);
        GLES30.glCullFace(GLES30.GL_BACK);
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
        GLES30.glDepthFunc(GLES30.GL_LEQUAL);
        GLES30.glDepthMask(true);
    }

    public void DrawFrame(float deltaTime)
    {
        Trace.beginSection("gl_draw_frame");

        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);

        if (!m_DeviceCoordInit) return;

        m_DataUpdateReadLock.lock();
        for (int i = 0; i < albumCoords.length; i++)
        {
            Vector3D albumCoord = albumCoords[i];
            double bearing = GeoLocationHelper.Heading(m_DeviceLat, m_DeviceLon, albumCoord.getX(), albumCoord.getY());
            double distance = GeoLocationHelper.Distance(m_DeviceLat, m_DeviceLon, albumCoord.getX(), albumCoord.getY());

            float x = (float) (distance * Math.sin(bearing));
            float z = (float) (distance * Math.cos(bearing));

            Matrix.setIdentityM(m_ModelMatrix, 0);
            Matrix.multiplyMM(m_ModelMatrix, 0, m_ModelMatrix, 0, m_OrientationMatrix, 0);
            Matrix.multiplyMM(m_ModelMatrix, 0, m_ModelMatrix, 0, DEVICE_TO_OPENGL, 0);
            Matrix.translateM(m_ModelMatrix, 0, x, 0.0f, z);
            Matrix.rotateM(m_ModelMatrix, 0, albumRotation[i], 0, 1, 0);

            m_AlbumModel.SetMVP(m_ModelMatrix, m_ViewMatrix, m_ProjectionMatrix);
            m_AlbumModel.Draw(m_TextureIndex);
        }
        m_DataUpdateReadLock.unlock();

        Trace.endSection();
    }

    public void Resize(int width, int height)
    {
        Log.d(TAG, "Resized | width: " + width + " height: " + height);
        m_Width = width;
        m_Height = height;
        GLES30.glViewport(0, 0, m_Width, m_Height);

        float ratio = (float) m_Width / m_Height;
        Matrix.frustumM(m_ProjectionMatrix, 0, -ratio, ratio, -1, 1, 3, 1000);
    }

    public void UpdateRotation(float[] rotationMatrix)
    {
        m_DataUpdateWriteLock.lock();
        m_OrientationMatrix = rotationMatrix.clone();
        m_DataUpdateWriteLock.unlock();
    }

    public void UpdateDeviceLocation(double lat, double lon)
    {
        m_DataUpdateWriteLock.lock();
        m_DeviceLat = lat;
        m_DeviceLon = lon;
        m_DeviceCoordInit = true;
        m_DataUpdateWriteLock.unlock();
    }

    public void onAlbumUpdate(List<Album> albums)
    {
//        m_RenderAlbums.addAll(albums);
    }
}
