package com.android.server.display.model;

import com.android.server.display.config.device.OplusDisplayDeviceConfig;

/* JADX INFO: loaded from: /run/media/sathiya/data/scratch/dex_extract/smali_oplus1/com/android/server/display/model/OplusDisplayBrightnessExponentModel.smali */
public class OplusDisplayBrightnessExponentModel extends OplusDisplayBrightnessModel {
    public static final String TAG = "OplusDisplayBrightnessExponentModel";

    public OplusDisplayBrightnessExponentModel(OplusDisplayDeviceConfig config, long physicalId, int type) {
        super(config, physicalId, type);
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
}
