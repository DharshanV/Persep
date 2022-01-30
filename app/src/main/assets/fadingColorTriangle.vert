attribute vec4 inPosition;
attribute vec3 inNormal;
attribute vec3 inColor;

uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;

varying vec3 outColor;
varying vec3 v_fragmentPos;
varying vec3 v_normal;

void main()
{
    outColor = inColor;
    v_fragmentPos =  (model * inPosition).xyz;
    v_normal =  (model * vec4(inNormal, 0.0)).xyz;
    gl_Position = projection * view * model * inPosition;
}