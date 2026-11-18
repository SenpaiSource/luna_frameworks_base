package android.hardware.display;

public class OplusDisplayManager {
    private static final OplusDisplayManager sInstance = new OplusDisplayManager();

    public static OplusDisplayManager getInstance() {
        return sInstance;
    }

    public float getCurrentScreenBrightness(int displayId) {
        return 0.0f;
    }

    public float getNitFromBrightness(int displayId, float brightness) {
        return brightness;
    }

    public float getBrightnessFromNit(int displayId, float nit) {
        return nit;
    }
}
