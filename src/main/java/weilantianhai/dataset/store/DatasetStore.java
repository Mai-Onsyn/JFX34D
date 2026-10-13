package weilantianhai.dataset.store;

import com.alibaba.fastjson2.JSONObject;
import weilantianhai.dataset.core.TaskRunRecord;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * 数据集存储：分类落盘 + 读取。
 *
 * <h3>目录结构</h3>
 *
 * <pre>
 * &lt;root&gt;/                                  (默认 data/dataset，见 {@link weilantianhai.dataset.DatasetPaths})
 * ├── raw/
 * │   └── 2026-10-11/
 * │       ├── deepseek__camera_bookmark.jsonl      ← key 是**分组标识**，不是 API 模型名
 * │       ├── deepseek__shape.jsonl
 * │       └── qwen__camera_bookmark.jsonl
 * ├── summary/
 * │   ├── 2026-10-11__deepseek.json
 * │   └── compare__2026-10-11.md
 * └── index.jsonl                     (全局索引：一行一条精简记录)
 * </pre>
 *
 * <p>分类主键 = {@code <model>__<category>}，两个轴都进文件名，
 * 因此「同一任务横向比不同模型」与「同一模型纵向看不同类别」都是一次文件扫描。
 *
 * <p><b>注意</b>：文件名里的 model 是 {@code deepseek} / {@code qwen} 这类**分组标识**
 * （见 {@code DatasetApp.modelKey}），不是 {@code deepseek-flash} / {@code qwen/qwen3.5-9b}
 * 这样的 API 模型名。后者记在记录的 {@code env.api_model} 里。混用会让同一模型在统计里裂成两行。
 */
public final class DatasetStore {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final Path root;

    public DatasetStore(Path root) {
        this.root = root;
    }

    /**
     * 默认根目录：项目下的 {@code data/dataset}。
     *
     * <p>这里**不用** {@code .workbuddy/} —— 那是工具的临时目录（已在 .gitignore 里），
     * 而数据集是要归档、要对比、要交给别人的实验产物。详见
     * {@link weilantianhai.dataset.DatasetPaths}。
     */
    public static DatasetStore defaultStore() {
        return new DatasetStore(weilantianhai.dataset.DatasetPaths.DATASET_ROOT);
    }

    public Path root() {
        return root;
    }

    public Path rawDir(LocalDate date) {
        return root.resolve("raw").resolve(date.format(DATE));
    }

    public Path summaryDir() {
        return root.resolve("summary");
    }

    /**
     * 追加一条记录到它所属的分类文件。
     *
     * <p>用追加写（不重写整文件）：脚本中途崩溃也只丢最后一条没写完的。
     */
    public void append(TaskRunRecord record) throws IOException {
        LocalDate date = LocalDate.now();
        Path dir = rawDir(date);
        Files.createDirectories(dir);
        Path file = dir.resolve(sanitize(record.getModel())
                + "__" + sanitize(record.getTask().category()) + ".jsonl");

        String line = record.toJson().toJSONString() + System.lineSeparator();
        Files.writeString(file, line, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);

        appendIndex(record, file);
    }

    /** 全局索引（精简字段，便于快速统计而不必扫所有原始文件）。 */
    private void appendIndex(TaskRunRecord record, Path rawFile) throws IOException {
        JSONObject idx = new JSONObject();
        idx.put("run_id", record.getRunId());
        idx.put("task_id", record.getTask().id());
        idx.put("category", record.getTask().category());
        idx.put("model", record.getModel());
        // api_model 也要进索引：否则只看 index 分不清 deepseek / deepseek-flash 是不是同一个模型
        if (record.getApiModel() != null && !record.getApiModel().isBlank()) {
            idx.put("api_model", record.getApiModel());
        }
        idx.put("has_check", record.getTask().hasCheck());
        idx.put("round_count", record.roundCount());
        idx.put("elapsed_ms", record.getElapsedMs());
        idx.put("verdict", record.getVerdict().name().toLowerCase());
        idx.put("scorer", record.getScorer());
        idx.put("raw_file", root.relativize(rawFile).toString().replace('\\', '/'));
        idx.put("all_rounds_parse_ok", record.roundCount() == 0 ? null : record.allRoundsParseOk());
        idx.put("all_rounds_type_ok", record.roundCount() == 0 ? null : record.allRoundsTypeOk());
        idx.put("all_rounds_exec_ok", record.roundCount() == 0 ? null : record.allRoundsExecOk());

        Files.createDirectories(root);
        Files.writeString(root.resolve("index.jsonl"),
                idx.toJSONString() + System.lineSeparator(),
                StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    /** 读回所有原始记录（扫 raw 下所有 jsonl）。 */
    public List<JSONObject> loadAllRaw() throws IOException {
        List<JSONObject> all = new ArrayList<>();
        Path raw = root.resolve("raw");
        if (!Files.exists(raw)) return all;
        try (Stream<Path> walk = Files.walk(raw)) {
            for (Path p : walk.filter(f -> f.toString().endsWith(".jsonl"))
                    // 跳过备份目录（名字以 . 开头）—— 它们不是数据
                    .filter(f -> !isUnderDotDir(raw, f))
                    .sorted()
                    .toList()) {
                all.addAll(readJsonl(p));
            }
        }
        return all;
    }

    /** 判断 {@code p} 是否位于 {@code base} 下某个以 {@code .} 开头的子目录里。 */
    private static boolean isUnderDotDir(Path base, Path p) {
        Path rel = base.relativize(p);
        for (Path seg : rel) {
            if (seg.toString().startsWith(".")) return true;
        }
        return false;
    }

    /** 读回某一天的原始记录。 */
    public List<JSONObject> loadRaw(LocalDate date) throws IOException {
        Path dir = rawDir(date);
        List<JSONObject> all = new ArrayList<>();
        if (!Files.exists(dir)) return all;
        try (Stream<Path> list = Files.list(dir)) {
            for (Path p : list.filter(f -> f.toString().endsWith(".jsonl")).toList()) {
                all.addAll(readJsonl(p));
            }
        }
        return all;
    }

    public static List<JSONObject> readJsonl(Path file) throws IOException {
        List<JSONObject> out = new ArrayList<>();
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            String s = line.strip();
            if (s.isEmpty()) continue;
            try {
                out.add(JSONObject.parseObject(s));
            } catch (Exception e) {
                System.err.println("[dataset] 跳过坏行: " + file.getFileName() + " -> " + e.getMessage());
            }
        }
        return out;
    }

    public void writeSummary(String fileName, String content) throws IOException {
        Files.createDirectories(summaryDir());
        try (Writer w = Files.newBufferedWriter(summaryDir().resolve(fileName), StandardCharsets.UTF_8)) {
            w.write(content);
        }
    }

    /** 文件名安全化：把路径分隔符等换成下划线。 */
    private static String sanitize(String s) {
        if (s == null || s.isBlank()) return "unknown";
        return s.replaceAll("[\\\\/:*?\"<>|\\s]+", "_");
    }

    /* ==================== 迁移：统一模型标识 ==================== */

    /**
     * 迁移记录：把 {@code env.model} 里的 **API 模型名**归一化成**分组标识**，
     * 并按新标识重排到 {@code <model>__<category>.jsonl}。
     *
     * <h3>为什么需要</h3>
     *
     * <p>早期版本的 {@code currentModelName()} 直接读 {@code deepseek.properties} 的
     * {@code model=}（值是 {@code deepseek-flash}），后来改成用下拉框的 key（{@code deepseek}）。
     * 于是同一个模型在原始数据里既有 {@code "model":"deepseek"} 又有 {@code "model":"deepseek-flash"}，
     * 汇总表里被拆成两行、样本数各算一半。
     *
     * <p>本方法按 {@code aliases} 映射把老名字归到新名字，同时
     * **把原值补写进 {@code env.api_model}**（若原本没有），信息不丢。
     *
     * <h3>安全性</h3>
     *
     * <p>先全部读进内存、确认无误，再**整体重写** raw 目录（原文件先挪到
     * {@code raw/.migrated-backup-<ts>/}），最后重建 {@code index.jsonl}。
     * 任何一步抛异常都不会留下半新半旧的状态。
     *
     * @param aliases 老名 → 新名，例如 {@code {"deepseek-flash": "deepseek"}}
     * @return 迁移摘要（改了哪些文件、共几条）
     */
    public MigrationReport normalizeModels(Map<String, String> aliases) throws IOException {
        Path raw = root.resolve("raw");
        if (!Files.exists(raw)) return new MigrationReport(0, List.of(), List.of());

        // 1. 读全部
        Map<Path, List<JSONObject>> byFile = new LinkedHashMap<>();
        try (Stream<Path> walk = Files.walk(raw)) {
            for (Path p : walk.filter(f -> f.toString().endsWith(".jsonl")).sorted().toList()) {
                byFile.put(p, readJsonl(p));
            }
        }

        // 2. 逐条改 env.model
        int changed = 0;
        List<String> touched = new ArrayList<>();
        for (Map.Entry<Path, List<JSONObject>> e : byFile.entrySet()) {
            boolean fileChanged = false;
            for (JSONObject rec : e.getValue()) {
                JSONObject env = rec.getJSONObject("env");
                if (env == null) continue;
                String old = env.getString("model");
                String now = aliases.getOrDefault(old, old);
                if (now != null && !now.equals(old)) {
                    // 旧值就是 API 模型名 —— 顺手补进 api_model，信息不丢
                    if (env.getString("api_model") == null && old != null) {
                        env.put("api_model", old);
                    }
                    env.put("model", now);
                    changed++;
                    fileChanged = true;
                }
            }
            if (fileChanged) touched.add(root.relativize(e.getKey()).toString().replace('\\', '/'));
        }
        if (changed == 0) return new MigrationReport(0, List.of(), List.of());

        // 3. 原 raw 整体备份
        //
        // 注意：备份必须放在 **raw/ 之外**。loadAllRaw() 是 Files.walk(raw) 递归扫描，
        // 备份若留在 raw/ 里会被当成数据重新读进来，条数直接翻倍。
        Path backup = root.resolve(".migrated-backup-" + System.currentTimeMillis());
        Files.createDirectories(backup);
        try (Stream<Path> dayDirs = Files.list(raw)) {
            for (Path d : dayDirs.filter(Files::isDirectory).toList()) {
                Files.move(d, backup.resolve(d.getFileName()));
            }
        }

        // 4. 按新 (model, category) 重写。
        //
        // 关键：必须写**内存里已改过 env.model 的 byFile**，不能重读备份目录 ——
        // 备份是磁盘上的原件，重读会把刚改的改动丢掉（踩过一次）。
        // 按「原文件所属的那一天」分桶写，保持 raw/<date>/ 结构。
        List<String> written = new ArrayList<>();
        // day 名 -> (文件名前缀 -> 记录列表)
        Map<String, Map<String, List<JSONObject>>> perDay = new LinkedHashMap<>();
        for (Map.Entry<Path, List<JSONObject>> e : byFile.entrySet()) {
            // byFile 的 key 是 raw/<date>/xxx.jsonl，取上一级目录名就是日期
            Path dayDir = e.getKey().getParent();
            String dayName = dayDir == null ? LocalDate.now().format(DATE)
                    : dayDir.getFileName().toString();
            for (JSONObject rec : e.getValue()) {
                String model = rec.getJSONObject("env") == null ? "unknown"
                        : rec.getJSONObject("env").getString("model");
                String cat = rec.getString("category");
                if (cat == null) cat = "unknown";
                perDay.computeIfAbsent(dayName, k -> new LinkedHashMap<>())
                        .computeIfAbsent(sanitize(model) + "__" + sanitize(cat),
                                k -> new ArrayList<>())
                        .add(rec);
            }
        }

        for (Map.Entry<String, Map<String, List<JSONObject>>> dayE : perDay.entrySet()) {
            Path outDay = raw.resolve(dayE.getKey());
            Files.createDirectories(outDay);
            for (Map.Entry<String, List<JSONObject>> e : dayE.getValue().entrySet()) {
                Path f = outDay.resolve(e.getKey() + ".jsonl");
                StringBuilder sb = new StringBuilder();
                for (JSONObject rec : e.getValue()) {
                    sb.append(rec.toJSONString()).append(System.lineSeparator());
                }
                Files.writeString(f, sb.toString(), StandardCharsets.UTF_8);
                written.add(raw.relativize(f).toString().replace('\\', '/'));
            }
        }

        // 5. 重建 index.jsonl
        rebuildIndex();
        return new MigrationReport(changed, touched,
                written.stream().distinct().sorted().toList());
    }

    /** 从 raw 全量重建 index.jsonl（迁移后用，避免旧索引残留老模型名）。 */
    public void rebuildIndex() throws IOException {
        Path idx = root.resolve("index.jsonl");
        if (Files.exists(idx)) Files.delete(idx);
        for (JSONObject rec : loadAllRaw()) {
            JSONObject o = new JSONObject();
            o.put("task_id", rec.getInteger("task_id"));
            o.put("category", rec.getString("category"));
            JSONObject env = rec.getJSONObject("env");
            o.put("model", env == null ? "unknown" : env.getString("model"));
            if (env != null && env.getString("api_model") != null) {
                o.put("api_model", env.getString("api_model"));
            }
            o.put("has_check", rec.getString("check") != null && !rec.getString("check").isBlank());
            o.put("round_count", rec.getInteger("round_count"));
            o.put("elapsed_ms", rec.getLong("elapsed_ms"));
            JSONObject v = rec.getJSONObject("verdict");
            o.put("verdict", v == null ? null : v.getString("label"));
            o.put("scorer", rec.getString("scorer"));
            Files.writeString(idx, o.toJSONString() + System.lineSeparator(),
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }
    }

    /** 迁移摘要。 */
    public record MigrationReport(int changedRecords, List<String> touchedFiles,
                                 List<String> writtenFiles) {
        public String describe() {
            StringBuilder sb = new StringBuilder();
            sb.append("归一化 ").append(changedRecords).append(" 条记录的 env.model\n");
            sb.append("改动文件：\n");
            for (String f : touchedFiles) sb.append("  - ").append(f).append('\n');
            sb.append("重写后的文件：\n");
            for (String f : writtenFiles) sb.append("  - ").append(f).append('\n');
            return sb.toString();
        }
    }

    /**
     * 命令行迁移入口（不依赖 JavaFX，可以直接 {@code java} 跑）：
     *
     * <pre>
     * java -cp ... weilantianhai.dataset.store.DatasetStore [数据集根目录]
     * </pre>
     *
     * <p>默认根目录 {@code data/dataset}；把 {@code deepseek-flash -> deepseek}、
     * {@code qwen/qwen3.5-9b -> qwen} 这类「API 名 → 分组标识」的映射应用一遍，
     * 原目录备份到 {@code .migrated-backup-<ts>/}（在 {@code raw/} **之外**）。
     */
    public static void main(String[] args) throws Exception {
        Path root = args.length > 0 ? Path.of(args[0])
                : weilantianhai.dataset.DatasetPaths.DATASET_ROOT;
        DatasetStore store = new DatasetStore(root);

        Map<String, String> aliases = new LinkedHashMap<>();
        aliases.put("deepseek-flash", "deepseek");
        aliases.put("deepseek-v4-flash", "deepseek");
        aliases.put("deepseek-v4.1-flash", "deepseek");
        aliases.put("deepseek-chat", "deepseek");
        aliases.put("qwen/qwen3.5-9b", "qwen");
        aliases.put("qwen3.5-9b", "qwen");

        System.out.println("[migrate] 根目录 = " + root.toAbsolutePath());
        DatasetStore.MigrationReport rep = store.normalizeModels(aliases);
        System.out.println(rep.describe());
        System.out.println("[migrate] 完成（原 raw 已备份到 raw/.migrated-backup-*）");
    }
}
