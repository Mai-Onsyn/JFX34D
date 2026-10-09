package HCloudbyte.ui.interfaces;

import HCloudbyte.ui.SceneOutliner;
import HCloudbyte.ui.ViewportOverlay;
import kotlin.Pair;
import mai_onsyn.renderer.utils.Coordinate4D;
import org.joml.Vector4f;

import java.util.List;

public interface UIInterface {

    /**
     * 获取UI中的模型列表当前选中的模型的路径
     * @return 选中的模型在场景中的路径（如果是纯逻辑节点也会返回，注意判断）
     */
    String getSelectedModelPath();

    /**
     * 向UI添加一个用于快速移动摄像机的摄像机机位
     * @param pos 摄像机位置
     * @param view 摄像机坐标系的四条坐标轴
     */
    void addCameraRecord(Vector4f pos, Coordinate4D view);

    /**
     * 获取UI中的所有摄像机机位
     * @return 机位列表（按UI显示顺序）
     */
    List<Pair<Vector4f, Coordinate4D>> getCameraRecords();

    /**
     * 删除一个摄像机机位
     * @param index 在机位列表的位置
     * @throws IndexOutOfBoundsException 如果超出列表范围
     */
    void removeCameraRecord(int index) throws IndexOutOfBoundsException;

    /**
     * 初始化：把接口实现绑定到场景集合树（读选中路径）与视口左上角悬浮窗（机位列表）。
     * 必须在 UI 构建完成后调用一次。
     */
    static void initialize(SceneOutliner outliner, ViewportOverlay overlay) {
        _UIInterface.INSTANCE = new UIInterfaceImpl(outliner, overlay);
    }

    static UIInterface getInstance() {
        if (_UIInterface.INSTANCE == null) throw new ExceptionInInitializerError("UIInterface instance is not initialized");
        else return _UIInterface.INSTANCE;
    }
}

class _UIInterface {
    static UIInterface INSTANCE;
}