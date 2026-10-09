package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONObject;
import mai_onsyn.renderer.interfaces.RendererInterface;
import weilantianhai.agent.ir.IRException;

/**
 * GET_TETRAHEDRON：获取单个四面体的四个顶点（文档 §6.2）。
 *
 * <p>参数：{@code name}、{@code tet}（该模型内的下标，从 0 开始）。
 *
 * <p>正文由渲染实现拼好，直接透传：
 *
 * <pre>
 * ## Tower/Base#0
 * v0: pos=(1.00, 1.00, 1.00, 1.00) color=#FFFF6666 normal=(1.00, 0.00, 0.00, 0.00)
 * v1: ...
 * </pre>
 *
 * <p>坐标为**局部坐标**（未乘模型矩阵）；下标越界会抛
 * {@code Tetrahedron index N is out of range [0, M) in "path"}。
 */
public class GetTetrahedronOperation extends GuardedOperation {

    private String name;
    private int tet;

    @Override
    public String name() {
        return "GET_TETRAHEDRON";
    }

    @Override
    protected void parse(JSONObject data) throws IRException {
        this.name = OperationParams.getRequiredPath(data, "name", "NAME");
        this.tet = OperationParams.getRequiredInt(data, "tet", "TET");
    }

    @Override
    protected String run(RendererInterface renderer) {
        return renderer.getGeometry().getTetrahedronInfos(name, tet);
    }
}
