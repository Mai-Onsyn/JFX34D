package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONObject;
import mai_onsyn.renderer.interfaces.AgentBridge;
import mai_onsyn.renderer.interfaces.RendererInterface;
import org.joml.Vector4f;
import weilantianhai.agent.ir.IRException;

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
