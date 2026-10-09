package HCloudbyte.ui;

import mai_onsyn.renderer.interfaces.ModelInterface;
import mai_onsyn.renderer.interfaces.RendererInterface;

/**
 * 模型的树操作集合：每个方法 = 一次「弹窗/直接执行 → 调接口 → 通知重建」。
 *
 * <p>只做业务动作，不碰 UI 结构与树的细节；所有写操作成功后统一走 {@code onChanged} 触发全量重建。
 */
final class SceneModelOperations {

    private SceneModelOperations() {
    }

    /** 重命名：新名是叶名；接口侧已支持中间/分组路径。 */
    static void rename(RendererInterface renderer, String path, Runnable onChanged) {
        SceneDialogs.form("重命名", "重命名 " + path,
                new String[]{"新名称（叶名，不含 /）"}, new String[]{leafOf(path)},
                values -> {
                    String leaf = values[0].trim();
                    if (leaf.isEmpty() || leaf.contains("/")) throw new IllegalArgumentException("请输入不含 / 的名称");
                    renderer.getModel().renameModel(path, leaf);
                    onChanged.run();
                });
    }

    /** 复制：接口侧递归复制整棵子树。 */
    static void copy(RendererInterface renderer, String path, Runnable onChanged) {
        SceneDialogs.form("复制模型", "复制 " + path,
                new String[]{"新路径（必须不存在）"}, new String[]{path + "_Copy"},
                values -> {
                    String dst = values[0].trim();
                    if (dst.isEmpty()) throw new IllegalArgumentException("新路径不能为空");
                    renderer.getModel().copyModel(path, dst);
                    onChanged.run();
                });
    }

    /** 创建空分组：挂到当前节点下。 */
    static void createGroup(RendererInterface renderer, String path, Runnable onChanged) {
        SceneDialogs.form("创建空分组", "创建分组到 " + display(path),
                new String[]{"分组名"}, new String[]{""},
                values -> {
                    String name = values[0].trim();
                    if (name.isEmpty()) throw new IllegalArgumentException("分组名不能为空");
                    String full = path == null || path.isEmpty() ? name : path + "/" + name;
                    renderer.getModel().createEmptyModel(full);
                    onChanged.run();
                });
    }

    /** 保存：把当前模型（含整棵子树）导出为 .4do 文件。文件名可含目录，相对程序工作目录。 */
    static void save(RendererInterface renderer, String path, Runnable onChanged) {
        SceneDialogs.form("保存模型", "保存 " + display(path),
                new String[]{"文件名（含 .4do 后缀）"}, new String[]{leafOf(path) + ".4do"},
                values -> {
                    String fileName = values[0].trim();
                    if (fileName.isEmpty()) throw new IllegalArgumentException("文件名不能为空");
                    renderer.getIo().saveModel(path, fileName);
                    SceneDialogs.info("保存成功",
                            "已保存 " + display(path) + "\n文件位置："
                                    + new java.io.File(fileName).getAbsolutePath());
                });
    }

    /** 删除：接口侧删除该路径及其整棵子树。 */
    static void delete(RendererInterface renderer, String path, Runnable onChanged) {
        run(() -> renderer.getModel().removeModel(path), onChanged);
    }

    /** 合并：把当前模型与另一个模型合并成新模型。 */
    static void merge(RendererInterface renderer, String path, Runnable onChanged) {
        SceneDialogs.form("合并模型", "合并 " + path + " 与另一个模型",
                new String[]{"第二个模型路径", "目标路径（必须不存在）"}, new String[]{"", path + "_Merged"},
                values -> {
                    String src2 = values[0].trim();
                    String dst = values[1].trim();
                    if (src2.isEmpty() || dst.isEmpty()) throw new IllegalArgumentException("源2与目标路径不能为空");
                    renderer.getModel().mergeModel(path, src2, dst);
                    onChanged.run();
                });
    }

    /** 烘焙：把变换矩阵写进顶点，产出新模型。 */
    static void bake(RendererInterface renderer, String path, Runnable onChanged) {
        SceneDialogs.form("烘焙变换", "烘焙 " + path + " 的变换到顶点",
                new String[]{"目标路径（必须不存在）"}, new String[]{path + "_Baked"},
                values -> {
                    String dst = values[0].trim();
                    if (dst.isEmpty()) throw new IllegalArgumentException("目标路径不能为空");
                    renderer.getModel().applyTransformToVertex(path, dst);
                    onChanged.run();
                });
    }

    /** 合并全部子模型：作用于当前节点自身 + 全部后代。 */
    static void mergeAllSub(RendererInterface renderer, String path, Runnable onChanged) {
        SceneDialogs.form("合并全部子模型", "合并 " + path + " 下全部子模型",
                new String[]{"目标路径（必须不存在）"}, new String[]{path + "_AllMerged"},
                values -> {
                    String dst = values[0].trim();
                    if (dst.isEmpty()) throw new IllegalArgumentException("目标路径不能为空");
                    renderer.getModel().mergeAllSubModels(path, dst);
                    onChanged.run();
                });
    }

    /* ==================== 内部工具 ==================== */

    private static void run(Runnable operation, Runnable onChanged) {
        try {
            operation.run();
            onChanged.run();
        } catch (Exception e) {
            SceneDialogs.error("操作失败", e.getMessage());
        }
    }

    private static String leafOf(String path) {
        int i = path.lastIndexOf('/');
        return i < 0 ? path : path.substring(i + 1);
    }

    private static String display(String path) {
        return path == null || path.isEmpty() ? "场景根" : path;
    }

    /** 供 ShapeCreateDialog 复用的目标解析：同级 = 父级，下一级 = 自身。 */
    static String parentOf(String path) {
        if (path == null || path.isEmpty()) return "";
        int i = path.lastIndexOf('/');
        return i < 0 ? "" : path.substring(0, i);
    }
}
