package weilantianhai.agent.trace;

/**
 * 一轮 LLM 交互完成时的回调（数据集采集 / 调试用）。
 *
 * <p>比抓 stdout 稳：结构化、带轮号、线程安全语义由调用方自行决定。
 * 回调在 {@code agent-worker} 线程上触发，实现里若要碰 UI 必须自己切 FX 线程。
 *
 * <p>注册方式：{@code AgentInterface.setTraceListener(...)}。只保留最后一个 listener，
 * 传 {@code null} 即注销（数据集脚本跑完会注销，避免影响正常 UI）。
 */
public interface TraceListener {

    /**
     * 一轮结束（无论解析成功失败、done 与否都会调）。
     *
     * @param requestId 所属请求 id
     * @param trace     结构化轮记录
     */
    void onRound(int requestId, RoundTrace trace);

    /**
     * 整个请求结束（{@code process} 方法退出时调，含异常路径）。
     *
     * @param requestId 请求 id
     * @param ok        正常收尾为 true；抛异常为 false
     */
    default void onRequestFinished(int requestId, boolean ok) {
    }
}
