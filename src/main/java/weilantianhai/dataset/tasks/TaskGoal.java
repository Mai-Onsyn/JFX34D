package weilantianhai.dataset.tasks;

import java.util.List;

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
