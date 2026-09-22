#version 330 core
in vec4 vColor;
in vec3 vNormal;
in vec2 vUV;
// phone 光照用: 世界坐标 / 世界法线
in vec3 vWorldPos;
in vec3 vWorldNormal;
out vec4 FragColor;

// 材质参数, 由 Mesh 按材质分组上传
uniform struct Material {
    vec3 ka;        // 环境光颜色
    vec4 kd;        // 漫反射颜色
    vec3 ks;        // 镜面反射颜色
    float ns;       // 镜面反射指数
    float d;        // 不透明度
    sampler2D mapKd;
    sampler2D mapKs;
    sampler2D mapD;
    sampler2D mapBump;
} material;

uniform bool uUseTexture;   // 有主贴图就走贴图, 否则走顶点色

// phone 光照 (全部世界空间)
uniform vec3 viewPos;       // 相机位置
uniform vec3 ambient;       // 全局环境光
uniform vec3 lightPos;
uniform vec3 lightColor;
uniform float lightIntensity;
uniform float lightRange;
uniform float attnA;        // 距离衰减 1 / (1 + a*d + b*d*d)
uniform float attnB;

void main() {
    // 有主贴图就 贴图 * kd, 否则退回顶点色
    vec4 base = uUseTexture ? texture(material.mapKd, vUV) * material.kd : vColor;
    vec3 albedo = base.rgb;
    float alpha = base.a * material.d;

    // 完全透明的像素丢掉, 不然它会写深度挡住后面的东西
    if (uUseTexture && alpha <= 0.0) discard;

    // ---- phone 光照 ----
    vec3 N = normalize(vWorldNormal);
    vec3 V = normalize(viewPos - vWorldPos);

    // 环境光 = ambient * Ka * 贴图色
    vec3 sum = ambient * material.ka * albedo;

    vec3 LDir = lightPos - vWorldPos;
    float d_i = length(LDir);
    if (d_i <= lightRange) {
        vec3 L = LDir / d_i;

        // 距离衰减 + 光强
        float attnDist = 1.0 / (1.0 + attnA * d_i + attnB * d_i * d_i);
        vec3 I_light = lightColor * (lightIntensity * attnDist);

        // 半兰伯特漫反射: (N.L * 0.5 + 0.5)^2
        float halfLambert = dot(N, L) * 0.5 + 0.5;
        vec3 diffuse = I_light * halfLambert * halfLambert;

        // 镜面反射: Blinn-Phone 半程向量
        vec3 H = normalize(V + L);
        vec3 specular = I_light * pow(max(0.0, dot(N, H)), max(material.ns, 1.0));

        sum += albedo * diffuse + material.ks * specular;
    }

    FragColor = vec4(sum, alpha);
//    FragColor = base;
}
