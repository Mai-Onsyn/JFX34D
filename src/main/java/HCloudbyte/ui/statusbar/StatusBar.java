package HCloudbyte.ui.statusbar;

import HCloudbyte.ui.theme.Theme;
import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import mai_onsyn.renderer.core.GL4DRegion;
import mai_onsyn.renderer.interfaces.RendererInterface;
import mai_onsyn.renderer.utils.Coordinate4D;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * 底部状态信息条：操作模式（3D / 4D / 未聚焦）、3D 坐标、4D 坐标与视线、双 FPS、场景四面体总数。
 *
 * <p>用 {@link AnimationTimer} 每帧刷新模式 / 坐标 / FPS；四面体总数解析较贵，约每 2 秒刷一次。
 */
public class StatusBar extends HBox {

    private final GL4DRegion region;
    private final RendererInterface renderer;

    private final Label modeLabel;
    private final Label pos3dLabel;
    private final Label pos4dLabel;
    private final Label view4dLabel;
    private final Label fps3dLabel;
    private final Label fps4dLabel;
    private final Label tetCountLabel;

    private long lastTetRefresh = 0;

    public StatusBar(GL4DRegion region, RendererInterface renderer) {
        super(12);
        this.region = region;
        this.renderer = renderer;

        setPadding(new Insets(8, 20, 8, 20));
        setAlignment(Pos.CENTER_LEFT);
        setStyle(Theme.GLASS);

        modeLabel = new Label("—");
        modeLabel.setMinWidth(52);
        modeLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: " + Theme.TEXT_DIM + ";");

        pos3dLabel = strongText();
        pos4dLabel = strongText();
        view4dLabel = coloredText(Theme.INFO);
        fps3dLabel = coloredText(Theme.SUCCESS);
        fps4dLabel = coloredText(Theme.WARNING);
        tetCountLabel = coloredText(Theme.INFO);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        getChildren().addAll(
                modeLabel,
                Theme.prefixLabel("3D"), pos3dLabel,
                Theme.prefixLabel("·"),
                Theme.prefixLabel("4D"), pos4dLabel,
                Theme.prefixLabel("·"),
                Theme.prefixLabel("视线"), view4dLabel,
                spacer,
                fps3dLabel,
                Theme.prefixLabel("·"),
                fps4dLabel,
                Theme.prefixLabel("·"),
                tetCountLabel);

        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                updateMode();
                updateCoords();
                updateFps();
                if (now - lastTetRefresh > 2_000_000_000L) {   // 2 秒
                    lastTetRefresh = now;
                    tetCountLabel.setText("四面体 " + TetrahedronCounter.count(renderer));
                }
            }
        };
        timer.start();
    }

    private static Label strongText() {
        Label l = new Label();
        l.setStyle("-fx-text-fill: " + Theme.TEXT_DIM + ";");
        return l;
    }

    private static Label coloredText(String color) {
        Label l = new Label();
        l.setStyle("-fx-text-fill: " + color + ";");
        return l;
    }

    /** 操作模式：3D 键盘模式 / 4D 键盘模式 / 视口未聚焦。 */
    private void updateMode() {
        if (!region.isFocused()) {
            modeLabel.setText("未聚焦");
            modeLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: " + Theme.TEXT_FAINT + ";");
        } else if (region.getEnableInput()) {
            modeLabel.setText("3D 模式");
            modeLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: " + Theme.SUCCESS + ";");
        } else {
            modeLabel.setText("4D 模式");
            modeLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: " + Theme.WARNING + ";");
        }
    }

    /** 3D 坐标 / 4D 坐标 / 4D 视线（vw 基向量）。 */
    private void updateCoords() {
        Vector3f p3 = region.getScene3D().getCamera().getPos();
        pos3dLabel.setText(String.format("(%.2f, %.2f, %.2f)", p3.x + 0f, p3.y + 0f, p3.z + 0f));

        Vector4f p4 = renderer.getCamera().getPosition();
        pos4dLabel.setText(String.format("(%.2f, %.2f, %.2f, %.2f)",
                p4.x + 0f, p4.y + 0f, p4.z + 0f, p4.w + 0f));

        Coordinate4D view = renderer.getCamera().getView();
        Vector4f vw = view.getVw();
        view4dLabel.setText(String.format("(%.2f, %.2f, %.2f, %.2f)",
                vw.x + 0f, vw.y + 0f, vw.z + 0f, vw.w + 0f));
    }

    /** 双 FPS：3D / 4D 各自的平均帧率与 1% Low 帧。 */
    private void updateFps() {
        float f3 = renderer.getScene().get3DFPS();
        float l3 = renderer.getScene().get3D1PercentLowFPS();
        float f4 = renderer.getScene().get4DFPS();
        float l4 = renderer.getScene().get4D1PercentLowFPS();
        fps3dLabel.setText(String.format("3D %.0f fps (1%% %.0f)", f3, l3));
        fps4dLabel.setText(String.format("4D %.0f fps (1%% %.0f)", f4, l4));
    }
}
