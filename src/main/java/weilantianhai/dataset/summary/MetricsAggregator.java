package weilantianhai.dataset.summary;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * 把原始数据集记录聚合成**量化评估指标**。
 *
 * <h3>指标定义（漏斗式分母，这是关键）</h3>
 *
 * <p>三个「率」是层层收窄的漏斗，分母不同，不能统一用总轮数：
 *
 * <pre>
 *   所有轮
 *     └─ 解析成功 ── parse_success_rate = 解析成功轮 / 所有轮
 *          └─ 类型匹配 ── type_match_rate = 类型匹配轮 / 解析成功轮
 *               └─ 数据匹配 ── data_match_rate = 数据匹配轮 / 类型匹配轮
 * </pre>
 *
 * <p>若不这样收窄，解析失败的轮会同时拉低类型率与数据率，造成双重惩罚，
 * 且会出现「类型率 > 解析率」的荒谬结果。
 *
 * <p>另外按**任务**（而非轮）统计两个指标：平均轮数、平均耗时。
 */
public final class MetricsAggregator {

    /** 一个模型（可再按类别细分）的指标。 */
    public static final class Metrics {
        public int tasks;
        public int rounds;
        public int parseOkRounds;
        public int typeOkRounds;
        public int execOkRounds;
        public long totalElapsedMs;
        public int correctTasks;
        public int humanScoredTasks;
        /** 有结构化判据的任务数（其余只能人工打分）。 */
        public int checkableTasks;
        /** 见过的 API 模型名（用于展示，防止分组标识与 API 名混淆）。 */
        public final java.util.Set<String> apiModels = new java.util.TreeSet<>();

        public double parseSuccessRate() {
            return rounds == 0 ? 0 : (double) parseOkRounds / rounds;
        }

        public double typeMatchRate() {
            return parseOkRounds == 0 ? 0 : (double) typeOkRounds / parseOkRounds;
        }

        public double dataMatchRate() {
            return typeOkRounds == 0 ? 0 : (double) execOkRounds / typeOkRounds;
        }

        public double avgRounds() {
            return tasks == 0 ? 0 : (double) rounds / tasks;
        }

        public double avgElapsedMs() {
            return tasks == 0 ? 0 : (double) totalElapsedMs / tasks;
        }

        /**
         * 任务正确率 —— 分母**只算有判据可判的任务**。
         *
         * <p>无判据的任务（纯主观题）不该进入分母：把它们算成"不通过"会凭空拉低正确率，
         * 算成"通过"又会虚高。它们的数量单独由 {@code checkableTasks} 与
         * {@link #uncheckedTasks()} 暴露出来。
         */
        public double taskCorrectRate() {
            return checkableTasks == 0 ? 0 : (double) correctTasks / checkableTasks;
        }

        /** 无结构化判据、只能人工打分的任务数。 */
        public int uncheckedTasks() {
            return tasks - checkableTasks;
        }

        void add(Metrics o) {
            tasks += o.tasks;
            rounds += o.rounds;
            parseOkRounds += o.parseOkRounds;
            typeOkRounds += o.typeOkRounds;
            execOkRounds += o.execOkRounds;
            totalElapsedMs += o.totalElapsedMs;
            correctTasks += o.correctTasks;
            humanScoredTasks += o.humanScoredTasks;
            checkableTasks += o.checkableTasks;
            apiModels.addAll(o.apiModels);
        }
    }

    /** 聚合结果：模型 → 指标；模型 → 类别 → 指标。 */
    public static final class Report {
        public final Map<String, Metrics> byModel = new TreeMap<>();
        public final Map<String, Map<String, Metrics>> byModelCategory = new TreeMap<>();
        public final Set<String> models = new LinkedHashSet<>();
        public final Set<String> categories = new LinkedHashSet<>();
        public int totalRecords;
    }

    private MetricsAggregator() {
    }

    /**
     * @param records 原始数据集记录（{@link weilantianhai.dataset.store.DatasetStore#loadAllRaw()}）
     */
    public static Report aggregate(List<JSONObject> records) {
        Report report = new Report();
        report.totalRecords = records.size();

        for (JSONObject rec : records) {
            String model = rec.getJSONObject("env") == null ? "unknown"
                    : rec.getJSONObject("env").getString("model");
            String category = rec.getString("category");
            if (model == null) model = "unknown";
            if (category == null) category = "unknown";

            report.models.add(model);
            report.categories.add(category);

            Metrics m = count(rec);
            report.byModel.computeIfAbsent(model, k -> new Metrics()).add(m);
            report.byModelCategory
                    .computeIfAbsent(model, k -> new LinkedHashMap<>())
                    .computeIfAbsent(category, k -> new Metrics())
                    .add(m);
        }
        return report;
    }

    /** 统计单条记录的指标。 */
    private static Metrics count(JSONObject rec) {
        Metrics m = new Metrics();
        m.tasks = 1;

        // API 模型名（展示用；分组仍按 env.model）
        JSONObject env = rec.getJSONObject("env");
        if (env != null) {
            String api = env.getString("api_model");
            if (api != null && !api.isBlank()) m.apiModels.add(api);
        }

        // 是否有结构化判据 —— 决定这条是否进"任务正确率"的分母
        String check = rec.getString("check");
        if (check != null && !check.isBlank()) m.checkableTasks = 1;

        JSONArray rounds = rec.getJSONArray("rounds");
        if (rounds != null) {
            for (int i = 0; i < rounds.size(); i++) {
                JSONObject r = rounds.getJSONObject(i);
                m.rounds++;
                boolean parseOk = Boolean.TRUE.equals(r.getBoolean("parse_ok"));
                if (parseOk) {
                    m.parseOkRounds++;
                    // type_ok 仅在 parse_ok 时为非 null
                    if (Boolean.TRUE.equals(r.getBoolean("type_ok"))) {
                        m.typeOkRounds++;
                        if (Boolean.TRUE.equals(r.getBoolean("exec_ok"))) {
                            m.execOkRounds++;
                        }
                    }
                }
            }
        }

        Long elapsed = rec.getLong("elapsed_ms");
        m.totalElapsedMs = elapsed == null ? 0 : elapsed;

        JSONObject verdict = rec.getJSONObject("verdict");
        if (verdict != null) {
            String label = verdict.getString("label");
            // 只统计**有判据**的任务：无判据的题没有客观对错，
            // 计入分子会让"正确率"虚高（也就不用进分母了）。
            if (m.checkableTasks == 1 && "correct".equalsIgnoreCase(label)) {
                m.correctTasks = 1;
            }
            if ("human".equalsIgnoreCase(rec.getString("scorer"))) m.humanScoredTasks = 1;
        }
        return m;
    }

    /* ==================== 渲染 ==================== */

    /** 生成「模型对比总表」的 Markdown —— 就是需求里那张表。 */
    public static String toMarkdown(Report report) {
        StringBuilder sb = new StringBuilder();
        sb.append("# 模型量化评估\n\n");
        sb.append("共 ").append(report.totalRecords).append(" 条记录，")
          .append(report.models.size()).append(" 个模型，")
          .append(report.categories.size()).append(" 个类别。\n\n");
        sb.append("> `模型` 列是**分组标识**（受控词表：`deepseek` / `qwen`），")
          .append("`API 模型` 列是实际请求的模型名。\n")
          .append("> `任务正确率` 的分母**只含有结构化判据的任务**，")
          .append("无判据的（纯查询/主观题）不计入，其数量见 `待人工` 列。\n\n");

        sb.append("## 总表\n\n");
        sb.append(HEADER);
        sb.append("|---|---|---|---|---|---|---|---|---|---|\n");
        for (Map.Entry<String, Metrics> e : report.byModel.entrySet()) {
            sb.append(row(e.getKey(), e.getValue()));
        }
        sb.append('\n');

        sb.append("## 分类别明细\n\n");
        for (Map.Entry<String, Map<String, Metrics>> me : report.byModelCategory.entrySet()) {
            sb.append("### ").append(me.getKey()).append("\n\n");
            sb.append(HEADER);
            sb.append("|---|---|---|---|---|---|---|---|---|---|\n");
            for (Map.Entry<String, Metrics> e : me.getValue().entrySet()) {
                sb.append(row(e.getKey(), e.getValue()));
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    /** 表头（总表与分类别明细共用，保证列对齐）。 */
    private static final String HEADER =
            "| 模型 | API 模型 | 解析成功率 | 类型匹配率 | 数据匹配率 | 平均轮数 | 平均耗时 | 任务正确率 | 样本数 | 待人工 |\n";

    private static String row(String name, Metrics m) {
        String api = m.apiModels.isEmpty() ? "-" : String.join("/", m.apiModels);
        return String.format("| %s | %s | %s | %s | %s | %.2f | %.1fs | %s | %d | %d |%n",
                name,
                api,
                pct(m.parseSuccessRate()),
                pct(m.typeMatchRate()),
                pct(m.dataMatchRate()),
                m.avgRounds(),
                m.avgElapsedMs() / 1000.0,
                pct(m.taskCorrectRate()),
                m.tasks,
                m.uncheckedTasks());
    }

    private static String pct(double v) {
        return String.format("%.1f%%", v * 100);
    }

    /** 生成 JSON 汇总。 */
    public static JSONObject toJson(Report report) {
        JSONObject root = new JSONObject();
        root.put("total_records", report.totalRecords);
        JSONObject models = new JSONObject();
        for (Map.Entry<String, Metrics> e : report.byModel.entrySet()) {
            models.put(e.getKey(), metricsJson(e.getValue()));
        }
        root.put("by_model", models);

        JSONObject byCat = new JSONObject();
        for (Map.Entry<String, Map<String, Metrics>> me : report.byModelCategory.entrySet()) {
            JSONObject cats = new JSONObject();
            for (Map.Entry<String, Metrics> e : me.getValue().entrySet()) {
                cats.put(e.getKey(), metricsJson(e.getValue()));
            }
            byCat.put(me.getKey(), cats);
        }
        root.put("by_model_category", byCat);
        return root;
    }

    private static JSONObject metricsJson(Metrics m) {
        JSONObject o = new JSONObject();
        o.put("n_tasks", m.tasks);
        o.put("n_rounds", m.rounds);
        o.put("parse_success_rate", round4(m.parseSuccessRate()));
        o.put("type_match_rate", round4(m.typeMatchRate()));
        o.put("data_match_rate", round4(m.dataMatchRate()));
        o.put("avg_rounds", round4(m.avgRounds()));
        o.put("avg_elapsed_ms", (long) m.avgElapsedMs());
        o.put("task_correct_rate", round4(m.taskCorrectRate()));
        // 正确率的分母口径必须暴露出来，否则"正确率 78%"会被误读成"78% 的题做对了"
        o.put("n_checkable_tasks", m.checkableTasks);
        o.put("n_unchecked_tasks", m.uncheckedTasks());
        if (!m.apiModels.isEmpty()) {
            o.put("api_models", new ArrayList<>(m.apiModels));
        }
        return o;
    }

    private static double round4(double v) {
        return Math.round(v * 10000.0) / 10000.0;
    }

    /** 控制台速览文本。 */
    public static String toConsole(Report report) {
        List<String> lines = new ArrayList<>();
        lines.add(String.format("%-12s %-22s %8s %8s %8s %8s %8s %8s %5s %6s",
                "模型", "API 模型", "解析率", "类型率", "数据率", "均轮数", "均耗时", "正确率", "样本", "待人工"));
        for (Map.Entry<String, Metrics> e : report.byModel.entrySet()) {
            Metrics m = e.getValue();
            String api = m.apiModels.isEmpty() ? "-" : String.join("/", m.apiModels);
            lines.add(String.format("%-12s %-22s %7s %8s %8s %7.2f %8s %8s %5d %6d",
                    e.getKey(), api,
                    pct(m.parseSuccessRate()), pct(m.typeMatchRate()), pct(m.dataMatchRate()),
                    m.avgRounds(), String.format("%.1fs", m.avgElapsedMs() / 1000.0),
                    pct(m.taskCorrectRate()), m.tasks, m.uncheckedTasks()));
        }
        return String.join(System.lineSeparator(), lines);
    }
}
