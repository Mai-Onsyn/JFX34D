package weilantianhai.agent.trace;

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
