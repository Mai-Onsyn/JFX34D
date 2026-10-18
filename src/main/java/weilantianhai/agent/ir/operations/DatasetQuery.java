package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONObject;
import weilantianhai.agent.ir.IRException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;

final class DatasetQuery {
    /** 覆盖数据集根目录的系统属性。 */
    static final String ROOT_PROPERTY = "jfx34d.dataset.root";

    /** 单条记录里回复正文的最大展示长度（太长会淹没上下文）。 */
    private static final int ANSWER_MAX = 240;

    private DatasetQuery() {
    }

    /* ==================== 定位与读取 ==================== */

    /** 数据集根目录（绝对路径）。 */
    static Path root() {
        String override = System.getProperty(ROOT_PROPERTY);
        if (override != null && !override.isBlank()) {
            return Path.of(override.trim());
        }
        Path rel = Path.of("data", "dataset");
        Path project = findProjectRoot();
        return project == null ? rel.toAbsolutePath().normalize() : project.resolve(rel);
    }

    /** 从工作目录向上找 {@code pom.xml}，最多 6 层。找不到返回 null。 */
    private static Path findProjectRoot() {
        Path dir = Path.of("").toAbsolutePath().normalize();
        for (int i = 0; i < 6 && dir != null; i++) {
            if (Files.exists(dir.resolve("pom.xml"))) {
                return dir;
            }
            dir = dir.getParent();
        }
        return null;
    }

    /** 扫 {@code raw/} 下所有 {@code .jsonl}（跳过以 {@code .} 开头的备份目录）。 */
    static List<JSONObject> loadAll() throws IRException {
        Path raw = root().resolve("raw");
        if (!Files.isDirectory(raw)) {
            return List.of();
        }
        List<JSONObject> all = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(raw)) {
            for (Path p : walk.filter(f -> f.toString().endsWith(".jsonl"))
                    .filter(f -> !isUnderDotDir(raw, f))
                    .sorted()
                    .toList()) {
                all.addAll(readJsonl(p));
            }
        } catch (IOException e) {
            throw new IRException("DATASET_IO", "读取数据集失败：" + e.getMessage());
        }
        return all;
    }

    private static boolean isUnderDotDir(Path base, Path p) {
        for (Path seg : base.relativize(p)) {
            if (seg.toString().startsWith(".")) {
                return true;
            }
        }
        return false;
    }

    private static List<JSONObject> readJsonl(Path file) {
        List<JSONObject> out = new ArrayList<>();
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String s = line.strip();
                if (s.isEmpty() || s.startsWith("#")) {
                    continue;
                }
                try {
                    out.add(JSONObject.parseObject(s));
                } catch (Exception e) {
                    System.err.println("[dataset-query] 跳过坏行 " + file.getFileName()
                            + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("[dataset-query] 读取失败 " + file + ": " + e.getMessage());
        }
        return out;
    }

    /* ==================== 分类目录 ==================== */

    /** 一个类别的汇总。 */
    private static final class Stat {
        int total;
        int correct;
        int incorrect;
        int other;
        final TreeSet<String> models = new TreeSet<>();
    }

    /** 生成分类目录（第一级接口的返回正文）。 */
    static String catalog() throws IRException {
        Path root = root();
        List<JSONObject> all = loadAll();

        StringBuilder sb = new StringBuilder();
        sb.append("## DATASET CATALOG\n");
        sb.append("root = ").append(root).append('\n');

        if (all.isEmpty()) {
            sb.append("共 0 条记录 —— 数据集还是空的。\n");
            sb.append("（用数据集脚本跑题并人工打分后，才会在这里出现数据；\n");
            sb.append(" 也可以直接查看 ").append(root.resolve("raw")).append(" 是否存在。）\n");
            return sb.toString();
        }

        Map<String, Stat> byCategory = new TreeMap<>();
        for (JSONObject rec : all) {
            String cat = rec.getString("category");
            if (cat == null || cat.isBlank()) {
                cat = "unknown";
            }
            Stat st = byCategory.computeIfAbsent(cat, k -> new Stat());
            st.total++;
            String label = verdictLabel(rec);
            if ("correct".equals(label)) {
                st.correct++;
            } else if ("incorrect".equals(label)) {
                st.incorrect++;
            } else {
                st.other++;
            }
            String model = modelOf(rec);
            if (model != null) {
                st.models.add(model);
            }
        }

        sb.append("共 ").append(all.size()).append(" 条记录，")
          .append(byCategory.size()).append(" 个类别。\n\n");
        sb.append("| category | records | correct | incorrect | other | models |\n");
        sb.append("| --- | --- | --- | --- | --- | --- |\n");
        for (Map.Entry<String, Stat> e : byCategory.entrySet()) {
            Stat st = e.getValue();
            sb.append("| ").append(e.getKey())
              .append(" | ").append(st.total)
              .append(" | ").append(st.correct)
              .append(" | ").append(st.incorrect)
              .append(" | ").append(st.other)
              .append(" | ").append(String.join(", ", st.models))
              .append(" |\n");
        }

        sb.append("\n下一步：用 GET_DATASET_RECORDS 取某个类别里的记录，例如\n");
        sb.append("  {\"type\":\"GET_DATASET_RECORDS\",\"id\":1,\"no_result\":false,")
          .append("\"data\":{\"category\":\"").append(byCategory.keySet().iterator().next())
          .append("\",\"limit\":5}}\n");
        sb.append("可选参数：model、verdict（correct/incorrect/skip/error/timeout）、limit（默认 5，最大 50）。\n");
        sb.append("注：判定标准 check 不在返回内容里，只回题目与人工结论。\n");
        return sb.toString();
    }

    /* ==================== 某类记录 ==================== */

    /**
     * 取某类别的记录。
     *
     * @param category 类别；传 {@code all} / {@code *} 表示不按类别过滤
     * @param model    可选，按模型标识过滤（{@code deepseek} / {@code qwen}）
     * @param verdict  可选，按结论过滤（{@code correct} / {@code incorrect} / …）
     * @param limit    最多返回几条
     */
    static String records(String category, String model, String verdict, int limit) throws IRException {
        List<JSONObject> all = loadAll();
        String catFilter = (category == null || category.isBlank()
                || "all".equalsIgnoreCase(category) || "*".equals(category))
                ? null : category.strip();
        String modelFilter = model == null || model.isBlank() ? null : model.strip();
        String verdictFilter = verdict == null || verdict.isBlank()
                ? null : verdict.strip().toLowerCase(Locale.ROOT);

        List<JSONObject> hit = new ArrayList<>();
        for (JSONObject rec : all) {
            if (catFilter != null && !catFilter.equalsIgnoreCase(rec.getString("category"))) {
                continue;
            }
            if (modelFilter != null && !modelFilter.equalsIgnoreCase(modelOf(rec))) {
                continue;
            }
            if (verdictFilter != null && !verdictFilter.equals(verdictLabel(rec))) {
                continue;
            }
            hit.add(rec);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("## DATASET RECORDS\n");
        sb.append("筛选：category=").append(catFilter == null ? "(不限)" : catFilter)
          .append("  model=").append(modelFilter == null ? "(不限)" : modelFilter)
          .append("  verdict=").append(verdictFilter == null ? "(不限)" : verdictFilter)
          .append('\n');

        if (hit.isEmpty()) {
            sb.append("没有匹配的记录。\n");
            TreeSet<String> cats = new TreeSet<>();
            for (JSONObject rec : all) {
                String c = rec.getString("category");
                if (c != null) {
                    cats.add(c);
                }
            }
            if (cats.isEmpty()) {
                sb.append("（数据集是空的 —— 先跑题并打分，或确认 root 是否正确。）\n");
                sb.append("root = ").append(root()).append('\n');
            } else {
                sb.append("可用的 category：").append(String.join(", ", cats)).append('\n');
                sb.append("也可以先用 LIST_DATASET_CATEGORIES 看分类目录。\n");
            }
            return sb.toString();
        }

        int show = Math.min(limit, hit.size());
        sb.append("共 ").append(hit.size()).append(" 条匹配，显示前 ").append(show).append(" 条：\n\n");
        for (int i = 0; i < show; i++) {
            JSONObject rec = hit.get(i);
            sb.append("### [task ").append(rec.getInteger("task_id"))
              .append("] ").append(rec.getString("category"))
              .append("  verdict=").append(verdictLabel(rec))
              .append("  scorer=").append(rec.getString("scorer"))
              .append("  rounds=").append(rec.getInteger("round_count"))
              .append('\n');
            sb.append("题目：").append(oneLine(rec.getString("prompt"))).append('\n');
            String comment = commentOf(rec);
            if (comment != null && !comment.isBlank()) {
                sb.append("人工评语：").append(oneLine(comment)).append('\n');
            }
            String answer = rec.getString("reply_markdown");
            if (answer != null && !answer.isBlank()) {
                sb.append("AI 当时的回答：").append(abbrev(answer)).append('\n');
            }
            sb.append('\n');
        }
        if (hit.size() > show) {
            sb.append("（还有 ").append(hit.size() - show)
              .append(" 条未显示；调大 limit 或用 verdict 缩小范围。）\n");
        }
        return sb.toString();
    }

    /* ==================== 小工具 ==================== */

    private static String verdictLabel(JSONObject rec) {
        JSONObject v = rec.getJSONObject("verdict");
        if (v == null) {
            return "unknown";
        }
        String label = v.getString("label");
        return label == null ? "unknown" : label.toLowerCase(Locale.ROOT);
    }

    private static String commentOf(JSONObject rec) {
        JSONObject v = rec.getJSONObject("verdict");
        return v == null ? null : v.getString("comment");
    }

    private static String modelOf(JSONObject rec) {
        JSONObject env = rec.getJSONObject("env");
        return env == null ? null : env.getString("model");
    }

    /** 把多行文本压成一行（记录里的 prompt/comment 可能含换行）。 */
    private static String oneLine(String s) {
        if (s == null) {
            return "";
        }
        return s.replaceAll("\\s+", " ").strip();
    }

    private static String abbrev(String s) {
        String flat = oneLine(s);
        return flat.length() <= ANSWER_MAX ? flat : flat.substring(0, ANSWER_MAX) + "…";
    }
}
