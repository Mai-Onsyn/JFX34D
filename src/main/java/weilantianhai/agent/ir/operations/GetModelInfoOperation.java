package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONObject;
import mai_onsyn.renderer.interfaces.RendererInterface;
import weilantianhai.agent.ir.IRException;

/**
 * GET_MODEL_INFO：获取模型整体信息（文档 §6.1）。
 *
 * <p>参数：{@code name} — 可以是真实模型，也可以是**分组路径**（此时返回它下面所有子模型的汇总）。
 *
 * <p>正文由渲染实现直接拼好（markdown），以 {@code ##} 开头，直接接在
 * {@code # GET_MODEL_INFO(id=n)} 首行下面，这里原样透传：
 *
 * <pre>
 * ## "Tower/Base"
 * Kind: SHAPE
 * Params: {"type":"Tesseract",...}
 * Tetrahedrons: 48
 * Bound: x[-1.00, 1.00] y[-1.00, 1.00] z[-1.00, 1.00] w[-1.00, 1.00]
 * Vertex list omitted, use GET_TETRAHEDRON with index in [0, 48)
 * Transform matrix:
 * ...
 * Sub models:
 * - Tower/Upper  GROUP  tets=0
 * </pre>
 */
public class GetModelInfoOperation extends GuardedOperation {

    private String name;

    @Override
    public String name() {
        return "GET_MODEL_INFO";
    }

    @Override
    protected void parse(JSONObject data) throws IRException {
        this.name = OperationParams.getRequiredPath(data, "name", "NAME");
    }

    @Override
    protected String run(RendererInterface renderer) {
        return renderer.getGeometry().getModelInfos(name);
    }
}
