package weilantianhai.dataset.tasks;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import weilantianhai.agent.ir.operations.IOperation;
import weilantianhai.agent.ir.resolver.OperationRegistry;
import weilantianhai.agent.llm.LLMClient;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * LLM 自动出题器：让模型基于**真实可用操作清单**生成任务，输出可被
 * {@link TaskLoader} 读的 JSONL。
 *
 * <h3>为什么出题 prompt 必须与 agent 的 system.md 隔离</h3>
 *
 * <p>如果把 agent 的 {@code system.md}（它规定了输出 IR JSON 的格式）喂给出题器，
 * 模型会不自觉地把「答案格式」也写进任务里，甚至直接给出 IR。所以这里用**独立的出题 prompt**，
 * 只告诉它：有哪些操作类型、每个操作要什么参数、以及输出 JSON 数组的格式。
 *
 * <h3>难度分级</h3>
 *
 * <ul>
 *   <li>{@code simple} —— 单操作，照抄参数即可（如「创建一个机位」）；</li>
 *   <li>{@code medium} —— 2~3 步，需要先查询再操作（如「先看有哪些模型，再删掉其中一个」）；</li>
 *   <li>{@code complex} —— 多步 + 依赖中间结果 + 路径寻址 / 坐标计算
 *       （如「建一个 Tower，里面放 Base 和 Top，各平移不同量，最后合并」）；</li>
 *   <li>{@code trap} —— 故意给不可能/冲突的要求，看模型会不会**正确地失败**
 *       （如「机位 pos 与 look_at 完全相同」应当被拒绝而不是硬造）。</li>
 * </ul>
 *
 * <h3>自检</h3>
 *
 * <p>生成后会做一遍**可完成性过滤**：把每条任务交给同一个模型做「这个任务只靠上面的操作能不能完成」
 * 的二分类判断，丢掉判为不可完成的（可关，见 {@link Options#selfFilter}）。
 */
public final class TaskGenerator {

    /** 出题参数。 */
    public static final class Options {
        public int count = 20;
        public String difficulty = "mixed";
        public String category = "mixed";
        /** 是否做可完成性自检（多花一轮 LLM 调用，但能过滤掉离谱题）。 */
        public boolean selfFilter = true;
        /** 出题温度。高一点更有多样性。 */
        public double temperature = 0.8;
        /** 随机种子，便于复现。 */
        public long seed = System.currentTimeMillis();
    }

    private final LLMClient client;
    private final Options options;

    public TaskGenerator(LLMClient client, Options options) {
        this.client = client;
        this.options = options;
    }

    /**
     * 生成任务。
     *
     * @param startId 起始任务 id（避免与已有清单冲突）
     * @return 生成的任务列表
     */
    public List<TaskGoal> generate(int startId) throws Exception {
        List<TaskGoal> all = new ArrayList<>();

        // 分批生成：一次让模型出太多会质量下降，这里每批 8 条
        int batchSize = 8;
        int remaining = options.count;
        int nextId = startId;
        int guard = 0;

        while (remaining > 0 && guard++ < 20) {
            int n = Math.min(batchSize, remaining);
            String difficulty = pickDifficulty();
            String category = pickCategory();

            List<TaskGoal> batch = generateBatch(n, difficulty, category, nextId);
            if (batch.isEmpty()) {
                System.err.println("[gen] 本批生成 0 条（difficulty=" + difficulty + "），重试");
                continue;
            }

            if (options.selfFilter) {
                batch = filterCompletable(batch);
            }

            all.addAll(batch);
            nextId += batch.size();
            remaining -= batch.size();
            System.out.println("[gen] 已完成 " + all.size() + "/" + options.count
                    + "（本批 " + batch.size() + " 条，difficulty=" + difficulty + "）");
        }
        return all;
    }

    /* ==================== 单批生成 ==================== */

    private List<TaskGoal> generateBatch(int n, String difficulty, String category, int startId)
            throws Exception {
        String system = buildSystemPrompt(difficulty, category, n);
        String user = "请生成 " + n + " 条任务。";

        // 出题用高温度以增加多样性（LLMClient 现在自带 withTemperature）
        String raw = client.withTemperature(options.temperature).chat(system, user);

        return parseGenerated(raw, difficulty, category, startId);
    }

    /** 从 LLM 输出里抠出 JSON 数组并转成任务列表。 */
    private List<TaskGoal> parseGenerated(String raw, String difficulty, String category, int startId) {
        List<TaskGoal> out = new ArrayList<>();
        if (raw == null || raw.isBlank()) return out;

        String json = extractJsonArray(raw);
        if (json == null) {
            System.err.println("[gen] 无法从输出里找到 JSON 数组，原文前 300 字：\n"
                    + raw.substring(0, Math.min(300, raw.length())));
            return out;
        }

        JSONArray arr;
        try {
            arr = JSONArray.parseArray(json);
        } catch (Exception e) {
            System.err.println("[gen] JSON 解析失败：" + e.getMessage());
            return out;
        }

        int id = startId;
        for (int i = 0; i < arr.size(); i++) {
            JSONObject o = arr.getJSONObject(i);
            if (o == null) continue;
            String prompt = o.getString("prompt");
            if (prompt == null || prompt.isBlank()) continue;

            String cat = o.getString("category");
            if (cat == null || cat.isBlank()) cat = category.equals("mixed") ? "composite" : category;

            String check = o.getString("check");
            String note = o.getString("note");
            if (note == null) note = "LLM 出题（难度 " + difficulty + "）";

            out.add(new TaskGoal(id++, cat, prompt.strip(), check == null ? "" : check.strip(), note));
        }
        return out;
    }

    /* ==================== 可完成性自检 ==================== */

    /** 让模型逐条判断「只靠可用操作能不能完成」，丢掉判为 false 的。 */
    private List<TaskGoal> filterCompletable(List<TaskGoal> batch) {
        List<String> prompts = new ArrayList<>();
        for (TaskGoal g : batch) prompts.add(g.prompt());

        String system = "你是四维建模引擎的能力审核员。下面是引擎**全部可用操作**：\n"
                + operationCatalog()
                + "\n\n用户会给你若干条任务，请逐条判断「只使用上述操作，这些任务能否被完成」。"
                + "注意：可以在任务之间查询状态（如读取模型列表、读取相机位置）再决定下一步。\n"
                + "输出严格的 JSON 数组，每项形如 {\"index\": 0, \"completable\": true, \"reason\": \"...\"}。"
                + "不要输出任何其它文字，不要用 markdown 代码围栏。";

        StringBuilder user = new StringBuilder("请判断以下 " + prompts.size() + " 条任务：\n");
        for (int i = 0; i < prompts.size(); i++) {
            user.append(i).append(". ").append(prompts.get(i)).append('\n');
        }

        try {
            // 自检要确定性 → 温度压到 0
            String raw = client.withTemperature(0.0).chat(system, user.toString());
            String json = extractJsonArray(raw);
            if (json == null) {
                System.err.println("[gen] 自检输出无 JSON，跳过过滤");
                return batch;
            }
            JSONArray verdicts = JSONArray.parseArray(json);
            Map<Integer, Boolean> ok = new LinkedHashMap<>();
            for (int i = 0; i < verdicts.size(); i++) {
                JSONObject v = verdicts.getJSONObject(i);
                if (v == null) continue;
                Integer idx = v.getInteger("index");
                Boolean completable = v.getBoolean("completable");
                if (idx != null && completable != null) ok.put(idx, completable);
            }

            List<TaskGoal> kept = new ArrayList<>();
            for (int i = 0; i < batch.size(); i++) {
                Boolean pass = ok.get(i);
                if (pass == null || pass) {
                    kept.add(batch.get(i));
                } else {
                    System.out.println("[gen] 过滤掉不可完成的任务：" + batch.get(i).prompt());
                }
            }
            return kept;
        } catch (Exception e) {
            System.err.println("[gen] 自检失败（保留全部）：" + e.getMessage());
            return batch;
        }
    }

    /* ==================== 出题 prompt ==================== */

    private String buildSystemPrompt(String difficulty, String category, int n) {
        StringBuilder sb = new StringBuilder();

        sb.append("你是「四维智构」引擎的**任务出题器**。你的工作是对着一个 AI 助手出题，")
          .append("考察它把自然语言指令翻译成引擎操作的能力。\n\n");

        sb.append("## 引擎可用的全部操作\n\n");
        sb.append(operationCatalog()).append('\n');

        sb.append("## 坐标系与格式约定\n\n");
        sb.append("- 四维坐标一律写成 `(x, y, z, w)`，例如 `(0, 10, 0, 0)`。\n");
        sb.append("- 旋转角度单位是**度**。\n");
        sb.append("- 模型路径用 `/` 分隔，例如 `Tower/Base`；中间层不必预先存在。\n");
        sb.append("- 坐标系是**左手系**。\n\n");

        sb.append("## 题目的可完成性（出题时必须遵守）\n\n");
        sb.append("被测模型拿到的是**一句自然语言指令 + 一个已经准备好的场景**。")
          .append("由此三条硬规则（违反会让题目根本做不了）：\n\n");
        sb.append("1. 题目里出现的模型，必须是**这道题自己创建的**。\n");
        sb.append("   把「创建」和后续的「修改 / 读取」写在**同一条 prompt 里**：\n");
        sb.append("   - 正例：「创建一个名为 Box 的空模型，然后把它改名为 Crate」。\n");
        sb.append("   - 反例：「把 Box 沿 x 轴平移 5」—— Box 从没被创建，")
          .append("被测模型只能回一句「模型不存在」，这道题什么也考不出来。\n");
        sb.append("2. **绝不要出依赖「别的题建出来的模型」的题** —— 不要假设场景里有某个东西。\n");
        sb.append("3. 需要多步操作时，仍然全部写在**这一条** prompt 里（可以写得很长），不要拆成两条题。\n\n");
        sb.append("一句话：**不要出现「引用一个题目里没创建的模型」的题**。\n\n");

        sb.append("## 本次要求\n\n");
        sb.append("- 出题类别：").append(describeCategory(category)).append('\n');
        sb.append("- 难度：").append(describeDifficulty(difficulty)).append('\n');
        sb.append("- 数量：").append(n).append(" 条\n\n");

        sb.append("## 难度说明\n\n");
        sb.append("- `simple`：单步操作，参数照抄即可。例：「创建一个叫 Box 的空模型」。\n");
        sb.append("- `medium`：2~3 步，通常需要**先查询再操作**。")
          .append("例：「创建一个叫 Box 的模型，先列出所有模型确认它建好了，然后把它隐藏」。\n");
        sb.append("- `complex`：多步且**后一步依赖前一步的结果**，涉及路径寻址 / 坐标计算 / 批量操作。")
          .append("例：「创建一个叫 Tower 的模型，在里面创建边长为 1 的超立方体 Base 和 Top，")
          .append("把 Base 沿 w 轴平移 2，把 Top 沿 w 轴平移 -2，最后把 Tower 的子模型全部合并」。\n");
        sb.append("- `trap`：**故意不可能或语义冲突**的要求，考察模型会不会正确地失败而不是硬造。")
          .append("例：「创建一个机位，位置在 (0,0,0,0)，朝向看向 (0,0,0,0)」（视线方向为零向量，应当被拒绝）。")
          .append("trap 题的 `check` 用 `reject with <回执里应当出现的片段>`（报错才算通过），")
          .append("`note` 写明**期望的正确行为**。\n\n");

        sb.append("## 输出格式（严格遵守）\n\n");
        sb.append("输出一个 JSON 数组，**不要**任何解释文字、**不要** markdown 代码围栏。每项：\n\n");
        sb.append("```\n");
        sb.append("{\n");
        sb.append("  \"category\":  \"camera | camera_bookmark | model_tree | transform | shape | carve | composite\",\n");
        sb.append("  \"prompt\":    \"给 AI 的自然语言指令（中文，像人话，不要出现操作名大写下划线形式）\",\n");
        sb.append("  \"check\":     \"可机器判定的断言，用分号分隔；不确定就写空字符串\",\n");
        sb.append("  \"note\":      \"给打分人看的说明（可选）\"\n");
        sb.append("}\n");
        sb.append("```\n\n");

        sb.append("## check 语法（能用就用，用不了就留空）\n\n");
        sb.append("场景类 —— 看「世界变成了什么」：\n");
        sb.append("```\n");
        sb.append("bookmark:<名字> exists\n");
        sb.append("bookmark:<名字> pos ≈ [x,y,z,w]\n");
        sb.append("bookmark:<名字> absent\n");
        sb.append("model:<路径> exists\n");
        sb.append("model:<路径> absent\n");
        sb.append("model:<路径> subtree_exists        （该路径或其任一后代存在 —— 合并类操作用）\n");
        sb.append("model:<路径> vertices == N\n");
        sb.append("model:<路径> tetrahedrons == N\n");
        sb.append("camera.pos ≈ [x,y,z,w]\n");
        sb.append("bookmark.count == N\n");
        sb.append("model.count == N\n");
        sb.append("```\n");
        sb.append("文本类 —— 看「AI 说了什么」，用于**纯查询题**（世界没变，快照恒等）：\n");
        sb.append("```\n");
        sb.append("reply contains <片段>              （回复里出现该子串，自动归一化空白与全角标点）\n");
        sb.append("reply matches <正则>\n");
        sb.append("```\n");
        sb.append("反向类 —— 用于**陷阱题**：语义是反的，**报错才算通过**：\n");
        sb.append("```\n");
        sb.append("reject with <片段>                 （这道题应当失败，且回执里出现该片段）\n");
        sb.append("```\n");
        sb.append("容差 1e-3。**只写你确实能算准的检查** —— 写错了会误判。\n");
        sb.append("注意 `model.count` 统计的是**整个场景的模型总数**，")
          .append("不是这道题新建了几个 —— 写等于号时要把场景里已存在的模型也算进去；")
          .append("算不准就别写这一条。\n");

        return sb.toString();
    }

    /* ==================== 操作清单（从注册表导出） ==================== */

    /**
     * 从 {@link OperationRegistry} 导出真实可用操作清单。
     *
     * <p>注意：{@code OperationRegistry} 只暴露 {@code creat(type)} 与 {@code size()}，
     * 没有「列出所有 type」的 API。这里为了**保持权威**，改为从注册表逐个探测已知 type，
     * 探测不到的不列出——这样一旦注册表增删操作，出题清单会自动跟随。
     * 参数说明则维护在下面的 {@link #OPERATION_DOCS} 里。
     */
    private static String operationCatalog() {
        OperationRegistry probe = new OperationRegistry();
        StringBuilder sb = new StringBuilder();

        // 按六大类分组列出，只保留注册表里真实存在的
        Map<String, List<String>> groups = new LinkedHashMap<>();
        groups.put("摄像机—位置", List.of("GET_CAMERA_POS", "MOVE_CAMERA_POS", "SET_CAMERA_POS"));
        groups.put("摄像机—视角", List.of("GET_CAMERA_VIEW", "ROTATE_CAMERA_VIEW", "SET_CAMERA_VIEW"));
        groups.put("摄像机—机位", List.of("LIST_CAMERA_BOOKMARKS", "SAVE_CAMERA_BOOKMARK",
                "SET_CAMERA_BOOKMARK", "GO_CAMERA_BOOKMARK",
                "RENAME_CAMERA_BOOKMARK", "DELETE_CAMERA_BOOKMARK"));
        groups.put("模型树", List.of("LIST_MODEL", "CREATE_MODEL", "DELETE_MODEL", "COPY_MODEL",
                "MERGE_MODEL", "APPLY_TRANSFORM_TO_VERTEX", "RENAME_MODEL",
                "MERGE_ALL_SUB_MODELS", "SET_MODEL_VISIBLE"));
        groups.put("模型变换", List.of("GET_MODEL_MATRIX", "TRANSFORM_MODEL"));
        groups.put("基础形状", List.of("CREATE_TETRAHEDRON", "CREATE_5CELL", "CREATE_16CELL",
                "CREATE_TESSERACT", "CREATE_PRISM4", "CREATE_CONE4", "CREATE_BALL4"));
        groups.put("几何雕刻", List.of("GET_MODEL_INFO", "GET_TETRAHEDRON", "SET_TETRAHEDRON_VERTEX",
                "TRANSFORM_TETRAHEDRONS", "ADD_TETRAHEDRON", "REMOVE_TETRAHEDRON", "SLICE_MODEL"));

        for (Map.Entry<String, List<String>> e : groups.entrySet()) {
            List<String> present = new ArrayList<>();
            for (String type : e.getValue()) {
                if (registered(probe, type)) present.add(type);
            }
            if (present.isEmpty()) continue;

            sb.append("### ").append(e.getKey()).append('\n');
            for (String type : present) {
                sb.append("- `").append(type).append("`：")
                  .append(OPERATION_DOCS.getOrDefault(type, "（无说明）"))
                  .append('\n');
            }
            sb.append('\n');
        }
        return sb.toString().stripTrailing();
    }

    private static boolean registered(OperationRegistry registry, String type) {
        try {
            IOperation op = registry.creat(type);
            return op != null;
        } catch (Exception e) {
            return false;
        }
    }

    /** 每个操作的参数说明（给人/AI 看的一句话）。 */
    private static final Map<String, String> OPERATION_DOCS = Map.ofEntries(
            Map.entry("GET_CAMERA_POS", "无参数，返回相机位置。"),
            Map.entry("MOVE_CAMERA_POS", "参数 `axis`(x|y|z|w) 与 `distance`(数值)，沿轴平移。"),
            Map.entry("SET_CAMERA_POS", "参数 `pos`：四维坐标，直接设相机位置（约 1 秒平滑动画）。"),

            Map.entry("GET_CAMERA_VIEW", "无参数，返回相机视线的四个基向量。"),
            Map.entry("ROTATE_CAMERA_VIEW", "参数 `plane`(xy|xz|xw|yz|yw|zw) 与 `angle`(度)。"),
            Map.entry("SET_CAMERA_VIEW", "参数 `view`：{vx,vy,vz,vw} 四个正交单位向量。"),

            Map.entry("LIST_CAMERA_BOOKMARKS", "无参数，列出所有机位及其下标。"),
            Map.entry("SAVE_CAMERA_BOOKMARK", "参数 `name`(可选)。把**当前相机**状态存为机位。"),
            Map.entry("SET_CAMERA_BOOKMARK", "参数 `name`、`pos`，可选 `look_at`（看向的点）或 `view`（显式四基）。"
                    + "**不碰相机**，可直接指定位置与朝向造机位；`look_at` 与 `view` 不能同时给；`pos` 与 `look_at` 不能相同。"),
            Map.entry("GO_CAMERA_BOOKMARK", "参数 `name` 或 `index`，把相机切到该机位（约 1 秒动画）。"),
            Map.entry("RENAME_CAMERA_BOOKMARK", "参数 `name` 或 `index`，加新名 `new_name`。"),
            Map.entry("DELETE_CAMERA_BOOKMARK", "参数 `name` 或 `index`。删后后续机位下标会前移。"),

            Map.entry("LIST_MODEL", "无参数，列出所有模型根路径。"),
            Map.entry("CREATE_MODEL", "参数 `name`：模型路径，创建空模型（容器）。"),
            Map.entry("DELETE_MODEL", "参数 `name`：删除该路径及其所有子模型。"),
            Map.entry("COPY_MODEL", "参数 `src` 与 `dst`：复制整个子树。"),
            Map.entry("MERGE_MODEL", "参数 `src1`、`src2`、`dst`：合并两个模型到一个新模型。"),
            Map.entry("APPLY_TRANSFORM_TO_VERTEX", "参数 `src`、`dst`：把变换矩阵烘焙进顶点，结果存为新模型。"),
            Map.entry("RENAME_MODEL", "参数 `path` 与 `newName`。"),
            Map.entry("MERGE_ALL_SUB_MODELS", "参数 `src`、`dst`：把 src 下所有子模型合并成 dst。"),
            Map.entry("SET_MODEL_VISIBLE", "参数 `name` 与 `visible`(true|false)。"),

            Map.entry("GET_MODEL_MATRIX", "参数 `name`，返回 5x5 变换矩阵。"),
            Map.entry("TRANSFORM_MODEL", "参数 `name` 与 `matrix`：5x5 行主序矩阵。"),

            Map.entry("CREATE_TETRAHEDRON", "参数 `name` 与四个顶点坐标。"),
            Map.entry("CREATE_5CELL", "参数 `name`、`center`、`size`：正五胞体。"),
            Map.entry("CREATE_16CELL", "参数 `name`、`center`、`size`：16-cell。"),
            Map.entry("CREATE_TESSERACT", "参数 `name`、`center`、`size`：超立方体。"),
            Map.entry("CREATE_PRISM4", "参数 `name`、`center`、`size` 等：四维棱柱。"),
            Map.entry("CREATE_CONE4", "参数 `name`、`center`、`size` 等：四维锥。"),
            Map.entry("CREATE_BALL4", "参数 `name`、`center`、`radius` 等：四维球。"),

            Map.entry("GET_MODEL_INFO", "参数 `name`，返回几何信息（顶点数、四面体数等）。"),
            Map.entry("GET_TETRAHEDRON", "参数 `name` 与 `index`，返回指定四面体的四个顶点。"),
            Map.entry("SET_TETRAHEDRON_VERTEX", "参数 `name`、`index`、`vertex`(0-3) 与新坐标。"),
            Map.entry("TRANSFORM_TETRAHEDRONS", "参数 `name` 与 `matrix`：只变换指定四面体。"),
            Map.entry("ADD_TETRAHEDRON", "参数 `name` 与四个顶点：往模型里加一个四面体。"),
            Map.entry("REMOVE_TETRAHEDRON", "参数 `name` 与 `index`。"),
            Map.entry("SLICE_MODEL", "参数 `name`、`plane` 等：按平面切割模型。")
    );

    /* ==================== 工具 ==================== */

    private String pickDifficulty() {
        if (!"mixed".equals(options.difficulty)) return options.difficulty;
        // mixed：simple 25% / medium 35% / complex 30% / trap 10%
        double r = random().nextDouble();
        if (r < 0.25) return "simple";
        if (r < 0.60) return "medium";
        if (r < 0.90) return "complex";
        return "trap";
    }

    private String pickCategory() {
        if (!"mixed".equals(options.category)) return options.category;
        String[] cats = {"camera", "camera_bookmark", "model_tree", "transform",
                "shape", "carve", "composite"};
        return cats[random().nextInt(cats.length)];
    }

    /** 单一 {@link Random} 实例（种子由 {@link Options#seed} 固定，便于复现）。 */
    private Random random() {
        if (rng == null) rng = new Random(options.seed);
        return rng;
    }

    private Random rng;

    private static String describeDifficulty(String d) {
        return switch (d) {
            case "simple" -> "简单（单步操作）";
            case "medium" -> "中等（2~3 步，需先查询）";
            case "complex" -> "复杂（多步且后一步依赖前一步结果）";
            case "trap" -> "陷阱（不可能或语义冲突，考察正确失败）";
            default -> "混合（四种难度都要有）";
        };
    }

    private static String describeCategory(String c) {
        return "mixed".equals(c)
                ? "不限（camera / camera_bookmark / model_tree / transform / shape / carve / composite 均可是混合）"
                : c;
    }

    /**
     * 从文本里抠出第一个平衡的 JSON 数组（容忍 markdown 围栏与前后废话）。
     *
     * <p>公开是为了能离线自检（不联网也能验证转义/嵌套/围栏这些边角）。
     */
    public static String extractJsonArray(String raw) {
        if (raw == null) return null;
        int start = raw.indexOf('[');
        if (start < 0) return null;

        int depth = 0;
        boolean inStr = false;
        boolean escaped = false;
        for (int i = start; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (inStr) {
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    inStr = false;
                }
                continue;
            }
            if (c == '"') {
                inStr = true;
            } else if (c == '[') {
                depth++;
            } else if (c == ']') {
                depth--;
                if (depth == 0) {
                    return raw.substring(start, i + 1);
                }
            }
        }
        return null;
    }

    /** 把任务写成 JSONL，供 {@code --tasks=} 使用。 */
    public static void save(List<TaskGoal> tasks, Path file) throws IOException {
        TaskLoader.save(tasks, file);
    }
}
