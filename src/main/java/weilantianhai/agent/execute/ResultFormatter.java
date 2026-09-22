package weilantianhai.agent.execute;

import weilantianhai.agent.ir.IRCommand;

public class ResultFormatter {
    private ResultFormatter(){}

    /** 成功片段： # TYPE(id=X)\n主体 */
    public static String success(IRCommand cmd,String body){
        return "# " + cmd.getType() + "(id=" + cmd.getId() + ")\n" +body;
    }

    /** 解析失败或格式错误 */
    public static String error(String rawJson, String reason){
        return "# Error\n"
                +"You just return an error format:\n"
                +"```json\n" + rawJson + "\n```\n"
                +"Reason:\n"
                +"```markdown\n" + reason + "\n```";
    }
}
