package HCloudbyte.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import weilantianhai.agent.interfaces.AgentInterface;

import java.util.concurrent.CompletableFuture;

/**
 * 右侧聊天视图（语音建模模式）。
 * 已接入 agent 侧 {@link AgentInterface}：发送消息 → LLM 翻译成 IR JSON → CommandExecutor 执行到
 * RendererInterface（黑色 GL 视口响应）。sendToLLM 为 void 且由 agent 内部吞掉异常，UI 只负责触发与状态提示。
 */
public class ChatPanel extends VBox {

    /** 发送中气泡样式。 */
    private static final String STYLE_SENDING =
            "-fx-background-color: rgba(58,62,67,0.95);" +
                    "-fx-background-radius: 14;" +
                    "-fx-text-fill: #c4c9cf;" +
                    "-fx-font-size: 12px;";
    /** 执行成功气泡样式。 */
    private static final String STYLE_SUCCESS =
            "-fx-background-color: rgba(127,208,138,0.14);" +
                    "-fx-background-radius: 14;" +
                    "-fx-text-fill: #7fd08a;" +
                    "-fx-font-size: 12px;";
    /** 执行失败气泡样式。 */
    private static final String STYLE_ERROR =
            "-fx-background-color: rgba(224,108,117,0.14);" +
                    "-fx-background-radius: 14;" +
                    "-fx-text-fill: #e06c75;" +
                    "-fx-font-size: 12px;";

    private final VBox chatMessages;
    private final ScrollPane chatScroll;
    private final TextField input;
    private final Button sendBtn;

    public ChatPanel(SceneOutliner outliner) {
        super(12);
        setPadding(new Insets(20));
        setStyle(UiTheme.GLASS);

        Label title = new Label("语音建模");
        title.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #e8eaed;");

        chatMessages = new VBox(10);
        chatMessages.setPadding(new Insets(4, 2, 4, 2));

        chatScroll = new ScrollPane(chatMessages);
        chatScroll.setFitToWidth(true);
        chatScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        chatScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(chatScroll, Priority.ALWAYS);

        input = new TextField();
        input.setPromptText("帮我用…建模");
        input.setStyle("-fx-background-radius: 18; -fx-padding: 8 14;");
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
        input.setOnAction(e -> sendAction.run());

        HBox inputBar = new HBox(8, input, sendBtn);
        inputBar.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(
                title,
                outliner,
                new Separator(),
                chatScroll,
                inputBar
        );
    }

    /** 是否允许发送消息（Agent 初始化失败时禁用输入框与发送按钮）。 */
    public void setInputEnabled(boolean enabled) {
        input.setDisable(!enabled);
        sendBtn.setDisable(!enabled);
    }

    /** 发送一条用户消息给 Agent：LLM 翻译 → IR 执行到黑色 GL 视口。 */
    private void handleUserMessage(String text) {
        chatMessages.getChildren().add(buildUserBubble(text));

        Label feedback = new Label("已发送给 Agent，正在执行…");
        feedback.setWrapText(true);
        feedback.setMaxWidth(220);
        feedback.setPadding(new Insets(8, 12, 8, 12));
        feedback.setStyle(STYLE_SENDING);
        HBox aiRow = new HBox(feedback);
        aiRow.setAlignment(Pos.CENTER_LEFT);
        chatMessages.getChildren().add(aiRow);
        scrollToBottom();

        // 接口调用：AgentInterface#sendToLLM —— 自然语言 → LLM → IR JSON → CommandExecutor 执行到 renderer（黑色视口响应）。
        // sendToLLM 是阻塞调用（LLM 网络请求 + 操作间延迟），必须放后台线程；完成后回 UI 线程更新状态。
        CompletableFuture.runAsync(() -> {
            try {
                AgentInterface.getInstance().submitUserInput(text);
                Platform.runLater(() -> {
                    feedback.setText("指令已执行，观察左侧视口变化。");
                    feedback.setStyle(STYLE_SUCCESS);
                    scrollToBottom();
                });
            } catch (Throwable t) {
                // AgentInterface 未初始化 / 网络失败等情况（getInstance 抛 Error，故捕 Throwable）
                Platform.runLater(() -> {
                    feedback.setText("指令执行失败：" + t.getMessage());
                    feedback.setStyle(STYLE_ERROR);
                    scrollToBottom();
                });
            }
        });
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
        label.setStyle(
                "-fx-background-color: rgba(255,138,26,0.9);" +
                        "-fx-background-radius: 14;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 12px;"
        );
        HBox row = new HBox(label);
        row.setAlignment(Pos.CENTER_RIGHT);
        return row;
    }
}
