package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONObject;
import mai_onsyn.renderer.interfaces.RendererInterface;
import weilantianhai.agent.ir.IRException;

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
