package weilantianhai.dataset.core;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import weilantianhai.agent.trace.RoundTrace;
import weilantianhai.dataset.tasks.TaskGoal;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public final class TaskRunRecord {
    /** 打分标签枚举。 */
    public enum Verdict {
        CORRECT, INCORRECT, SKIP, TIMEOUT, ERROR
    }

    private final TaskGoal task;
    private final String model;
    private final String systemPromptHash;
    private final String runId;
    /** API 侧的具体模型名（如 {@code qwen/qwen3.5-9b}）；与展示用的 {@link #model} 区分。 */
    private String apiModel;

    private OffsetDateTime startedAt;
    private OffsetDateTime finishedAt;
    private long elapsedMs;

    private int requestId = -1;
    private String replyMarkdown = "";
    private boolean agentError;
    private String agentErrorMessage;

    private final List<RoundTrace> rounds = new ArrayList<>();
    private SceneSnapshot snapshotBefore;
    private SceneSnapshot snapshotAfter;

    private Verdict verdict = Verdict.SKIP;
    private String comment = "";
    private String scorer = "human";
    /** 自动判据的预填结论（可能为 null）。 */
    private String autoCheckDetail;
    private Boolean autoCheckPassed;

    public TaskRunRecord(TaskGoal task, String model, String systemPromptHash, String runId) {
        this.task = task;
        this.model = model;
        this.systemPromptHash = systemPromptHash;
        this.runId = runId;
    }

    /* ==================== 采集 ==================== */

    public void markStart() {
        this.startedAt = OffsetDateTime.now();
    }

    public void markFinish(long elapsedMs) {
        this.finishedAt = OffsetDateTime.now();
        this.elapsedMs = elapsedMs;
    }

    public void addRound(RoundTrace trace) {
        rounds.add(trace);
    }

    /* ==================== 汇总指标（派生） ==================== */

    public int roundCount() {
        return rounds.size();
    }

    /** 该任务是否**每一轮**都解析成功。 */
    public boolean allRoundsParseOk() {
        return !rounds.isEmpty() && rounds.stream().allMatch(RoundTrace::isParseOk);
    }

    public boolean allRoundsTypeOk() {
        return !rounds.isEmpty() && rounds.stream().allMatch(RoundTrace::getTypeOk);
    }

    public boolean allRoundsExecOk() {
        return !rounds.isEmpty() && rounds.stream().allMatch(RoundTrace::getExecOk);
    }

    public String allReceipts() {
        StringBuilder sb = new StringBuilder();
        for (RoundTrace t : rounds) {
            String r = t.getReceipts();
            if (r != null && !r.isBlank()) {
                sb.append(r).append('\n');
            }
        }
        return sb.toString();
    }

    public String finalAnswer() {
        for (int i = rounds.size() - 1; i >= 0; i--) {
            RoundTrace t = rounds.get(i);
            if (t.isDone() && t.getContent() != null && !t.getContent().isBlank()) {
                return t.getContent();
            }
        }
        return replyMarkdown;
    }

    /* ==================== 序列化 ==================== */

    public JSONObject toJson() {
        JSONObject o = new JSONObject();
        o.put("dataset_version", 2);
        o.put("run_id", runId);
        o.put("task_id", task.id());
        o.put("category", task.category());
        o.put("prompt", task.prompt());
        o.put("check", task.check());
        if (task.note() != null) o.put("note", task.note());

        JSONObject env = new JSONObject();
        env.put("model", model);
        if (apiModel != null && !apiModel.isBlank()) env.put("api_model", apiModel);
        env.put("system_prompt_hash", systemPromptHash);
        o.put("env", env);

        o.put("started_at", startedAt == null ? null : startedAt.toString());
        o.put("finished_at", finishedAt == null ? null : finishedAt.toString());
        o.put("elapsed_ms", elapsedMs);

        o.put("request_id", requestId);
        o.put("reply_markdown", replyMarkdown);
        o.put("agent_error", agentError);
        if (agentError) o.put("agent_error_message", agentErrorMessage);

        JSONArray roundsArr = new JSONArray();
        for (RoundTrace t : rounds) {
            roundsArr.add(roundToJson(t));
        }
        o.put("rounds", roundsArr);
        o.put("round_count", rounds.size());
        o.put("all_rounds_parse_ok", rounds.isEmpty() ? null : allRoundsParseOk());
        o.put("all_rounds_type_ok", rounds.isEmpty() ? null : allRoundsTypeOk());
        o.put("all_rounds_exec_ok", rounds.isEmpty() ? null : allRoundsExecOk());

        if (snapshotBefore != null) o.put("state_before", snapshotBefore.toJson());
        if (snapshotAfter != null) o.put("state_after", snapshotAfter.toJson());

        JSONObject v = new JSONObject();
        v.put("label", verdict.name().toLowerCase());
        v.put("comment", comment);
        v.put("auto_check_passed", autoCheckPassed);
        v.put("auto_check_detail", autoCheckDetail);
        o.put("verdict", v);
        o.put("scorer", scorer);
        return o;
    }

    private static JSONObject roundToJson(RoundTrace t) {
        JSONObject r = new JSONObject();
        r.put("round", t.getRound());
        r.put("raw_llm_json", t.getRawLlmJson());
        r.put("parse_ok", t.isParseOk());
        r.put("parse_error_code", t.getParseErrorCode());
        r.put("parse_error_message", t.getParseErrorMessage());
        r.put("op_types", t.getOpTypes());
        r.put("type_ok", t.isParseOk() ? t.getTypeOk() : null);
        r.put("type_errors", t.getTypeErrors());
        r.put("exec_ok", t.isParseOk() ? t.getExecOk() : null);
        r.put("exec_errors", t.getExecErrors());
        r.put("done", t.isDone());
        r.put("content", t.getContent());
        r.put("receipts", t.getReceipts());
        r.put("elapsed_ms", t.getElapsedMs());
        if (t.getStateAfter() != null) r.put("state_after", t.getStateAfter());
        return r;
    }

    /* ==================== getter / setter ==================== */

    public TaskGoal getTask() {
        return task;
    }

    public String getModel() {
        return model;
    }

    public void setApiModel(String apiModel) {
        this.apiModel = apiModel;
    }

    public String getApiModel() {
        return apiModel;
    }

    public String getRunId() {
        return runId;
    }

    public List<RoundTrace> getRounds() {
        return rounds;
    }

    public void setRequestId(int requestId) {
        this.requestId = requestId;
    }

    public void setReplyMarkdown(String replyMarkdown) {
        this.replyMarkdown = replyMarkdown == null ? "" : replyMarkdown;
    }

    public String getReplyMarkdown() {
        return replyMarkdown;
    }

    public void setAgentError(boolean agentError, String message) {
        this.agentError = agentError;
        this.agentErrorMessage = message;
    }

    public boolean isAgentError() {
        return agentError;
    }

    public String getAgentErrorMessage() {
        return agentErrorMessage;
    }

    public void setSnapshotBefore(SceneSnapshot s) {
        this.snapshotBefore = s;
    }

    public void setSnapshotAfter(SceneSnapshot s) {
        this.snapshotAfter = s;
    }

    public SceneSnapshot getSnapshotAfter() {
        return snapshotAfter;
    }

    public void setVerdict(Verdict verdict, String comment, String scorer) {
        this.verdict = verdict;
        this.comment = comment == null ? "" : comment;
        this.scorer = scorer;
    }

    public void setAutoCheck(Boolean passed, String detail) {
        this.autoCheckPassed = passed;
        this.autoCheckDetail = detail;
    }

    public Boolean getAutoCheckPassed() {
        return autoCheckPassed;
    }

    public String getAutoCheckDetail() {
        return autoCheckDetail;
    }

    public Verdict getVerdict() {
        return verdict;
    }

    public String getComment() {
        return comment;
    }

    public String getScorer() {
        return scorer;
    }

    public long getElapsedMs() {
        return elapsedMs;
    }
}
