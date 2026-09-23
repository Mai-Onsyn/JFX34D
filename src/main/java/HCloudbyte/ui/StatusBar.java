package HCloudbyte.ui;

import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import mai_onsyn.renderer.interfaces.RendererInterface;
import org.joml.Vector4f;

/**
 * 底部状态栏：FPS、模型统计、速度滑块、摄像机坐标。
 *
 * <p>【接口调用】（AI Api Interface document v2.md §0.1，直接调用 Kotlin 侧已定义接口）
 * <ul>
 *   <li>CAM 坐标 → {@code RendererInterface.getCamera().getPosition()}（§3.1.1 GET_CAMERA_POS）：AnimationTimer 每帧刷新 ✅</li>
 *   <li>FPS / 顶点统计 → GL3DRegion 回调 + {@code GeometryInterface#getModelInfos(name)}（§6.1，后端 TODO，暂不接）</li>
 *   <li>速度滑块 → 渲染播放速度（RendererInterface 无对应操作）</li>
 * </ul>
 */
public class StatusBar extends HBox {

    private final RendererInterface renderer;
    private final Label cam;

    public StatusBar(RendererInterface renderer) {
        super(20);
        this.renderer = renderer;

        Label fps = new Label("FPS 60");
        // 接口接线：FPS ← GL3DRegion 帧率回调（暂为静态占位）
        Label stats = new Label("顶点 9.6K · 四面体 2.4K · 胞 8");
        // 接口接线：模型统计 ← GeometryInterface#getModelInfos(name)（后端 TODO，暂不接）
        Slider slider = new Slider(0, 1, 0.66);
        slider.setPrefWidth(200);
        cam = new Label("CAM 01 · (0.00, 0.00, 0.00, 0.00)");

        Region s1 = new Region();
        Region s2 = new Region();
        HBox.setHgrow(s1, Priority.ALWAYS);
        HBox.setHgrow(s2, Priority.ALWAYS);

        getChildren().addAll(fps, stats, s1, slider, s2, cam);
        setPadding(new Insets(10, 20, 10, 20));
        setAlignment(Pos.CENTER_LEFT);
        setStyle(UiTheme.GLASS);

        // 接口调用：每帧刷新摄像机坐标 ← RendererInterface.getCamera().getPosition()（Kotlin 侧已实现）
        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                Vector4f p = renderer.getCamera().getPosition();
                cam.setText(String.format("CAM 01 · (%.2f, %.2f, %.2f, %.2f)",
                        p.x, p.y, p.z, p.w));
            }
        };
        timer.start();
    }
}