package com.example.persep.shapes;

import android.content.Context;
import android.opengl.GLES30;
import android.opengl.Matrix;

import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import de.javagl.obj.Obj;
import de.javagl.obj.ObjData;
import de.javagl.obj.ObjReader;
import de.javagl.obj.ObjUtils;

public class AlbumModel
{
    static private FloatBuffer m_VertexPosition = null;
    static private FloatBuffer m_VertexNormals = null;
    static private IntBuffer m_VertexIndices = null;
    static private FloatBuffer m_VertexUV = null;
    static private int m_NumOfIndices = 0;

    private float[] m_Transform = new float[16];

    public AlbumModel()
    {
        Matrix.setIdentityM(m_Transform, 0);
    }

    public void Draw()
    {
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, m_NumOfIndices * 3, GLES30.GL_UNSIGNED_INT, m_VertexIndices);
    }

    public float[] GetTransform() { return m_Transform; }

    static public void LoadModel(Context context, String modelPath)
    {
        InputStream in = null;
        try
        {
            in = context.getAssets().open(modelPath);
            Obj obj = ObjUtils.convertToRenderable(ObjReader.read(in));
            m_NumOfIndices = obj.getNumFaces();
            m_VertexIndices = ObjData.getFaceVertexIndices(obj, 3);
            m_VertexPosition = ObjData.getVertices(obj);
            m_VertexUV = ObjData.getTexCoords(obj, 2);
            m_VertexNormals = ObjData.getNormals(obj);
        } catch (Exception ex) { throw new RuntimeException("couldn't load '" + modelPath + "'", ex); }
        finally
        {
            if (in != null)
            {
                try { in.close(); } catch (Exception ex) { ex.printStackTrace(); }
            }
        }
    }

    static public FloatBuffer GetPositionBuffer() { return m_VertexPosition; }

    static public FloatBuffer GetNormalBuffer() { return m_VertexNormals; }

    static public FloatBuffer GetUVBuffer() { return m_VertexUV; }
}
