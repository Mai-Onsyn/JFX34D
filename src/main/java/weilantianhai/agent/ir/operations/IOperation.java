package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONObject;
import mai_onsyn.renderer.interfaces.RendererInterface;
import weilantianhai.agent.ir.IRException;

/**
 * 所有 IR 操作的统一接口。
 * 每个具体操作实现这个接口
 */
public interface IOperation {

    /** 操作名称，必须和 JSON 里的 op 字段一致，如 “move” */
    String name();

    /** 接受 JSON 参数并绑定到当前操作对象 */
    void load(JSONObject args) throws IRException;

    /**
     * 执行操作，返回结果主体（不含 # TYPE(id=X) 头）。
     * 例如 "Success\nNow Pos=(-5, 0, 0, 0)"。
     */
    String execute(RendererInterface renderer) throws IRException;
}
