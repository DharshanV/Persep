package com.example.persep.render;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Trace;
import android.util.Log;

import com.example.persep.shapes.Triangle;

public class OpenGLRenderer
{
    private static final String TAG = "Renderer";

    private Context m_Context;
    private int m_Width;
    private int m_Height;

    private final float[] m_ProjectionMatrix = new float[16];
    private final float[] m_ViewMatrix = new float[16];

    private Triangle m_Triangle;
    private Shader m_TriangleShader;

    private float[] m_OrientationMatrix = new float[16];

    OpenGLRenderer(Context context)
    {
        m_Context = context;

        Matrix.setIdentityM(m_ProjectionMatrix, 0);
        Matrix.setIdentityM(m_ViewMatrix, 0);
        Matrix.setIdentityM(m_OrientationMatrix, 0);
    }

    public void Initialize()
    {
        Log.d(TAG, "Initialized renderer");
        InitGLSettings();

        m_Triangle = new Triangle();
        m_TriangleShader = new Shader();
        m_TriangleShader.CreateShaderProgram(m_Context, "fadingColorTriangle.vert", "fadingColorTriangle.frag");
        m_TriangleShader.SetFloatVertexArray("inPosition", 3, 3 * 4, m_Triangle.GetPositionBuffer());
        m_TriangleShader.SetFloatVertexArray("inColor", 3, 3 * 4, m_Triangle.GetColorBuffer());
    }

    private void InitGLSettings()
    {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
//        GLES20.glEnable(GLES20.GL_CULL_FACE);
//        GLES20.glCullFace(GLES20.GL_BACK);
    }

    public void DrawFrame(long deltaTime)
    {
        Trace.beginSection("gl_draw_frame");

        Matrix.setLookAtM(m_ViewMatrix, 0, 0f, 0f, 3f, 0f, 0f, 0f, 0f, 1.0f, 0.0f);

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        Matrix.setIdentityM(m_Triangle.GetTransform(), 0);
        Matrix.multiplyMM(m_Triangle.GetTransform(), 0, m_Triangle.GetTransform(), 0, m_OrientationMatrix, 0);
        Matrix.translateM(m_Triangle.GetTransform(), 0, -4.0f, -10.0f, 0.0f);
        Matrix.rotateM(m_Triangle.GetTransform(), 0, 90, 1, 0, 0);

        m_TriangleShader.UseProgram();
        m_TriangleShader.SetMatrix4("model", m_Triangle.GetTransform());
        m_TriangleShader.SetMatrix4("view", m_ViewMatrix);
        m_TriangleShader.SetMatrix4("projection", m_ProjectionMatrix);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3);

        Trace.endSection();
    }

    public void Resize(int width, int height)
    {
        Log.d(TAG, "Resized | width: " + width + " height: " + height);
        m_Width = width;
        m_Height = height;
        GLES20.glViewport(0, 0, m_Width, m_Height);

        float ratio = (float) m_Width / m_Height;
        Matrix.frustumM(m_ProjectionMatrix, 0, -ratio, ratio, -1, 1, 3, 1000);
    }

    public void UpdateRotation(float[] rotationMatrix)
    {
        m_OrientationMatrix = rotationMatrix.clone();
    }
}
