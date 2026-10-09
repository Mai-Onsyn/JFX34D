package HCloudbyte.ui.interfaces;

import kotlin.Pair;
import mai_onsyn.renderer.utils.Coordinate4D;
import org.joml.Vector4f;

import java.util.List;

public class UIInterfaceImpl implements UIInterface {

    public UIInterfaceImpl(
            // ...
    ) {
        // ...
    }

    @Override
    public String getSelectedModelPath() {
        return "";
    }

    @Override
    public void addCameraRecord(Vector4f pos, Coordinate4D view) {

    }

    @Override
    public List<Pair<Vector4f, Coordinate4D>> getCameraRecords() {
        return List.of();
    }

    @Override
    public void removeCameraRecord(int index) throws IndexOutOfBoundsException {

    }
}
