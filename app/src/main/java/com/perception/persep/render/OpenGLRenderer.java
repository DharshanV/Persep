package com.perception.persep.render;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES30;
import android.opengl.Matrix;
import android.os.Trace;
import android.util.Log;

import com.perception.persep.album_data.Album;
import com.perception.persep.constants.QueryConstants;
import com.perception.persep.math_helpers.GeoLocationHelper;
import com.perception.persep.shapes.AlbumModel;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class OpenGLRenderer
{
    private static final String TAG = "OpenGLRenderer";

    private final Context m_Context;
    private int m_Width;
    private int m_Height;

    private final float[] m_ModelMatrix = new float[16];
    private final float[] m_ProjectionMatrix = new float[16];
    private final float[] m_ViewMatrix = new float[16];

    // N(+z): <0,0,1>, S(-z): <0,0,-1>, W(+x): <1,0,0>, E(-x): <-1,0,0>
    private float[] m_OrientationMatrix = new float[16];
    static private final float[] DEVICE_TO_OPENGL = {-1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f};

    private AlbumModel m_AlbumModel;
    private final float[] m_AlbumRotMatrix = new float[16];

    private final Map<Album, Integer> m_Albums = new HashMap<>();
    private final int[] m_AlbumTextureIDs = new int[QueryConstants.MAX_QUERY_COUNT];
    private final List<Integer> m_UnusedTextureIndex = new LinkedList<>();

    private final ReadWriteLock m_DataUpdateReadWriteLock = new ReentrantReadWriteLock();
    private final Lock m_DataUpdateWriteLock = m_DataUpdateReadWriteLock.writeLock();
    private final Lock m_DataUpdateReadLock = m_DataUpdateReadWriteLock.writeLock();
    private double m_DeviceLat = 0;
    private double m_DeviceLon = 0;
    private boolean m_DeviceCoordInit = false;

    OpenGLRenderer(Context context)
    {
        m_Context = context;

        Matrix.setIdentityM(m_ModelMatrix, 0);
        Matrix.setIdentityM(m_ProjectionMatrix, 0);
        Matrix.setLookAtM(m_ViewMatrix, 0, 0f, 0f, 0.0f, 0f, 0f, -1.0f, 0f, 1.0f, 0.0f);
        Matrix.setIdentityM(m_OrientationMatrix, 0);
    }

    public void Initialize()
    {
        Log.d(TAG, "Initialized renderer");
        InitGLSettings();

        GLES30.glGenTextures(m_AlbumTextureIDs.length, m_AlbumTextureIDs, 0);
        for (int i = 0; i < m_AlbumTextureIDs.length; i++) m_UnusedTextureIndex.add(i);

        m_AlbumModel = AlbumModel.LoadModel(m_Context);
        m_AlbumModel.BuildShaders(m_Context);
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
        for (Album album : m_Albums.keySet())
        {
            if (album != null)
            {
                Integer textureIndex = m_Albums.get(album);
                if (textureIndex != null)
                {
                    int albumTextureID = m_AlbumTextureIDs[textureIndex];
                    if (album.ShouldUpdateTexture())
                    {
                        Bitmap image = album.getCurrentPostBitmap();
                        if (image != null)
                        {
                            m_AlbumModel.UpdateSurfaceTexture(albumTextureID, image);
                        }
                    }

                    double bearing = GeoLocationHelper.Heading(m_DeviceLat, m_DeviceLon, album.m_Lat, album.m_Lon);
                    double distance = GeoLocationHelper.Distance(m_DeviceLat, m_DeviceLon, album.m_Lat, album.m_Lon);
                    distance = Math.max(3.0, distance);

                    float x = (float) (distance * Math.sin(bearing));
                    float z = (float) (distance * Math.cos(bearing));

                    Matrix.setIdentityM(m_ModelMatrix, 0);
                    Matrix.multiplyMM(m_ModelMatrix, 0, m_ModelMatrix, 0, m_OrientationMatrix, 0);
                    Matrix.multiplyMM(m_ModelMatrix, 0, m_ModelMatrix, 0, DEVICE_TO_OPENGL, 0);
                    Matrix.translateM(m_ModelMatrix, 0, x, 0.0f, z);

                    // Rotate the album to always face the camera
                    float originToModelX = -x;
                    float originToModelZ = -z;
                    double length = Math.sqrt(originToModelX * originToModelX + originToModelZ * originToModelZ);
                    double angle = Math.toDegrees(Math.acos(originToModelX / length));
                    if (originToModelZ > 0.0f) angle *= -1;
                    Matrix.rotateM(m_ModelMatrix, 0, (float) angle, 0, 1, 0);

                    // Scale the model based on distance
                    float scale = (float) Math.exp(length / 75);
                    Matrix.scaleM(m_ModelMatrix, 0, scale, scale, scale);

                    m_AlbumModel.SetMVP(m_ModelMatrix, m_ViewMatrix, m_ProjectionMatrix);
                    m_AlbumModel.DrawFrame();
                    m_AlbumModel.DrawSurface(albumTextureID);
                } else
                {
                    Log.e(TAG, "Couldn't find texture ID for album!!");
                }
            } else
            {
                Log.e(TAG, "Found a null album to render!!");
            }
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
        Matrix.frustumM(m_ProjectionMatrix, 0, -ratio, ratio, -1, 1, 2.0f, 100);
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
        m_DataUpdateWriteLock.lock();

        // Clear all invalid albums in map
        Integer invalidIndex = m_Albums.get(null);
        if (invalidIndex != null)
        {
            m_Albums.remove(null);
            m_UnusedTextureIndex.add(invalidIndex);
        }
        for (Album mapAlbum : m_Albums.keySet())
        {
            // Remove stale album
            if (!albums.contains(mapAlbum))
            {
                m_Albums.remove(mapAlbum);
                m_UnusedTextureIndex.add(m_Albums.get(mapAlbum));
            }
        }

        // Map new and incoming albums to an unused texture id
        for (Album album : albums)
        {
            if (!m_Albums.containsKey(album))
            {
                if (m_UnusedTextureIndex.isEmpty())
                {
                    Log.e(TAG, "No more textures to assign to album!");
                    break;
                }
                m_Albums.put(album, m_UnusedTextureIndex.remove(0));
            }
        }

        m_DataUpdateWriteLock.unlock();
    }
}
