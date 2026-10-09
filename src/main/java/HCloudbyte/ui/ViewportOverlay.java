package HCloudbyte.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import kotlin.Pair;
import mai_onsyn.renderer.interfaces.RendererInterface;
import mai_onsyn.renderer.utils.Coordinate4D;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

/**
 * 黑色视口左上角悬浮窗：摄像机机位管理（白色半透明背景）。
 * <ul>
 *   <li>「记录」：把当前相机位置 + 视线（{@code getPosition()}/{@code getView()}）存为一个机位；</li>
 *   <li>每行「相机N」：鼠标移入 hover 高亮，左键点击 = 应用（切到该机位），右键弹出「删除」；</li>
 * </ul>
 * 数据由 {@link HCloudbyte.ui.interfaces.UIInterface} 对外提供（AI 可增删查机位、读选中模型路径）。
 */
public class ViewportOverlay extends VBox {

    private final RendererInterface renderer;
    private final List<Pair<Vector4f, Coordinate4D>> records = new ArrayList<>();
    private final VBox rows = new VBox(2);

    private static final String ROW_DEFAULT =
            "-fx-padding: 3 8; -fx-background-radius: 6; -fx-background-color: transparent; -fx-text-fill: #e8eaed; -fx-font-size: 12px;";
    private static final String ROW_HOVER =
            "-fx-padding: 3 8; -fx-background-radius: 6; -fx-background-color: rgba(255,255,255,0.20); -fx-text-fill: #ffffff; -fx-font-size: 12px;";

    public ViewportOverlay(RendererInterface renderer) {
        super(4);
        this.renderer = renderer;
        setPadding(new Insets(6, 8, 6, 8));
        setMaxSize(290, 250);
        setMinSize(290, 250);
        // 白色半透明背景 + 白色半透明边框
        setStyle("-fx-background-color: rgba(255,255,255,0.10); -fx-background-radius: 10;"
                + "-fx-border-color: rgba(255,255,255,0.25); -fx-border-radius: 10;");
        // 右键菜单样式：必须走 CSS 类（ContextMenu.setStyle 作用不到皮肤节点）
        java.net.URL css = getClass().getResource("/css/viewport-overlay.css");
        if (css != null) getStylesheets().add(css.toExternalForm());

        Label title = new Label("相机机位");
        title.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #c7ccd1;");
        Button recBtn = new Button("记录");
        recBtn.setStyle("-fx-padding: 1 8; -fx-background-color: rgba(255,255,255,0.12);"
                + "-fx-background-radius: 6; -fx-text-fill: #e8eaed;");
        recBtn.setOnMouseEntered(e -> recBtn.setStyle("-fx-padding: 1 8; -fx-background-color: rgba(255,255,255,0.24);"
                + "-fx-background-radius: 6; -fx-text-fill: #ffffff;"));
        recBtn.setOnMouseExited(e -> recBtn.setStyle("-fx-padding: 1 8; -fx-background-color: rgba(255,255,255,0.12);"
                + "-fx-background-radius: 6; -fx-text-fill: #e8eaed;"));
        recBtn.setOnAction(e -> addRecord(renderer.getCamera().getPosition(), renderer.getCamera().getView()));

        HBox head = new HBox(6, title, recBtn);
        head.setAlignment(Pos.CENTER_LEFT);
        getChildren().addAll(head, rows);
        rebuild();
    }

    /** 新增一个机位（pos = 位置，view = 视线四轴），并刷新列表。 */
    public void addRecord(Vector4f pos, Coordinate4D view) {
        Coordinate4D copy = new Coordinate4D(
                new Vector4f(view.getVx()), new Vector4f(view.getVy()),
                new Vector4f(view.getVz()), new Vector4f(view.getVw()));
        records.add(new Pair<>(new Vector4f(pos), copy));
        rebuild();
    }

    /** 当前所有机位（深拷贝，防止外部改内部数据）。 */
    public List<Pair<Vector4f, Coordinate4D>> getRecords() {
        List<Pair<Vector4f, Coordinate4D>> copy = new ArrayList<>();
        for (Pair<Vector4f, Coordinate4D> r : records) {
            Coordinate4D view = r.getSecond();
            copy.add(new Pair<>(new Vector4f(r.getFirst()),
                    new Coordinate4D(new Vector4f(view.getVx()), new Vector4f(view.getVy()),
                            new Vector4f(view.getVz()), new Vector4f(view.getVw()))));
        }
        return copy;
    }

    /** 删除第 index 个机位；越界抛 {@link IndexOutOfBoundsException}。 */
    public void removeRecord(int index) {
        if (index < 0 || index >= records.size()) {
            throw new IndexOutOfBoundsException("index=" + index + ", size=" + records.size());
        }
        records.remove(index);
        rebuild();
    }

    private void rebuild() {
        rows.getChildren().clear();
        for (int i = 0; i < records.size(); i++) {
            final int idx = i;
            Label name = new Label("相机" + (i + 1));
            name.setStyle(ROW_DEFAULT);
            name.setMaxWidth(Double.MAX_VALUE);
            name.setOnMouseEntered(e -> name.setStyle(ROW_HOVER));
            name.setOnMouseExited(e -> name.setStyle(ROW_DEFAULT));
            // 左键点击 = 应用该机位
            name.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY) {
                    Pair<Vector4f, Coordinate4D> p = records.get(idx);
                    renderer.getCamera().setPosition(p.getFirst());
                    renderer.getCamera().setView(p.getSecond());
                }
            });
            // 右键菜单：删除（白色半透明风格，样式在 css/viewport-overlay.css）
            ContextMenu menu = new ContextMenu();
            menu.getStyleClass().add("overlay-menu");
            MenuItem del = new MenuItem("删除");
            del.setOnAction(e -> removeRecord(idx));
            menu.getItems().add(del);
            name.setContextMenu(menu);
            rows.getChildren().add(name);
        }
    }
}
