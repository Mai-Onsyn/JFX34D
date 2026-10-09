package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONObject;
import mai_onsyn.renderer.interfaces.AgentBridge;
import mai_onsyn.renderer.interfaces.RendererInterface;
import org.joml.Vector4f;
import weilantianhai.agent.ir.IRException;

/**
 * ADD_TETRAHEDRON：直接指定四个顶点，往模型里追加一个四面体（文档 §6.5）。
 *
 * <p>参数：{@code name}、{@code v0}~{@code v3}（每个是 {@code {pos, color?, normal?}}），
 * 可选操作级 {@code color}（一次指定四个顶点，会被顶点自己的 {@code color} 覆盖）。
 *
 * <pre>
 * # ADD_TETRAHEDRON(id=26)
 * Successfully add tetrahedron #0 to "Sculpt/Patch"
 * Tetrahedrons: 1
 * Now kind is CARVED
 * </pre>
 *
 * <p>往一个空模型（{@code GROUP}）里加胞是合法的，它会变成 {@code CARVED}。
 *
 * <p><b>跨语言说明</b>：Java 无法构造渲染侧的 {@code Vertex4D}（构造函数 private、
 * 公开那个带 {@code DefaultConstructorMarker} 且是 synthetic），所以经由
 * {@link AgentBridge#addTetrahedron} 转发，位置/颜色/法向都用扁平数组传递。
 *
 * <p><b>法向量缺省</b>：四个顶点的法向都留空时，由渲染侧补一个由这四个点算出的
 * 超平面单位法向量（{@code hyperplaneNormal}）。
 */
public class AddTetrahedronOperation extends GuardedOperation {

    private static final String[] VERTEX_KEYS = {"v0", "v1", "v2", "v3"};

    private String name;
    private final float[] positions = new float[16];
    private final int[] colors = new int[4];
    private final float[] normals = new float[16];

    @Override
    public String name() {
        return "ADD_TETRAHEDRON";
    }

    @Override
    protected void parse(JSONObject data) throws IRException {
        this.name = OperationParams.getRequiredPath(data, "name", "NAME");
        int defaultArgb = OperationParams.parseArgb(
                data.get("color"), OperationParams.DEFAULT_COLOR, "color", "COLOR");

        for (int i = 0; i < 4; i++) {
            String key = VERTEX_KEYS[i];
            Object raw = data.get(key);
            if (raw == null) {
                throw new IRException("VERTEX_MISSING", "缺少必填字段 " + key);
            }
            if (!(raw instanceof JSONObject v)) {
                throw new IRException("VERTEX_INVALID",
                        key + " 必须是对象，形如 {\"pos\":[0,0,0,0], \"color\":\"#FFB0B0B0\"}");
            }
            Object rawPos = v.get("pos");
            if (rawPos == null) {
                throw new IRException("VERTEX_POS_MISSING", key + " 缺少 pos");
            }
            Vector4f pos = OperationParams.toVector4(rawPos, key + ".pos", "VERTEX");
            positions[i * 4] = pos.x;
            positions[i * 4 + 1] = pos.y;
            positions[i * 4 + 2] = pos.z;
            positions[i * 4 + 3] = pos.w;

            colors[i] = OperationParams.parseArgb(
                    v.get("color"), defaultArgb, key + ".color", "VERTEX");

            Vector4f n = OperationParams.getOptionalVector4(v, "normal", "VERTEX");
            if (n != null) {
                normals[i * 4] = n.x;
                normals[i * 4 + 1] = n.y;
                normals[i * 4 + 2] = n.z;
                normals[i * 4 + 3] = n.w;
            }
            // 缺失时保持 0，交给渲染侧按"全部为零"补超平面法向量
        }
    }

    @Override
    protected String run(RendererInterface renderer) {
        int index = AgentBridge.addTetrahedron(name, positions, colors, normals);
        return "Successfully add tetrahedron #" + index + " to \"" + name + "\"\n"
                + "Tetrahedrons: " + ModelQuery.tetrahedronText(renderer, name) + "\n"
                + "Now kind is CARVED";
    }
}
