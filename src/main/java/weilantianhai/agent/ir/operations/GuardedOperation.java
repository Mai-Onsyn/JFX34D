package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONObject;
import mai_onsyn.renderer.interfaces.RendererInterface;
import weilantianhai.agent.ir.IRException;

public abstract class GuardedOperation implements IOperation {
    /** 参数解析失败的异常，延迟到 execute 阶段转成错误正文 */
    private IRException loadError;

    /**
     * 解析并校验参数。
     *
     * @param data 操作参数，永不为 null（缺失时是空对象）
     * @throws IRException 参数缺失 / 非法时抛出
     */
    protected abstract void parse(JSONObject data) throws IRException;

    /**
     * 执行操作。
     *
     * @return 结果主体，例如 {@code "Success\nNow Pos=(-5, 0, 0, 0)"}
     */
    protected abstract String run(RendererInterface renderer) throws IRException;

    @Override
    public final void load(JSONObject data) {
        try {
            parse(data == null ? new JSONObject() : data);
        } catch (IRException e) {
            this.loadError = e;
        }
    }

    @Override
    public final String execute(RendererInterface renderer) {
        if (loadError != null) {
            return OperationParams.errorBody(loadError);
        }
        try {
            return run(renderer);
        } catch (RuntimeException e) {
            return OperationParams.errorBody(e);
        } catch (IRException e) {
            return OperationParams.errorBody(e);
        }
    }
}
