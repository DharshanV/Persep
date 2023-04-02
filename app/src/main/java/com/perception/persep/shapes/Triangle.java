package com.perception.persep.shapes;

import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class Triangle
{
    private final FloatBuffer m_VertexPosition;
    private final FloatBuffer m_VertexColor;
    private final float[] m_Transform = new float[16];

    static private final float[] triangleCoords = {
            -0.25f, -0.5f, 0.0f, // left
            0.25f, -0.5f, 0.0f, // right
            0.0f, 0.5f, 0.0f  // top
    };

    static private final float[] triangleColor = {
            1.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f,
            0.0f, 0.0f, 1.0f
    };

    public Triangle()
    {
        m_VertexPosition = ByteBuffer.allocateDirect(triangleCoords.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        m_VertexPosition.put(triangleCoords).position(0);

        m_VertexColor = ByteBuffer.allocateDirect(triangleColor.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        m_VertexColor.put(triangleColor).position(0);

        Matrix.setIdentityM(m_Transform, 0);
    }

    public FloatBuffer GetPositionBuffer()
    {
        return m_VertexPosition;
    }

    public FloatBuffer GetColorBuffer()
    {
        return m_VertexColor;
    }

    public float[] GetTransform()
    {
        return m_Transform;
    }
}
