package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONObject;
import mai_onsyn.renderer.interfaces.RendererInterface;
import weilantianhai.agent.ir.IRException;

/**
 * 操作实现的公共基类：把"参数错误 / 执行错误"统一变成**该条操作的错误正文**。
 *
 * <h3>为什么这样做</h3>
 *
 * v2 文档 §2.3 / §2.4 要求：**单条操作失败只影响它自己，其余操作继续执行**，
 * 返回形如
 *
 * <pre>
 * # CREATE_MODEL(id=5)
 * Error: Model "Cube A" already exists
 * </pre>
 *
 * 而 {@code CommandExecutor} 是在收到 {@link IRException} 时**整批中止**并返回一个
 * Error Markdown（那是给 §2.3 的"格式错误"用的）。两者的差别在于：
 *
 * <ul>
 *   <li>格式错误（JSON 不合法、缺 type/id）→ 应该整批中止，由 executor 处理；</li>
 *   <li>单条参数非法 / 对象不存在 → 应该只让这一条返回 Error 正文。</li>
 * </ul>
 *
 * 由于 {@code load()} 抛出的 {@link IRException} 会一路冒到 {@code IRParser}，
 * 造成整批中止，这里把"参数解析"与"执行"都**推迟到 {@code execute()} 里**，
 * 并在内部捕获，改写成错误正文返回。这样无需改动 {@code IRParser} / {@code CommandExecutor}
 * 就能满足文档语义。
 *
 * <h3>子类约定</h3>
 *
 * <ul>
 *   <li>{@link #parse(JSONObject)} —— 解析并校验参数，非法就抛 {@link IRException}；</li>
 *   <li>{@link #run(RendererInterface)} —— 真正执行，返回结果主体（不含 {@code # TYPE(id=n)} 头）。</li>
 * </ul>
 */
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
            // 渲染侧用 IllegalArgumentException / NoSuchElementException / IndexOutOfBoundsException
            // 表达"参数不合法""模型不存在""下标越界"等业务错误，消息本身就是给 AI 看的，
            // 直接透传（文档 §2.3：错误文本直接采用渲染实现抛出的消息）。
            return OperationParams.errorBody(e);
        } catch (IRException e) {
            return OperationParams.errorBody(e);
        }
    }
}
