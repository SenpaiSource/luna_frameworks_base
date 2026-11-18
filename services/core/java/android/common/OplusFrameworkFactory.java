package android.common;

public class OplusFrameworkFactory {
    private static final OplusFrameworkFactory sInstance = new OplusFrameworkFactory();

    public static OplusFrameworkFactory getInstance() {
        return sInstance;
    }

    @SuppressWarnings("unchecked")
    public <T> T getFeature(T defaultInstance, Object... args) {
        return defaultInstance;
    }
}
