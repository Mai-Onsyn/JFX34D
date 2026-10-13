package weilantianhai.dataset.tasks;

import java.util.List;

/**
 * 一条任务目标：给 AI 的一句自然语言指令 + 一条可机器判定的验收判据。
 *
 * <h3>{@code check} 的结构化判据语法</h3>
 *
 * <p>为了能自动打分，{@code check} 不是自然语言，而是一组**用 {@code ;} 分隔的短断言**，
 * 每条形如 {@code <subject> <predicate>}。
 *
 * <pre>
 * bookmark:俯视原点 exists                      -- 机位列表里存在该名字
 * bookmark:俯视原点 pos ≈ [0,10,0,0]            -- 且位置在容差内
 * model:Tower  exists                           -- 模型树路径存在
 * model:Tower/Base vertices == 5                -- 模型信息里的顶点数
 * camera.pos ≈ [0,0,-10,0]                      -- 相机位置
 * </pre>
 *
 * <h3>判两类"看不见"的题</h3>
 *
 * <p>场景快照只能反映"世界变成了什么"，判不了**纯查询题**（只是 GET，世界没变）
 * 与**陷阱题**（考察"正确地失败"，成功反而是错的）。为此另加两类断言：
 *
 * <pre>
 * reply contains (0, 0, -10, 0)     -- 回复文本里出现该片段（判查询题答对没有）
 * reply matches \d+                 -- 回复文本匹配正则
 * reject with 未知操作类型           -- 该任务**必须失败**，且回执里出现该片段（判陷阱题）
 * </pre>
 *
 * <p>自动判定失败时不影响流程：脚本会把它当**预填建议**，人工打分器仍可覆盖。
 * 容差默认 {@code 1e-3}。
 *
 * @param id       任务编号（在任务集内唯一）
 * @param category 任务类别，与 {@code OperationRegistry} 的六大类对齐
 *                 （{@code camera} / {@code camera_bookmark} / {@code model_tree} /
 *                 {@code transform} / {@code shape} / {@code carve} / {@code composite}）
 * @param prompt   给 AI 的自然语言指令
 * @param check    结构化验收判据，可为空串（表示只能人工打分）
 * @param note     给打分人看的补充说明，可为 null
 */
public record TaskGoal(
        int id,
        String category,
        String prompt,
        String check,
        String note
) {

    /** 把 {@code check} 拆成一条条断言。 */
    public List<String> assertions() {
        if (check == null || check.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(check.split(";"))
                .map(String::strip)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    public boolean hasCheck() {
        return !assertions().isEmpty();
    }

    /**
     * 该任务是否要求「正确地失败」（陷阱题）。
     *
     * <p>这类题的判据必须以 {@code reject} 开头。运行器据此反转默认预期：
     * 有 {@code reject} 断言时，**执行报错才是对的**。
     */
    public boolean expectsRejection() {
        return assertions().stream().anyMatch(a -> a.startsWith("reject"));
    }

    /**
     * 该任务是否需要「回复文本」才能判定（纯查询题）。
     *
     * <p>只有 {@code reply} 类断言的题，场景快照完全一样，判定依据全在回复里。
     */
    public boolean needsReplyText() {
        List<String> as = assertions();
        return !as.isEmpty() && as.stream().allMatch(a -> a.startsWith("reply"));
    }
}
