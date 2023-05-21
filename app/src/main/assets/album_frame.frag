precision mediump float;

varying vec3 vFragmentPos;
varying vec3 vFragmentNormal;

void main()
{
    vec3 lightColor = vec3(1.0);
    vec3 lightDir = normalize(vec3(1, -1, -1));
    vec3 objectColor = vec3(1.0);

    // ambient
    float ambientStrength = 0.35;
    vec3 ambient = ambientStrength * lightColor;

    // diffuse
    vec3 norm = normalize(vFragmentNormal);
    float diff = max(dot(norm, lightDir), 0.0);
    vec3 diffuse = diff * lightColor;

    vec3 result = (ambient + diffuse) * objectColor;
    gl_FragColor = vec4(result, 1.0);
}