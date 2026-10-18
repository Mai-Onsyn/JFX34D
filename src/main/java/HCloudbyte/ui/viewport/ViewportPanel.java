package HCloudbyte.ui.viewport;

import HCloudbyte.ui.theme.Theme;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.StackPane;
import mai_onsyn.renderer.core.GL4DRegion;
import mai_onsyn.renderer.interfaces.RendererInterface;

public class ViewportPanel extends StackPane {
    private final CameraBookmarksView overlay;

    public ViewportPanel(GL4DRegion viewport, RendererInterface renderer) {
        // 中央视口：去圆角（在 GLASS 基础上覆盖 radius=0），其余面板圆角不变
        setStyle(Theme.GLASS + "-fx-background-radius: 0; -fx-border-radius: 0;");
        StackPane.setAlignment(viewport, Pos.CENTER);
        getChildren().add(viewport);

        overlay = new CameraBookmarksView(renderer);
        StackPane.setAlignment(overlay, Pos.TOP_LEFT);
        StackPane.setMargin(overlay, new Insets(8));
        getChildren().add(overlay);
    }

    /** 供 UIInterface 初始化的机位悬浮窗引用。 */
    public CameraBookmarksView getOverlay() {
        return overlay;
    }
}
