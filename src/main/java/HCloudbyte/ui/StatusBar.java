package HCloudbyte.ui;

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
 * <p>【接口调用】（直接调用 Kotlin 侧已定义接口）
 * <ul>
 *   <li>操作模式 → {@code GL4DRegion#isEnableInput()}：true=3D 键盘模式，false=4D 键盘模式；
 *       结合 {@code isFocused()} 显示"未聚焦"</li>
 *   <li>3D 坐标 → {@code region.getScene3D().getCamera().getPos()}（3D 相机不在 RendererInterface 内，直接读视口）</li>
 *   <li>4D 坐标 → {@code RendererInterface#getCamera().getPosition()}（CameraInterface#getPosition ✅）</li>
 *   <li>4D 视线 → {@code getCamera().getView()}：Coordinate4D 的 vw 基向量即视线方向（CameraInterface#getView ✅）</li>
 *   <li>双 FPS → {@code getScene().get3DFPS()/get3D1PercentLowFPS()/get4DFPS()/get4D1PercentLowFPS()}
 *       （SceneInterface ✅）</li>
 *   <li>四面体总数 → 遍历 {@code getModel().listModel()} 后逐个 {@code getGeometry().getModelInfos(root)}
 *       解析（实体 mesh 报 "Tetrahedrons: N"，分组路径报 "Total tetrahedrons: N"）</li>
 * </ul>
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

    /** 四面体统计低频刷新（getModelInfos 解析有开销），约每 2 秒一次。 */
    private long lastTetRefresh = 0;

    public StatusBar(GL4DRegion region, RendererInterface renderer) {
        super(12);
        this.region = region;
        this.renderer = renderer;

        setPadding(new Insets(8, 20, 8, 20));
        setAlignment(Pos.CENTER_LEFT);
        setStyle(UiTheme.GLASS);

        modeLabel = new Label("—");
        modeLabel.setMinWidth(52);
        modeLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #9aa0a6;");

        pos3dLabel = new Label();
        pos3dLabel.setStyle("-fx-text-fill: #9aa0a6;");

        pos4dLabel = new Label();
        pos4dLabel.setStyle("-fx-text-fill: #9aa0a6;");

        view4dLabel = new Label();
        view4dLabel.setStyle("-fx-text-fill: #8ab4f8;");

        fps3dLabel = new Label();
        fps3dLabel.setStyle("-fx-text-fill: #7fd08a;");

        fps4dLabel = new Label();
        fps4dLabel.setStyle("-fx-text-fill: #ffb36b;");

        tetCountLabel = new Label();
        tetCountLabel.setStyle("-fx-text-fill: #8ab4f8;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        getChildren().addAll(
                modeLabel,
                prefix("3D"), pos3dLabel,
                prefix("·"),
                prefix("4D"), pos4dLabel,
                prefix("·"),
                prefix("视线"), view4dLabel,
                spacer,
                fps3dLabel,
                prefix("·"),
                fps4dLabel,
                prefix("·"),
                tetCountLabel
        );

        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                updateMode();
                updateCoords();
                updateFps();
                if (now - lastTetRefresh > 2_000_000_000L) {   // 2 秒
                    lastTetRefresh = now;
                    tetCountLabel.setText("四面体 " + countTetrahedrons());
                }
            }
        };
        timer.start();
    }

    /** 灰色小前缀标签（用于分隔信息段）。 */
    private static Label prefix(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #6a7076;");
        return l;
    }

    /** 操作模式：3D 键盘模式（enableInput=true）/ 4D 键盘模式 / 视口未聚焦。 */
    private void updateMode() {
        if (!region.isFocused()) {
            modeLabel.setText("未聚焦");
            modeLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #6a7076;");
        } else if (region.getEnableInput()) {
            modeLabel.setText("3D 模式");
            modeLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #7fd08a;");
        } else {
            modeLabel.setText("4D 模式");
            modeLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #ffb36b;");
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

    /** 场景四面体总数：listModel 返回去重根路径，逐个取模型信息并解析数量。 */
    private int countTetrahedrons() {
        int total = 0;
        try {
            for (String root : renderer.getModel().listModel()) {
                String info = renderer.getGeometry().getModelInfos(root);
                int n = parseCount(info, "Total tetrahedrons: ");   // 分组路径的汇总
                if (n == 0) n = parseCount(info, "Tetrahedrons: "); // 实体模型的单模型数
                total += n;
            }
        } catch (Exception ignored) {
            // 个别模型信息解析失败不影响整体显示
        }
        return total;
    }

    /** 从 markdown 信息串里解析 "key N" 的数字部分，找不到返回 0。 */
    private static int parseCount(String text, String key) {
        int i = text.indexOf(key);
        if (i < 0) return 0;
        int j = i + key.length();
        int k = j;
        while (k < text.length() && Character.isDigit(text.charAt(k))) k++;
        return k > j ? Integer.parseInt(text.substring(j, k)) : 0;
    }
}
