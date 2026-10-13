package weilantianhai.dataset.tasks;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 任务清单加载：从 JSONL 文件读任务，或使用内置清单。
 *
 * <p>JSONL 每行一个对象：
 * <pre>
 * {"id": 1, "category": "camera_bookmark", "prompt": "...", "check": "...", "note": "..."}
 * </pre>
 * {@code id} 可省（按行号自动编号）。
 */
public final class TaskLoader {

    private TaskLoader() {
    }

    /** 从文件加载。 */
    public static List<TaskGoal> load(Path file) throws IOException {
        List<TaskGoal> tasks = new ArrayList<>();
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        int autoId = 1;
        for (String raw : lines) {
            String line = raw.strip();
            if (line.isEmpty() || line.startsWith("//") || line.startsWith("#")) continue;
            JSONObject o;
            try {
                o = JSONObject.parseObject(line);
            } catch (Exception e) {
                System.err.println("[dataset] 跳过坏任务行: " + e.getMessage());
                continue;
            }
            int id = o.getIntValue("id", autoId);
            autoId = Math.max(autoId, id) + 1;
            tasks.add(new TaskGoal(
                    id,
                    o.getString("category"),
                    o.getString("prompt"),
                    o.getString("check"),
                    o.getString("note")));
        }
        return tasks;
    }

    /**
     * 内置任务清单：覆盖六大类的一张最小可跑集合。
     *
     * <h3>两条硬规则（都踩过坑）</h3>
     *
     * <ol>
     *   <li><b>题目之间不能有状态依赖。</b>
     *       {@code SceneSnapshotter.reset()} 在**每道题执行前**都会清空模型树 / 机位 / 相机，
     *       所以"这题用上题建的 Box"必然失败 —— 记录下来的不是模型能力，而是"找不到模型"。
     *       每道题要么**自带 setup**（在同一句 prompt 里先建后改），要么只依赖复位放回的那个
     *       固定 seed 模型（名字见 {@code SceneSnapshotter.seedModelName}，当前是 {@code Tesseract}）。</li>
     *   <li><b>判据要能判。</b> 纯查询题用 {@code reply contains ...}，
     *       陷阱题用 {@code reject with ...}（详情见 {@link TaskGoal}）。</li>
     * </ol>
     */
    public static List<TaskGoal> builtin() {
        List<TaskGoal> t = new ArrayList<>();

        // ---- 摄像机：位置 ----
        t.add(new TaskGoal(1, "camera",
                "把摄像机位置设为 (0, 0, -10, 0)。",
                "camera.pos ≈ [0,0,-10,0]",
                "基础：SET_CAMERA_POS"));
        t.add(new TaskGoal(2, "camera",
                "读取当前摄像机位置并告诉我它是什么。",
                "reply contains (0,0,-10,0)",
                "纯查询：GET_CAMERA_POS。复位后相机固定在 (0,0,-10,0)，故答案可预期"));

        // ---- 摄像机：机位 ----
        t.add(new TaskGoal(3, "camera_bookmark",
                "创建一个机位，名字叫「俯视原点」，位置在 (0, 10, 0, 0)，朝向看向原点 (0, 0, 0, 0)。",
                "bookmark:俯视原点 exists; bookmark:俯视原点 pos ≈ [0,10,0,0]",
                "首选 look_at 方案"));
        t.add(new TaskGoal(4, "camera_bookmark",
                "创建两个机位：一个叫 A 位于 (2, 0, 0, 0) 看向原点，另一个叫 B 位于 (-2, 0, 0, 0) 看向原点。",
                "bookmark:A exists; bookmark:B exists; bookmark.count == 2",
                "一轮内造多个机位，验证不受相机动画影响"));
        t.add(new TaskGoal(5, "camera_bookmark",
                "创建一个机位叫「原点」，位于原点本身，朝向看向原点。",
                "reject with 不能相同",
                "陷阱任务：pos 与 look_at 相同，视线方向是零向量，**正确的行为是拒绝**。"
                        + "若模型硬造一个机位（比如偷偷改 look_at），本条判为不通过"));

        // ---- 模型树 ----
        t.add(new TaskGoal(6, "model_tree",
                "列出当前场景里所有的模型。",
                "reply contains Tesseract",
                "纯查询：LIST_MODEL。复位后场景里固定有一个 Tesseract，故答案可预期"));
        t.add(new TaskGoal(7, "model_tree",
                "创建一个名为 Box 的空模型。",
                "model:Box exists; model.count == 2",
                "CREATE_MODEL（复位后已有 seed Tesseract，故总数是 2）"));
        t.add(new TaskGoal(8, "model_tree",
                "创建一个名为 Box 的空模型，然后把它改名为 Crate。",
                "model:Crate exists; model:Box absent",
                "RENAME_MODEL —— **必须自带 setup**：Box 不能依赖上一题（7）建，"
                        + "因为每题执行前都会复位。这句 prompt 把「建」和「改」写在一起正是为此"));

        // ---- 基础形状 ----
        t.add(new TaskGoal(9, "shape",
                "在原点创建一个边长为 2 的超立方体（tesseract），命名为 T1。",
                "model:T1 exists",
                "CREATE_TESSERACT"));
        t.add(new TaskGoal(10, "shape",
                "在原点创建一个边长 1 的 5-cell，命名为 P5。",
                "model:P5 exists",
                "CREATE_5CELL"));

        // ---- 变换 ----
        t.add(new TaskGoal(11, "transform",
                "把名为 T1 的模型沿 x 轴平移 5，然后读取它的变换矩阵。",
                "reply matches \\[|矩阵|matrix",
                "TRANSFORM_MODEL + GET_MODEL_MATRIX。**自带 setup**（先建后读），"
                        + "判据只要求回复里带出矩阵（数值格式不稳定，不做精确比对）"));

        // ---- 几何雕刻 ----
        t.add(new TaskGoal(12, "carve",
                "在原点创建一个边长 1 的超立方体叫 T1，然后读取它的几何信息（顶点数、四面体数）。",
                "reply contains 四面体",
                "CREATE_TESSERACT + GET_MODEL_INFO。**自带 setup**。"
                        + "具体数值随实现变化，只校验回复里确实汇报了几何信息"));

        // ---- 复合 ----
        t.add(new TaskGoal(13, "composite",
                "创建一个叫 Tower 的模型，在里面创建两个边长为 1 的超立方体 Base 和 Top，最后把 Tower 的子模型合并。",
                "model:Tower subtree_exists",
                "复合任务：多步 + 路径寻址。不能断言 Tower/Base —— 合并后子模型会被吸收，"
                        + "所以只校验 Tower 这棵子树非空"));

        t.add(new TaskGoal(14, "composite",
                "创建一个机位叫「正视图」，位置 (0,0,-10,0) 看向原点；然后把摄像机切到这个机位。",
                "bookmark:正视图 exists; camera.pos ≈ [0,0,-10,0]",
                "机位 + 相机跳转的复合（相机恰好也落在同一位置，两条件可同时成立）"));

        return t;
    }

    /** 把任务列表写成 JSONL（供出题器产出后保存）。 */
    public static void save(List<TaskGoal> tasks, Path file) throws IOException {
        JSONArray arr = new JSONArray();
        for (TaskGoal g : tasks) {
            JSONObject o = new JSONObject();
            o.put("id", g.id());
            o.put("category", g.category());
            o.put("prompt", g.prompt());
            o.put("check", g.check());
            if (g.note() != null) o.put("note", g.note());
            arr.add(o);
        }
        Files.createDirectories(file.getParent() == null ? Path.of(".") : file.getParent());
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.size(); i++) {
            sb.append(arr.getJSONObject(i).toJSONString()).append(System.lineSeparator());
        }
        Files.writeString(file, sb.toString(), StandardCharsets.UTF_8);
    }
}
