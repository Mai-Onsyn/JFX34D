package weilantianhai.dataset.tasks;

import weilantianhai.dataset.DatasetPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * 题库管理：把「题目清单文件」当作可切换的**题库**。
 *
 * <h3>为什么要有这一层</h3>
 *
 * <p>出题器每次产出一份 JSONL，如果只能靠 {@code --tasks=} 命令行指定，
 * 那就得改启动参数才能换题集 —— 而"换一套题再跑一遍"正是最频繁的操作。
 * 所以把 {@code data/tasks/} 下的每个 {@code *.jsonl} 都当作一个可选题库，
 * 在窗口里直接下拉切换。
 *
 * <p>题库来源有两处：
 * <ol>
 *   <li>{@link #BUILTIN} —— 内置 14 题（写死在 {@link TaskLoader#builtin()} 里，没有文件）；</li>
 *   <li>{@code data/tasks/*.jsonl} —— 出题器产物或手工编写的清单。</li>
 * </ol>
 *
 * <p>首次启动时会把内置题**导出成文件**（{@code data/tasks/builtin.jsonl}），
 * 方便用户直接改它来手写题库，而不用去翻 Java 源码。
 */
public final class TaskBanks {

    /** 下拉框里代表「内置题」的那一项。 */
    public static final String BUILTIN = DatasetPaths.BUILTIN_BANK;

    private static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private TaskBanks() {
    }

    /** 题库目录（不存在就创建）。 */
    public static Path dir() throws IOException {
        Path d = DatasetPaths.TASKS_DIR;
        Files.createDirectories(d);
        return d;
    }

    /**
     * 列出所有可选题库的**下拉框显示名**。
     *
     * <p>内置题永远排第一；其余按文件名排序。只收 {@code *.jsonl} 且能读出至少一条任务的
     * （空文件/坏文件不列出来，免得选了之后一片空白还以为是程序坏了）。
     */
    public static List<String> list() {
        List<String> out = new ArrayList<>();
        out.add(BUILTIN);
        try {
            Path d = DatasetPaths.TASKS_DIR;
            if (!Files.isDirectory(d)) return out;
            try (Stream<Path> s = Files.list(d)) {
                s.filter(p -> p.toString().endsWith(".jsonl"))
                        .filter(Files::isRegularFile)
                        .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                        .forEach(p -> {
                            String name = p.getFileName().toString();
                            // 空题库不列出：选了它会跑 0 题，用户会以为是 bug
                            if (countTasks(p) > 0) out.add(name);
                        });
            }
        } catch (IOException e) {
            System.err.println("[dataset] 列题库失败: " + e.getMessage());
        }
        return out;
    }

    /** 题库显示名 → 实际文件路径；内置题返回 null（表示走 {@link TaskLoader#builtin()}）。 */
    public static Path pathOf(String bankName) {
        if (bankName == null || bankName.isBlank() || BUILTIN.equals(bankName)) return null;
        return DatasetPaths.TASKS_DIR.resolve(bankName);
    }

    /** 按题库名加载任务。 */
    public static List<TaskGoal> load(String bankName) throws IOException {
        Path p = pathOf(bankName);
        return p == null ? TaskLoader.builtin() : TaskLoader.load(p);
    }

    /** 题库里的题目数量（用于下拉框提示"共 N 题"）；读不出来返回 0。 */
    public static int countTasks(Path bankFile) {
        try {
            return TaskLoader.load(bankFile).size();
        } catch (Exception e) {
            return 0;
        }
    }

    public static int countTasks(String bankName) {
        Path p = pathOf(bankName);
        if (p == null) return TaskLoader.builtin().size();
        return countTasks(p);
    }

    /**
     * 给新题库起一个**保证不重名**的文件名，形如 {@code generated-20261011-181230.jsonl}。
     *
     * <p>命名规则：{@code <前缀>-<yyyyMMdd-HHmmss>.jsonl}。带上时间戳是为了能按时间排序，
     * 但**光有时间戳不够** —— 精度只到秒，同一秒内生成两次（或你手速快点了两下「开始生成」）
     * 就会撞名。而 {@code TaskLoader.save} 走的是 {@code Files.writeString}（**截断覆盖**），
     * 撞名就会**静默顶掉**前一次的结果。
     *
     * <p>所以这里在时间戳之外再补一道保险：若目标文件已存在，依次尝试
     * {@code -2}、{@code -3}… 直到找到一个空位（和系统"同名(1)"一个意思，只是格式不同）。
     *
     * <p>前缀里的路径分隔符等会被替换成下划线 —— 用户能直接输入这个名字，
     * 不能让它跑到别的目录去。
     */
    public static Path newBankFile(String prefix) {
        String safe = sanitizePrefix(prefix);
        String base = safe + "-" + LocalDateTime.now().format(STAMP);
        Path candidate = DatasetPaths.TASKS_DIR.resolve(base + ".jsonl");
        for (int i = 2; i <= 99 && Files.exists(candidate); i++) {
            candidate = DatasetPaths.TASKS_DIR.resolve(base + "-" + i + ".jsonl");
        }
        return candidate;
    }

    /** 题库名前缀安全化：路径分隔符、通配符、空白等一律换成下划线。 */
    private static String sanitizePrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return "generated";
        }
        String s = prefix.strip().replaceAll("[\\\\/:*?\"<>|\\s]+", "_");
        // 全是非法字符时兜底，避免出现 "-20261013-223020.jsonl" 这种没有主名的文件
        return s.isBlank() || s.replace("_", "").isBlank() ? "generated" : s;
    }

    /**
     * 保存任务为一份**新题库**，返回文件名（下拉框里可以直接选它）。
     */
    public static String saveAsBank(List<TaskGoal> tasks, Path file) throws IOException {
        Files.createDirectories(file.getParent() == null ? DatasetPaths.TASKS_DIR : file.getParent());
        TaskLoader.save(tasks, file);
        return file.getFileName().toString();
    }

    /**
     * 确保题库目录存在，并把内置 14 题导出成 {@code data/tasks/builtin.jsonl}（仅首次）。
     *
     * <p>这样用户想手写题库时有个现成模板可抄，而不是去看 Java 源码里的 {@link TaskLoader#builtin()}。
     *
     * @return 导出文件的路径；若已存在则返回现有文件
     */
    public static Path ensureBuiltinExport() throws IOException {
        Path dir = dir();
        Path out = dir.resolve("builtin.jsonl");
        if (Files.exists(out)) return out;
        List<TaskGoal> tasks = TaskLoader.builtin();
        // 手写一个带注释头的版本，说明两条硬规则 —— TaskLoader.load 会跳过 # 开头的行
        StringBuilder sb = new StringBuilder();
        sb.append("# 内置 14 题（首次启动自动导出，可自由修改/另存）\n");
        sb.append("# 两条硬规则：\n");
        sb.append("#   1) 题目之间不能有状态依赖 —— 每题执行前都会复位场景，\n");
        sb.append("#      引用上一题建出来的模型必然失败。要「先建后改」就写在同一句 prompt 里。\n");
        sb.append("#   2) 每条题都要有能机器判定的 check —— 纯查询题用 reply contains，\n");
        sb.append("#      陷阱题用 reject with（报错才算通过）。留空就只能人工打分。\n");
        for (TaskGoal g : tasks) {
            com.alibaba.fastjson2.JSONObject o = new com.alibaba.fastjson2.JSONObject();
            o.put("id", g.id());
            o.put("category", g.category());
            o.put("prompt", g.prompt());
            o.put("check", g.check());
            if (g.note() != null) o.put("note", g.note());
            sb.append(o.toJSONString()).append(System.lineSeparator());
        }
        Files.writeString(out, sb.toString(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        return out;
    }
}
