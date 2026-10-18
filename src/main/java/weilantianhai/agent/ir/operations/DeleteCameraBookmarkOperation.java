package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONObject;
import mai_onsyn.renderer.interfaces.RendererInterface;
import weilantianhai.agent.ir.IRException;

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
