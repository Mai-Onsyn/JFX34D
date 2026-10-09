package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONObject;
import mai_onsyn.renderer.interfaces.AgentBridge;
import mai_onsyn.renderer.interfaces.RendererInterface;
import org.joml.Vector4f;
import weilantianhai.agent.ir.IRException;

/**
 * SLICE_MODEL：用一个超平面把模型**精确**切成两半，产出两个新模型（文档 §6.7）。
 *
 * <p>参数：
 * <ul>
 *   <li>{@code name} — 源模型路径（必须是无子节点的真实模型）</li>
 *   <li>{@code plane} — 超平面，由四个**仿射无关**的顶点定义，坐标取**源模型的局部坐标系**；
 *       四个顶点各给一个 {@code pos} 即可（颜色/法向量对切片无意义）</li>
 *   <li>{@code pathA} — {@code f(p) < 0} 那一半的路径，必须不存在</li>
 *   <li>{@code pathB} — {@code f(p) >= 0} 那一半的路径，必须不存在且与 {@code pathA} 不同</li>
 * </ul>
 *
 * <pre>
 * # SLICE_MODEL(id=28)
 * Successfully slice "Sculpt/Box"
 * Created "Sculpt/Left" (42 tetrahedrons, f &lt; 0)
 * Created "Sculpt/Right" (6 tetrahedrons, f &gt;= 0)
 * Source "Sculpt/Box" is unchanged
 * </pre>
 *
 * <p>切片是真正的 {@code 1:3} / {@code 2:2} 切分（体积守恒）；两个产物都是 {@code CARVED}；
 * 源模型保持不变。
 *
 * <p><b>跨语言说明</b>：渲染接口的 {@code plane} 参数是一个 {@code Tetrahedron}，
 * 而 Java 无法构造 {@code Vertex4D}，所以经由 {@link AgentBridge#sliceModel} 转发，
 * 四个点用 16 个 float 传递。
 */
public class SliceModelOperation extends GuardedOperation {

    private static final String[] PLANE_KEYS = {"v0", "v1", "v2", "v3"};

    private String name;
    private String pathA;
    private String pathB;
    private final float[] planePositions = new float[16];

    @Override
    public String name() {
        return "SLICE_MODEL";
    }

    @Override
    protected void parse(JSONObject data) throws IRException {
        this.name = OperationParams.getRequiredPath(data, "name", "NAME");
        this.pathA = OperationParams.getRequiredPath(data, "pathA", "PATH_A");
        this.pathB = OperationParams.getRequiredPath(data, "pathB", "PATH_B");
        if (pathA.equals(pathB)) {
            throw new IRException("SLICE_DEST_SAME",
                    "Slice destinations must differ, both are \"" + pathA + "\"");
        }

        Object rawPlane = data.get("plane");
        if (rawPlane == null) {
            throw new IRException("PLANE_MISSING", "缺少必填字段 plane");
        }
        if (!(rawPlane instanceof JSONObject plane)) {
            throw new IRException("PLANE_INVALID", "plane 必须是对象，含 v0~v3 四个顶点");
        }
        for (int i = 0; i < 4; i++) {
            String key = PLANE_KEYS[i];
            Object raw = plane.get(key);
            if (raw == null) {
                throw new IRException("PLANE_VERTEX_MISSING", "plane 缺少必填字段 " + key);
            }
            if (!(raw instanceof JSONObject v)) {
                throw new IRException("PLANE_VERTEX_INVALID",
                        "plane." + key + " 必须是对象，形如 {\"pos\":[0,0,0,0]}");
            }
            Object rawPos = v.get("pos");
            if (rawPos == null) {
                throw new IRException("PLANE_VERTEX_POS_MISSING", "plane." + key + " 缺少 pos");
            }
            Vector4f p = OperationParams.toVector4(rawPos, "plane." + key + ".pos", "PLANE");
            planePositions[i * 4] = p.x;
            planePositions[i * 4 + 1] = p.y;
            planePositions[i * 4 + 2] = p.z;
            planePositions[i * 4 + 3] = p.w;
        }
    }

    @Override
    protected String run(RendererInterface renderer) {
        AgentBridge.sliceModel(name, planePositions, pathA, pathB);
        return "Successfully slice \"" + name + "\"\n"
                + "Created \"" + pathA + "\" (" + ModelQuery.tetrahedronText(renderer, pathA)
                + " tetrahedrons, f < 0)\n"
                + "Created \"" + pathB + "\" (" + ModelQuery.tetrahedronText(renderer, pathB)
                + " tetrahedrons, f >= 0)\n"
                + "Source \"" + name + "\" is unchanged";
    }
}
