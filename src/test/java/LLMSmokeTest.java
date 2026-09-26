import com.alibaba.fastjson2.JSON;
import weilantianhai.agent.ir.IRBundle;
import weilantianhai.agent.ir.IRCommand;
import weilantianhai.agent.ir.IRException;
import weilantianhai.agent.ir.resolver.IRParser;
import weilantianhai.agent.llm.LLMClient;
import weilantianhai.agent.llm.PromptLoader;

public class LLMSmokeTest {

    public static void main(String[] args) throws Exception {
        // 1. 读提示词
        String systemPrompt = PromptLoader.loadSystemPrompt();
        System.out.println("=== 提示词已加载，长度 " + systemPrompt.length() + " ===");

        // 2. 建 client（改成本地或 DeepSeek）
        LLMClient client = LLMClient.deepSeek();          // 本地 LM Studio
        // LLMClient client = LLMClient.deepSeek();    // 或用 DeepSeek

        // 3. 调 LLM
        String userInput = "向右移动 5 个单位";
        System.out.println("\n=== 用户输入 ===\n" + userInput);

        String json = client.chat(systemPrompt, userInput);
        System.out.println("\n=== LLM 返回 JSON ===\n" + json);

        // 4. 解析
        try {
            IRBundle bundle = new IRParser().parseAsBundle(json);
            System.out.println("\n=== 解析结果 ===");
            System.out.println("version=" + bundle.getVersion());
            System.out.println("done=" + bundle.isDone());
            for (IRCommand cmd : bundle.getOperations()) {
                System.out.println("  type=" + cmd.getType()
                        + " id=" + cmd.getId()
                        + " noResult=" + cmd.isNoResult());
            }
        } catch (IRException e) {
            System.out.println("\n=== 解析失败 ===");
            System.out.println(e.getCode() + ": " + e.getMessage());
        }
    }
}