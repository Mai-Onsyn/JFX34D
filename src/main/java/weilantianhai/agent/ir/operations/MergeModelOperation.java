package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONObject;
import mai_onsyn.renderer.interfaces.RendererInterface;
import weilantianhai.agent.ir.IRException;

public class MergeModelOperation extends GuardedOperation {
    private String src1;
    private String src2;
    private String dst;

    @Override
    public String name() {
        return "MERGE_MODEL";
    }

    @Override
    protected void parse(JSONObject data) throws IRException {
        this.src1 = OperationParams.getRequiredPath(data, "src1", "SRC1");
        this.src2 = OperationParams.getRequiredPath(data, "src2", "SRC2");
        this.dst = OperationParams.getRequiredPath(data, "dst", "DST");
        if (src1.equals(src2)) {
            throw new IRException("SRC1_EQUALS_SRC2", "src1 与 src2 不能相同：" + src1);
        }
    }

    @Override
    protected String run(RendererInterface renderer) {
        renderer.getModel().mergeModel(src1, src2, dst);
        return "Successfully merge \"" + src1 + "\" + \"" + src2 + "\" into \"" + dst + "\"\n"
                + "Tetrahedrons: " + ModelQuery.tetrahedronText(renderer, dst);
    }
}
