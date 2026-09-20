package weilantianhai.agent.ir;

import com.alibaba.fastjson2.JSONObject;

/**
 * 所有 IR 操作的统一接口。
 * 每个具体操作实现这个接口
 */
public interface IOperation {

    /** 操作名称，必须和 JSON 里的 op 字段一致，如 “move” */
    String name();

    /** 接受 JSON 参数并绑定到当前操作对象 */
    void load(JSONObject args) throws IRException;

    /** 统一执行入口，由具体操作实现自己的逻辑 */
    void execute(Object obj);
}
