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

    /**
     * 解析 LLM 返回的 JSON，执行操作，拼装 Markdown 返回给 LLM。
     * 解析失败或执行失败不抛异常，返回 Error Markdown，让 LLM 重试。
     *
     * @param jsonText LLM 返回的原始 JSON 文本
     * @return 成功：拼接好的 Markdown（可能为空，所有操作都 no_result=true 时）
     *         done：返回 "# Done"
     *         失败：Error Markdown
     */
    public String execute(String jsonText) throws IRException {
        IRBundle bundle;
        try{
            bundle = parser.parseAsBundle(jsonText);
        }catch(IRException e){
            return ResultFormatter.error(jsonText,e.getCode()+":"+e.getMessage());
        }

        if(bundle.isDone()){
            return DONE_MARKER;
        }

        StringBuilder sb = new StringBuilder();
        for(IRCommand cmd : bundle.getOperations()){
            String body;
            try{
                body = cmd.getImpl().execute(renderer);
            }catch(IRException e){
                return ResultFormatter.error(jsonText,
                        "executing"+cmd.getType()+"(id="+cmd.getId()+"):"
                        +e.getCode()+":"+e.getMessage()
                );
            }
            if(!cmd.isNoResult()){
                sb.append(ResultFormatter.success(cmd,body)).append("\n\n");
            }

            // 操作之间延迟 3 秒，让渲染有过渡效果
            try {
                Thread.sleep(3000);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return sb.toString().trim();
    }
}
