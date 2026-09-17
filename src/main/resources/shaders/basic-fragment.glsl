#version 330 core
in vec4 vColor;
in vec3 vNormal;
in vec2 vUV;
out vec4 FragColor;

uniform sampler2D uTexture;
uniform bool uUseTexture;

void main() {
    vec4 base = uUseTexture ? texture(uTexture, vUV) : vColor;
    FragColor = base;
}