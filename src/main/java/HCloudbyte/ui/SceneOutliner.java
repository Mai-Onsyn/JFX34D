package HCloudbyte.ui;

import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import mai_onsyn.renderer.interfaces.RendererInterface;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 场景集合树（TreeView，IDE 风格）：按模型路径（'/' 分段）递归构建。
 * - 折叠：TreeView 自带箭头（像 IDE 项目树）
 * - 右键：浮动 ContextMenu 出现在鼠标位置
 * - 添加：新建模型挂到当前选中项下面（target = 选中路径）
 * - 选中实体节点 → 回调 {@code Consumer<String>}（传完整路径）
 *
 * <p>对外接口：
 * <ul>
 *   <li>{@link #getSelectedPath()} —— 当前选中模型的完整路径（未选中返回 null）</li>
 *   <li>{@link #refresh()} —— 场景模型变化后重建整棵树，保留原选中</li>
 * </ul>
 */
public class SceneOutliner extends VBox {

    /** "Sub models:" 段里的子模型行：- 完整路径  类型  tets=N（路径可能含空格，如 "Unnamed Mesh/123"，用非贪婪 .+? 抓路径） */
    private static final Pattern CHILD_LINE = Pattern.compile("(?m)^- (.+?)\\s+(\\w+)\\s+tets=\\d+$");
    /** 实体模型的类型行：Kind: SHAPE/CARVED/MERGED（分组路径没有该行 → GROUP） */
    private static final Pattern KIND_LINE = Pattern.compile("(?m)^Kind: (\\w+)$");

    private final RendererInterface renderer;
    private final Consumer<String> onSelect;
    private final TreeView<String> tree;

    private String selectedPath;

    public SceneOutliner(RendererInterface renderer) {
        this(renderer, null);
    }

    /**
     * @param renderer 渲染接口（取 model 列表递归构建树）
     * @param onSelect 选中实体模型回调（传完整路径，可为 null）
     */
    public SceneOutliner(RendererInterface renderer, Consumer<String> onSelect) {
        super(6);
        this.renderer = renderer;
        this.onSelect = onSelect;

        Label title = new Label("场景集合");
        title.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #e8eaed;");

        tree = new TreeView<>();
        tree.setShowRoot(false);
        tree.setPrefHeight(240);
        tree.setStyle("-fx-background-color: transparent; -fx-control-inner-background: transparent;"
                + "-fx-border-color: transparent; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
        tree.setCellFactory(tv -> new ModelTreeCell());

        // 选中 → 记录路径 + 联动 PropertyPanel
        tree.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV == null) {
                selectedPath = null;
                return;
            }
            selectedPath = newV.getValue();
            if (onSelect != null) onSelect.accept(selectedPath);
        });

        // 单击左键选中 → 单独显示该模型（隐藏其余），点不同节点即可在模型间切换显示
        tree.setOnMouseClicked(e -> {
            if (e.getButton() != javafx.scene.input.MouseButton.PRIMARY) return;
            TreeItem<String> item = tree.getSelectionModel().getSelectedItem();
            if (item != null && item.getValue() != null && !item.getValue().isEmpty()) {
                solo(item.getValue());
            }
        });

        // 右键：选中该行 + 浮动菜单出现在鼠标位置（IDE 风格）。
        // 注意：不做 tree 级右键——TreeCell 自己处理，保证"右键哪行 = 对哪行操作"。
        // 已迁移到 ModelTreeCell.setOnContextMenuRequested。

        getChildren().addAll(title, tree);
        refresh();
    }

    /** 外部接口：当前选中的模型完整路径（未选中返回 null）。 */
    public String getSelectedPath() {
        return selectedPath;
    }

    /** 重建整棵树（场景模型变化后调用，如 AI 创建/删除模型后）。根默认展开，其余默认折叠；尽量保留原选中。 */
    public void refresh() {
        String keep = selectedPath;
        TreeItem<String> root = new TreeItem<>("");
        root.setExpanded(true);

        List<String> roots = renderer.getModel().listModel();
        for (String r : roots) root.getChildren().add(buildItem(r));
        tree.setRoot(root);

        selectedPath = null;
        if (keep != null) {
            TreeItem<String> item = findItem(root, keep);
            if (item != null) {
                tree.getSelectionModel().select(item);
                return;
            }
        }
        // 默认选中第一个实体根，保证 PropertyPanel 有初始目标
        for (TreeItem<String> item : root.getChildren()) {
            if (item.getValue() != null) {
                tree.getSelectionModel().select(item);
                break;
            }
        }
    }

    /** 深度优先查找完整路径对应的 TreeItem（找不到返回 null）。 */
    private TreeItem<String> findItem(TreeItem<String> node, String path) {
        if (path.equals(node.getValue())) return node;
        for (TreeItem<String> child : node.getChildren()) {
            TreeItem<String> found = findItem(child, path);
            if (found != null) return found;
        }
        return null;
    }

    /** 递归构建一个 TreeItem；value = 完整路径，cell 显示叶名。 */
    private TreeItem<String> buildItem(String path) {
        String info = safeInfos(path);
        String kind = parseKind(info);
        TreeItem<String> item = new TreeItem<>(path);
        item.setExpanded(true);
        for (String c : parseChildren(info)) item.getChildren().add(buildItem(c));
        return item;
    }

    /* ==================== 单元格渲染（IDE 风格） ==================== */

    /** TreeCell：显示叶名 + 类型颜色 + 行尾小眼睛（点击切换可见性）；选中橙色高亮。 */
    private final class ModelTreeCell extends TreeCell<String> {
        private static final String EYE_OPEN = "M1 6 Q 8 -1 15 6 Q 8 13 1 6 M8 3.8 A 2.6 2.2 0 1 1 8 8.2 A 2.6 2.2 0 1 1 8 3.8";
        private static final String EYE_CLOSED = "M1 6 Q 8 -1 15 6 Q 8 13 1 6 M1 1 L15 11";

        private final Label nameLabel = new Label();
        private final Region spacer = new Region();
        private final SVGPath eye = new SVGPath();
        private final HBox rowBox = new HBox(4);

        ModelTreeCell() {
            spacer.setMinWidth(8);
            HBox.setHgrow(spacer, Priority.ALWAYS);
            rowBox.setAlignment(Pos.CENTER_LEFT);
            rowBox.setPrefWidth(Double.MAX_VALUE);
            rowBox.getChildren().addAll(nameLabel, spacer, eye);
            eye.setStrokeWidth(1.5);
            eye.setStrokeLineCap(StrokeLineCap.ROUND);
            eye.setCursor(Cursor.HAND);
            // 点击眼睛：读取当前可见性并取反（小眼睛切换隐藏/显示）；consume 防止冒泡触发树级单击 solo
            eye.setOnMouseClicked(e -> {
                e.consume();
                if (!(eye.getUserData() instanceof String p)) return;
                try {
                    boolean v = renderer.getModel().getModel(p).getVisible();
                    renderer.getModel().setModelVisible(p, !v);
                } catch (Exception ex) {
                    alertError("操作失败", ex.getMessage());
                }
                refresh();
            });
            setGraphic(rowBox);
            setStyle("-fx-background-color: transparent; -fx-padding: 3 6 3 10;");
            selectedProperty().addListener((o, a, b) -> applyCellStyle());
            hoverProperty().addListener((o, a, b) -> applyCellStyle());
            // 行自身响应右键：先选中该行，再在鼠标位置弹菜单（保证"右键哪行=对哪行操作"）
            setOnContextMenuRequested(e -> {
                if (isEmpty() || getItem() == null) return;
                String path = getItem();
                int row = getIndex();
                tree.getSelectionModel().select(row);
                buildMenu(path).show(this, e.getScreenX(), e.getScreenY());
                e.consume();
            });
        }

        @Override
        protected void updateItem(String path, boolean empty) {
            super.updateItem(path, empty);
            if (empty || path == null) {
                setGraphic(null);
                return;
            }
            setGraphic(rowBox);
            String leaf = path.contains("/") ? path.substring(path.lastIndexOf('/') + 1) : path;
            boolean vis = true;
            try {
                vis = renderer.getModel().getModel(path).getVisible();
            } catch (Exception ignored) { }
            String kind = parseKind(safeInfos(path));
            nameLabel.setText(leaf + (vis ? "" : "（隐藏）"));
            nameLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + colorFor(kind)
                    + (isGroup(kind) ? "; -fx-font-weight: bold;" : "")
                    + (vis ? "" : "; -fx-font-style: italic;"));
            eye.setContent(vis ? EYE_OPEN : EYE_CLOSED);
            eye.setStroke(vis ? Color.web("#c7ccd1") : Color.web("#565b60"));
            eye.setUserData(path);
            applyCellStyle();
        }

        private void applyCellStyle() {
            if (isEmpty()) {
                setStyle("-fx-background-color: transparent; -fx-padding: 3 6 3 10;");
                return;
            }
            String bg = isSelected()
                    ? "linear-gradient(to right, rgba(255,138,26,0.22), rgba(255,138,26,0.06));"
                    : (isHover() ? "rgba(100,107,115,0.45);" : "transparent;");
            setStyle("-fx-background-color: " + bg
                    + "-fx-background-radius: 8;"
                    + "-fx-padding: 3 6 3 10;"
                    + "-fx-border-color: transparent;");
        }
    }

    private String safeInfos(String path) {
        try {
            return renderer.getGeometry().getModelInfos(path);
        } catch (Exception ignored) {
            return "";
        }
    }

    /* ==================== 右键浮动菜单（IDE 风格，出现在鼠标位置） ==================== */

    /** 构建当前行的右键菜单；分组禁用需实体的操作。 */
    private ContextMenu buildMenu(String path) {
        String kind = parseKind(safeInfos(path));
        boolean isGroup = isGroup(kind);

        MenuItem rename = new MenuItem("重命名");
        rename.setOnAction(e -> doRename(path));
        rename.setDisable(isGroup);

        MenuItem copy = new MenuItem("复制");
        copy.setOnAction(e -> doCopy(path));
        copy.setDisable(isGroup);

        MenuItem add = new MenuItem("添加");
        add.setOnAction(e -> doAdd(path));

        MenuItem del = new MenuItem("删除");
        del.setOnAction(e -> doDelete(path));

        MenuItem merge = new MenuItem("合并");
        merge.setOnAction(e -> doMerge(path));
        merge.setDisable(isGroup);

        MenuItem bake = new MenuItem("烘焙");
        bake.setOnAction(e -> doBake(path));
        bake.setDisable(isGroup);

        MenuItem mergeAll = new MenuItem("合并全部子模型");
        mergeAll.setOnAction(e -> doMergeAll(path));

        MenuItem group = new MenuItem("创建空分组");
        group.setOnAction(e -> doCreateGroup(path));

        ContextMenu menu = new ContextMenu();
        menu.getItems().addAll(rename, copy, add, del, merge, bake, mergeAll, new SeparatorMenuItem(), group);
        return menu;
    }

    /* ==================== 解析 ==================== */

    private static boolean isGroup(String kind) {
        return "GROUP".equals(kind);
    }

    /** 节点文字颜色：SHAPE 蓝 / CARVED 橙 / MERGED 紫 / GROUP 灰。 */
    private static String colorFor(String kind) {
        switch (kind) {
            case "SHAPE":   return "#8ab4f8";
            case "CARVED":  return "#ffa116";
            case "MERGED":  return "#c792ea";
            default:        return "#9aa0a6";
        }
    }

    private static String parseKind(String info) {
        Matcher m = KIND_LINE.matcher(info);
        return m.find() ? m.group(1) : "GROUP";
    }

    /** 解析 "Sub models:" 段的所有子模型完整路径。 */
    private static List<String> parseChildren(String info) {
        List<String> out = new ArrayList<>();
        Matcher m = CHILD_LINE.matcher(info);
        while (m.find()) out.add(m.group(1));
        return out;
    }

    /* ==================== 模型操作 ==================== */

    /** 复制：copyModel(旧, 新)，弹窗填新路径（默认 原名_Copy）。 */
    private void doCopy(String path) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("复制模型");
        dialog.setHeaderText("复制 " + path);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        TextField nameField = new TextField(path + "_Copy");
        Label error = new Label();
        error.setWrapText(true);
        error.setMaxWidth(300);
        error.setStyle("-fx-text-fill: #e06c75;");
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setPadding(new Insets(12));
        grid.addRow(0, new Label("新路径"), nameField);
        grid.addRow(1, error);
        dialog.getDialogPane().setContent(grid);
        Button okBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.addEventFilter(ActionEvent.ACTION, e -> {
            String newName = nameField.getText().trim();
            if (newName.isEmpty() || newName.equals(path)) {
                error.setText("请输入不同的新路径");
                e.consume();
                return;
            }
            try {
                renderer.getModel().copyModel(path, newName);
                refresh();
            } catch (Exception ex) {
                error.setText("复制失败：" + ex.getMessage());
                e.consume();
            }
        });
        dialog.showAndWait();
    }

    /** 合并：mergeModel(源1=右键行, 源2, 目标)，弹窗填源2与目标名。 */
    private void doMerge(String path) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("合并模型");
        dialog.setHeaderText("合并 " + path + " 与另一个模型");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        TextField s2 = new TextField();
        s2.setPromptText("第二个模型路径");
        TextField dst = new TextField(path + "_Merged");
        Label error = new Label();
        error.setWrapText(true);
        error.setMaxWidth(300);
        error.setStyle("-fx-text-fill: #e06c75;");
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setPadding(new Insets(12));
        grid.addRow(0, new Label("源1"), new TextField(path));
        grid.addRow(1, new Label("源2"), s2);
        grid.addRow(2, new Label("目标"), dst);
        grid.addRow(3, error);
        dialog.getDialogPane().setContent(grid);
        Button okBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.addEventFilter(ActionEvent.ACTION, e -> {
            String src2 = s2.getText().trim();
            String target = dst.getText().trim();
            if (src2.isEmpty() || target.isEmpty()) {
                error.setText("源2与目标路径不能为空");
                e.consume();
                return;
            }
            try {
                renderer.getModel().mergeModel(path, src2, target);
                refresh();
            } catch (Exception ex) {
                error.setText("合并失败：" + ex.getMessage());
                e.consume();
            }
        });
        dialog.showAndWait();
    }

    /** 烘焙：applyTransformToVertex(源=右键行, 目标)，把变换永久写入顶点生成新模型。 */
    private void doBake(String path) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("烘焙变换");
        dialog.setHeaderText("烘焙 " + path + " 的变换到顶点");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        TextField dst = new TextField(path + "_Baked");
        Label error = new Label();
        error.setWrapText(true);
        error.setMaxWidth(300);
        error.setStyle("-fx-text-fill: #e06c75;");
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setPadding(new Insets(12));
        grid.addRow(0, new Label("源"), new TextField(path));
        grid.addRow(1, new Label("目标"), dst);
        grid.addRow(2, error);
        dialog.getDialogPane().setContent(grid);
        Button okBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.addEventFilter(ActionEvent.ACTION, e -> {
            String target = dst.getText().trim();
            if (target.isEmpty()) {
                error.setText("目标路径不能为空");
                e.consume();
                return;
            }
            try {
                renderer.getModel().applyTransformToVertex(path, target);
                refresh();
            } catch (Exception ex) {
                error.setText("烘焙失败：" + ex.getMessage());
                e.consume();
            }
        });
        dialog.showAndWait();
    }

    /** 创建空分组：createEmptyModel(name 可含路径)，右键某节点时挂到该节点下。 */
    private void doCreateGroup(String parentPath) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("创建空分组");
        dialog.setHeaderText("创建分组到 " + (parentPath == null || parentPath.isEmpty() ? "场景根" : parentPath));
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        TextField nameField = new TextField();
        nameField.setPromptText("分组名，如 Group1");
        Label error = new Label();
        error.setWrapText(true);
        error.setMaxWidth(300);
        error.setStyle("-fx-text-fill: #e06c75;");
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setPadding(new Insets(12));
        grid.addRow(0, new Label("分组名"), nameField);
        grid.addRow(1, error);
        dialog.getDialogPane().setContent(grid);
        Button okBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.addEventFilter(ActionEvent.ACTION, e -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                error.setText("分组名不能为空");
                e.consume();
                return;
            }
            // createEmptyModel 支持含路径的 name：右键在哪，分组就挂到哪
            String full = (parentPath == null || parentPath.isEmpty()) ? name : parentPath + "/" + name;
            try {
                renderer.getModel().createEmptyModel(full);
                refresh();
            } catch (Exception ex) {
                error.setText("创建失败：" + ex.getMessage());
                e.consume();
            }
        });
        dialog.showAndWait();
    }

    /** 合并全部子模型：mergeAllSubModels(选中路径, 目标)，把该节点下所有子模型合成一个。 */
    private void doMergeAll(String path) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("合并全部子模型");
        dialog.setHeaderText("合并 " + path + " 下全部子模型");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        TextField dst = new TextField(path + "_AllMerged");
        Label error = new Label();
        error.setWrapText(true);
        error.setMaxWidth(300);
        error.setStyle("-fx-text-fill: #e06c75;");
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setPadding(new Insets(12));
        grid.addRow(0, new Label("源"), new TextField(path));
        grid.addRow(1, new Label("目标"), dst);
        grid.addRow(2, error);
        dialog.getDialogPane().setContent(grid);
        Button okBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.addEventFilter(ActionEvent.ACTION, e -> {
            String target = dst.getText().trim();
            if (target.isEmpty()) {
                error.setText("目标路径不能为空");
                e.consume();
                return;
            }
            try {
                renderer.getModel().mergeAllSubModels(path, target);
                refresh();
            } catch (Exception ex) {
                error.setText("合并失败：" + ex.getMessage());
                e.consume();
            }
        });
        dialog.showAndWait();
    }

    /** 重命名：根级模型走渲染侧 renameModel 真接口；子级暂用 copy+remove（渲染侧 renameModel 对子级有 bug：会把名字拼成 path/newName 且 requireNotContains(path) 自检查冲突）。 */
    private void doRename(String path) {
        TextInputDialog dialog = new TextInputDialog(path);
        dialog.setTitle("重命名");
        dialog.setHeaderText("重命名 " + path);
        dialog.setContentText("新名称（可含路径，如 Tower/Base）：");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(newName -> {
            newName = newName.trim();
            if (newName.isEmpty() || newName.equals(path)) return;
            try {
                if (path.contains("/")) {
                    // 渲染侧 renameModel 子级路径有 bug（见类注释），暂用复制后删源模拟
                    renderer.getModel().copyModel(path, newName);
                    renderer.getModel().removeModel(path);
                } else {
                    renderer.getModel().renameModel(path, newName);
                }
                refresh();
            } catch (Exception ex) {
                alertError("重命名失败", ex.getMessage());
            }
        });
    }

    /** 删除：removeModel（实体删自己；分组删整棵子树）。 */
    private void doDelete(String path) {
        try {
            renderer.getModel().removeModel(path);
            refresh();
        } catch (Exception ex) {
            alertError("删除失败", ex.getMessage());
        }
    }

    /**
     * 添加形状：创建到场景根（顶层独立模型）。
     * 注意：不走"挂到右键行下"——渲染侧 filterVisible 语义是"父隐藏 ⇒ 子树连带隐藏"，
     * 若创建为子级再隐藏父，新模型也会被过滤掉（视口空）。创建到根后自动 solo 新模型，
     * 隐藏其余所有模型，实现"视口只显示新模型"。
     */
    private void doAdd(String parentPath) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("添加形状");
        dialog.setHeaderText("创建形状（到场景根）");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        ComboBox<String> shapeBox = new ComboBox<>();
        shapeBox.getItems().addAll("正四面体", "5Cell", "16Cell", "超立方体", "超球");
        shapeBox.getSelectionModel().select(0);

        TextField nameField = new TextField();
        nameField.setPromptText("留空自动命名");

        TextField cx = new TextField("0.00"), cy = new TextField("0.00"),
                cz = new TextField("0.00"), cw = new TextField("0.00");
        HBox centerRow = new HBox(4,
                new Label("X"), compact(cx), new Label("Y"), compact(cy),
                new Label("Z"), compact(cz), new Label("W"), compact(cw));

        Label paramLabel = new Label("半径");
        TextField param = new TextField("1.00");
        Label densityLabel = new Label("密度");
        TextField density = new TextField("3.00");
        HBox paramRow = new HBox(4, paramLabel, compact(param), densityLabel, compact(density));

        shapeBox.valueProperty().addListener((o, oldV, newV) -> {
            paramLabel.setText(paramLabelFor(newV));
            boolean ball = "超球".equals(newV);
            density.setVisible(ball);
            density.setManaged(ball);
            densityLabel.setVisible(ball);
            densityLabel.setManaged(ball);
        });
        density.setVisible(false);
        density.setManaged(false);
        densityLabel.setVisible(false);
        densityLabel.setManaged(false);

        Label error = new Label();
        error.setWrapText(true);
        error.setMaxWidth(300);
        error.setStyle("-fx-text-fill: #e06c75;");

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setPadding(new Insets(12));
        grid.addRow(0, new Label("形状"), shapeBox);
        grid.addRow(1, new Label("名称"), nameField);
        grid.addRow(2, new Label("中心"), centerRow);
        grid.addRow(3, new Label("参数"), paramRow);
        grid.addRow(4, error);
        dialog.getDialogPane().setContent(grid);

        // 确定时校验并执行；失败不关窗，错误显示在窗内
        Button okBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        String target = "";   // 创建到场景根（顶层），见 doAdd 注释
        okBtn.addEventFilter(ActionEvent.ACTION, e -> {
            String name = null;
            try {
                name = nameField.getText().trim();
                if (name.isEmpty()) name = autoName(shapeBox.getValue());
                Vector4f center = new Vector4f(
                        parseF(cx.getText(), 0f), parseF(cy.getText(), 0f),
                        parseF(cz.getText(), 0f), parseF(cw.getText(), 0f));
                float p = parseF(param.getText(), 1f);
                switch (shapeBox.getValue()) {
                    case "正四面体" -> renderer.getShape().createTetrahedron(target, name, center, p);
                    case "5Cell"    -> renderer.getShape().create5Cell(target, name, center, p);
                    case "16Cell"   -> renderer.getShape().create16Cell(target, name, center, p);
                    case "超立方体"  -> renderer.getShape().createTesseract(target, name, center, p);
                    case "超球"     -> renderer.getShape().createBall4(target, name, center, p,
                            parseF(density.getText(), 0.5f));
                }
            } catch (Exception ex) {
                ex.printStackTrace(System.err);
                error.setText("添加失败：" + ex.getMessage());
                e.consume();   // 阻止对话框关闭
                return;
            }
            // 创建成功后再刷新列表，并选中新模型；随后 solo（隐藏其余模型，视口只显示新模型）
            String createdPath = (target.isEmpty() ? "" : target + "/") + name;
            try {
                refresh();
                selectPath(createdPath);
                solo(createdPath);
            } catch (Exception ex) {
                ex.printStackTrace(System.err);
                error.setText("已创建 " + name + "，但列表刷新失败：" + ex.getMessage());
                e.consume();
            }
        });

        dialog.showAndWait();
    }

    /** 按完整路径选中树节点：展开父链、选中、滚动到可见（找不到则忽略）。 */
    private void selectPath(String path) {
        TreeItem<String> item = findItem(tree.getRoot(), path);
        if (item == null) return;
        TreeItem<String> p = item.getParent();
        while (p != null) {
            p.setExpanded(true);
            p = p.getParent();
        }
        tree.getSelectionModel().select(item);
        tree.scrollTo(tree.getSelectionModel().getSelectedIndex());
    }

    /** 单独显示指定模型（含其全部子级），隐藏场景中其余模型；点树节点即在模型间切换显示。 */
    private void solo(String path) {
        List<String> all = new ArrayList<>();
        collectPaths(tree.getRoot(), all);
        for (String p : all) {
            boolean show = p.equals(path) || p.startsWith(path + "/");
            try {
                if (renderer.getModel().getModel(p).getVisible() != show) {
                    renderer.getModel().setModelVisible(p, show);
                }
            } catch (Exception ignored) { }
        }
        refresh();
    }

    /** 收集树中所有节点完整路径（不含根哨兵）。 */
    private void collectPaths(TreeItem<String> node, List<String> out) {
        if (node != null && node.getValue() != null && !node.getValue().isEmpty()) out.add(node.getValue());
        for (TreeItem<String> c : node.getChildren()) collectPaths(c, out);
    }

    private static TextField compact(TextField f) {
        f.setPrefWidth(46);
        f.setStyle("-fx-padding: 2 6;");
        return f;
    }

    private static String paramLabelFor(String shape) {
        switch (shape) {
            case "正四面体": return "半径";
            case "5Cell":   return "大小";
            case "16Cell":  return "半径";
            case "超立方体": return "边长";
            default:        return "半径";
        }
    }

    private static float parseF(String text, float def) {
        try {
            return Float.parseFloat(text.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    /** 自动命名：类型_时间戳（渲染侧要求路径不重复）。 */
    private static String autoName(String shape) {
        String t;
        switch (shape) {
            case "正四面体": t = "Tet"; break;
            case "5Cell":    t = "FiveCell"; break;
            case "16Cell":   t = "SixteenCell"; break;
            case "超立方体":  t = "Tesseract"; break;
            default:         t = "Ball";
        }
        return t + "_" + (System.currentTimeMillis() % 100000);
    }

    private void alertError(String title, String msg) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg == null ? "未知错误" : msg);
        alert.showAndWait();
    }
}
