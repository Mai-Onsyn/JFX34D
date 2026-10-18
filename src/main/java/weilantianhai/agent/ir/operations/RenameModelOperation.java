package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONObject;
import mai_onsyn.renderer.interfaces.RendererInterface;
import weilantianhai.agent.ir.IRException;

public class RenameModelOperation extends GuardedOperation {
    private String path;
    private String newName;

    @Override
    public String name() {
        return "RENAME_MODEL";
    }

    @Override
    protected void parse(JSONObject data) throws IRException {
        this.path = OperationParams.getRequiredPath(data, "path", "PATH");
        this.newName = OperationParams.getRequiredString(data, "newName", "NEW_NAME");
    }

    @Override
    protected String run(RendererInterface renderer) {
        renderer.getModel().renameModel(path, newName);
        String newPath = ModelQuery.renamedPath(path, newName);
        if (newPath.equals(path)) {
            return "Successfully rename \"" + path + "\" to \"" + newPath + "\" (unchanged)";
        }
        return "Successfully rename \"" + path + "\" to \"" + newPath + "\"";
    }
}
