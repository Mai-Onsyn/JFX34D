package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONObject;
import mai_onsyn.renderer.interfaces.RendererInterface;
import weilantianhai.agent.ir.IRException;

/**
 * GET_DATASET_RECORDS：取数据集里某个类别的记录。数据集查询的**第二级**。
 *
 * <pre>
 * {"type":"GET_DATASET_RECORDS","id":1,"no_result":false,
 *  "data":{"category":"camera_bookmark","limit":5}}
 * </pre>
 *
 * <table>
 *   <caption>参数</caption>
 *   <tr><td>{@code category}</td><td>必填。类别名，取自 {@link ListDatasetCategoriesOperation}
 *       的分类目录；传 {@code all} 表示不限类别</td></tr>
 *   <tr><td>{@code model}</td><td>可选。按模型标识过滤，{@code deepseek} / {@code qwen}</td></tr>
 *   <tr><td>{@code verdict}</td><td>可选。按人工结论过滤：
 *       {@code correct} / {@code incorrect} / {@code skip} / {@code error} / {@code timeout}</td></tr>
 *   <tr><td>{@code limit}</td><td>可选。最多返回几条，默认 5，上限 50</td></tr>
 * </table>
 *
 * <p>返回的每条包含：题目、人工结论、人工评语、以及 AI 当时的回答（截断）。
 * **不含** {@code check}（机器判定的标准答案）—— 避免让模型"看到答案"而污染评测。
 */
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
