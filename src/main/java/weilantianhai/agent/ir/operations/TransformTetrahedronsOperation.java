package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONObject;
import mai_onsyn.renderer.cpu4dkt.Matrix5f;
import mai_onsyn.renderer.interfaces.RendererInterface;
import weilantianhai.agent.ir.IRException;

import java.util.List;

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
