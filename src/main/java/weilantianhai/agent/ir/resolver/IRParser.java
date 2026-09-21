package weilantianhai.agent.ir.resolver;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import weilantianhai.agent.config.AppConfig;
import weilantianhai.agent.ir.IOperation;
import weilantianhai.agent.ir.IRCommand;
import weilantianhai.agent.ir.IRException;

import java.util.ArrayList;
import java.util.List;

public class IRParser {
    private final OperationRegistry registry = new OperationRegistry();

    public IRBundle pares(String jsonText) throws IRException {
        JSONObject root;
        try {
            root = JSON.parseObject(jsonText);
        }catch (Exception e){
            throw new IRException("INVALID_JSON","不是合法 json："+ e.getMessage(),e);
        }
        if(root == null){
            throw new IRException("INVALID_JSON","JSON 解析为空");
        }

        int version = root.getIntValue("version",-1);
        if(version !=1){
            throw new IRException("UNSUPPORTED_VERSION","不支持的 version："+ version);
        }

        JSONArray opsJson = root.getJSONArray("operations");
        if(opsJson == null || opsJson.isEmpty()){
            throw new IRException("EMPTY_OPERATIONS","operations 不能为空");
        }
        if(opsJson.size() > AppConfig.MAX_OPERATIONS){
            throw new IRException("TOO_MANY_OPERATIONS","操作数超过上限："+AppConfig.MAX_OPERATIONS);
        }

        List<IRCommand> commands = new ArrayList<>();
        for(int i = 0; i < opsJson.size(); i++){
            JSONObject opJson = opsJson.getJSONObject(i);
            commands.add(parseOne(opJson,i));
        }

        IRBundle bundle = new IRBundle();
        bundle.setVersion(version);
        bundle.setOperations(commands);
        return bundle;
    }

    private IRCommand parseOne(JSONObject opJson, int index) throws IRException {
        String type = opJson.getString("type");
        if(type ==null || type.isEmpty()){
            throw new IRException("MISSING_TYPE","第"+ index + "条操作缺少 type");
        }

        int id = opJson.getIntValue("id",-1);

        if(id<0){
            throw new IRException("MISSING_ID","第"+ index + "条操作缺少合法id");
        }

        boolean noResult = opJson.getBooleanValue("no_result", true);
        JSONObject data = opJson.getJSONObject("data");
        if(data == null){
            data = new JSONObject();
        }
        IOperation impl = registry.creat(type);
        impl.load(data);

        IRCommand cmd = new IRCommand();
        cmd.setType(type);
        cmd.setId(id);
        cmd.setNoResult(noResult);
        cmd.setImpl(impl);
        return cmd;
    }
}
