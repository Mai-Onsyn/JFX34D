import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import mai_onsyn.renderer.core.GL4DRegion;
import mai_onsyn.renderer.cpu4dkt.Mesh4D;
import mai_onsyn.renderer.cpu4dkt.generator.HypercubeKt;
import mai_onsyn.renderer.interfaces.RendererInterface;

import weilantianhai.agent.execute.CommandExecutor;
import weilantianhai.agent.llm.LLMClient;

public class agentTest extends Application {

    private static final String SYSTEM_PROMPT = """
你是 JFX34D 四维建模助手 Agent，负责把用户的自然语言指令翻译成四维操作 JSON。

只输出一个 JSON 对象，不要解释文字，不要 markdown 代码块。

## 输出格式

{"version":1,"done":false,"operations":[
  {"type":"MOVE_CAMERA_POS","id":1,"no_result":false,
   "data":{"axis":"x","distance":5.0}}
]}

## 可用操作

### MOVE_CAMERA_POS：摄像机沿单轴移动
data = { axis: "x"|"y"|"z"|"w", distance: float }

轴与方向的对应关系：
- x 轴：右（+x）/ 左（-x）
- y 轴：上（+y）/ 下（-y）
- z 轴：前（+z）/ 后（-z）
- w 轴：第四维正方向（+w）/ 反方向（-w）

distance 为正数表示沿正方向，负数表示反方向。
例如“向左 3” = axis:"x", distance:-3。

## 规则

- id 从 1 开始递增，同一次返回中必须唯一。
- no_result 默认 true。只有需要看到执行结果的操作才设 false。
- 本轮对话结束时返回 {"version":1,"done":true,"operations":[]}。
- 不要编造不存在的操作类型。
- 不要用 markdown 代码块包裹输出。

## 示例

示例 1：单个操作
用户：向右移动 5
输出：{"version":1,"done":false,"operations":[{"type":"MOVE_CAMERA_POS","id":1,"no_result":false,"data":{"axis":"x","distance":5.0}}]}

示例 2：多个操作，放在同一次返回里
用户：向上移动 2，然后沿第四维反方向移动 1
输出：{"version":1,"done":false,"operations":[{"type":"MOVE_CAMERA_POS","id":1,"no_result":false,"data":{"axis":"y","distance":2.0}},{"type":"MOVE_CAMERA_POS","id":2,"no_result":false,"data":{"axis":"w","distance":-1.0}}]}

示例 3：用户输入不需要执行任何操作
用户：你好
输出：{"version":1,"done":true,"operations":[]}
            """;

    @Override
    public void start(Stage stage) throws Exception {
        GL4DRegion region = new GL4DRegion();
        region.setOutlineRendering(true);
        region.getScene4D().getMeshList().add(new Mesh4D(HypercubeKt.constructHypercubeWithCellColors()));
        RendererInterface.Companion.init(region.getScene4D().getCamera());

        stage.setScene(new Scene(region, 800, 600));
        stage.show();

        // 窗口起来后，起后台线程跑 Agent
        Thread agentThread = new Thread(this::runAgent, "agent-test");
        agentThread.setDaemon(true);
        agentThread.start();
    }

    private void runAgent() {
        try {
            Thread.sleep(500);   // 给窗口留一点就绪时间

            LLMClient client = new LLMClient();
            CommandExecutor executor = new CommandExecutor(RendererInterface.Companion.getINSTANCE());

            String userInput = "向上5，向左3，向后4，第四维正方向3，向后3，向右7";
            System.out.println("=== 用户输入 ===\n" + userInput);

            String json = client.chat(SYSTEM_PROMPT, userInput);
            System.out.println("\n=== LLM JSON ===\n" + json);

            String md = executor.execute(json);
            System.out.println("\n=== Markdown ===\n" + md);

        } catch (Throwable t) {
            System.out.println("\n!!! Agent 执行失败 !!!");
            t.printStackTrace(System.out);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}