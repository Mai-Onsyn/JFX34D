package weilantianhai.agent.execute;

import mai_onsyn.renderer.interfaces.RendererInterface;
import weilantianhai.agent.ir.IRBundle;
import weilantianhai.agent.ir.IRCommand;
import weilantianhai.agent.ir.IRException;
import weilantianhai.agent.ir.resolver.IRParser;

public class CommandExecutor {
    /** done=true 时返回的结束标记 */
    public static final String DONE_MARKER = "# Done";

    private final IRParser parser;
    private final RendererInterface renderer;

    public CommandExecutor(RendererInterface renderer){
        this.parser = new IRParser();
        this.renderer = renderer;
    }

    public String execute(String jsonText) throws IRException {
        IRBundle bundle;
        try{
            bundle = parser.parseAsBundle(jsonText);
        }catch(IRException e){
            return ResultFormatter.error(jsonText,e.getCode()+":"+e.getMessage());
        }
        return execute(bundle, jsonText);
    }

    public String execute(IRBundle bundle, String rawJson) throws IRException {
        if(bundle.isDone()){
            return DONE_MARKER;
        }

        StringBuilder sb = new StringBuilder();
        for(IRCommand cmd : bundle.getOperations()){
            String body;
            try{
                body = cmd.getImpl().execute(renderer);
            }catch(IRException e){
                return ResultFormatter.error(rawJson,
                        "executing"+cmd.getType()+"(id="+cmd.getId()+"):"
                        +e.getCode()+":"+e.getMessage()
                );
            }
            if(!cmd.isNoResult()){
                sb.append(ResultFormatter.success(cmd,body)).append("\n\n");
            }

            // 操作之间延迟 3 秒，让渲染有过渡效果
//            try {
//                Thread.sleep(3000);
//            } catch (InterruptedException ie) {
//                Thread.currentThread().interrupt();
//                break;
//            }
        }
        return sb.toString().trim();
    }
}
