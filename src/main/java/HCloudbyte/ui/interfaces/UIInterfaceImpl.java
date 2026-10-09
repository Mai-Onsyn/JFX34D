package HCloudbyte.ui.interfaces;

import HCloudbyte.ui.SceneOutliner;
import HCloudbyte.ui.ViewportOverlay;
import kotlin.Pair;
import mai_onsyn.renderer.utils.Coordinate4D;
import org.joml.Vector4f;

import java.util.List;

/**
 * UIInterface 的真实实现：把接口调用接到 UI 状态上。
 * <ul>
 *   <li>选中模型路径 → 场景集合树（SceneOutliner）当前选中节点；</li>
 *   <li>摄像机机位 → 视口左上角悬浮窗（ViewportOverlay）的机位列表。</li>
 * </ul>
 */
public class UIInterfaceImpl implements UIInterface {

    private final SceneOutliner outliner;
    private final ViewportOverlay overlay;

    public UIInterfaceImpl(SceneOutliner outliner, ViewportOverlay overlay) {
        this.outliner = outliner;
        this.overlay = overlay;
    }

    @Override
    public String getSelectedModelPath() {
        String p = outliner == null ? null : outliner.getSelectedPath();
        return p == null ? "" : p;
    }

    @Override
    public void addCameraRecord(Vector4f pos, Coordinate4D view) {
        overlay.addRecord(pos, view);
    }

    @Override
    public List<Pair<Vector4f, Coordinate4D>> getCameraRecords() {
        return overlay.getRecords();
    }

    @Override
    public void removeCameraRecord(int index) throws IndexOutOfBoundsException {
        overlay.removeRecord(index);
    }
}
