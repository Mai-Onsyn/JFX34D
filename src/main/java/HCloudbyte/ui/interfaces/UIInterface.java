package HCloudbyte.ui.interfaces;

import HCloudbyte.ui.scenetree.SceneTree;
import HCloudbyte.ui.viewport.CameraBookmarksView;
import HCloudbyte.ui.viewport.CameraRecord;

import java.util.List;

/**
 * UI 对外接口：把「选中模型路径」与「摄像机机位」暴露给外部（主要是 agent 侧）。
 * 真实实现见 {@link UIInterfaceImpl}。
 */
public interface UIInterface {

    /**
     * 获取 UI 中模型列表当前选中的模型的路径。
     *
     * @return 选中的模型在场景中的路径（如果是纯逻辑节点也会返回，注意判断）
     */
    String getSelectedModelPath();

    /**
     * 向 UI 添加一个摄像机机位。
     * 机位自带名称、位置、视线（见 {@link CameraRecord}）。
     *
     * @param record 机位；其 position / view 会被深拷贝，外部后续改动不影响 UI
     */
    void addCameraRecord(CameraRecord record);

    /**
     * 获取 UI 中的所有摄像机机位。
     *
     * @return 机位列表（按 UI 显示顺序，元素为副本）
     */
    List<CameraRecord> getCameraRecords();

    /**
     * 记录当前相机状态为一个机位（等价于 UI 上的「记录」按钮，但可自定义名称）。
     *
     * @param name 机位名称；为 null / 空白时自动生成 "机位N"
     * @return 新机位在列表中的下标
     */
    int captureCameraRecord(String name);

    /**
     * 应用（跳转）一个摄像机机位：把相机切到该机位的位置与视线。
     *
     * <p>与鼠标左键点击机位行等价。底层会触发摄像机的平滑过渡动画（约 1 秒）。
     *
     * @param index 在机位列表的位置
     * @throws IndexOutOfBoundsException 如果超出列表范围
     */
    void applyCameraRecord(int index) throws IndexOutOfBoundsException;

    /**
     * 重命名一个摄像机机位。
     *
     * @param index 在机位列表的位置
     * @param name  新名称
     * @throws IndexOutOfBoundsException 如果超出列表范围
     */
    void renameCameraRecord(int index, String name) throws IndexOutOfBoundsException;

    /**
     * 删除一个摄像机机位。
     *
     * @param index 在机位列表的位置
     * @throws IndexOutOfBoundsException 如果超出列表范围
     */
    void removeCameraRecord(int index) throws IndexOutOfBoundsException;

    /**
     * 初始化：把接口实现绑定到场景集合树（读选中路径）与视口机位悬浮窗。
     * 必须在 UI 构建完成后调用一次。
     */
    static void initialize(SceneTree sceneTree, CameraBookmarksView overlay) {
        Holder.INSTANCE = new UIInterfaceImpl(sceneTree, overlay);
    }

    static UIInterface getInstance() {
        if (Holder.INSTANCE == null) {
            throw new ExceptionInInitializerError("UIInterface instance is not initialized");
        }
        return Holder.INSTANCE;
    }

    /** 单例持有者（接口内嵌静态类，避免额外文件）。 */
    final class Holder {
        static UIInterface INSTANCE;
    }
}
