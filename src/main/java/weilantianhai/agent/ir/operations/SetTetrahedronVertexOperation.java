package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONObject;
import mai_onsyn.renderer.interfaces.AgentBridge;
import mai_onsyn.renderer.interfaces.RendererInterface;
import org.joml.Vector4f;
import weilantianhai.agent.ir.IRException;

public class SetTetrahedronVertexOperation extends GuardedOperation {
    private String name;
    private int tet;
    private int vertex;
    private Vector4f pos;
    private boolean hasColor;
    private int argb;
    private boolean hasNormal;
    private Vector4f normal;

    @Override
    public String name() {
        return "SET_TETRAHEDRON_VERTEX";
    }

    @Override
    protected void parse(JSONObject data) throws IRException {
        this.name = OperationParams.getRequiredPath(data, "name", "NAME");
        this.tet = OperationParams.getRequiredInt(data, "tet", "TET");
        this.vertex = OperationParams.getRequiredInt(data, "vertex", "VERTEX");
        if (vertex > OperationParams.MAX_VERTEX_INDEX) {
            throw new IRException("VERTEX_OUT_OF_RANGE",
                    "Vertex number must be in [0, " + OperationParams.MAX_VERTEX_INDEX
                            + "], got " + vertex);
        }
        this.pos = OperationParams.getRequiredVector4(data, "pos", "POS");

        Object rawColor = data.get("color");
        this.hasColor = rawColor != null;
        this.argb = hasColor
                ? OperationParams.parseArgb(rawColor, OperationParams.DEFAULT_COLOR, "color", "COLOR")
                : 0;

        this.normal = OperationParams.getOptionalVector4(data, "normal", "NORMAL");
        this.hasNormal = normal != null;
        if (!hasNormal) {
            normal = new Vector4f(0f, 0f, 0f, 0f);
        }
    }

    @Override
    protected String run(RendererInterface renderer) {
        AgentBridge.setVertex(name, tet, vertex, new Vector4f(pos), hasColor, argb, hasNormal, normal);

        StringBuilder sb = new StringBuilder();
        sb.append("Success\n\"")
          .append(name).append("\" #").append(tet).append(" v").append(vertex)
          .append(" = pos").append(OperationParams.formatVector4(pos));
        if (hasColor) {
            sb.append(" color=").append(OperationParams.formatArgb(argb));
        }
        sb.append("\nNow kind is CARVED (params no longer describe it)");
        return sb.toString();
    }
}
