attribute vec4 inPosition;
attribute vec3 inColor;

uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;

varying vec3 outColor;

void main()
{
    outColor = inColor;
    gl_Position = projection * view * model * inPosition;
}