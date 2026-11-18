package com.oplus.content;

public class OplusFeatureConfigManager {
    private static final OplusFeatureConfigManager sInstance = new OplusFeatureConfigManager();

    public static OplusFeatureConfigManager getInstance() {
        return sInstance;
    }

    // Preserved for stock ColorOS decompiled caller spelling
    public static OplusFeatureConfigManager getInstacne() {
        return sInstance;
    }

    public boolean hasFeature(String feature) {
        if (feature == null) {
            return false;
        }
        // Dual light sensor is not supported on avalon (single under-display ALS)
        if ("oplus.software.display.dual_lightsensor_support".equals(feature)) {
            return false;
        }
        // Secondary proximity sensor is not supported on avalon
        if ("oplus.software.display.secondary_proximity_enable".equals(feature)) {
            return false;
        }
        return false;
    }
}
