import org.junit.jupiter.api.Test;
import weilantianhai.agent.ir.IRBundle;
import weilantianhai.agent.ir.resolver.IRParser;
import weilantianhai.agent.llm.LLMClient;

public class LLMConnectTest {
    @Test
    public void testConnect() throws Exception {
        LLMClient client = new LLMClient();

        String systemPrompt = """
                你是一个四维摄像机控制助手。
                用户会用自然语言描述摄像机移动，你必须直接输出纯 JSON，不要用 markdown 代码块包裹，不要加任何解释文字：
                
                {
                  "version": "1.0",
                  "operations": [
                    {"op": "move", "direction": "right", "distance": 1.0}
                  ]
                }
                
                方向只能是：right(x+), up(y+), ana(z+), forward(w+)。
                距离为正数表示沿正方向，负数表示反方向。
                """;

        String userInput = "向右移动1个单位";

        System.out.println("=== 发送请求 ===");
        String result = client.chat(systemPrompt, userInput);
        IRParser parser = new IRParser();
        IRBundle bundle = parser.parseAsBundle(result);

    }
}

