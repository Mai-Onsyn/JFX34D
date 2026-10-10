package HCloudbyte.ui.scenetree;

import mai_onsyn.renderer.cpu4dkt.Mesh4D;
import mai_onsyn.renderer.cpu4dkt.MeshKind;
import mai_onsyn.renderer.interfaces.ModelInterface;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * 场景模型只读门面：把「某路径是不是真实模型」「它的类型/可见性」这类查询收口在这里。
 *
 * <p>树节点只存路径字符串，不持有任何 {@link Mesh4D}；需要模型信息时都走这里。
 * 真实模型通过 {@link ModelInterface#getModel(String)} 取，取不到（结构节点）按 null 处理。
 */
final class SceneModelAccess {

    private final ModelInterface model;
    /** 当前场景全部真实模型的完整路径（每次重建树时刷新）。 */
    private volatile Set<String> realPaths = Set.of();

    SceneModelAccess(ModelInterface model) {
        this.model = model;
    }

    /** 每次重建树时同步一次真实路径集合。 */
    void updateKeys(Collection<String> paths) {
        this.realPaths = new HashSet<>(paths);
    }

    /** 该路径是否是真实存在的模型（而非中间结构节点）。 */
    boolean isRealModel(String path) {
        return realPaths.contains(path);
    }

    /** 真实模型的种类；结构节点返回 null（表示"无实体"）。 */
    MeshKind kindOf(String path) {
        try {
            return model.getModel(path).getKind();
        } catch (Exception e) {
            return null;
        }
    }

    /** 真实模型的可见性；结构节点返回 true。 */
    boolean visibleOf(String path) {
        try {
            return model.getModel(path).getVisible();
        } catch (Exception e) {
            return true;
        }
    }

    /** 切换可见性（接口层已支持中间/分组路径）。 */
    void setVisible(String path, boolean visible) {
        model.setModelVisible(path, visible);
    }
}
