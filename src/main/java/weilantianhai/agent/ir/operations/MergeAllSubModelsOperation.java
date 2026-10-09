package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONObject;
import mai_onsyn.renderer.interfaces.RendererInterface;
import weilantianhai.agent.ir.IRException;

/**
 * MERGE_ALL_SUB_MODELS：把 {@code src} 命中的节点**连同它整棵子树**烘焙后合并成一个新模型
 * （文档 §4.1.8）。
 *
 * <p>参数：
 * <ul>
 *   <li>{@code src} — 源路径；既可以是真实模型，也可以是分组路径
 *       （命中时作用于"自身 + 全部后代"）</li>
 *   <li>{@code dst} — 新路径，必须不存在</li>
 * </ul>
 *
 * <pre>
 * # MERGE_ALL_SUB_MODELS(id=13)
 * Successfully merge 3 models into "Tower/All"
 * Tetrahedrons: 492
 * </pre>
 *
 * <p>与 {@code MERGE_MODEL} 的差别：源是一个可展开的路径而不是两个精确路径；
 * 源模型（含被展开的子树）**不会被删除**；结果节点默认 {@code visible = true}。
 */
public class MergeAllSubModelsOperation extends GuardedOperation {

    private String src;
    private String dst;

    @Override
    public String name() {
        return "MERGE_ALL_SUB_MODELS";
    }

    @Override
    protected void parse(JSONObject data) throws IRException {
        this.src = OperationParams.getRequiredPath(data, "src", "SRC");
        this.dst = OperationParams.getRequiredPath(data, "dst", "DST");
        if (src.equals(dst)) {
            throw new IRException("SRC_EQUALS_DST", "src 与 dst 不能相同：" + src);
        }
        if (dst.startsWith(src + "/")) {
            throw new IRException("DST_INSIDE_SRC",
                    "dst 不能位于 src 的子树内：" + dst + " 在 " + src + " 之下");
        }
    }

    @Override
    protected String run(RendererInterface renderer) {
        renderer.getModel().mergeAllSubModels(src, dst);
        return "Successfully merge \"" + src + "\" and all its sub models into \"" + dst + "\"\n"
                + "Tetrahedrons: " + ModelQuery.tetrahedronText(renderer, dst);
    }
}
