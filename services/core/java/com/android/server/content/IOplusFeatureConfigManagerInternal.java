package com.android.server.content;

import android.common.IOplusCommonFeature;
import android.common.OplusFeatureList;

public interface IOplusFeatureConfigManagerInternal extends IOplusCommonFeature {
    String NAME = "IOplusFeatureConfigManagerInternal";
    IOplusFeatureConfigManagerInternal DEFAULT = new IOplusFeatureConfigManagerInternal() {};

    default OplusFeatureList.OplusIndex index() {
        return OplusFeatureList.OplusIndex.IOplusFeatureConfigManagerInternal;
    }

    default IOplusCommonFeature getDefault() {
        return DEFAULT;
    }

    default boolean hasFeature(String featureName) {
        // LUX_SMALL_DEBOUNCE_EXPAND_SUPPORT is false on avalon (uses standard 2-region small debounce)
        return false;
    }
}
