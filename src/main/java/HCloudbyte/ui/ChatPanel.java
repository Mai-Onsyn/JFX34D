package HCloudbyte.ui;

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
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * 右侧聊天视图（语音建模模式）。
 * 当前为模拟对话：用 Timeline 假扮 AI 思考过程，尚未接入 AgentInterface。
 *
 * <p>【接口接线点】（AI Api Interface document v2.md §2）
 * <ul>
 *   <li>发送消息 → {@link AgentService#send(String, String)}（userMessage + contextMarkdown），
 *       返回 {@code CompletableFuture<AgentService.ChatTurn>}</li>
 *   <li>contextMarkdown（场景上下文）由场景快照拼装：
 *       {@code ModelInterface#listModel()}（§4.1.1）+ {@code GeometryInterface#getModelInfos(name)}（§6.1）</li>
 *   <li>执行轨迹 steps → {@code AgentService.ChatTurn#steps()}（逐条打勾展示）</li>
 *   <li>最终回复 → {@code AgentService.ChatTurn#reply()}；结果 markdown → {@code #resultMarkdown()}</li>
 *   <li>AI 返回的 operations 落点：RendererInterface 六个子接口（camera / model / transform / shape / geometry / io）</li>
 * </ul>
 */
public class ChatPanel extends VBox {

    private final VBox chatMessages;
    private final ScrollPane chatScroll;

    public ChatPanel(SceneOutliner outliner) {
        super(12);
        setPadding(new Insets(20));
        setStyle(UiTheme.GLASS);

        Label title = new Label("语音建模");
        title.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #333;");

        chatMessages = new VBox(10);
        chatMessages.setPadding(new Insets(4, 2, 4, 2));

        chatScroll = new ScrollPane(chatMessages);
        chatScroll.setFitToWidth(true);
        chatScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        chatScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(chatScroll, Priority.ALWAYS);

        TextField input = new TextField();
        input.setPromptText("帮我用…建模");
        input.setStyle("-fx-background-radius: 18; -fx-padding: 8 14;");
        HBox.setHgrow(input, Priority.ALWAYS);

        Button sendBtn = new Button("↑");
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

    /** 发送一条用户消息并播放 AI 模拟回复动画。 */
    private void handleUserMessage(String text) {
        // 【接口接线】真实实现时替换下方 Timeline 模拟：
        //   1) 拼装 contextMarkdown：renderer.model().listModel() + renderer.geometry().getModelInfos(...)
        //   2) agentService.send(text, contextMarkdown).thenAccept(turn -> {
        //        // 用 turn.steps() 逐条打勾、turn.reply() 更新气泡、turn.resultMarkdown() 可调试
        //      });
        chatMessages.getChildren().add(buildBubble("user", text));

        VBox aiBubble = new VBox(6);
        aiBubble.setPadding(new Insets(8, 12, 8, 12));
        aiBubble.setMaxWidth(220);
        aiBubble.setStyle(
                "-fx-background-color: rgba(255,255,255,0.95);" +
                        "-fx-background-radius: 14;"
        );

        Label thinking = new Label("正在思考中…");
        thinking.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");
        aiBubble.getChildren().add(thinking);

        HBox aiRow = new HBox(aiBubble);
        aiRow.setAlignment(Pos.CENTER_LEFT);
        chatMessages.getChildren().add(aiRow);

        scrollToBottom();

        String[] steps = {
                "理解需求",
                "检索四维模板",
                "生成操作指令",
                "执行建模"
        };
        // 接口接线：steps 对应 AgentService.ChatTurn#steps() 的执行轨迹

        Timeline timeline = new Timeline();
        KeyFrame replaceFrame = new KeyFrame(Duration.millis(600), e -> {
            aiBubble.getChildren().clear();
            VBox stepsBox = new VBox(6);
            for (int i = 0; i < steps.length; i++) {
                Label stepLabel = new Label("○ " + steps[i]);
                stepLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #999;");
                stepsBox.getChildren().add(stepLabel);
            }
            aiBubble.getChildren().add(stepsBox);

            Timeline stepTimeline = new Timeline();
            for (int i = 0; i < steps.length; i++) {
                final int idx = i;
                KeyFrame kf = new KeyFrame(
                        Duration.millis(700 + idx * 500),
                        ev -> {
                            Label stepLabel = (Label) stepsBox.getChildren().get(idx);
                            stepLabel.setText("✓ " + steps[idx]);
                            stepLabel.setStyle(
                                    "-fx-font-size: 12px;" +
                                            "-fx-text-fill: #28a745;" +
                                            "-fx-font-weight: bold;"
                            );
                            scrollToBottom();
                        }
                );
                stepTimeline.getKeyFrames().add(kf);
            }

            KeyFrame finalFrame = new KeyFrame(
                    Duration.millis(700 + steps.length * 500 + 300),
                    ev -> {
                        Label result = new Label("已创建 Tesseract 001，共 16 个顶点、8 个胞。");
                        // 接口接线：最终回复对应 AgentService.ChatTurn#reply()（数量来自 GeometryInterface#getModelInfos）
                        result.setWrapText(true);
                        result.setMaxWidth(200);
                        result.setPadding(new Insets(6, 0, 0, 0));
                        result.setStyle("-fx-font-size: 12px; -fx-text-fill: #222;");
                        aiBubble.getChildren().add(result);
                        scrollToBottom();
                    }
            );
            stepTimeline.getKeyFrames().add(finalFrame);
            stepTimeline.play();
        });
        timeline.getKeyFrames().add(replaceFrame);
        timeline.play();
    }

    private void scrollToBottom() {
        Platform.runLater(() -> chatScroll.setVvalue(1.0));
    }

    private HBox buildBubble(String role, String msg) {
        boolean isUser = "user".equals(role);

        Label label = new Label(msg);
        label.setWrapText(true);
        label.setMaxWidth(220);
        label.setPadding(new Insets(8, 12, 8, 12));
        label.setStyle(
                "-fx-background-color: " + (isUser ? "rgba(74,144,226,0.9)" : "rgba(255,255,255,0.95)") + ";" +
                        "-fx-background-radius: 14;" +
                        "-fx-text-fill: " + (isUser ? "white" : "#222") + ";" +
                        "-fx-font-size: 12px;"
        );

        HBox row = new HBox(label);
        row.setAlignment(isUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        return row;
    }
}