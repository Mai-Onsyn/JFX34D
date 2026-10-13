package weilantianhai.dataset.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import weilantianhai.dataset.core.CheckEvaluator;
import weilantianhai.dataset.core.TaskRunRecord;
import weilantianhai.dataset.tasks.TaskGoal;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * 打分组件：浮在 4D 窗口右上角，展示当前任务、AI 干了什么、自动判据结果，
 * 并让打分人给出「正确 / 不正确 + 评语」。
 *
 * <p>阻塞语义：{@link #awaitScore} 会在调用线程上等待，直到打分人点「提交并继续」或「跳过」。
 * 因此它必须被非 FX 线程调用（脚本主循环线程）。
 */
public final class ScorePanel extends StackPane {

    private static final String PANEL_STYLE =
            "-fx-background-color: rgba(20,22,26,0.94);"
                    + "-fx-background-radius: 12;"
                    + "-fx-border-color: rgba(140,146,154,0.45);"
                    + "-fx-border-radius: 12;"
                    + "-fx-border-width: 1;"
                    + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 20, 0.2, 0, 8);";

    private static final String BTN =
            "-fx-padding: 6 16; -fx-background-radius: 6; -fx-text-fill: #ffffff;"
                    + "-fx-font-size: 12px; -fx-cursor: hand;";
    private static final String BTN_PRIMARY = "-fx-background-color: rgba(255,138,26,0.92);" + BTN;
    private static final String BTN_OK = "-fx-background-color: rgba(78,168,96,0.92);" + BTN;
    private static final String BTN_BAD = "-fx-background-color: rgba(190,80,90,0.92);" + BTN;
    private static final String BTN_GHOST =
            "-fx-padding: 6 14; -fx-background-radius: 6; -fx-background-color: rgba(90,95,102,0.75);"
                    + "-fx-text-fill: #e2e5e8; -fx-font-size: 12px; -fx-cursor: hand;";

    private final Label taskLabel = new Label();
    private final Label metaLabel = new Label();
    private final Label autoLabel = new Label();
    private final TextArea transcript = new TextArea();
    private final TextArea comment = new TextArea();
    private final RadioButton correctBtn = new RadioButton("正确");
    private final RadioButton incorrectBtn = new RadioButton("不正确");
    private final Button submitBtn;
    private final Button skipBtn;
    private final Button abortBtn;

    private CountDownLatch latch;
    private final AtomicReference<TaskRunRecord.Verdict> pendingVerdict = new AtomicReference<>();

    /** 终止请求回调（脚本读到它后停止循环）。 */
    private volatile boolean abortRequested = false;

    public ScorePanel(Consumer<Integer> onViewTrace) {
        setStyle(PANEL_STYLE);
        setPadding(new Insets(12, 14, 14, 14));
        setMaxWidth(420);
        setMaxHeight(Region.USE_PREF_SIZE);
        setVisible(false);
        setManaged(false);

        Label title = new Label("数据集打分");
        title.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #ffb36b;");

        taskLabel.setWrapText(true);
        taskLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #e8eaed;");

        metaLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #9aa0a6;");

        autoLabel.setWrapText(true);
        autoLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #8ab4f8;");

        transcript.setEditable(false);
        transcript.setWrapText(true);
        transcript.setPrefRowCount(8);
        transcript.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 11px;"
                + "-fx-control-inner-background: rgba(30,33,38,0.9); -fx-text-fill: #c7ccd1;");

        comment.setPromptText("评语（例如：机位名对但朝向不一致）");
        comment.setWrapText(true);
        comment.setPrefRowCount(3);
        comment.setStyle("-fx-font-size: 12px; -fx-control-inner-background: rgba(30,33,38,0.9);");

        ToggleGroup group = new ToggleGroup();
        correctBtn.setToggleGroup(group);
        incorrectBtn.setToggleGroup(group);
        correctBtn.setStyle("-fx-text-fill: #7fd08a; -fx-font-size: 12px;");
        incorrectBtn.setStyle("-fx-text-fill: #e06c75; -fx-font-size: 12px;");

        submitBtn = new Button("提交并继续");
        submitBtn.setStyle(BTN_PRIMARY);
        submitBtn.setOnAction(e -> finish());

        skipBtn = new Button("跳过");
        skipBtn.setStyle(BTN_GHOST);
        skipBtn.setOnAction(e -> {
            pendingVerdict.set(TaskRunRecord.Verdict.SKIP);
            latch.countDown();
        });

        abortBtn = new Button("终止");
        abortBtn.setStyle(BTN_BAD);
        abortBtn.setOnAction(e -> {
            abortRequested = true;
            pendingVerdict.set(TaskRunRecord.Verdict.SKIP);
            latch.countDown();
        });

        Button viewTrace = new Button("完整 trace");
        viewTrace.setStyle(BTN_GHOST);
        viewTrace.setOnAction(e -> onViewTrace.accept(currentTaskId));

        HBox verdictRow = new HBox(14, correctBtn, incorrectBtn);
        verdictRow.setAlignment(Pos.CENTER_LEFT);

        HBox actionRow = new HBox(8, submitBtn, skipBtn, abortBtn, viewTrace);
        actionRow.setAlignment(Pos.CENTER_LEFT);

        VBox box = new VBox(8, title, taskLabel, metaLabel, autoLabel, transcript, verdictRow, comment, actionRow);
        getChildren().add(box);
        StackPane.setAlignment(box, Pos.TOP_RIGHT);
    }

    private int currentTaskId;

    /**
     * 展示一条任务并阻塞等待打分。
     *
     * @param task       任务定义
     * @param record     已执行完的记录（含 trace）
     * @param autoReport 自动判据报告，可为 null
     * @return 打分结论；调用方负责把 verdict/comment 写回 record
     */
    public Score awaitScore(TaskGoal task, TaskRunRecord record,
                            CheckEvaluator.Report autoReport) {
        this.currentTaskId = task.id();
        this.latch = new CountDownLatch(1);
        this.pendingVerdict.set(null);

        Platform.runLater(() -> {
            taskLabel.setText("[" + task.id() + "] " + task.prompt());
            metaLabel.setText(String.format("类别 %s ｜ 轮数 %d ｜ 耗时 %.1fs ｜ 模型 %s",
                    task.category(), record.roundCount(), record.getElapsedMs() / 1000.0, record.getModel()));

            if (autoReport == null || autoReport.results().isEmpty()) {
                autoLabel.setText("自动判据：无（请人工判断）");
                correctBtn.setSelected(false);
                incorrectBtn.setSelected(false);
            } else {
                // 陷阱题是"反着判"的：执行报错才算对。这里显式标出来，
                // 免得打分人看到「reject 通过」时以为是写错了。
                String prefix = task.expectsRejection() ? "【陷阱题：应当拒绝，拒绝=正确】\n" : "";
                String head = autoReport.allPassed() ? "自动判据：全部通过 ✓"
                        : (autoReport.hasUnknown() ? "自动判据：部分无法判定" : "自动判据：存在不通过 ✗");
                autoLabel.setText(prefix + head + "\n" + autoReport.describe());
                // 预填：自动判定只做建议，人可改
                correctBtn.setSelected(autoReport.allPassed());
                incorrectBtn.setSelected(!autoReport.allPassed());
            }

            transcript.setText(buildTranscript(record));
            comment.clear();
            setVisible(true);
            setManaged(true);
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Platform.runLater(() -> {
            setVisible(false);
            setManaged(false);
        });

        TaskRunRecord.Verdict v = pendingVerdict.get();
        if (v == null) {
            v = correctBtn.isSelected()
                    ? TaskRunRecord.Verdict.CORRECT
                    : TaskRunRecord.Verdict.INCORRECT;
        }
        String c = comment.getText() == null ? "" : comment.getText().strip();
        // comment 读取必须在 FX 线程之外已经拿到字符串快照
        final String commentSnapshot = c;
        final TaskRunRecord.Verdict verdictSnapshot = v;
        return new Score(verdictSnapshot, commentSnapshot);
    }

    private void finish() {
        if (!correctBtn.isSelected() && !incorrectBtn.isSelected()) {
            // 未选就默认不正确，避免卡住
            incorrectBtn.setSelected(true);
        }
        pendingVerdict.set(correctBtn.isSelected()
                ? TaskRunRecord.Verdict.CORRECT
                : TaskRunRecord.Verdict.INCORRECT);
        latch.countDown();
    }

    /** 从 record 拼一个可读的执行摘要。 */
    private static String buildTranscript(TaskRunRecord record) {
        StringBuilder sb = new StringBuilder();
        sb.append("AI 回复：\n").append(record.getReplyMarkdown()).append("\n\n");
        sb.append("逐轮操作（").append(record.roundCount()).append(" 轮）：\n");
        record.getRounds().forEach(r -> {
            sb.append("  R").append(r.getRound())
              .append(r.isParseOk() ? " 解析✓" : " 解析✗(" + r.getParseErrorCode() + ")")
              .append(" types=").append(r.getOpTypes())
              .append(" ").append(r.getElapsedMs()).append("ms\n");
            if (!r.getTypeErrors().isEmpty()) sb.append("     类型错: ").append(r.getTypeErrors()).append('\n');
            if (!r.getExecErrors().isEmpty()) sb.append("     执行错: ").append(r.getExecErrors()).append('\n');
        });
        return sb.toString();
    }

    /** 打分结果。 */
    public record Score(TaskRunRecord.Verdict verdict, String comment) {
    }

    public boolean isAbortRequested() {
        return abortRequested;
    }
}
