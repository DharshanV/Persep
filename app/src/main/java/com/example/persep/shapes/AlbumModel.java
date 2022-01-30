package com.example.persep.shapes;

import android.content.Context;
import android.opengl.GLES30;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class AlbumModel
{
    static private FloatBuffer m_VertexPosition = null;
    static private ShortBuffer m_VertexIndices = null;
    static private FloatBuffer m_VertexUV = null;
    static private FloatBuffer m_VertexColor = null;

    private static final float[] VERTICES = {
            -1.0f, -0.75f, -0.1f,    //v0
            1.0f, -0.75f, -0.1f,     //v1
            1.0f,  0.75f, -0.1f,     //v3
            -1.0f, 0.75f, -0.1f,     //v2
            -1.0f, -0.75f,  0.1f,    //v6
            1.0f, -0.75f,  0.1f,     //v4
            1.0f,  0.75f,  0.1f,     //v5
            -1.0f,  0.75f,  0.1f     //v7
    };

    private static final float[] COLORS = {
            0.0f,  1.0f,  0.0f,
            0.0f,  1.0f,  0.0f,
            1.0f,  0.5f,  0.0f,
            1.0f,  0.5f,  0.0f,
            1.0f,  0.0f,  0.0f,
            1.0f,  0.0f,  0.0f,
            0.0f,  0.0f,  1.0f,
            1.0f,  0.0f,  1.0f
    };

    private static final short[] INDICES = {
            0, 4, 5, 0, 5, 1,
            1, 5, 6, 1, 6, 2,
            2, 6, 7, 2, 7, 3,
            3, 7, 4, 3, 4, 0,
            4, 7, 6, 4, 6, 5,
            3, 0, 1, 3, 1, 2
    };

    private float[] m_Transform = new float[16];

    public AlbumModel()
    {
        Matrix.setIdentityM(m_Transform, 0);
    }

    public void Draw()
    {
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, INDICES.length, GLES30.GL_UNSIGNED_SHORT, m_VertexIndices);
    }

    public float[] GetTransform() { return m_Transform; }

    static public void LoadModel(Context context, String modelPath)
    {
        m_VertexPosition = ByteBuffer.allocateDirect(VERTICES.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        m_VertexPosition.put(VERTICES).position(0);

        m_VertexIndices = ByteBuffer.allocateDirect(INDICES.length * 2)
                .order(ByteOrder.nativeOrder()).asShortBuffer();
        m_VertexIndices.put(INDICES).position(0);

        m_VertexColor = ByteBuffer.allocateDirect(COLORS.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        m_VertexColor.put(COLORS).position(0);
    }

    static public FloatBuffer GetPositionBuffer() { return m_VertexPosition; }

    static public ShortBuffer GetIndicesBuffer() { return m_VertexIndices; }

    static public FloatBuffer GetUVBuffer() { return m_VertexUV; }

    static public FloatBuffer GetColorBuffer() { return m_VertexColor; }
}
