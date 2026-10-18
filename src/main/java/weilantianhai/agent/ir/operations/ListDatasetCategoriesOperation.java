package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONObject;
import mai_onsyn.renderer.interfaces.RendererInterface;
import weilantianhai.agent.ir.IRException;

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
