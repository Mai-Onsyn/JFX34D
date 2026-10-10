package HCloudbyte.ui.interfaces;

import HCloudbyte.ui.scenetree.SceneTree;
import HCloudbyte.ui.viewport.CameraBookmarksView;
import HCloudbyte.ui.viewport.CameraRecord;

import java.util.List;

/**
 * {@link UIInterface} 的真实实现：把接口调用接到 UI 状态上。
 * <ul>
 *   <li>选中模型路径 → 场景集合树（{@link SceneTree}）当前选中节点；</li>
 *   <li>摄像机机位 → 视口机位悬浮窗（{@link CameraBookmarksView}）的机位列表。</li>
 * </ul>
 */
public class UIInterfaceImpl implements UIInterface {

    private final SceneTree sceneTree;
    private final CameraBookmarksView overlay;

    public UIInterfaceImpl(SceneTree sceneTree, CameraBookmarksView overlay) {
        this.sceneTree = sceneTree;
        this.overlay = overlay;
    }

    @Override
    public String getSelectedModelPath() {
        String path = sceneTree == null ? null : sceneTree.getSelectedPath();
        return path == null ? "" : path;
    }

    @Override
    public void addCameraRecord(CameraRecord record) {
        overlay.addRecord(record);
    }

    @Override
    public List<CameraRecord> getCameraRecords() {
        return overlay.getRecords();
    }

    @Override
    public void renameCameraRecord(int index, String name) throws IndexOutOfBoundsException {
        overlay.renameRecord(index, name);
    }

    @Override
    public void removeCameraRecord(int index) throws IndexOutOfBoundsException {
        overlay.removeRecord(index);
    }
}
