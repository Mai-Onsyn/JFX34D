package HCloudbyte.ui.interfaces;

import HCloudbyte.ui.scenetree.SceneTree;
import HCloudbyte.ui.viewport.CameraBookmarksView;
import HCloudbyte.ui.viewport.CameraRecord;

import java.util.List;

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
        overlay().addRecord(record);
    }

    @Override
    public int captureCameraRecord(String name) {
        CameraBookmarksView view = overlay();
        String finalName = (name == null || name.isBlank())
                ? "机位" + (view.getRecords().size() + 1)
                : name.trim();
        int index = view.getRecords().size();
        view.addRecord(view.capture(finalName));
        return index;
    }

    @Override
    public List<CameraRecord> getCameraRecords() {
        return overlay().getRecords();
    }

    @Override
    public void applyCameraRecord(int index) throws IndexOutOfBoundsException {
        overlay().applyRecord(index);
    }

    @Override
    public void renameCameraRecord(int index, String name) throws IndexOutOfBoundsException {
        overlay().renameRecord(index, name);
    }

    @Override
    public void removeCameraRecord(int index) throws IndexOutOfBoundsException {
        overlay().removeRecord(index);
    }

    /** 机位悬浮窗；未绑定时给出明确错误，而不是让调用方吃 NPE。 */
    private CameraBookmarksView overlay() {
        if (overlay == null) {
            throw new IllegalStateException("机位悬浮窗尚未初始化（overlay == null）");
        }
        return overlay;
    }
}
