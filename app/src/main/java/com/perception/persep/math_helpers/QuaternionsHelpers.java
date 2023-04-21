package com.perception.persep.math_helpers;

public class QuaternionsHelpers
{
    public static float[] negate(float[] a)
    {
        return new float[]{-a[0], -a[1], -a[2], -a[3]};
    }

    public static double dot(float[] a, float[] b)
    {
        return a[0]*b[0] + a[1]*b[1] + a[2]*b[2] + a[3]*b[3];
    }

    public static float[] normalise(float[] a)
    {
        float l = (float) (1.0f / Math.sqrt(dot(a, a)));
        return new float[]{l * a[0], l * a[1], l * a[2], l * a[3]};
    }

    public static float[] lerp(float[] a, float[] b, float t)
    {
        // negate second quat if dot product is negative
        double l2 = dot(a, b);
        if (l2 < 0.0)
        {
            b = negate(b);
        }
        float[] c = new float[4];
        // c = a + t(b - a)  -->   c = a - t(a - b)
        // the latter is slightly better on x64
        c[0] = a[0] - t * (a[0] - b[0]);
        c[1] = a[1] - t * (a[1] - b[1]);
        c[2] = a[2] - t * (a[2] - b[2]);
        c[3] = a[3] - t * (a[3] - b[3]);
        return c;
    }

    // this is the method you want
    public static float[] nlerp(float[] a, float[] b, float t)
    {
        return normalise(lerp(a, b, t));
    }
}
