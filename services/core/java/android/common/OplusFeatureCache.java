package android.common;

import java.util.concurrent.ConcurrentHashMap;

public class OplusFeatureCache {
    private static final ConcurrentHashMap<Class<?>, Object> sFeatureMap = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public static <T extends IOplusCommonFeature> T getOrCreate(T defaultInstance, Object... args) {
        if (defaultInstance == null) {
            return null;
        }
        Object instance = sFeatureMap.get(defaultInstance.getClass());
        if (instance != null) {
            return (T) instance;
        }
        return defaultInstance;
    }

    public static <T> void set(Class<T> clazz, T instance) {
        if (instance != null) {
            sFeatureMap.put(clazz, instance);
        } else {
            sFeatureMap.remove(clazz);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T get(Class<T> clazz) {
        return (T) sFeatureMap.get(clazz);
    }
}
