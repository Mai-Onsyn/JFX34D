package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONObject;
import mai_onsyn.renderer.interfaces.RendererInterface;
import weilantianhai.agent.ir.IRException;

/**
 * APPLY_TRANSFORM_TO_VERTEX：把变换矩阵烘焙进顶点，产出顶点已在世界坐标的新模型（文档 §4.1.6）。
 *
 * <p>参数：{@code name}（源路径，必须存在）、{@code dest}（目标路径，必须不存在）。
 *
 * <pre>
 * # APPLY_TRANSFORM_TO_VERTEX(id=11)
 * Successfully apply transform of "Tower/Base"
 * Created "Tower/Baked", matrix is identity
 * Tetrahedrons: 48
 * </pre>
 *
 * <p>语义：{@code p_new = M · p_old}；结果的 {@code kind} 为 {@code CARVED}；源模型不变。
 *
 * <p><b>注意</b>：早期实现会无条件要求 {@code dest} 不存在，导致文档描述的
 * "原地烘焙"（{@code dest == name}）必然失败；文档 §4.1.6 现在也明确要求
 * {@code dest} 不存在，所以这里按"必须换一个新路径"来处理，并在 {@code dest == name}
 * 时给出明确错误，避免它崩在渲染层。
 */
public class ApplyTransformToVertexOperation extends GuardedOperation {

    private String name;
    private String dest;

    @Override
    public String name() {
        return "APPLY_TRANSFORM_TO_VERTEX";
    }

    @Override
    protected void parse(JSONObject data) throws IRException {
        this.name = OperationParams.getRequiredPath(data, "name", "NAME");
        this.dest = OperationParams.getRequiredPath(data, "dest", "DEST");
        if (name.equals(dest)) {
            throw new IRException("DEST_EQUALS_NAME",
                    "dest 不能等于 name（不支持原地烘焙），请换一个新路径：" + dest);
        }
    }

    @Override
    protected String run(RendererInterface renderer) {
        renderer.getModel().applyTransformToVertex(name, dest);
        return "Successfully apply transform of \"" + name + "\"\n"
                + "Created \"" + dest + "\", matrix is identity\n"
                + "Tetrahedrons: " + ModelQuery.tetrahedronText(renderer, dest);
    }
}
