precision mediump float;
uniform float dimmingFactor;
varying vec3 outColor;

void main()
{
    gl_FragColor = dimmingFactor * vec4(outColor, 1.0);
}