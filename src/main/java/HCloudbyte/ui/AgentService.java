package HCloudbyte.ui;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * AI 对话服务 —— 桥接 ChatPanel 与 {@link mai_onsyn.renderer.interfaces.RendererInterface}（Kotlin 侧已定义）。
 *
 * <p>一轮 {@link #send} 的流程（对应 AI Api Interface document v2.md §2）：
 * <ol>
 *   <li>拼接上下文 markdown：用户输入 + 场景快照</li>
 *   <li>调用 LLM，拿到 JSON（version / done / operations）</li>
 *   <li>逐条解析 operations：校验 → 分发到 RendererInterface → 收集 no_result=false 结果</li>
 *   <li>把结果 markdown 回填 LLM 继续，直到 done:true</li>
 *   <li>返回 {@link ChatTurn}，供 UI 展示</li>
 * </ol>
 *
 * ==== [接口预留] ====
 * 目前 ChatPanel 用 Timeline 模拟思考；实现本接口后调用
 * {@code ChatPanel#setAgentService} 接入。
 */
public interface AgentService {

    /**
     * 发送一条用户消息。
     *
     * @param userMessage     用户自然语言输入
     * @param contextMarkdown 场景上下文（模型列表、摄像机、选中对象等）
     * @return 异步返回一轮对话结果
     */
    CompletableFuture<ChatTurn> send(String userMessage, String contextMarkdown);

    /** 用户中断当前请求。默认空实现。 */
    default void cancel() { }

    /**
     * 一轮对话的结果。
     *
     * @param reply           给用户看的自然语言回复（可空）
     * @param steps           执行轨迹，供 ChatPanel 逐条打勾
     * @param resultMarkdown  回填给 LLM 的 markdown（§2.1），可用于调试
     * @param done            本轮是否结束（对应 json 的 done）
     */
    record ChatTurn(String reply,
                    List<String> steps,
                    String resultMarkdown,
                    boolean done) { }
}