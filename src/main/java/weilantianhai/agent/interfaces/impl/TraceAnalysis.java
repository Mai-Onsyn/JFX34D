package weilantianhai.agent.interfaces.impl;

import weilantianhai.agent.trace.RoundTrace;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
