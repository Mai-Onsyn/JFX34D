package mai_onsyn.renderer.cpu4dkt;

import mai_onsyn.renderer.ogl3d.data.Tetrahedron3D;

import java.util.ArrayList;
import java.util.List;

public class JNIRasterizer {
    public static native float[] project(
        float[] tetrahedrons,
        int count,                  // tetrahedron count
        float[] modelMatrix,        // 5*5
        float[] viewMatrix,         // 5*5
        float[] projectionMatrix,   // 5*5
        float[] viewPortMatrix      // 4*4
    );

    static {
        List<String> paths = List.of(
                "./jni/jfx34d_jni",
                "./jfx34d_jni"
        );
        boolean loaded = false;
        for (String p : paths) {
            try {
                System.loadLibrary(p);
                loaded = true;
            } catch (UnsatisfiedLinkError ignored) {}
        }
        if (!loaded) {
            throw new ExceptionInInitializerError("Cannot find native library: jfx34d_jni");
        }
    }

    public static float[] packMesh4D(Mesh4D mesh) {
        List<Tetrahedron> tetrahedrons = mesh.getTetrahedrons();
        float[] result = new float[tetrahedrons.size() * 36];

        int writeIndex = 0;
        for (Tetrahedron tetrahedron : tetrahedrons) {
            tetrahedron.pack(result, writeIndex);
            writeIndex += 36;
        }

        return result;
    }

    public static Mesh4D extractMesh4D(float[] arr) {
        List<Tetrahedron> tetrahedrons = new ArrayList<>(arr.length / 36);

        for (int i = 0; i < arr.length; i += 36) {
            tetrahedrons.add(Tetrahedron.Companion.extract(arr, i));
        }
        return new Mesh4D(tetrahedrons);
    }

    public static List<Tetrahedron3D> extractTetrahedrons(float[] arr) {
        List<Tetrahedron3D> tetrahedrons = new ArrayList<>(arr.length / 28);

        for (int i = 0; i < arr.length; i += 28) {
            tetrahedrons.add(Tetrahedron3D.Companion.extract(arr, i));
        }
        return tetrahedrons;
    }
}
