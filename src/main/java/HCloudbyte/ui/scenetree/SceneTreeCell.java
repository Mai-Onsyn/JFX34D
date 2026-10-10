package HCloudbyte.ui.scenetree;

import HCloudbyte.ui.dialog.Dialogs;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.TreeCell;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import mai_onsyn.renderer.cpu4dkt.MeshKind;

import java.util.function.Function;

/**
 * 树节点单元格：只负责「怎么显示一行」。
 * <ul>
 *   <li>文字用单元格自身的 text（叶名 + 类型配色），行尾小眼睛用 graphic + {@link ContentDisplay#RIGHT} 靠右。</li>
 *   <li><b>不要给 graphic 设 {@code prefWidth = Double.MAX_VALUE}</b>：会把单元格宽度撑成
 *       {@code Double.MAX_VALUE}，导致背景几何退化、背景完全画不出来（踩过）。</li>
 *   <li>行背景 / hover / 选中高亮一律用内联 {@code setStyle} 控制（靠监听 selected/hover 触发）。</li>
 *   <li>右键：先选中本行，再在鼠标位置弹出菜单（菜单由外部工厂构造）。</li>
 * </ul>
 *
 * <p>本类不持有模型数据，所有信息都经 {@link SceneModelAccess} 现查。
 */
final class SceneTreeCell extends TreeCell<String> {

    private static final String EYE_OPEN = "M1 6 Q 8 -1 15 6 Q 8 13 1 6 M8 3.8 A 2.6 2.2 0 1 1 8 8.2 A 2.6 2.2 0 1 1 8 3.8";
    private static final String EYE_CLOSED = "M1 6 Q 8 -1 15 6 Q 8 13 1 6 M1 1 L15 11";

    /** 选中：明显橙色底 + 橙色描边。 */
    private static final String SELECTED_BG =
            "-fx-background-color: rgba(255,138,26,0.60);"
                    + "-fx-background-radius: 8; -fx-border-color: rgba(255,138,26,0.95); -fx-border-radius: 8;";
    /** 悬停（未选中）：中性灰，明显亮于面板底。 */
    private static final String HOVER_BG =
            "-fx-background-color: rgba(162,168,176,0.26);"
                    + "-fx-background-radius: 8; -fx-border-color: transparent; -fx-border-radius: 8;";
    /** 常态：透明，透出面板玻璃底。 */
    private static final String NORMAL_BG =
            "-fx-background-color: transparent;"
                    + "-fx-background-radius: 8; -fx-border-color: transparent; -fx-border-radius: 8;";

    private final SceneModelAccess access;
    private final Runnable onSceneChanged;
    private final Function<String, ContextMenu> contextMenuFactory;
    private final SVGPath eye = new SVGPath();

    /** 当前行的基础文字样式（选中时会被白色覆盖）。 */
    private String baseTextColor = "#9aa0a6";
    private boolean boldText;
    private boolean italicText;

    SceneTreeCell(SceneModelAccess access, Runnable onSceneChanged,
                  Function<String, ContextMenu> contextMenuFactory) {
        this.access = access;
        this.onSceneChanged = onSceneChanged;
        this.contextMenuFactory = contextMenuFactory;

        setContentDisplay(ContentDisplay.RIGHT);   // 文字靠左、眼睛靠右
        setAlignment(Pos.CENTER_LEFT);

        eye.setStrokeWidth(1.5);
        eye.setStrokeLineCap(StrokeLineCap.ROUND);
        eye.setCursor(Cursor.HAND);
        eye.setOnMouseClicked(e -> {
            e.consume();                           // 别冒泡成"点行选中"
            toggleVisible();
        });

        setOnContextMenuRequested(this::showContextMenu);
        selectedProperty().addListener((o, was, isNow) -> applyRowStyle());
        hoverProperty().addListener((o, was, isNow) -> applyRowStyle());
    }

    @Override
    protected void updateItem(String path, boolean empty) {
        super.updateItem(path, empty);
        if (empty || path == null) {
            setText(null);
            setGraphic(null);
            setStyle(NORMAL_BG);
            return;
        }
        if (access.isRealModel(path)) {
            renderModelRow(path);
        } else {
            renderStructureRow(path);
        }
        applyRowStyle();
    }

    /** 真实模型：类型配色 + 可见性小眼睛。 */
    private void renderModelRow(String path) {
        boolean visible = access.visibleOf(path);
        MeshKind kind = access.kindOf(path);

        baseTextColor = colorFor(kind);
        boldText = kind == MeshKind.GROUP;
        italicText = !visible;

        setText(leafOf(path));          // 隐藏与否只由行尾小眼睛反馈，不加多余文字
        setGraphic(eye);
        eye.setContent(visible ? EYE_OPEN : EYE_CLOSED);
        eye.setStroke(visible ? Color.web("#c7ccd1") : Color.web("#565b60"));
    }

    /** 中间结构节点：灰色、无实体、不显示小眼睛。 */
    private void renderStructureRow(String path) {
        baseTextColor = "#9aa0a6";
        boldText = false;
        italicText = false;
        setText(leafOf(path));
        setGraphic(null);
    }

    /** 行背景 + 文字样式：选中 > 悬停 > 常态。 */
    private void applyRowStyle() {
        if (isEmpty()) {
            setStyle(NORMAL_BG);
            return;
        }
        String textColor = isSelected() ? "#ffffff" : baseTextColor;
        setStyle((isSelected() ? SELECTED_BG : (isHover() ? HOVER_BG : NORMAL_BG))
                + "-fx-font-size: 12px;"
                + "-fx-text-fill: " + textColor + ";"
                + (boldText ? "-fx-font-weight: bold;" : "")
                + (italicText ? "-fx-font-style: italic;" : ""));
    }

    private void toggleVisible() {
        String path = getItem();
        if (path == null || !access.isRealModel(path)) return;
        try {
            access.setVisible(path, !access.visibleOf(path));
            onSceneChanged.run();
        } catch (Exception e) {
            Dialogs.error("操作失败", e.getMessage());
        }
    }

    private void showContextMenu(ContextMenuEvent e) {
        if (isEmpty() || getItem() == null) return;
        getTreeView().getSelectionModel().select(getIndex());
        ContextMenu menu = contextMenuFactory.apply(getItem());
        if (menu != null) menu.show(this, e.getScreenX(), e.getScreenY());
        e.consume();
    }

    /** 节点类型配色：SHAPE 蓝 / CARVED 橙 / MERGED 紫 / GROUP 灰。 */
    private static String colorFor(MeshKind kind) {
        if (kind == null) return "#9aa0a6";
        return switch (kind) {
            case SHAPE -> "#8ab4f8";
            case CARVED -> "#ffa116";
            case MERGED -> "#c792ea";
            default -> "#9aa0a6";
        };
    }

    private static String leafOf(String path) {
        int i = path.lastIndexOf('/');
        return i < 0 ? path : path.substring(i + 1);
    }
}
