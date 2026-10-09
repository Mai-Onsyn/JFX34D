package HCloudbyte.ui;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import mai_onsyn.renderer.interfaces.RendererInterface;

/**
 * 节点右键菜单：把「菜单项 → 操作」简单地接线起来。
 *
 * <p>启停规则：只要底层接口支持该路径（含中间/分组路径）就启用；
 * 只有「合并 / 烘焙」要求源是一个真实模型，故对结构节点禁用。
 */
final class SceneItemMenu {

    private SceneItemMenu() {
    }

    static ContextMenu build(RendererInterface renderer, SceneModelAccess access, String path, Runnable onChanged) {
        MenuItem rename = item("重命名", () -> SceneModelOperations.rename(renderer, path, onChanged));
        MenuItem copy = item("复制", () -> SceneModelOperations.copy(renderer, path, onChanged));
        MenuItem addShape = item("添加形状", () -> ShapeCreateDialog.show(renderer, path, onChanged));
        MenuItem delete = item("删除", () -> SceneModelOperations.delete(renderer, path, onChanged));
        MenuItem merge = item("合并", () -> SceneModelOperations.merge(renderer, path, onChanged));
        MenuItem bake = item("烘焙变换", () -> SceneModelOperations.bake(renderer, path, onChanged));
        MenuItem mergeAll = item("合并全部子模型", () -> SceneModelOperations.mergeAllSub(renderer, path, onChanged));
        MenuItem createGroup = item("创建空分组", () -> SceneModelOperations.createGroup(renderer, path, onChanged));

        boolean real = access.isRealModel(path);
        merge.setDisable(!real);   // 合并、烘焙要求源是真实模型
        bake.setDisable(!real);

        ContextMenu menu = new ContextMenu();
        menu.getItems().addAll(rename, copy, addShape, delete, merge, bake, mergeAll,
                new SeparatorMenuItem(), createGroup);
        return menu;
    }

    private static MenuItem item(String text, Runnable action) {
        MenuItem menuItem = new MenuItem(text);
        menuItem.setOnAction(e -> action.run());
        return menuItem;
    }

    /** 场景根菜单（右键树空白区域）：只在根下新建，删空后仍可继续添加。 */
    static ContextMenu buildRoot(RendererInterface renderer, Runnable onChanged) {
        MenuItem addShape = item("添加形状", () -> ShapeCreateDialog.show(renderer, "", onChanged));
        MenuItem createGroup = item("创建空分组", () -> SceneModelOperations.createGroup(renderer, "", onChanged));
        ContextMenu menu = new ContextMenu();
        menu.getItems().addAll(addShape, createGroup);
        return menu;
    }
}
