package weilantianhai.agent.interfaces.impl;

import weilantianhai.agent.trace.RoundTrace;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 从 {@code CommandExecutor} 产出的回执 Markdown 里**反推错误级别**，填进 {@link RoundTrace}。
 *
 * <h3>为什么要反推</h3>
 *
 * <p>{@code CommandExecutor.execute} 只回一个拼好的字符串，不告诉你"这条是参数错还是执行错"。
 * 而数据集量化评估需要的恰恰是这个区分：
 *
 * <ul>
 *   <li><b>类型级</b>（typeOk）—— 参数缺失 / 枚举非法 / 向量非单位 / 非正交 / 未知操作类型。
 *       这些是「模型把 IR 写错了」，属于 prompt 与格式理解问题；</li>
 *   <li><b>执行级</b>（execOk）—— 模型不存在 / 机位不存在 / 下标越界 / UI 不可用。
 *       这些是「模型对世界状态的判断错了」，属于语义与多轮记忆问题。</li>
 * </ul>
 *
 * <h3>判定依据</h3>
 *
 * <p>两类错误在文本上可区分：
 * <ul>
 *   <li>整批格式错：{@code # Error} 开头（由 {@code ResultFormatter.error} 产出）；</li>
 *   <li>单条错：{@code # TYPE(id=n)\nError: 原因}（由 {@code OperationParams.errorBody} 产出）；</li>
 *   <li>类型级关键词：{@code INVALID} / {@code MISSING} / {@code 必须是} / {@code 不能为空} /
 *       {@code 非法} / {@code 未知操作类型} / {@code 超过上限} / {@code 不能同时} /
 *       {@code 单位向量} / {@code 正交};</li>
 *   <li>其余单条 Error 归为执行级。</li>
 * </ul>
 *
 * <p>这是个**启发式**判定，不追求 100% 精确；真正的仲裁是人工打分器。
 * 它的价值在自动刷量模式下给出一个可用的近似值。
 */
final class TraceAnalysis {

    /** 单条操作回执的头：{@code # TYPE(id=3)} */
    private static final Pattern RECEIPT_HEAD = Pattern.compile("^#\\s+([A-Z0-9_]+)\\(id=(\\d+)\\)\\s*$");

    /** 类型级错误关键词（命中任一 → typeErrors）。 */
    private static final String[] TYPE_HINTS = {
            "INVALID", "MISSING", "UNKNOWN_OP", "未知操作类型",
            "必须是", "不能为空", "非法", "超过上限", "不支持",
            "不能同时", "单位向量", "正交", "缺少"
    };

    private TraceAnalysis() {
    }

    /** 扫描回执 Markdown，把错误分别归到 typeErrors / execErrors。 */
    static void analyze(String receipts, RoundTrace trace) {
        if (receipts == null || receipts.isBlank()) {
            return;
        }

        // 整批格式错：直接判类型级（IR 本身写得不合法）
        if (receipts.startsWith("# Error")) {
            String reason = extractReason(receipts);
            trace.addTypeError(reason);
            return;
        }

        String[] lines = receipts.split("\\R");
        String currentType = null;
        boolean inErrorBlock = false;
        StringBuilder errorBuf = new StringBuilder();

        for (String raw : lines) {
            String line = raw.strip();

            Matcher m = RECEIPT_HEAD.matcher(line);
            if (m.matches()) {
                flushError(currentType, errorBuf, trace);
                currentType = m.group(1);
                inErrorBlock = false;
                errorBuf.setLength(0);
                continue;
            }

            if (line.startsWith("Error:") || line.startsWith("Error：")) {
                inErrorBlock = true;
                errorBuf.setLength(0);
                errorBuf.append(line);
                continue;
            }

            if (inErrorBlock) {
                // Error 正文可能跨行（业务错误消息里带换行）
                if (line.isEmpty() || line.startsWith("#")) {
                    flushError(currentType, errorBuf, trace);
                    inErrorBlock = false;
                } else {
                    errorBuf.append(' ').append(line);
                }
            }
        }
        flushError(currentType, errorBuf, trace);
    }

    private static void flushError(String type, StringBuilder buf, RoundTrace trace) {
        if (buf.length() == 0) {
            return;
        }
        String msg = buf.toString().strip();
        String tagged = (type == null ? "" : "[" + type + "] ") + msg;
        if (isTypeLevel(msg)) {
            trace.addTypeError(tagged);
        } else {
            trace.addExecError(tagged);
        }
        buf.setLength(0);
    }

    private static boolean isTypeLevel(String message) {
        String upper = message.toUpperCase(java.util.Locale.ROOT);
        for (String hint : TYPE_HINTS) {
            if (upper.contains(hint.toUpperCase(java.util.Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    /** 从 {@code # Error} 块里抠出 Reason 行。 */
    private static String extractReason(String receipts) {
        int idx = receipts.indexOf("Reason:");
        if (idx < 0) {
            return firstLine(receipts);
        }
        String tail = receipts.substring(idx + "Reason:".length()).strip();
        // 去掉包裹的 ``` 围栏
        tail = tail.replaceAll("(?s)```[a-zA-Z]*", "").strip();
        return tail.isEmpty() ? firstLine(receipts) : firstLine(tail);
    }

    private static String firstLine(String s) {
        int nl = s.indexOf('\n');
        return nl < 0 ? s.strip() : s.substring(0, nl).strip();
    }
}
