package com.example.persep.shapes;

import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class Triangle
{
    private FloatBuffer m_VertexPosition;
    private FloatBuffer m_VertexColor;

    private float[] m_Transform = new float[16];

    public Triangle()
    {
        float[] triangleCoords = {
            -0.5f, -0.5f, -2.0f, // left
             0.5f, -0.5f, -2.0f, // right
             0.0f,  0.5f, -2.0f  // top
        };
        m_VertexPosition = ByteBuffer.allocateDirect(triangleCoords.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        m_VertexPosition.put(triangleCoords).position(0);

        float[] triangleColor = {
                1.0f, 0.0f, 0.0f,
                0.0f, 1.0f, 0.0f,
                0.0f, 0.0f, 1.0f
        };
        m_VertexColor = ByteBuffer.allocateDirect(triangleColor.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        m_VertexColor.put(triangleColor).position(0);

        Matrix.setIdentityM(m_Transform, 0);
    }

    public FloatBuffer GetPositionBuffer() { return m_VertexPosition; }

    public FloatBuffer GetColorBuffer() { return m_VertexColor; }

    public float[] GetTransform() { return m_Transform; }
}
