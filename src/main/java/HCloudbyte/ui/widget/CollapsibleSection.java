package HCloudbyte.ui.widget;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.TitledPane;

/**
 * 可折叠区块：背景与面板底色统一的 TitledPane。
 *
 * <p>做三件事：默认整块透明（透出面板背景）、悬停微亮、展开保持透明。
 * 标题栏 / 内容区 / 箭头也全部压成透明，否则 TitledPane 默认底会把面板玻璃底盖掉。
 */
public final class CollapsibleSection extends TitledPane {

    private static final String BASE =
            "-fx-background-color: transparent; -fx-border-color: transparent; -fx-border-width: 0;"
                    + "-fx-background-radius: 6; -fx-padding: 0; -fx-text-fill: #e8e8e8;";
    private static final String HOVER =
            "-fx-background-color: rgba(255,255,255,0.05); -fx-border-color: transparent; -fx-border-width: 0;"
                    + "-fx-background-radius: 6; -fx-padding: 0; -fx-text-fill: #e8e8e8;";

    public CollapsibleSection(String title, Node content) {
        super(title, content);
        setAnimated(false);
        setExpanded(false);
        setStyle(BASE);

        styleInnerParts();
        // 挂到场景后再补一次：未入场景时 applyCss/lookup 可能失败，内容区会残留默认底色
        Platform.runLater(this::styleInnerParts);

        setOnMouseEntered(e -> {
            if (!isExpanded()) setStyle(HOVER);
        });
        setOnMouseExited(e -> {
            if (!isExpanded()) setStyle(BASE);
        });
        expandedProperty().addListener((obs, was, now) -> setStyle(BASE));
    }

    /** 标题栏 / 内容区 / 展开箭头统一透明。 */
    private void styleInnerParts() {
        applyCss();
        layout();
        Node bar = lookup(".title");
        if (bar != null) bar.setStyle("-fx-background-color: transparent; -fx-padding: 7 10 7 10;");
        Node body = lookup(".content");
        if (body != null) body.setStyle("-fx-background-color: transparent; -fx-padding: 4 0 0 0;"
                + "-fx-border-color: transparent; -fx-border-width: 0;");
        Node arrow = lookup(".arrow");
        if (arrow != null) arrow.setStyle("-fx-background-color: #c7ccd1;");
    }
}
