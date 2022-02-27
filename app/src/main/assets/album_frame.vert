attribute vec4 iPosition;
attribute vec3 iNormal;

uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;

varying vec3 vFragmentPos;
varying vec3 vFragmentNormal;

void main()
{
    vFragmentPos =  (model * iPosition).xyz;
    vFragmentNormal =  (model * vec4(iNormal, 0.0)).xyz;
    gl_Position = projection * view * model * iPosition;
}