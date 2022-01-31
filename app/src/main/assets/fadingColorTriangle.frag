precision mediump float;

varying vec3 vFragmentPos;
varying vec2 vFragmentUV;
varying vec3 vFragmentNormal;

//https://stackoverflow.com/questions/30119553/shader-simple-sss-lighting-issue
void main()
{
    vec3 viewPos = vec3(0f, 0f, 3f);
    vec3 objectColor = vec3(0.95f);
    vec3 lightPos = vec3(0, 10, 0);
    vec3 lightColor = vec3(0.7f);

    float ambientStrength = 0.1;
    vec3 ambient = ambientStrength * lightColor;

    vec3 norm = normalize(vFragmentNormal);
    vec3 lightDir = normalize(lightPos - vFragmentPos);
    float diff = max(dot(norm, lightDir), 0.0);
    vec3 diffuse = diff * lightColor;

    float specularStrength = 0.1;
    vec3 viewDir = normalize(viewPos - vFragmentPos);
    vec3 reflectDir = reflect(-lightDir, norm);
    float spec = pow(max(dot(viewDir, reflectDir), 0.0f), 8.0f);
    vec3 specular = specularStrength * spec * lightColor;

    vec3 result = (ambient + diffuse + specular) * objectColor;
    gl_FragColor = vec4(result, 1.0);
}