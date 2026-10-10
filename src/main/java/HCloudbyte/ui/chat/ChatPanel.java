package HCloudbyte.ui.chat;

import HCloudbyte.ui.scenetree.SceneTree;
import HCloudbyte.ui.theme.Theme;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import weilantianhai.agent.interfaces.AgentInterface;
import weilantianhai.agent.interfaces.ResponseListener;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 右侧聊天视图（语音建模模式）。
 *
 * <p>已接入 agent 侧 {@link AgentInterface}（队列 + worker 线程架构）：
 * {@code submitUserInput(text)} 提交自然语言指令 → worker 线程 LLM → IR JSON →
 * CommandExecutor 执行到 RendererInterface（黑色 GL 视口响应），
 * 结果经 {@code setResponseListener} 的 onResponse / onError 回到 UI 线程更新气泡。
 */
public class ChatPanel extends VBox {

    private static final String STYLE_SENDING =
            "-fx-background-color: rgba(58,62,67,0.95); -fx-background-radius: 14;"
                    + " -fx-text-fill: #c4c9cf; -fx-font-size: 12px;";
    private static final String STYLE_SUCCESS =
            "-fx-background-color: rgba(127,208,138,0.14); -fx-background-radius: 14;"
                    + " -fx-text-fill: " + Theme.SUCCESS + "; -fx-font-size: 12px;";
    private static final String STYLE_ERROR =
            "-fx-background-color: rgba(224,108,117,0.14); -fx-background-radius: 14;"
                    + " -fx-text-fill: " + Theme.ERROR + "; -fx-font-size: 12px;";

    private static final String[] SPINNER = {"⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏"};

    private final VBox chatMessages;
    private final ScrollPane chatScroll;
    private final TextArea input;
    private final Button sendBtn;
    /** 场景树：AI 指令执行成功后重建，让新建/删除的模型立刻出现在树上。 */
    private final SceneTree outliner;
    /** requestId → 反馈气泡，多请求并发时各气泡独立更新。 */
    private final Map<Integer, Label> pendingFeedback = new ConcurrentHashMap<>();
    private final Label statusLabel = new Label("Agent 未就绪");
    private final Label spinnerLabel = new Label("");

    public ChatPanel(SceneTree outliner) {
        super(12);
        this.outliner = outliner;
        setPadding(new Insets(20));
        setStyle(Theme.GLASS);

        Label title = Theme.titleLabel("语音建模");

        chatMessages = new VBox(10);
        chatMessages.setPadding(new Insets(4, 2, 4, 2));

        chatScroll = new ScrollPane(chatMessages);
        chatScroll.setFitToWidth(true);
        chatScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        chatScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(chatScroll, Priority.ALWAYS);

        // 多行输入框：内容超一行自动增高（最多 4 行），Enter 发送 / Shift+Enter 换行
        input = new TextArea();
        input.setPromptText("帮我用…建模");
        input.setWrapText(true);
        input.setPrefRowCount(1);
        input.setStyle("-fx-background-radius: 18; -fx-background-insets: 0; -fx-border-color: transparent;"
                + " -fx-padding: 6 14; -fx-control-inner-background: #ffffff;");
        HBox.setHgrow(input, Priority.ALWAYS);

        sendBtn = new Button("↑");
        sendBtn.getStyleClass().add("accent");
        sendBtn.setStyle("-fx-background-radius: 18; -fx-font-size: 14px; -fx-padding: 6 12;");
        sendBtn.setCursor(Cursor.HAND);

        Runnable sendAction = () -> {
            String text = input.getText().trim();
            if (text.isEmpty()) return;
            input.clear();
            handleUserMessage(text);
        };
        sendBtn.setOnAction(e -> sendAction.run());

        // 自动增高：按换行数调整行数（1~4 行）
        input.textProperty().addListener((o, a, b) -> {
            int lines = 1;
            for (int i = 0; i < b.length(); i++) {
                if (b.charAt(i) == '\n') lines++;
            }
            input.setPrefRowCount(Math.max(1, Math.min(4, lines)));
        });
        // Enter 发送（Shift+Enter 换行）
        input.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ENTER && !e.isShiftDown()) {
                e.consume();
                sendAction.run();
            }
        });

        HBox inputBar = new HBox(8, input, sendBtn);
        inputBar.setAlignment(Pos.CENTER_LEFT);

        registerResponseListener();

        // 状态行 + 转圈动画：每 150ms 刷新状态摘要，isBusy 时显示旋转字符
        spinnerLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: " + Theme.ACCENT + ";");
        spinnerLabel.setPrefWidth(18);
        statusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + Theme.TEXT_MUTED + ";");
        HBox statusRow = new HBox(8, spinnerLabel, statusLabel);
        statusRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(statusRow, Priority.ALWAYS);
        startStatusPolling();

        // 布局：场景集合（模型树）在最上，语音建模对话区在下
        getChildren().addAll(
                outliner,
                new Separator(),
                title,
                statusRow,
                new Separator(),
                chatScroll,
                inputBar);
    }

    /** 是否允许发送消息（Agent 初始化失败时禁用输入框与发送按钮）。 */
    public void setInputEnabled(boolean enabled) {
        input.setDisable(!enabled);
        sendBtn.setDisable(!enabled);
    }

    /** 注册 agent 结果监听：worker 线程处理完成后回调 onResponse / onError。 */
    private void registerResponseListener() {
        // Agent 未初始化（initialize 失败）时 getInstance 抛 Error，必须捕获；
        // 此时输入框已由 setInputEnabled(false) 禁用，用户不会实际触发提交。
        try {
            AgentInterface.getInstance().setResponseListener(new ResponseListener() {
                @Override
                public void onResponse(int requestId, String responseText) {
                    Platform.runLater(() -> {
                        Label fb = pendingFeedback.remove(requestId);
                        if (fb == null) return;
                        fb.setText(responseText);
                        fb.setStyle(STYLE_SUCCESS);
                        scrollToBottom();
                        // 指令执行成功 → 场景可能变了（建模/删除），重建场景树
                        outliner.refresh();
                    });
                }

                @Override
                public void onError(int requestId, String errorMessage) {
                    Platform.runLater(() -> {
                        Label fb = pendingFeedback.remove(requestId);
                        if (fb == null) return;
                        fb.setText("指令执行失败：" + errorMessage);
                        fb.setStyle(STYLE_ERROR);
                        scrollToBottom();
                    });
                }
            });
        } catch (Throwable ignored) {
            // Agent 未初始化：静默等待上层弹窗提示
        }
    }

    /** 轮询 Agent 状态：转圈 + 状态摘要（isBusy / getStatusSummary，全线程安全）。 */
    private void startStatusPolling() {
        int[] spinIdx = {0};
        Timeline timeline = new Timeline(new KeyFrame(Duration.millis(150), e -> {
            try {
                AgentInterface agent = AgentInterface.getInstance();
                if (agent.isBusy()) {
                    spinnerLabel.setText(SPINNER[spinIdx[0]]);
                    spinIdx[0] = (spinIdx[0] + 1) % SPINNER.length;
                    statusLabel.setText("AI 正在操作… " + agent.getStatusSummary());
                } else {
                    spinnerLabel.setText("");
                    statusLabel.setText(agent.getStatusSummary());
                }
            } catch (Throwable ignored) {
                spinnerLabel.setText("");
                statusLabel.setText("Agent 未就绪");
            }
        }));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }

    /** 发送一条用户消息给 Agent：提交到队列，worker 线程 LLM 翻译 → IR 执行到黑色 GL 视口。 */
    private void handleUserMessage(String text) {
        chatMessages.getChildren().add(buildUserBubble(text));

        int reqId;
        try {
            reqId = AgentInterface.getInstance().submitUserInput(text);
        } catch (Throwable t) {
            // Agent 未初始化 / 其他异常（getInstance 抛 Error，故捕 Throwable）
            Label fb = bubble("指令执行失败：" + t.getMessage(), STYLE_ERROR);
            HBox row = new HBox(fb);
            row.setAlignment(Pos.CENTER_LEFT);
            chatMessages.getChildren().add(row);
            scrollToBottom();
            return;
        }

        Label feedback = bubble("已发送给 Agent，正在执行…", STYLE_SENDING);
        HBox aiRow = new HBox(6, feedback, buildCancelButton(reqId, feedback));
        aiRow.setAlignment(Pos.CENTER_LEFT);
        chatMessages.getChildren().add(aiRow);
        pendingFeedback.put(reqId, feedback);
        scrollToBottom();
    }

    /** 单条气泡的取消按钮：只能取消还在排队中的请求（已开始执行的返回 false）。 */
    private Button buildCancelButton(int requestId, Label feedback) {
        Button cancel = new Button("✕");
        cancel.getStyleClass().add("accent");
        cancel.setStyle("-fx-padding: 2 6; -fx-background-radius: 10; -fx-font-size: 10px;");
        cancel.setCursor(Cursor.HAND);
        cancel.setOnAction(e -> {
            try {
                boolean ok = AgentInterface.getInstance().cancelRequest(requestId);
                if (ok) {
                    feedback.setText("已取消");
                    feedback.setStyle(STYLE_ERROR);
                    pendingFeedback.remove(requestId);
                } else {
                    feedback.setText("正在执行，无法取消");
                }
                scrollToBottom();
            } catch (Throwable ignored) {
                feedback.setText("取消失败：Agent 未就绪");
            }
        });
        return cancel;
    }

    private void scrollToBottom() {
        Platform.runLater(() -> chatScroll.setVvalue(1.0));
    }

    /** 用户消息气泡（橙色底，Blender 强调色）。 */
    private HBox buildUserBubble(String msg) {
        Label label = new Label(msg);
        label.setWrapText(true);
        label.setMaxWidth(220);
        label.setPadding(new Insets(8, 12, 8, 12));
        label.setStyle("-fx-background-color: rgba(255,138,26,0.9); -fx-background-radius: 14;"
                + " -fx-text-fill: white; -fx-font-size: 12px;");
        HBox row = new HBox(label);
        row.setAlignment(Pos.CENTER_RIGHT);
        return row;
    }

    /** 通用气泡样式（左对齐用）。 */
    private static Label bubble(String text, String style) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setMaxWidth(220);
        label.setPadding(new Insets(8, 12, 8, 12));
        label.setStyle(style);
        return label;
    }
}
