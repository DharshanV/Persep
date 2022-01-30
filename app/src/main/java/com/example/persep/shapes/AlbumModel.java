package com.example.persep.shapes;

import android.content.Context;
import android.opengl.GLES30;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class AlbumModel
{
    static private FloatBuffer m_VertexPosition = null;
    static private FloatBuffer m_VertexUV = null;
    static private FloatBuffer m_VertexColor = null;

    private float[] m_Transform = new float[16];

    public AlbumModel()
    {
        Matrix.setIdentityM(m_Transform, 0);
    }

    public void Draw()
    {
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 3);
    }

    public float[] GetTransform() { return m_Transform; }

    static public void LoadModel(Context context, String modelPath)
    {
        final float[] triangleCoords = {
                -0.25f, -0.5f, 0.0f, // left
                0.25f, -0.5f, 0.0f, // right
                0.0f,  0.5f, 0.0f  // top
        };

        final float[] triangleColor = {
                1.0f, 0.0f, 0.0f,
                0.0f, 1.0f, 0.0f,
                0.0f, 0.0f, 1.0f
        };

        m_VertexPosition = ByteBuffer.allocateDirect(triangleCoords.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        m_VertexPosition.put(triangleCoords).position(0);

        m_VertexColor = ByteBuffer.allocateDirect(triangleColor.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        m_VertexColor.put(triangleColor).position(0);
    }

    static public FloatBuffer GetPositionBuffer() { return m_VertexPosition; }

    static public FloatBuffer GetUVBuffer() { return m_VertexUV; }

    static public FloatBuffer GetColorBuffer() { return m_VertexColor; }
}
