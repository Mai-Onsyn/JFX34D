package HCloudbyte.ui;

import javafx.geometry.Pos;
import javafx.scene.layout.StackPane;
import mai_onsyn.renderer.core.GL4DRegion;
import mai_onsyn.renderer.interfaces.RendererInterface;

/**
 * 中央视口：直接承载 {@link GL4DRegion}（OpenGL 渲染区域，自带 4D 键盘操作）。
 * 对应 Kotlin 侧 MainApp.kt 的 start4DTest2 用法，视口区域保持干净，不加遮挡浮层。
 *
 * <p>【键盘操作（由 GL4DRegion 内置）】
 * <ul>
 *   <li>点击视口获得焦点；按 I 启用 / 禁用 4D 键盘输入</li>
 *   <li>W/S 前后、A/D 左右、空格/Shift 上下、E/Q w±（4D 的 ana / negAna）</li>
 * </ul>
 *
 * <p>【接口绑定】MainApp 里 {@code RendererInterface.init(region.getScene4D().getCamera())}
 * 已把 RendererInterface 绑定到本视口的摄像机（UI 的 getCamera() == region 的 scene4D.camera4D）。
 */
public class ViewportPanel extends StackPane {

    public ViewportPanel(RendererInterface renderer, GL4DRegion viewport) {
        setStyle(UiTheme.GLASS);
        StackPane.setAlignment(viewport, Pos.CENTER);
        getChildren().add(viewport);
    }
}
