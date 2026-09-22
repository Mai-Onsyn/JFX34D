import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import mai_onsyn.renderer.core.GL4DRegion;
import mai_onsyn.renderer.cpu4dkt.Mesh4D;
import mai_onsyn.renderer.cpu4dkt.generator.HypercubeKt;
import mai_onsyn.renderer.interfaces.RendererInterface;

public class AgentDisplayTest extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        GL4DRegion region = new GL4DRegion();
        region.setOutlineRendering(true);
        region.getScene4D().getMeshList().add(new Mesh4D(HypercubeKt.constructHypercubeWithCellColors()));
        RendererInterface.Companion.init(region.getScene4D().getCamera());


        stage.setScene(new Scene(region, 800, 600));
        stage.show();
    }
}