precision mediump float;

varying vec3 vFragmentPos;
varying vec2 vFragmentUV;
varying vec3 vFragmentNormal;

uniform sampler2D mTexture;

void main()
{
    gl_FragColor = texture2D(mTexture, vFragmentUV);
}