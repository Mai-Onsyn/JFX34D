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

// 光源数组 (全部世界空间)。数组长度必须和 GL3DEngine.MAX_LIGHTS 一致
const int MAX_LIGHTS = 16;
uniform int lightCount;
uniform vec3 lightPositions[MAX_LIGHTS];
uniform vec3 lightColors[MAX_LIGHTS];
// 面光源朝向; 零向量表示点光源 (和 C++ 侧 LightType::Point / Face 对应)
uniform vec3 lightDirs[MAX_LIGHTS];
// 每个光源 4 个: intensity / range / a / b
uniform vec4 lightParams[MAX_LIGHTS];

// phone 光照 (全部世界空间)
uniform vec3 viewPos;       // 相机位置
uniform vec3 ambient;       // 全局环境光
// 光照总开关。false 时完全不参与光照计算, 直接输出材质本来的颜色
uniform bool uLighting;

void main() {
    // 有主贴图就 贴图 * kd, 否则退回顶点色
    vec4 base = uUseTexture ? texture(material.mapKd, vUV) * material.kd : vColor;
    vec3 albedo = base.rgb;
    // 输出必须带上 alpha: 混合和 openglfx 合成都要靠它
    float alpha = base.a * material.d;

    // 关掉光照: 材质原样输出, 连光源循环都不进
    if (!uLighting) {
        FragColor = vec4(albedo, alpha);
        return;
    }

    // ---- phone 光照 ----
    vec3 N = normalize(vWorldNormal);
    vec3 V = normalize(viewPos - vWorldPos);

    // 环境光 = ambient * Ka * 贴图色
    vec3 sum = ambient * material.ka * albedo;

    // 遍历光源累加, 每一项和 C++ 的 fragmentShader_Sequence 对齐
    for (int i = 0; i < lightCount; i++) {
        vec3 LDir = lightPositions[i] - vWorldPos;
        float d_i = length(LDir);
        // 超出有效距离的光源直接跳过
        if (d_i > lightParams[i].y) continue;
        vec3 L = LDir / d_i;

        // 距离衰减
        float attnDist = 1.0 / (1.0 + lightParams[i].z * d_i + lightParams[i].w * d_i * d_i);

        // 方向衰减: 面光源只照朝向的反方向, 点光源恒为 1
        vec3 dir = lightDirs[i];
        float attnDir = dot(dir, dir) > 0.0 ? max(0.0, -dot(dir, L)) : 1.0;

        // 最终光强 = 颜色 * 强度 * 距离衰减 * 方向衰减
        vec3 I_light = lightColors[i] * (lightParams[i].x * attnDist * attnDir);

        // 半兰伯特漫反射: (N.L * 0.5 + 0.5)^2
        float halfLambert = dot(N, L) * 0.5 + 0.5;
        vec3 diffuse = I_light * halfLambert * halfLambert;

        // 镜面反射: Blinn-Phone 半程向量
        vec3 H = normalize(V + L);
        vec3 specular = I_light * pow(max(0.0, dot(N, H)), max(material.ns, 1.0));

        sum += albedo * diffuse + material.ks * specular;
    }

    // 多光源叠加很容易过 1, 交给混合前先钳一下
    FragColor = vec4(clamp(sum, 0.0, 1.0), alpha);
}
