package HCloudbyte.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.StackPane;
import mai_onsyn.renderer.core.GL4DRegion;
import mai_onsyn.renderer.interfaces.RendererInterface;

/**
 * 中央视口：直接承载 {@link GL4DRegion}（OpenGL 渲染区域，自带 4D 键盘操作）。
 * 左上角叠加悬浮窗（ViewportOverlay：相机机位管理），其余区域保持干净。
 *
 * <p>【键盘操作（由 GL4DRegion 内置）】
 * <ul>
 *   <li>点击视口获得焦点；按 I 启用 / 禁用 4D 键盘输入</li>
 *   <li>W/S 前后、A/D 左右、空格/Shift 上下、E/Q w±（4D 的 ana / negAna）</li>
 * </ul>
 *
 * <p>【接口绑定】MainApp 里 {@code RendererInterface.init(region)}
 * 已把 RendererInterface 绑定到 GL4DRegion（UI 的 getCamera() == region 的 scene4D.camera4D）。
 */
public class ViewportPanel extends StackPane {

    private final ViewportOverlay overlay;

    public ViewportPanel(GL4DRegion viewport, RendererInterface renderer) {
        // 中央视口：去圆角（GLASS 基础上覆盖 radius=0），其余面板圆角不变
        setStyle(UiTheme.GLASS + "-fx-background-radius: 0; -fx-border-radius: 0;");
        StackPane.setAlignment(viewport, Pos.CENTER);
        getChildren().add(viewport);

        // 左上角悬浮窗：相机机位管理
        overlay = new ViewportOverlay(renderer);
        StackPane.setAlignment(overlay, Pos.TOP_LEFT);
        StackPane.setMargin(overlay, new Insets(8));
        getChildren().add(overlay);
    }

    /** 供 UIInterface 初始化的机位悬浮窗引用。 */
    public ViewportOverlay getOverlay() {
        return overlay;
    }
}
