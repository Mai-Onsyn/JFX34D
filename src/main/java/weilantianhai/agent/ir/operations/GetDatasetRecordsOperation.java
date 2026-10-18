package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONObject;
import mai_onsyn.renderer.interfaces.RendererInterface;
import weilantianhai.agent.ir.IRException;

public class GetDatasetRecordsOperation extends GuardedOperation {
    /** limit 上限，防止一次拉回整个数据集把上下文撑爆。 */
    private static final int LIMIT_MAX = 50;
    private static final int LIMIT_DEFAULT = 5;

    private String category;
    private String model;
    private String verdict;
    private int limit = LIMIT_DEFAULT;

    @Override
    public String name() {
        return "GET_DATASET_RECORDS";
    }

    @Override
    protected void parse(JSONObject data) throws IRException {
        category = OperationParams.getRequiredString(data, "category", name());
        model = OperationParams.getOptionalString(data, "model", null);
        verdict = OperationParams.getOptionalString(data, "verdict", null);
        int raw = OperationParams.getOptionalInt(data, "limit", LIMIT_DEFAULT, name());
        limit = Math.max(1, Math.min(LIMIT_MAX, raw));
    }

    @Override
    protected String run(RendererInterface renderer) throws IRException {
        return DatasetQuery.records(category, model, verdict, limit);
    }
}
