package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONObject;
import mai_onsyn.renderer.interfaces.RendererInterface;
import weilantianhai.agent.ir.IRException;

import java.util.List;

public class ListCameraBookmarksOperation extends GuardedOperation {
    @Override
    public String name() {
        return "LIST_CAMERA_BOOKMARKS";
    }

    @Override
    protected void parse(JSONObject data) {
        // 无参数操作
    }

    @Override
    protected String run(RendererInterface renderer) throws IRException {
        List<HCloudbyte.ui.viewport.CameraRecord> records;
        try {
            records = FxThreads.call(() ->
                    HCloudbyte.ui.interfaces.UIInterface.getInstance().getCameraRecords());
        } catch (Exception e) {
            throw new IRException("UI_UNAVAILABLE", uiMessage(e));
        }

        if (records.isEmpty()) {
            return "当前没有任何机位。";
        }
        StringBuilder sb = new StringBuilder("共 ").append(records.size()).append(" 个机位：");
        for (int i = 0; i < records.size(); i++) {
            HCloudbyte.ui.viewport.CameraRecord r = records.get(i);
            sb.append("\n[").append(i).append("] ").append(r.getName())
              .append("  pos=").append(OperationParams.formatVector4(r.getPosition()));
        }
        return sb.toString();
    }

    /** 把 UI 侧异常（含未初始化 / 非 FX 环境）转成给 AI 看的简短原因。 */
    static String uiMessage(Exception e) {
        String msg = e.getMessage();
        return "机位功能不可用：" + (msg == null || msg.isBlank() ? e.getClass().getSimpleName() : msg);
    }
}
