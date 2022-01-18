attribute vec4 inPosition;
attribute vec3 inColor;

uniform mat4 inMVPMatrix;

varying vec3 outColor;

void main()
{
    outColor = inColor;
    gl_Position = inMVPMatrix * inPosition;
}