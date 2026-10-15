package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONObject;
import mai_onsyn.renderer.interfaces.RendererInterface;
import weilantianhai.agent.ir.IRException;

/**
 * LIST_DATASET_CATEGORIES：查看数据集分类目录。**无参数**。
 *
 * <pre>
 * # LIST_DATASET_CATEGORIES(id=1)
 * ## DATASET CATALOG
 * | category | records | correct | incorrect | other | models |
 * | camera_bookmark | 6 | 6 | 0 | 0 | deepseek |
 * ...
 * </pre>
 *
 * <p>这是数据集查询的**第一级**：先看有哪些类别、各多少条、判对多少，
 * 再用 {@link GetDatasetRecordsOperation} 取某个类别的具体记录。
 *
 * <p>它读取的是数据集脚本（{@code DatasetLauncher}）产出的、**经人工审核打分**的记录，
 * 目录形如 {@code data/dataset/raw/<日期>/<模型>__<类别>.jsonl}。
 * 详见 {@link DatasetQuery}。
 */
public class ListDatasetCategoriesOperation extends GuardedOperation {

    @Override
    public String name() {
        return "LIST_DATASET_CATEGORIES";
    }

    @Override
    protected void parse(JSONObject data) {
        // 无参数操作
    }

    @Override
    protected String run(RendererInterface renderer) throws IRException {
        return DatasetQuery.catalog();
    }
}
