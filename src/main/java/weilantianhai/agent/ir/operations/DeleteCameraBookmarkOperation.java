package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONObject;
import mai_onsyn.renderer.interfaces.RendererInterface;
import weilantianhai.agent.ir.IRException;

/**
 * DELETE_CAMERA_BOOKMARK：删除一个机位。
 *
 * <p>参数：{@code name} 或 {@code index}（二选一，name 优先）。
 *
 * <p>删除后，<b>其后所有机位的下标会前移一位</b>——连续操作多个机位时，
 * 建议一次性用名字定位，或每删一个都重新 {@code LIST_CAMERA_BOOKMARKS}。
 */
public class DeleteCameraBookmarkOperation extends GuardedOperation {

    private JSONObject data;

    @Override
    public String name() {
        return "DELETE_CAMERA_BOOKMARK";
    }

    @Override
    protected void parse(JSONObject data) {
        this.data = data == null ? new JSONObject() : data;
    }

    @Override
    protected String run(RendererInterface renderer) throws IRException {
        int index = CameraBookmarkResolver.resolve(data, name());
        try {
            return FxThreads.call(() -> {
                var ui = HCloudbyte.ui.interfaces.UIInterface.getInstance();
                String removed = ui.getCameraRecords().get(index).getName();
                ui.removeCameraRecord(index);
                return "Success\n已删除机位 [" + index + "] " + removed
                        + "\n剩余 " + ui.getCameraRecords().size() + " 个机位";
            });
        } catch (Exception e) {
            throw new IRException("DELETE_CAMERA_BOOKMARK_FAILED",
                    ListCameraBookmarksOperation.uiMessage(e));
        }
    }
}
