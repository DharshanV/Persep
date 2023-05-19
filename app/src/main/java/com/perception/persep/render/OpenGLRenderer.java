package com.perception.persep.render;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES30;
import android.opengl.Matrix;
import android.os.Trace;
import android.util.Log;

import com.perception.persep.album_data.Album;
import com.perception.persep.album_data.Post;
import com.perception.persep.constants.QueryConstants;
import com.perception.persep.data_gatherer.AlbumManager;
import com.perception.persep.data_gatherer.PostManager;
import com.perception.persep.math_helpers.GeoLocationHelper;
import com.perception.persep.shapes.AlbumModel;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
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
    private final float[] m_ScreenToOpenGLMatrix = new float[16];
    private final float[] m_Near = {0, 0, 0, 0};
    private final float[] m_Far = {0, 0, 0, 0};

    // N(+z): <0,0,1>, S(-z): <0,0,-1>, W(+x): <1,0,0>, E(-x): <-1,0,0>
    private float[] m_OrientationMatrix = new float[16];
    static private final float[] DEVICE_TO_OPENGL = {-1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f};

    private AlbumModel m_AlbumModel;
    private final float[] m_AlbumRotMatrix = new float[16];

    private Stack<Integer> m_ValidIndex = new Stack<>();
    private Map<String, Integer> m_AlbumIDIndex = new HashMap<>();
    private final float[][] m_AlbumTransforms = new float[QueryConstants.MAX_QUERY_COUNT][16];
    private final int[] m_AlbumTextureIDs = new int[QueryConstants.MAX_QUERY_COUNT];

    private final ReadWriteLock m_DataUpdateReadWriteLock = new ReentrantReadWriteLock();
    private final Lock m_DataUpdateWriteLock = m_DataUpdateReadWriteLock.writeLock();
    private final Lock m_DataUpdateReadLock = m_DataUpdateReadWriteLock.writeLock();
    private double m_DeviceLat = 0;
    private double m_DeviceLon = 0;
    private boolean m_DeviceCoordInit = false;

    private final Vector3D[] ALBUM_SURFACE_VERT = new Vector3D[4];

    OpenGLRenderer(Context context)
    {
        m_Context = context;

        ALBUM_SURFACE_VERT[0] = new Vector3D(0.020064, -0.400567, 0.541449);
        ALBUM_SURFACE_VERT[1] = new Vector3D(0.020064, 0.400567, 0.541449);
        ALBUM_SURFACE_VERT[2] = new Vector3D(0.020064, 0.400567, -0.541449);
        ALBUM_SURFACE_VERT[3] = new Vector3D(0.020064, -0.400567, -0.541449);

        Matrix.setIdentityM(m_ModelMatrix, 0);
        Matrix.setIdentityM(m_ProjectionMatrix, 0);
        Matrix.setLookAtM(m_ViewMatrix, 0, 0f, 0f, 0.0f, 0f, 0f, -1.0f, 0f, 1.0f, 0.0f);
        Matrix.setIdentityM(m_OrientationMatrix, 0);

        for (int i=0;i<QueryConstants.MAX_QUERY_COUNT; i++) {
            m_ValidIndex.push(i);
        }
    }

    public void Initialize()
    {
        Log.d(TAG, "Initialized renderer");
        InitGLSettings();

        GLES30.glGenTextures(m_AlbumTextureIDs.length, m_AlbumTextureIDs, 0);

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

    public void DrawFrame()
    {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);

        if (!m_DeviceCoordInit) return;

        updateAlbums();
        for(Map.Entry<String, Integer> entry : m_AlbumIDIndex.entrySet()) {
            Integer index = entry.getValue();
            Album album = AlbumManager.getInstance().getAlbum(entry.getKey());
            Post post = PostManager.getInstance().getPost(album.getCurrentPostID());
            if (album != null && post != null && index != -1 && index < QueryConstants.MAX_QUERY_COUNT)
            {
                int albumTextureID = m_AlbumTextureIDs[index];
                float[] albumTransform = m_AlbumTransforms[index];

                if (album.ShouldUpdateTexture() && post.m_Image != null) {
                    m_AlbumModel.UpdateSurfaceTexture(albumTextureID, post.m_Image);
                }

                double albumLat = album.m_GeoPoint.getLatitude();
                double albumLon = album.m_GeoPoint.getLongitude();
                double bearing = GeoLocationHelper.Heading(m_DeviceLat, m_DeviceLon, albumLat, albumLon);
                double distance = GeoLocationHelper.Distance(m_DeviceLat, m_DeviceLon, albumLat, albumLon);
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
                float scale = (float) (1.0 + 4 * Math.log10(length - 2));
                Matrix.scaleM(m_ModelMatrix, 0, scale, scale, scale);
                System.arraycopy(m_ModelMatrix, 0, albumTransform, 0, m_ModelMatrix.length);

                m_AlbumModel.SetMVP(m_ModelMatrix, m_ViewMatrix, m_ProjectionMatrix);
                m_AlbumModel.DrawFrame();
                m_AlbumModel.DrawSurface(albumTextureID);

            }
        }

//        m_DataUpdateReadLock.lock();
//        for (Album album : m_Albums.keySet())
//        {
//            if (album != null)
//            {
//                Integer textureIndex = m_Albums.get(album);
//                if (textureIndex != null)
//                {
//                    int albumTextureID = m_AlbumTextureIDs[textureIndex];
//                    if (album.ShouldUpdateTexture())
//                    {
//                        Bitmap image = album.getCurrentPost().m_Image;
//                        if (image != null)
//                        {
//                            m_AlbumModel.UpdateSurfaceTexture(albumTextureID, image);
//                        }
//                    }
//
//                    double bearing = GeoLocationHelper.Heading(m_DeviceLat, m_DeviceLon, album.m_Lat, album.m_Lon);
//                    double distance = GeoLocationHelper.Distance(m_DeviceLat, m_DeviceLon, album.m_Lat, album.m_Lon);
//                    distance = Math.max(3.0, distance);
//
//                    float x = (float) (distance * Math.sin(bearing));
//                    float z = (float) (distance * Math.cos(bearing));
//
//                    Matrix.setIdentityM(m_ModelMatrix, 0);
//                    Matrix.multiplyMM(m_ModelMatrix, 0, m_ModelMatrix, 0, m_OrientationMatrix, 0);
//                    Matrix.multiplyMM(m_ModelMatrix, 0, m_ModelMatrix, 0, DEVICE_TO_OPENGL, 0);
//                    Matrix.translateM(m_ModelMatrix, 0, x, 0.0f, z);
//
//                    // Rotate the album to always face the camera
//                    float originToModelX = -x;
//                    float originToModelZ = -z;
//                    double length = Math.sqrt(originToModelX * originToModelX + originToModelZ * originToModelZ);
//                    double angle = Math.toDegrees(Math.acos(originToModelX / length));
//                    if (originToModelZ > 0.0f) angle *= -1;
//                    Matrix.rotateM(m_ModelMatrix, 0, (float) angle, 0, 1, 0);
//
//                    // Scale the model based on distance
//                    float scale = (float) (1.0 + 4 * Math.log10(length - 2));
//                    Matrix.scaleM(m_ModelMatrix, 0, scale, scale, scale);
//                    float[] albumTransform = m_AlbumTransforms.get(album);
//                    if (albumTransform != null)
//                    {
//                        System.arraycopy(m_ModelMatrix, 0, albumTransform, 0, m_ModelMatrix.length);
//                    } else
//                    {
//                        m_AlbumTransforms.put(album, m_ModelMatrix.clone());
//                    }
//
//                    m_AlbumModel.SetMVP(m_ModelMatrix, m_ViewMatrix, m_ProjectionMatrix);
//                    m_AlbumModel.DrawFrame();
//                    m_AlbumModel.DrawSurface(albumTextureID);
//                } else
//                {
//                    Log.e(TAG, "Couldn't find texture ID for album!!");
//                }
//            } else
//            {
//                Log.e(TAG, "Found a null album to render!!");
//            }
//        }
//        m_DataUpdateReadLock.unlock();
    }

    public void DrawClear()
    {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);
    }

    public void Resize(int width, int height)
    {
        Log.d(TAG, "Resized | width: " + width + " height: " + height);
        m_Width = width;
        m_Height = height;
        GLES30.glViewport(0, 0, m_Width, m_Height);

        float ratio = (float) m_Width / m_Height;
        Matrix.frustumM(m_ProjectionMatrix, 0, -ratio, ratio, -1, 1, 2.0f, 100);

        // https://stackoverflow.com/questions/7692988/opengl-math-projecting-screen-space-to-world-space-coords
        Matrix.multiplyMM(m_ScreenToOpenGLMatrix, 0, m_ViewMatrix, 0, m_ProjectionMatrix, 0);
        Matrix.invertM(m_ScreenToOpenGLMatrix, 0, m_ScreenToOpenGLMatrix, 0);
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

    private void updateAlbums()
    {
        // Assign index to new albums
        AlbumManager.getInstance().readLock();
        for (Album album : AlbumManager.getInstance().getAlbums()) {
            if (!album.m_PostIDs.isEmpty() && !m_AlbumIDIndex.containsKey(album.m_AlbumID)) {
                int validIndex = m_ValidIndex.pop();
                m_AlbumIDIndex.put(album.m_AlbumID, validIndex);
            }
        }

        // Unassign index of invalid albums
        for (String currAlbumID : m_AlbumIDIndex.keySet()) {
            Album album = AlbumManager.getInstance().getAlbum(currAlbumID);
            if (album == null) {
                int freedIndex = m_AlbumIDIndex.remove(currAlbumID);
                m_ValidIndex.push(freedIndex);
            }
        }
        AlbumManager.getInstance().readUnlock();
    }

    private Vector3D getWorldTriVert(float[] transform, Vector3D vert)
    {
        float[] result = new float[4];
        result[0] = (float) vert.getX();
        result[1] = (float) vert.getY();
        result[2] = (float) vert.getZ();
        result[3] = 1.0f;
        Matrix.multiplyMV(result, 0, transform, 0, result, 0);
        return new Vector3D(result[0], result[1], result[2]);
    }

    private boolean intersectRayWithAlbumSurface(Vector3D rayDir, float[] albumTransform)
    {
        Vector3D V0 = getWorldTriVert(albumTransform, ALBUM_SURFACE_VERT[0]);
        Vector3D V1 = getWorldTriVert(albumTransform, ALBUM_SURFACE_VERT[1]);
        Vector3D V3 = getWorldTriVert(albumTransform, ALBUM_SURFACE_VERT[3]);

        Vector3D E0 = V1.subtract(V0);
        Vector3D E1 = V3.subtract(V0);
        Vector3D N = E0.crossProduct(E1);

        Vector3D dir = rayDir.negate();

        double NdotDir = N.dotProduct(dir);

        if (Math.abs(NdotDir) < 1e-6f) return false;

        double t = -N.dotProduct(V0.negate()) / NdotDir;
        Vector3D hitPoint = dir.scalarMultiply(t);

        Vector3D hitVec = hitPoint.subtract(V0);
        double u = hitVec.dotProduct(E0);
        double v = hitVec.dotProduct(E1);

        return (u >= 0.0f && u <= E0.dotProduct(E0) && v >= 0.0f && v <= E1.dotProduct(E1));
    }

    public String rayCast(float x, float y)
    {
        m_Near[0] = x;
        m_Near[1] = y;
        m_Near[2] = -1;
        m_Near[3] = 1;

        m_Far[0] = x;
        m_Far[1] = y;
        m_Far[2] = 1;
        m_Far[3] = 1;

        Matrix.multiplyMV(m_Near, 0, m_ScreenToOpenGLMatrix, 0, m_Near, 0);
        Matrix.multiplyMV(m_Far, 0, m_ScreenToOpenGLMatrix, 0, m_Far, 0);

        m_Near[0] /= m_Near[3];
        m_Near[1] /= m_Near[3];
        m_Near[2] /= m_Near[3];

        m_Far[0] /= m_Far[3];
        m_Far[1] /= m_Far[3];
        m_Far[2] /= m_Far[3];

        Vector3D near = new Vector3D(m_Near[0], m_Near[1], m_Near[2]);
        Vector3D far = new Vector3D(m_Far[0], m_Far[1], m_Far[2]);

        Vector3D rayDir = (far.subtract(near)).normalize();

        m_DataUpdateReadLock.lock();
        AlbumManager.getInstance().readLock();
        for(Map.Entry<String, Integer> entry : m_AlbumIDIndex.entrySet()) {
            float[] albumTransform = m_AlbumTransforms[entry.getValue()];
            if (intersectRayWithAlbumSurface(rayDir, albumTransform))
            {
                m_DataUpdateReadLock.unlock();
                AlbumManager.getInstance().readUnlock();
                return entry.getKey();
            }
        }
        m_DataUpdateReadLock.unlock();
        AlbumManager.getInstance().readUnlock();
        return null;
    }
}
