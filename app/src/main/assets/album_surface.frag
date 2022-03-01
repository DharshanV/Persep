precision mediump float;

varying vec3 vFragmentPos;
varying vec2 vFragmentUV;
varying vec3 vFragmentNormal;

void main()
{
    gl_FragColor = vec4(vFragmentUV.x, vFragmentUV.y, 0.0, 1.0);
}