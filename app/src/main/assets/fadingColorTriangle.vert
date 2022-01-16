attribute vec4 inPosition;
attribute vec3 inColor;

varying vec3 outColor;

void main()
{
    outColor = inColor;
    gl_Position = inPosition;
}