package com.android.server.display.marvels.module;

import android.common.IOplusCommonFeature;
import android.common.OplusFeatureList;

public interface IORBrightnessMarvelsDataCollector extends IOplusCommonFeature {
    IORBrightnessMarvelsDataCollector DEFAULT = new IORBrightnessMarvelsDataCollector() {};

    default OplusFeatureList.OplusIndex index() {
        return OplusFeatureList.OplusIndex.IORBrightnessMarvelsDataCollector;
    }

    default IOplusCommonFeature getDefault() {
        return DEFAULT;
    }

    default void setLux(float lux) {}
    default void setStableLux(float lux) {}
    default void onDisplayFoldedStateChange(boolean folded) {}
    default void setAutoTargetBrightness(float lux, float nit) {}
}
