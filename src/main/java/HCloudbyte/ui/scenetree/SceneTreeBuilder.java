package HCloudbyte.ui.scenetree;

import javafx.scene.control.TreeItem;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * 纯路径建树：只吃「完整路径集合」，不依赖任何渲染 / 模型对象。
 *
 * <p>规则：路径按 {@code /} 分段，每段拼出一个完整路径作为节点值。
 * 中间段即使不是真实模型（如 {@code Tower/Upper}）也会自动建出结构节点。
 * 节点只存自己的完整路径，绝不缓存模型数据。
 */
final class SceneTreeBuilder {

    private SceneTreeBuilder() {
    }

    /**
     * 由全量模型路径构建整棵树。
     *
     * @param paths         场景全部模型的完整路径（场景 Map 的 keySet）
     * @param expandedPaths 需要展开的路径集合（含即展开）
     * @param expandAll     是否全展开（首次建树用 true）
     * @return 一个隐藏的根（哨兵），其子节点即各根路径
     */
    static TreeItem<String> build(Collection<String> paths, Collection<String> expandedPaths, boolean expandAll) {
        TreeItem<String> root = new TreeItem<>("");
        root.setExpanded(true);

        Map<String, TreeItem<String>> byPath = new HashMap<>();
        for (String path : paths) {
            if (path == null || path.isEmpty()) continue;
            buildChain(root, byPath, path, expandedPaths, expandAll);
        }
        return root;
    }

    /** 为一条完整路径逐段建节点（已存在的复用）。 */
    private static void buildChain(TreeItem<String> root, Map<String, TreeItem<String>> byPath,
                                   String path, Collection<String> expandedPaths, boolean expandAll) {
        TreeItem<String> parent = root;
        String nodePath = "";
        for (String segment : path.split("/")) {
            if (segment.isEmpty()) continue;
            nodePath = nodePath.isEmpty() ? segment : nodePath + "/" + segment;
            TreeItem<String> node = byPath.get(nodePath);
            if (node == null) {
                node = new TreeItem<>(nodePath);
                node.setExpanded(expandAll || expandedPaths.contains(nodePath));
                parent.getChildren().add(node);
                byPath.put(nodePath, node);
            }
            parent = node;
        }
    }
}
