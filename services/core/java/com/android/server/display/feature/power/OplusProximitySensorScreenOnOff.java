package com.android.server.display.feature.power;

import android.util.Slog;

public final class OplusProximitySensorScreenOnOff {
    private static final String TAG = "OplusProximitySensorScreenOnOff";
    private static final OplusProximitySensorScreenOnOff sInstance = new OplusProximitySensorScreenOnOff();

    private volatile boolean mIsScreenOffByProximityPositive = false;

    public static OplusProximitySensorScreenOnOff getInstance() {
        return sInstance;
    }

    public boolean isProximityPositive() {
        return this.mIsScreenOffByProximityPositive;
    }

    public void setProximityPositive(boolean positive) {
        if (this.mIsScreenOffByProximityPositive != positive) {
            Slog.d(TAG, "setProximityPositive: " + positive);
            this.mIsScreenOffByProximityPositive = positive;
        }
    }
}
