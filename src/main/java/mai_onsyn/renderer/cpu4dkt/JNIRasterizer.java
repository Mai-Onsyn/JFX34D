package mai_onsyn.renderer.cpu4dkt;

public class JNIRasterizer {
    public static native float[] process(float[] triangles, int count);

    static {
        System.loadLibrary("./jni/jfx34d_jni");
    }
}
