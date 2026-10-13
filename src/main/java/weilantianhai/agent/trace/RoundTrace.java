package weilantianhai.agent.trace;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 一轮 LLM 交互的结构化记录（数据集生成用）。
 *
 * <p>和 stdout 上那串 {@code === RoundN LLM JSON ===} 是同一份信息，但**结构化了**，
 * 可以直接落盘成 JSONL，不需要再用正则去切日志。
 *
 * <h3>三个基础标记</h3>
 *
 * <p>量化评估（解析成功率 / 类型匹配率 / 数据匹配率）全部建立在这三个机械可判定的布尔值上：
 * <ul>
 *   <li>{@link #parseOk} —— 这一轮的 JSON 能不能解析成合法 bundle（{@code IRParser.parseAsBundle}）；</li>
 *   <li>{@link #getTypeOk()} —— type 是否注册、参数能不能读进来（{@code OperationRegistry.creat} + {@code IOperation.load}）；</li>
 *   <li>{@link #getExecOk()} —— 真正落到渲染器上有没有成功（{@code CommandExecutor.execute} 是否返回 Error）。</li>
 * </ul>
 *
 * <p><b>注意 typeOk 的口径</b>：{@code IOperation.load()} 把参数错误吞进 {@code loadError}（见
 * {@code GuardedOperation}），所以"参数非法"在 parse 阶段是看不出来的——它只会在执行时
 * 以 {@code Error: ...} 出现在回执里。因此本类把 typeOk 的判定交给
 * {@link #setTypeErrors}：由上层从**每一条操作的回执**里反推，而不是从解析异常里推。
 */
public final class RoundTrace {

    private final int round;
    private final String rawLlmJson;

    private boolean parseOk;
    private String parseErrorCode;
    private String parseErrorMessage;

    /** 解析后 bundle 里出现过的操作类型（按出现顺序）。 */
    private final List<String> opTypes = new ArrayList<>();

    /** type 级问题：UNKNOWN_OP、参数缺失、枚举非法等（从回执里反推）。 */
    private final List<String> typeErrors = new ArrayList<>();

    /** 执行级问题：模型不存在、下标越界、UI 不可用等（从回执里反推）。 */
    private final List<String> execErrors = new ArrayList<>();

    /** 这一轮是不是 {@code done=true} 的收尾轮。 */
    private boolean done;

    /** LLM 在收尾轮给的总结（{@code content}）。 */
    private String content;

    /** 回传给 LLM 的 Markdown（成功片段拼接，或一整块 Error）。 */
    private String receipts;

    /** 这一轮耗时（毫秒），含 LLM 请求 + 执行。 */
    private long elapsedMs;

    /** 执行完之后的世界状态快照（模型树 / 相机 / 机位）。 */
    private JSONObject stateAfter;

    public RoundTrace(int round, String rawLlmJson) {
        this.round = round;
        this.rawLlmJson = rawLlmJson;
    }

    public int getRound() {
        return round;
    }

    public String getRawLlmJson() {
        return rawLlmJson;
    }

    public boolean isParseOk() {
        return parseOk;
    }

    public String getParseErrorCode() {
        return parseErrorCode;
    }

    public String getParseErrorMessage() {
        return parseErrorMessage;
    }

    public List<String> getOpTypes() {
        return Collections.unmodifiableList(opTypes);
    }

    public List<String> getTypeErrors() {
        return Collections.unmodifiableList(typeErrors);
    }

    public List<String> getExecErrors() {
        return Collections.unmodifiableList(execErrors);
    }

    public boolean isDone() {
        return done;
    }

    public String getContent() {
        return content;
    }

    public String getReceipts() {
        return receipts;
    }

    public long getElapsedMs() {
        return elapsedMs;
    }

    public void setElapsedMs(long elapsedMs) {
        this.elapsedMs = elapsedMs;
    }

    public JSONObject getStateAfter() {
        return stateAfter;
    }

    public void setStateAfter(JSONObject stateAfter) {
        this.stateAfter = stateAfter;
    }

    /* ==================== 派生标记 ==================== */

    /**
     * 类型是否匹配。
     *
     * <p>口径：**只在解析成功的前提下**有意义。解析失败时视为 false（漏斗式分母要求如此：
     * 解析都过不了的轮次不该被计入类型率的分母）。
     */
    public boolean getTypeOk() {
        return parseOk && typeErrors.isEmpty();
    }

    /**
     * 数据是否匹配上。
     *
     * <p>口径：**只在类型通过的前提下**有意义，避免双重惩罚。
     */
    public boolean getExecOk() {
        return getTypeOk() && execErrors.isEmpty();
    }

    /* ==================== 写入（由 AgentInterfaceImpl 调用） ==================== */

    public void parseFailed(String code, String message) {
        this.parseOk = false;
        this.parseErrorCode = code;
        this.parseErrorMessage = message;
    }

    public void parseSucceeded(List<String> types, boolean isDone, String content) {
        this.parseOk = true;
        this.opTypes.clear();
        if (types != null) {
            this.opTypes.addAll(types);
        }
        this.done = isDone;
        this.content = content;
    }

    public void addTypeError(String error) {
        if (error != null && !error.isBlank()) {
            typeErrors.add(error);
        }
    }

    public void addExecError(String error) {
        if (error != null && !error.isBlank()) {
            execErrors.add(error);
        }
    }

    public void setReceipts(String receipts) {
        this.receipts = receipts;
    }
}
