package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONObject;
import mai_onsyn.renderer.interfaces.RendererInterface;
import org.joml.Vector4f;
import weilantianhai.agent.ir.IRException;

/**
 * CREATE_BALL4：创建四维球的三维边界（3-球面）（文档 §5.2.7）。
 *
 * <p>参数：{@code target}、{@code name}、{@code center}、{@code radius}（{@code > 0}）、
 * {@code density}（{@code > 0}）。
 *
 * <pre>
 * # CREATE_BALL4(id=20)
 * Successfully create "Tower/Top" (BALL4)
 * Center=(0, 0, 0, 2.2), radius=0.9, density=2.0
 * Tetrahedrons: 384
 * </pre>
 *
 * <p>实现把超立方体的 8 个胞各细分成 {@code k×k×k} 个小立方体（cubed sphere），
 * {@code k = clamp(ceil(density), 1, 64)}，胞数 = {@code 8 · k³ · 6}。
 * 因为代价是 {@code O(k³)}，这里对 {@code density} 做了上限保护，避免 LLM 给一个
 * 夸张值直接把内存吃光。
 */
public class CreateBall4Operation extends GuardedOperation {

    /** density 上限；文档里渲染侧默认细分上限是 64（约 1260 万四面体），这里收紧到 24 防呆 */
    private static final float MAX_DENSITY = 24f;

    private ShapeTarget target;
    private Vector4f center;
    private float radius;
    private float density;

    @Override
    public String name() {
        return "CREATE_BALL4";
    }

    @Override
    protected void parse(JSONObject data) throws IRException {
        this.target = ShapeTarget.parse(data, "CREATE_BALL4");
        this.center = OperationParams.getRequiredVector4(data, "center", "CENTER");
        this.radius = OperationParams.getRequiredPositiveFloat(data, "radius", "RADIUS");
        this.density = OperationParams.getRequiredPositiveFloat(data, "density", "DENSITY");
        if (density > MAX_DENSITY) {
            throw new IRException("DENSITY_TOO_LARGE",
                    "density 不能超过 " + OperationParams.fmt(MAX_DENSITY)
                            + "（胞数是 O(k³)，过大会耗光内存）：" + density);
        }
    }

    @Override
    protected String run(RendererInterface renderer) {
        renderer.getShape().createBall4(target.target, target.name, center, radius, density);
        return "Successfully create \"" + target.path + "\" (BALL4)\n"
                + "Center=" + OperationParams.formatVector4(center)
                + ", radius=" + OperationParams.fmt(radius)
                + ", density=" + OperationParams.fmt(density) + "\n"
                + "Tetrahedrons: " + ModelQuery.tetrahedronText(renderer, target.path);
    }
}
