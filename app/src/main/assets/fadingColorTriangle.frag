precision mediump float;

varying vec3 outColor;
//https://stackoverflow.com/questions/30119553/shader-simple-sss-lighting-issue
void main()
{
    gl_FragColor = vec4(outColor, 1.0);
}