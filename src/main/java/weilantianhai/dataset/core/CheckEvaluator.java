package weilantianhai.dataset.core;

import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 结构化判据求值器：把 {@link weilantianhai.dataset.tasks.TaskGoal#check()} 里的每条断言
 * 在 {@link SceneSnapshot} 上求值。
 *
 * <h3>支持的断言形态</h3>
 *
 * <pre>
 * bookmark:&lt;name&gt; exists
 * bookmark:&lt;name&gt; pos ≈ [x,y,z,w]
 * model:&lt;path&gt; exists
 * model:&lt;path&gt; vertices == N
 * model:&lt;path&gt; tetrahedrons == N
 * model:&lt;path&gt; absent
 * camera.pos ≈ [x,y,z,w]
 * bookmark.count == N
 * model.count == N
 *
 * -- 需要"回复文本"的（纯查询题）
 * reply contains &lt;片段&gt;          -- 回复里出现该子串
 * reply matches &lt;正则&gt;           -- 回复匹配该正则
 *
 * -- 需要"回执"的（陷阱题：必须失败）
 * reject with &lt;片段&gt;             -- 该任务应当失败，且回执里出现该片段
 * </pre>
 *
 * <p>「≈」比数值（容差 {@link SceneSnapshot#EPS}），「==」比整数。
 * 求值失败（语法不认）返回 {@code null} 的结果，表示"这条判据无法自动判定"，
 * 脚本会把整条任务标为「建议人工打分」。
 *
 * <h3>为什么要 reply / reject</h3>
 *
 * <p>场景快照只能反映"世界变成了什么"。纯查询题世界不变（快照恒等），
 * 陷阱题则要求"正确地失败"（成功反而是错的）—— 这两类都得看文本，
 * 所以引入了 {@link Context} 把回复与回执一起带进来。
 */
public final class CheckEvaluator {

    /** 断言结果。 */
    public record Result(boolean passed, String detail) {
    }

    /**
     * 求值上下文：除了场景快照，还带上**文本证据**。
     *
     * <p>{@code reply} 是 AI 最后给出的自然语言回答（{@code done=true} 的 content）；
     * {@code receipts} 是所有轮的操作回执拼接。两者都可为 null，
     * 此时依赖它们的断言会返回"无法自动判定"。
     */
    public record Context(SceneSnapshot snapshot, String reply, String receipts) {
        public static Context of(SceneSnapshot snapshot) {
            return new Context(snapshot, null, null);
        }
    }

    private static final Pattern POS_PATTERN =
            Pattern.compile("pos\\s*≈\\s*\\[\\s*([-\\d.eE]+)\\s*,\\s*([-\\d.eE]+)\\s*,\\s*([-\\d.eE]+)\\s*,\\s*([-\\d.eE]+)\\s*]");
    private static final Pattern INT_PATTERN =
            Pattern.compile("^(\\w+)\\s*==\\s*(\\d+)$");
    private static final Pattern VEC_PATTERN =
            Pattern.compile("≈\\s*\\[\\s*([-\\d.eE]+)\\s*,\\s*([-\\d.eE]+)\\s*,\\s*([-\\d.eE]+)\\s*,\\s*([-\\d.eE]+)\\s*]");

    private CheckEvaluator() {
    }

    /** 便捷重载：只按场景快照判定（不带文本证据）。 */
    public static Report evaluate(List<String> assertions, SceneSnapshot snap) {
        return evaluate(assertions, Context.of(snap));
    }

    /**
     * 对整组断言求值。
     *
     * @return 每条断言的说明（含通过与否）；全通过则 overall 为 true。
     */
    public static Report evaluate(List<String> assertions, Context ctx) {
        List<Result> results = new ArrayList<>();
        boolean allPassed = true;
        boolean anyUnknown = false;

        for (String a : assertions) {
            Result r = evaluateOne(a, ctx);
            if (r == null) {
                results.add(new Result(false, "无法自动判定：" + a));
                anyUnknown = true;
                allPassed = false;
            } else {
                results.add(r);
                if (!r.passed()) allPassed = false;
            }
        }
        return new Report(allPassed, anyUnknown, results);
    }

    /** 整组求值报告。 */
    public record Report(boolean allPassed, boolean hasUnknown, List<Result> results) {
        public String describe() {
            StringBuilder sb = new StringBuilder();
            for (Result r : results) {
                sb.append(r.passed() ? "  ✓ " : "  ✗ ").append(r.detail()).append('\n');
            }
            return sb.toString().stripTrailing();
        }
    }

    /* ==================== 单条 ==================== */

    private static Result evaluateOne(String assertion, Context ctx) {
        String s = assertion.strip();
        SceneSnapshot snap = ctx.snapshot();

        // -- 文本类：不需要场景快照 --
        if (s.startsWith("reply")) {
            return evalReply(s, ctx);
        }
        if (s.startsWith("reject")) {
            return evalReject(s, ctx);
        }

        if (s.startsWith("bookmark:")) {
            return evalBookmark(s.substring("bookmark:".length()), snap);
        }
        if (s.startsWith("model:")) {
            return evalModel(s.substring("model:".length()), snap);
        }
        if (s.startsWith("camera.pos")) {
            return evalCameraPos(s, snap);
        }
        if (s.startsWith("bookmark.count")) {
            return evalCount(s, snap.bookmarkNames().size(), "bookmark");
        }
        if (s.startsWith("model.count")) {
            return evalCount(s, snap.modelPaths().size(), "model");
        }
        return null;
    }

    private static Result evalBookmark(String rest, SceneSnapshot snap) {
        // <name> exists / <name> pos ≈ [...]
        int posIdx = rest.indexOf(" pos");
        String name;
        String tail;

        if (posIdx >= 0) {
            name = rest.substring(0, posIdx).strip();
            tail = rest.substring(posIdx).strip();
        } else {
            // "<name> exists"
            int sp = rest.lastIndexOf(' ');
            if (sp < 0) return null;
            name = rest.substring(0, sp).strip();
            tail = rest.substring(sp + 1).strip();
        }

        List<String> names = snap.bookmarkNames();
        boolean exists = names.contains(name);

        if (tail.equals("exists")) {
            return new Result(exists,
                    exists ? "机位「" + name + "」存在" : "机位「" + name + "」不存在（现有：" + names + "）");
        }

        Matcher m = POS_PATTERN.matcher(tail);
        if (m.find()) {
            if (!exists) {
                return new Result(false, "机位「" + name + "」不存在，无法比较位置");
            }
            Vector4f expected = parseVec(m);
            Vector4f actual = snap.bookmarkPos(name);
            boolean ok = SceneSnapshot.vecEq(expected, actual, SceneSnapshot.EPS);
            return new Result(ok, "机位「" + name + "」pos=" + fmt(actual)
                    + (ok ? " ✓" : "，期望 " + fmt(expected)));
        }

        if (tail.equals("absent")) {
            return new Result(!exists, exists ? "机位「" + name + "」不该存在" : "机位「" + name + "」确实不存在");
        }
        return null;
    }

    private static Result evalModel(String rest, SceneSnapshot snap) {
        // rest 形如 "A exists" / "A absent" / "A vertices == 20" / "A tetrahedrons == 5"
        // / "A subtree_exists"
        // 路径里可能含 '/'，但不会含空格；字段名与运算符之间必有空格。
        // 因此按**第一个空格**切分路径与 tail 最稳。
        int sp = rest.indexOf(' ');
        if (sp < 0) return null;
        String path = rest.substring(0, sp).strip();
        String tail = rest.substring(sp + 1).strip();

        boolean exists = snap.modelPaths().contains(path);

        if (tail.equals("exists")) {
            return new Result(exists,
                    exists ? "模型「" + path + "」存在" : "模型「" + path + "」不存在（现有：" + snap.modelPaths() + "）");
        }
        if (tail.equals("absent")) {
            return new Result(!exists, exists ? "模型「" + path + "」不该存在" : "模型「" + path + "」确实不存在");
        }

        // "subtree_exists"：路径本身或它的任一后代存在。
        // 用于「合并子模型」这类操作 —— 合并后子模型会被吸收，
        // 直接断言子路径会误判，只能校验这棵子树非空。
        if (tail.equals("subtree_exists")) {
            String prefix = path.endsWith("/") ? path : path + "/";
            List<String> hits = snap.modelPaths().stream()
                    .filter(p -> p.equals(path) || p.startsWith(prefix))
                    .toList();
            boolean ok = !hits.isEmpty();
            return new Result(ok, ok
                    ? "子树「" + path + "」非空（" + hits + "）✓"
                    : "子树「" + path + "」为空（现有：" + snap.modelPaths() + "）");
        }

        // "vertices == N" / "tetrahedrons == N"
        Matcher m = INT_PATTERN.matcher(tail);
        if (m.matches()) {
            String field = m.group(1);
            int expected = Integer.parseInt(m.group(2));
            if (!exists) {
                return new Result(false, "模型「" + path + "」不存在，无法读 " + field);
            }
            var info = snap.modelInfo(path);
            if (info == null || info.get(field) == null) {
                return new Result(false, "模型「" + path + "」没有 " + field + " 信息（需脚本采集）");
            }
            int actual = info.getIntValue(field);
            return new Result(actual == expected,
                    "模型「" + path + "」" + field + "=" + actual
                            + (actual == expected ? " ✓" : "，期望 " + expected));
        }
        return null;
    }

    private static Result evalCameraPos(String s, SceneSnapshot snap) {
        Matcher m = VEC_PATTERN.matcher(s);
        if (!m.find()) return null;
        Vector4f expected = parseVec(m);
        Vector4f actual = snap.cameraPos();
        boolean ok = SceneSnapshot.vecEq(expected, actual, SceneSnapshot.EPS);
        return new Result(ok, "相机 pos=" + fmt(actual) + (ok ? " ✓" : "，期望 " + fmt(expected)));
    }

    private static Result evalCount(String s, int actual, String what) {
        Matcher m2 = Pattern.compile("==\\s*(\\d+)").matcher(s);
        if (!m2.find()) return null;
        int expected = Integer.parseInt(m2.group(1));
        return new Result(actual == expected,
                what + ".count=" + actual + (actual == expected ? " ✓" : "，期望 " + expected));
    }

    /* ==================== 文本类断言 ==================== */

    /**
     * {@code reply contains <片段>} / {@code reply matches <正则>}
     *
     * <p>用于**纯查询题**：世界没变，只有回答文本能判对错。
     * 判定前先把两边的空白与全角逗号归一化，免得「(0, 0, -10, 0)」与「(0,0,-10,0)」
     * 因为写法差异被误判为错。
     */
    private static Result evalReply(String s, Context ctx) {
        String reply = ctx.reply();
        if (reply == null) {
            return null;   // 没有回复文本 → 无法判定
        }

        String body = s.substring("reply".length()).strip();
        String norm = normalizeText(reply);

        if (body.startsWith("contains")) {
            String needle = body.substring("contains".length()).strip();
            if (needle.isEmpty()) return null;
            String n = normalizeText(needle);
            boolean ok = norm.contains(n);
            return new Result(ok, "回复" + (ok ? "包含" : "**不**包含") + "「" + needle + "」"
                    + (ok ? " ✓" : "（回复：" + abbreviate(reply) + "）"));
        }
        if (body.startsWith("matches")) {
            String regex = body.substring("matches".length()).strip();
            if (regex.isEmpty()) return null;
            try {
                boolean ok = Pattern.compile(regex).matcher(reply).find();
                return new Result(ok, "回复" + (ok ? "匹配" : "**不**匹配") + " /" + regex + "/ "
                        + (ok ? "✓" : "（回复：" + abbreviate(reply) + "）"));
            } catch (Exception e) {
                return null;   // 正则写错 → 无法判定
            }
        }
        return null;
    }

    /**
     * {@code reject with <片段>} —— **该任务应当失败**，且回执里出现该片段。
     *
     * <p>用于陷阱题。注意与其它断言**语义相反**：这里"报错"才是通过。
     * 若模型硬造出一个结果（回执 Success）反而判为不通过。
     */
    private static Result evalReject(String s, Context ctx) {
        String receipts = ctx.receipts();
        if (receipts == null) {
            return null;   // 没有回执 → 无法判定
        }

        String body = s.substring("reject".length()).strip();
        String expect = body.startsWith("with") ? body.substring("with".length()).strip() : body;

        boolean rejected = receipts.contains("Error:") || receipts.contains("# Error");

        if (!rejected) {
            return new Result(false, "**没有拒绝** —— 回执里全是 Success，模型硬造了一个结果");
        }
        if (expect.isEmpty()) {
            return new Result(true, "正确地拒绝了 ✓");
        }
        boolean hit = receipts.contains(expect);
        return new Result(hit, hit
                ? "正确地拒绝，且理由含「" + expect + "」✓"
                : "拒绝了，但理由里没有「" + expect + "」（回执：" + abbreviate(receipts) + "）");
    }

    /* ==================== 工具 ==================== */

    /** 文本归一化：去掉所有空白、全角逗号/括号统一成半角，便于宽松比对。 */
    private static String normalizeText(String s) {
        if (s == null) return "";
        return s.replace("（", "(").replace("）", ")")
                .replace("，", ",").replace("：", ":")
                .replaceAll("\\s+", "")
                .toLowerCase();
    }

    private static String abbreviate(String s) {
        if (s == null) return "null";
        String one = s.replaceAll("\\s+", " ").strip();
        return one.length() <= 120 ? one : one.substring(0, 120) + "…";
    }

    private static Vector4f parseVec(Matcher m) {
        return new Vector4f(
                Float.parseFloat(m.group(1)), Float.parseFloat(m.group(2)),
                Float.parseFloat(m.group(3)), Float.parseFloat(m.group(4)));
    }

    private static String fmt(Vector4f v) {
        if (v == null) return "(null)";
        return String.format("(%.3f, %.3f, %.3f, %.3f)", v.x, v.y, v.z, v.w);
    }
}
