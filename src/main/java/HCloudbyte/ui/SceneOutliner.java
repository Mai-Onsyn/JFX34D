package HCloudbyte.ui;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.VBox;
import mai_onsyn.renderer.interfaces.ModelInterface;
import mai_onsyn.renderer.interfaces.RendererInterface;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * 场景集合树：把「场景 = Map&lt;String, Mesh4D&gt;」按路径分隔符渲染成一棵 IDE 风格的树。
 *
 * <p>三条铁律：
 * <ol>
 *   <li><b>节点只存完整路径</b>，绝不缓存 {@code Mesh4D}；要操作模型就按路径经接口取。</li>
 *   <li><b>永不部分更新</b>：只在模型 keySet 变化时按 {@link ModelInterface#listAllModels()} 全量重建。</li>
 *   <li>重建时保留展开状态与选中（选中失效则回退第一个节点）。</li>
 * </ol>
 *
 * <p>重建来源有两处：UI 自身操作后立即重建；以及一个每秒轮询的虚拟线程（因为 AI 也会改场景）。
 * 本类是**可复用组件**，各面板各自 new 一个，彼此独立、无静态共享状态。
 */
public class SceneOutliner extends VBox {

    private static final long POLL_INTERVAL_MILLIS = 1000;

    private final RendererInterface renderer;
    private final ModelInterface model;
    private final SceneModelAccess access;
    private final Consumer<String> onSelect;

    private final TreeView<String> tree = new TreeView<>();
    /** 处于展开状态的节点路径（含即展开）；重建时据实际状态回填，自动剔除失效项。 */
    private final Set<String> expandedPaths = new HashSet<>();

    private final SceneTreePoller poller;
    private String selectedPath;
    private boolean suppressSelectionCallback;
    private boolean everBuilt;

    public SceneOutliner(RendererInterface renderer) {
        this(renderer, null);
    }

    /**
     * @param renderer 渲染接口（取模型列表与模型对象）
     * @param onSelect 选中回调，参数为节点完整路径（不以 / 结尾）；结构节点同样回调，由调用方自行判空
     */
    public SceneOutliner(RendererInterface renderer, Consumer<String> onSelect) {
        super(6);
        this.renderer = renderer;
        this.model = renderer.getModel();
        this.onSelect = onSelect;
        this.access = new SceneModelAccess(model);

        getChildren().addAll(buildTitle(), buildTree());

        rebuildFromScene();
        everBuilt = true;

        this.poller = new SceneTreePoller(model::listAllModels, this::onPolledKeysChanged);
        this.poller.start(POLL_INTERVAL_MILLIS);
    }

    /** 当前选中的节点路径（未选中返回 null）。 */
    public String getSelectedPath() {
        return selectedPath;
    }

    /** 外部统一入口：按当前场景全量重建整棵树。 */
    public void refresh() {
        rebuildFromScene();
    }

    /* ==================== 界面搭建 ==================== */

    private Label buildTitle() {
        Label title = new Label("场景集合");
        title.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #e8eaed;");
        return title;
    }

    private TreeView<String> buildTree() {
        tree.setShowRoot(false);
        tree.setMinHeight(240);          // 不被其他组件压缩
        tree.setPrefHeight(280);
        tree.getStyleClass().add("scene-tree");
        applyStylesheet();
        tree.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        tree.setCellFactory(tv -> new SceneTreeCell(access, this::refresh, this::buildRowMenu));
        tree.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldItem, newItem) -> handleSelectionChanged(newItem));
        // 空白区域右键 → 场景根菜单（删空后仍能新建）
        tree.setOnContextMenuRequested(e -> {
            ContextMenu menu = SceneItemMenu.buildRoot(renderer, this::refresh);
            menu.show(tree, e.getScreenX(), e.getScreenY());
            e.consume();
        });
        return tree;
    }

    /** 加载组件样式表（压平 TreeView 内部底色、定义 hover/selected 高亮）。 */
    private void applyStylesheet() {
        java.net.URL css = SceneOutliner.class.getResource("/css/scene-tree.css");
        if (css != null) tree.getStylesheets().add(css.toExternalForm());
    }

    private ContextMenu buildRowMenu(String path) {
        return SceneItemMenu.build(renderer, access, path, this::refresh);
    }

    /* ==================== 重建 ==================== */

    private void rebuildFromScene() {
        rebuild(model.listAllModels());
    }

    /** 全量重建：由给定 keySet 建树，并尽量恢复展开与选中。 */
    private void rebuild(Collection<String> paths) {
        access.updateKeys(paths);

        boolean expandAll = !everBuilt;
        TreeItem<String> root = SceneTreeBuilder.build(paths, expandedPaths, expandAll);
        expandedPaths.clear();
        collectExpanded(root, expandedPaths);   // 用实际展开状态回填（顺带剔除失效路径）
        attachExpansionTracking(root);

        // setRoot 会瞬间清空选中并触发监听，用开关屏蔽这次"空选中"回调
        suppressSelectionCallback = true;
        try {
            tree.setRoot(root);
        } finally {
            suppressSelectionCallback = false;
        }

        restoreSelection();
        if (poller != null) poller.acknowledge(paths);
    }

    /** 收集当前树中"有子节点且已展开"的路径。 */
    private void collectExpanded(TreeItem<String> node, Set<String> out) {
        if (!node.getChildren().isEmpty() && node.isExpanded() && !node.getValue().isEmpty()) {
            out.add(node.getValue());
        }
        for (TreeItem<String> child : node.getChildren()) collectExpanded(child, out);
    }

    /** 给每个节点挂展开监听，让 expandedPaths 始终跟随用户操作。 */
    private void attachExpansionTracking(TreeItem<String> node) {
        if (!node.getValue().isEmpty()) {
            node.expandedProperty().addListener((obs, was, isNow) -> {
                if (isNow) expandedPaths.add(node.getValue());
                else expandedPaths.remove(node.getValue());
            });
        }
        for (TreeItem<String> child : node.getChildren()) attachExpansionTracking(child);
    }

    /** 恢复选中：原路径仍在则选它，否则回退第一个节点；都没有则清空。 */
    private void restoreSelection() {
        TreeItem<String> target = selectedPath == null ? null : findItem(tree.getRoot(), selectedPath);
        if (target == null) target = firstItem();
        if (target == null) {
            selectedPath = null;
            return;
        }
        selectItem(target);
    }

    private TreeItem<String> firstItem() {
        List<TreeItem<String>> roots = tree.getRoot().getChildren();
        return roots.isEmpty() ? null : roots.get(0);
    }

    private void selectItem(TreeItem<String> item) {
        for (TreeItem<String> p = item.getParent(); p != null; p = p.getParent()) p.setExpanded(true);
        tree.getSelectionModel().select(item);
        tree.scrollTo(tree.getSelectionModel().getSelectedIndex());
    }

    private TreeItem<String> findItem(TreeItem<String> node, String path) {
        if (path.equals(node.getValue())) return node;
        for (TreeItem<String> child : node.getChildren()) {
            TreeItem<String> found = findItem(child, path);
            if (found != null) return found;
        }
        return null;
    }

    /* ==================== 选中与轮询回调 ==================== */

    private void handleSelectionChanged(TreeItem<String> item) {
        if (suppressSelectionCallback) return;
        String path = (item == null) ? null : item.getValue();
        selectedPath = path;
        if (path != null && onSelect != null) onSelect.accept(path);
    }

    /** 轮询发现 keySet 变化（已在 FX 线程）→ 全量重建。 */
    private void onPolledKeysChanged(Set<String> keys) {
        rebuild(keys);
    }
}
