package com.android.server.display.model;

import com.android.server.display.config.device.OplusDisplayDeviceConfig;

/* JADX INFO: loaded from: /run/media/sathiya/data/scratch/dex_extract/smali_oplus1/com/android/server/display/model/OplusDisplayBrightnessApolloModel.smali */
public class OplusDisplayBrightnessApolloModel extends OplusDisplayBrightnessModel {
    public static final String TAG = "OplusDisplayBrightnessApolloModel";

    public OplusDisplayBrightnessApolloModel(OplusDisplayDeviceConfig config, long physicalId, int type) {
        super(config, physicalId, type);
    }

    public int getBrightnessRampRateFast() {
        return this.mBrightnessRampRateFast;
    }

    public int getBrightnessRampRateSlow() {
        return this.mBrightnessRampRateSlow;
    }

    public int getBrightnessRampRateBoost() {
        return this.mBrightnessRampRateScreenOn;
    }

    public float getBacklightFromBrightness(float brightness) {
        if (brightness < 0.0f) {
            return 0.0f;
        }
        float maxPanel = this.mMaxPanelBrightness > 0.0f ? this.mMaxPanelBrightness : 4095.0f;
        if (this.mPanelBacklights == null || this.mPanelBacklights.length == 0) {
            return brightness / maxPanel;
        }
        if (brightness > this.mPanelBacklights.length - 1) {
            return 1.0f;
        }
        float backlight = brightness <= 1.0f ? brightness : this.mPanelBacklights[Math.min(Math.max((int) brightness, 0), this.mPanelBacklights.length - 1)];
        return backlight / maxPanel;
    }

    public float getColorFromBrightness(float brightness) {
        if (brightness < 0.0f) {
            return 0.0f;
        }
        if (this.mPanelPcc == null || this.mPanelPcc.length == 0) {
            return 1.0f;
        }
        if (brightness > this.mPanelPcc.length - 1) {
            return 1.0f;
        }
        return this.mPanelPcc[Math.min(Math.max((int) brightness, 0), this.mPanelPcc.length - 1)];
    }
}
