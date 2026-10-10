package HCloudbyte.ui.viewport;

import HCloudbyte.ui.dialog.Dialogs;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import mai_onsyn.renderer.interfaces.RendererInterface;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

/**
 * 视口左上角悬浮窗：摄像机机位（书签）管理。半透明深色玻璃面板，可折叠、列表可滚动。
 *
 * <p>展开时（宽 300）：
 * <pre>
 *   ┌ 相机机位  ………………  [记录] [▾]
 *   ├ 机位1  (1.2, 3.4, -2.0, 0.5)     <-- 单行：名称 + 位置
 *   └ …
 * </pre>
 * 折叠时：整块收成一个「相机机位 ▸」小按钮（宽高都收缩）。
 *
 * <p>交互：左键点击某行 = 切到该机位；右键菜单 = 重命名 / 删除。
 * 数据由 {@link HCloudbyte.ui.interfaces.UIInterface} 对外提供（AI 可增删查机位）。
 */
public class CameraBookmarksView extends VBox {

    /** 展开时的固定宽度。 */
    private static final double EXPANDED_WIDTH = 200;
    /** 列表最大高度：超出后滚动，避免悬浮窗盖住整个视口。 */
    private static final double MAX_LIST_HEIGHT = 280;
    /** 名称列最大宽度（超出省略号），保证单行紧凑、位置不被挤没。 */
    private static final double NAME_MAX_WIDTH = 120;

    /* ==================== 配色 / 样式 ==================== */

    private static final String PANEL_STYLE =
            "-fx-background-color: rgba(24,27,31,0.58);"
                    + "-fx-background-radius: 12;"
                    + "-fx-border-color: rgba(255,255,255,0.12);"
                    + "-fx-border-radius: 12;"
                    + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 16, 0.2, 0, 6);";

    private static final String TITLE_STYLE =
            "-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #dfe3e8;";

    private static final String RECORD_BTN =
            "-fx-padding: 2 12; -fx-background-color: rgba(255,138,26,0.85); -fx-background-radius: 6;"
                    + "-fx-text-fill: #ffffff; -fx-font-size: 11px;";
    private static final String RECORD_BTN_HOVER =
            "-fx-padding: 2 12; -fx-background-color: rgba(255,138,26,1.0); -fx-background-radius: 6;"
                    + "-fx-text-fill: #ffffff; -fx-font-size: 11px;";

    private static final String CHEVRON_BTN =
            "-fx-padding: 1 6; -fx-background-color: transparent; -fx-text-fill: #9aa0a6; -fx-font-size: 11px;";
    private static final String CHEVRON_BTN_HOVER =
            "-fx-padding: 1 6; -fx-background-color: rgba(255,255,255,0.10); -fx-background-radius: 6;"
                    + "-fx-text-fill: #ffffff; -fx-font-size: 11px;";

    private static final String ROW_DEFAULT =
            "-fx-background-color: transparent; -fx-background-radius: 6; -fx-padding: 2 6;";
    private static final String ROW_HOVER =
            "-fx-background-color: rgba(255,255,255,0.09); -fx-background-radius: 6; -fx-padding: 2 6;";

    private static final String ROW_NAME =
            "-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #e8eaed;";
    private static final String ROW_POS =
            "-fx-font-size: 10px; -fx-text-fill: #9aa0a6;";
    private static final String EMPTY_HINT =
            "-fx-font-size: 11px; -fx-text-fill: #6a7076; -fx-padding: 2 2;";

    /* ==================== 状态 ==================== */

    private final RendererInterface renderer;
    private final List<CameraRecord> records = new ArrayList<>();
    private final VBox rows = new VBox(2);

    private ScrollPane scroll;
    private final Button collapseBtn = new Button();
    private final Button recordBtn = new Button("记录");
    private final Region spacer = new Region();
    private boolean expanded = true;

    public CameraBookmarksView(RendererInterface renderer) {
        super(8);
        this.renderer = renderer;

        // 右键菜单样式走 CSS 类（ContextMenu.setStyle 作用不到皮肤节点）
        java.net.URL css = getClass().getResource("/css/viewport-overlay.css");
        if (css != null) getStylesheets().add(css.toExternalForm());

        setPadding(new Insets(8, 10, 10, 10));
        setStyle(PANEL_STYLE);
        setMaxHeight(Region.USE_PREF_SIZE);   // 高度随内容，不撑满

        getChildren().addAll(buildHeader(), buildList());

        setExpanded(true);
        rebuild();
    }

    /* ==================== 第一行（标题 + 记录 + 折叠） ==================== */

    private HBox buildHeader() {
        Label title = new Label("相机机位");
        title.setStyle(TITLE_STYLE);
        title.setCursor(Cursor.HAND);
        title.setOnMouseClicked(e -> setExpanded(!expanded));   // 点标题也能折叠/展开

        HBox.setHgrow(spacer, Priority.ALWAYS);

        recordBtn.setStyle(RECORD_BTN);
        recordBtn.setCursor(Cursor.HAND);
        recordBtn.setOnMouseEntered(e -> recordBtn.setStyle(RECORD_BTN_HOVER));
        recordBtn.setOnMouseExited(e -> recordBtn.setStyle(RECORD_BTN));
        recordBtn.setOnAction(e -> recordCurrent());

        collapseBtn.setStyle(CHEVRON_BTN);
        collapseBtn.setCursor(Cursor.HAND);
        collapseBtn.setOnMouseEntered(e -> collapseBtn.setStyle(CHEVRON_BTN_HOVER));
        collapseBtn.setOnMouseExited(e -> collapseBtn.setStyle(CHEVRON_BTN));
        collapseBtn.setOnAction(e -> setExpanded(!expanded));

        HBox head = new HBox(8, title, spacer, recordBtn, collapseBtn);
        head.setAlignment(Pos.CENTER_LEFT);
        head.setCursor(Cursor.HAND);
        head.setOnMouseClicked(e -> {
            // 点空白处（非按钮）也能折叠
            if (e.getTarget() == head || e.getTarget() == spacer) setExpanded(!expanded);
        });
        return head;
    }

    /* ==================== 列表（可滚动） ==================== */

    private ScrollPane buildList() {
        scroll = new ScrollPane(rows);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setMinHeight(0);
        scroll.setMaxHeight(MAX_LIST_HEIGHT);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-background-insets: 0;");

        // 内容高度变化 → 同步视口高度（含最小改动判断，防止重排循环）
        rows.heightProperty().addListener((o, oldH, newH) -> {
            if (!expanded) return;
            double target = Math.min(newH.doubleValue(), MAX_LIST_HEIGHT);
            if (Math.abs(scroll.getPrefViewportHeight() - target) > 0.5) {
                scroll.setPrefViewportHeight(target);
                requestReflow();
            }
        });
        return scroll;
    }

    /* ==================== 折叠 / 展开 ==================== */

    private void setExpanded(boolean value) {
        this.expanded = value;

        scroll.setVisible(value);
        scroll.setManaged(value);
        recordBtn.setVisible(value);
        recordBtn.setManaged(value);
        spacer.setVisible(value);
        spacer.setManaged(value);
        collapseBtn.setText(value ? "▾" : "▸");

        // 展开：固定宽 300；折叠：宽度收缩到内容（小按钮）
        if (value) {
            setMinWidth(EXPANDED_WIDTH);
            setPrefWidth(EXPANDED_WIDTH);
            setMaxWidth(EXPANDED_WIDTH);
        } else {
            setMinWidth(Region.USE_PREF_SIZE);
            setPrefWidth(Region.USE_COMPUTED_SIZE);
            setMaxWidth(Region.USE_PREF_SIZE);
        }

        if (value) syncListHeight();
        requestReflow();
    }

    /* ==================== 对外 API ==================== */

    /** 记录当前相机为一个机位（名称自动生成）。 */
    public void recordCurrent() {
        addRecord(capture("机位" + (records.size() + 1)));
    }

    /** 用当前相机状态拍一个机位快照。 */
    public CameraRecord capture(String name) {
        return new CameraRecord(name,
                renderer.getCamera().getPosition(),
                renderer.getCamera().getView(),
                renderer.getScene().get4DCameraOrientation());
    }

    /** 新增一个机位，并刷新列表。 */
    public void addRecord(CameraRecord record) {
        if (record == null) return;
        records.add(record.copy());
        rebuild();
    }

    /** 当前所有机位（副本，防止外部改内部数据）。 */
    public List<CameraRecord> getRecords() {
        List<CameraRecord> copy = new ArrayList<>();
        for (CameraRecord r : records) copy.add(r.copy());
        return copy;
    }

    /** 重命名第 index 个机位；越界抛 {@link IndexOutOfBoundsException}。 */
    public void renameRecord(int index, String name) {
        records.get(checkIndex(index)).setName(name);
        rebuild();
    }

    /** 删除第 index 个机位；越界抛 {@link IndexOutOfBoundsException}。 */
    public void removeRecord(int index) {
        records.remove(checkIndex(index));
        rebuild();
    }

    /* ==================== 列表重建 ==================== */

    private void rebuild() {
        rows.getChildren().clear();
        if (records.isEmpty()) {
            Label empty = new Label("暂无机位，点「记录」保存当前视角");
            empty.setWrapText(true);
            empty.setStyle(EMPTY_HINT);
            rows.getChildren().add(empty);
        } else {
            for (int i = 0; i < records.size(); i++) {
                rows.getChildren().add(buildRow(i, records.get(i)));
            }
        }
        // 内容变了 → 重新计算悬浮窗尺寸并强制重排（否则要鼠标移上去才刷新）
        Platform.runLater(this::afterContentChanged);
    }

    /** 内容变化后：同步列表高度 + 强制重排，让悬浮窗立刻撑开/收缩。 */
    private void afterContentChanged() {
        if (expanded) syncListHeight();
        requestReflow();
    }

    /** 列表视口高度 = 内容高度，封顶 {@link #MAX_LIST_HEIGHT}。 */
    private void syncListHeight() {
        double content = rows.prefHeight(Math.max(EXPANDED_WIDTH - 26, 100));
        scroll.setPrefViewportHeight(Math.min(content, MAX_LIST_HEIGHT));
    }

    /** 请求重排：只靠子节点变化有时不会让父级（StackPane）重算尺寸，需显式请求。 */
    private void requestReflow() {
        requestLayout();
        if (getParent() != null) getParent().requestLayout();
    }

    private HBox buildRow(int index, CameraRecord record) {
        Label name = new Label(record.getName());
        name.setStyle(ROW_NAME);
        name.setMaxWidth(NAME_MAX_WIDTH);
        name.setTextOverrun(OverrunStyle.ELLIPSIS);

        Label pos = new Label(formatPosition(record.getPosition()));
        pos.setStyle(ROW_POS);

        HBox row = new HBox(8, name, pos);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle(ROW_DEFAULT);
        row.setMaxWidth(Double.MAX_VALUE);
        row.setCursor(Cursor.HAND);

        row.setOnMouseEntered(e -> row.setStyle(ROW_HOVER));
        row.setOnMouseExited(e -> row.setStyle(ROW_DEFAULT));

        // 左键点击 = 切到该机位
        row.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) applyRecord(record);
        });
        // 右键菜单：重命名 / 删除（VBox/HBox 是 Pane，没有 setContextMenu，需手动 show）
        row.setOnContextMenuRequested(e -> {
            buildRowMenu(index, record).show(row, e.getScreenX(), e.getScreenY());
            e.consume();
        });
        return row;
    }

    private ContextMenu buildRowMenu(int index, CameraRecord record) {
        ContextMenu menu = new ContextMenu();
        menu.getStyleClass().add("overlay-menu");

        MenuItem rename = new MenuItem("重命名");
        rename.setOnAction(e -> Dialogs.form("重命名机位", "机位 " + (index + 1),
                new String[]{"名称"}, new String[]{record.getName()},
                values -> {
                    String name = values[0].trim();
                    if (name.isEmpty()) throw new IllegalArgumentException("名称不能为空");
                    renameRecord(index, name);
                }));

        MenuItem delete = new MenuItem("删除");
        delete.setOnAction(e -> removeRecord(index));

        menu.getItems().addAll(rename, new SeparatorMenuItem(), delete);
        return menu;
    }

    /** 应用机位：设置相机位置与视线。 */
    private void applyRecord(CameraRecord record) {
        renderer.getCamera().setPosition(record.getPosition());
        renderer.getCamera().setView(record.getView());
    }

    private static String formatPosition(Vector4f p) {
        return String.format("(%.1f, %.1f, %.1f, %.1f)", p.x, p.y, p.z, p.w);
    }

    private int checkIndex(int index) {
        if (index < 0 || index >= records.size()) {
            throw new IndexOutOfBoundsException("index=" + index + ", size=" + records.size());
        }
        return index;
    }
}
