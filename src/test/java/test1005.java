import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import mai_onsyn.renderer.core.GL4DRegion;
import mai_onsyn.renderer.cpu4dkt.Mesh4D;
import mai_onsyn.renderer.cpu4dkt.generator.HypercubeKt;
import mai_onsyn.renderer.interfaces.RendererInterface;
import weilantianhai.agent.execute.CommandExecutor;
import weilantianhai.agent.interfaces.AgentInterface;
import weilantianhai.agent.llm.LLMClient;

public class test1005 {

    public static void main(String[] args) {
        Application.launch(FxApp.class, args);
    }

    public static class FxApp extends Application {
        @Override
        public void start(Stage stage) throws Exception {
            // 渲染器
            GL4DRegion region = new GL4DRegion();
            region.setOutlineRendering(true);
            region.getScene4D().getMeshList().add(
                    new Mesh4D(HypercubeKt.constructHypercubeWithCellColors()));
            RendererInterface.Companion.init(region);
            stage.setScene(new Scene(region, 800, 600));
            stage.show();

            // Agent
            AgentInterface.initialize(
                    LLMClient.deepSeek(),
                    new CommandExecutor(RendererInterface.Companion.getINSTANCE())
            );
            AgentInterface agent = AgentInterface.getInstance();
            agent.start();

            // 后台跑队列测试
            new Thread(() -> {
                try {
                    System.out.println(">>> 提交 3 条 <<<");
                    System.out.println("id1 = " + agent.submitUserInput("向右移动 5"));
                    System.out.println("id2 = " + agent.submitUserInput("向上移动 2"));
                    System.out.println("id3 = " + agent.submitUserInput("向左移动 1"));

                    Thread.sleep(150);

                    System.out.println("\n>>> 队列状态 <<<");
                    System.out.println("pending = " + agent.getPendingCount());
                    System.out.println("busy    = " + agent.isBusy());
                    System.out.println("summary = " + agent.getStatusSummary());

                    System.out.println("\n>>> 清空 <<<");
                    System.out.println("cleared = " + agent.clearPending());
                    System.out.println("after clear, pending = " + agent.getPendingCount());

                    Thread.sleep(15000);
                    Platform.exit();
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }, "queue-test").start();
        }
    }
}