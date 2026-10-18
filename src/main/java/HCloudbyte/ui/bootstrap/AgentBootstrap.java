package HCloudbyte.ui.bootstrap;

import mai_onsyn.renderer.interfaces.RendererInterface;
import weilantianhai.agent.execute.CommandExecutor;
import weilantianhai.agent.interfaces.AgentInterface;
import weilantianhai.agent.llm.LLMClient;

public final class AgentBootstrap {
    private AgentBootstrap() {
    }

    /** 引导结果：是否就绪 + 失败异常（就绪时为 null）。 */
    public record Result(boolean ready, Exception error) {
    }

    /** 初始化并启动 Agent worker 线程（必须 start 才会消费指令队列）。 */
    public static Result init(RendererInterface renderer) {
        try {
            AgentInterface.initialize(LLMClient.deepSeek(), new CommandExecutor(renderer));
            AgentInterface.getInstance().start();
            return new Result(true, null);
        } catch (Exception e) {
            return new Result(false, e);
        }
    }
}
