package com.android.server.display.config.global;

import com.android.server.display.util.AutoReleaseReadWriteLock;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class OplusDisplayGlobalConfig {
    private static final String TAG = "OplusDisplayGlobalConfig";
    private static final OplusDisplayGlobalConfig sInstance = new OplusDisplayGlobalConfig();

    private final AutoReleaseReadWriteLock mLock = new AutoReleaseReadWriteLock();
    private final List<IGlobalConfigListener> mListeners = new CopyOnWriteArrayList<>();
    private ComfortableBrightnessConfig mComfortableBrightnessConfig = new ComfortableBrightnessConfig();
    private boolean mDarkEnvBrightnessSmoothOptimizeSupport = false;
    private boolean mDualLightSensorFusionSupport = false;
    private boolean mRemapDisable = false;

    public interface IGlobalConfigListener {
        void onRusConfigLoadSuccess();
    }

    public static OplusDisplayGlobalConfig getInstance() {
        return sInstance;
    }

    public ComfortableBrightnessConfig getComfortableBrightnessConfig() {
        return this.mLock.executeReadOperation(() -> this.mComfortableBrightnessConfig);
    }

    public boolean isSupportDarkEnvBrightnessSmoothOptimiz() {
        return this.mLock.executeReadOperation(() -> this.mDarkEnvBrightnessSmoothOptimizeSupport);
    }

    public boolean isDualLightSensorFusionSupport() {
        return this.mLock.executeReadOperation(() -> this.mDualLightSensorFusionSupport);
    }

    public boolean hasRemapDisable() {
        return this.mLock.executeReadOperation(() -> this.mRemapDisable);
    }

    public boolean isLightSensorAlwaysOnSupport() {
        return false;
    }

    public boolean isAiEventMinimumBrightnessControlSupport() {
        return false;
    }

    public void register(IGlobalConfigListener listener) {
        if (listener != null && !this.mListeners.contains(listener)) {
            this.mListeners.add(listener);
        }
    }

    public void unregister(IGlobalConfigListener listener) {
        if (listener != null) {
            this.mListeners.remove(listener);
        }
    }

    public float[] getBrightnessRegionSlopes() {
        return null;
    }

    public java.util.HashMap<Float, float[]> geRegionRateMap() {
        return new java.util.HashMap<>();
    }

    public void transformAppInfo(com.android.server.display.model.OplusSpline brightness, com.android.server.display.model.OplusSpline nits) {
    }

    public void notifyRusListeners() {
        for (IGlobalConfigListener listener : this.mListeners) {
            listener.onRusConfigLoadSuccess();
        }
    }
}
