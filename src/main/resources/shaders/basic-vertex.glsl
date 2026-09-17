#version 330 core
layout (location = 0) in vec3 aPos;
layout (location = 1) in vec4 aColor;
layout (location = 2) in vec3 aNormal;
layout (location = 3) in vec2 aUV;

uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;

out vec4 vColor;
out vec3 vNormal;
out vec2 vUV;
// 给 phone 光照留的: 世界坐标 / 世界法线 (fragment 里 viewPos/lightPos 都是世界空间的)
out vec3 vWorldPos;
out vec3 vWorldNormal;

void main() {
    vec4 worldPos = model * vec4(aPos, 1.0);
    gl_Position = projection * view * worldPos;

    vColor = aColor;
    vNormal = aNormal;
    vUV = aUV;
    vWorldPos = worldPos.xyz;
    vWorldNormal = mat3(model) * aNormal;
}