package com.example.persep.render;

import android.content.Context;
import android.opengl.GLES30;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.os.Trace;
import android.util.Log;

import com.example.persep.shapes.AlbumModel;

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

    OpenGLRenderer(Context context)
    {
        m_Context = context;

        Matrix.setIdentityM(m_ModelMatrix, 0);
        Matrix.setIdentityM(m_ProjectionMatrix, 0);
        Matrix.setIdentityM(m_ViewMatrix, 0);
        Matrix.setIdentityM(m_OrientationMatrix, 0);
    }

    public void Initialize()
    {
        Log.d(TAG, "Initialized renderer");
        InitGLSettings();

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

    public void DrawFrame(long deltaTime)
    {
        Trace.beginSection("gl_draw_frame");

        //matrix calculation
        Matrix.setLookAtM(m_ViewMatrix, 0, 0f, 0f, 3f, 0f, 0f, 0f, 0f, 1.0f, 0.0f);

        Matrix.setIdentityM(m_ModelMatrix, 0);
        Matrix.multiplyMM(m_ModelMatrix, 0, m_ModelMatrix, 0, m_OrientationMatrix, 0);
        Matrix.translateM(m_ModelMatrix, 0, -4.0f, -10.0f, 0.0f);
        Matrix.multiplyMM(m_ModelMatrix, 0, m_ModelMatrix, 0, DEVICE_TO_OPENGL, 0);
        Matrix.rotateM(m_ModelMatrix, 0, (float)(SystemClock.elapsedRealtime()/10), 0, 1, 0);
        m_AlbumModel.SetMVP(m_ModelMatrix, m_ViewMatrix, m_ProjectionMatrix);

        //drawing
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);
        m_AlbumModel.Draw();

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
        m_OrientationMatrix = rotationMatrix.clone();
    }
}
