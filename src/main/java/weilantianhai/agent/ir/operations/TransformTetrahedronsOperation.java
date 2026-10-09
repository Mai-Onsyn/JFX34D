package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONObject;
import mai_onsyn.renderer.cpu4dkt.Matrix5f;
import mai_onsyn.renderer.interfaces.RendererInterface;
import weilantianhai.agent.ir.IRException;

import java.util.List;

/**
 * TRANSFORM_TETRAHEDRONS：对指定四面体应用 5×5 变换矩阵（**直接改顶点**，模型矩阵不变）（文档 §6.4）。
 *
 * <p>参数：{@code name}、{@code tets}（下标数组；**空数组 {@code []} 表示该模型内全部四面体**）、
 * {@code row0}~{@code row4}（5×5 行主序）。
 *
 * <pre>
 * # TRANSFORM_TETRAHEDRONS(id=25)
 * Success
 * "Tower/Lid" transformed 2 tetrahedrons: [0, 2]
 * Now kind is CARVED
 * </pre>
 */
public class TransformTetrahedronsOperation extends GuardedOperation {

    private String name;
    private List<Integer> tets;
    private float[] matrix;

    @Override
    public String name() {
        return "TRANSFORM_TETRAHEDRONS";
    }

    @Override
    protected void parse(JSONObject data) throws IRException {
        this.name = OperationParams.getRequiredPath(data, "name", "NAME");
        this.tets = OperationParams.getOptionalIntList(data, "tets", "TETS");
        if (tets == null) {
            // 文档里 tets 是必填，但空数组表示"全部"；缺失时按"全部"处理更宽容
            tets = List.of();
        }
        this.matrix = OperationParams.getRequiredMatrix5(data, "MATRIX");
    }

    @Override
    protected String run(RendererInterface renderer) {
        renderer.getGeometry().transformTetrahedrons(name, tets, new Matrix5f(matrix));
        String scope = tets.isEmpty()
                ? "all tetrahedrons"
                : tets.size() + (tets.size() == 1 ? " tetrahedron: " : " tetrahedrons: ") + tets;
        return "Success\n\"" + name + "\" transformed " + scope + "\nNow kind is CARVED";
    }
}
