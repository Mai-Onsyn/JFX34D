package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONObject;
import mai_onsyn.renderer.interfaces.RendererInterface;
import weilantianhai.agent.ir.IRException;

/**
 * COPY_MODEL：复制一个模型节点（文档 §4.1.4）。
 *
 * <p>参数：{@code src}（源路径，必须存在）、{@code dst}（新路径，必须不存在）。
 *
 * <pre>
 * # COPY_MODEL(id=9)
 * Successfully copy "Tower/Base" to "Tower/BaseCopy"
 * Tetrahedrons: 48
 * </pre>
 *
 * <p>复制内容：全部四面体（四面体独立、顶点实例复用）、{@code kind}、{@code params}
 * 以及变换矩阵。
 *
 * <p><b>与文档的差异</b>：文档 §4.1.4 写的是"不递归子模型"，但当前实现
 * （{@code ModelInterfaceImpl.copyModel}）会连同**整棵子树**一起复制
 * （{@code src/... → dst/...}）；{@code src} 也可以是中间/分组路径。
 * 这里以实际实现为准，返回的四面体数是 {@code dst} 自身的数量。
 */
public class CopyModelOperation extends GuardedOperation {

    private String src;
    private String dst;

    @Override
    public String name() {
        return "COPY_MODEL";
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
        renderer.getModel().copyModel(src, dst);
        return "Successfully copy \"" + src + "\" to \"" + dst + "\"\n"
                + "Tetrahedrons: " + ModelQuery.tetrahedronText(renderer, dst);
    }
}
