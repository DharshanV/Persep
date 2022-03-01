precision mediump float;

varying vec3 vFragmentPos;
varying vec2 vFragmentUV;
varying vec3 vFragmentNormal;

uniform sampler2D mTexture;

void main()
{
    vec2 texCoord = vFragmentUV;
    texCoord.y = 1.0 - texCoord.y;
    gl_FragColor = texture2D(mTexture, texCoord);
}