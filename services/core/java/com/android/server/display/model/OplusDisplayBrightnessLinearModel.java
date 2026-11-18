package com.android.server.display.model;

import com.android.server.display.config.device.OplusDisplayDeviceConfig;

/* JADX INFO: loaded from: /run/media/sathiya/data/scratch/dex_extract/smali_oplus1/com/android/server/display/model/OplusDisplayBrightnessLinearModel.smali */
public class OplusDisplayBrightnessLinearModel extends OplusDisplayBrightnessModel {
    public static final String TAG = "OplusDisplayBrightnessLinearModel";

    public OplusDisplayBrightnessLinearModel(OplusDisplayDeviceConfig config, long physicalId, int type) {
        super(config, physicalId, type);
    }

    public float getBacklightFromBrightness(float brightness) {
        if (brightness < 0.0f) {
            return 0.0f;
        }
        float maxPanel = this.mMaxPanelBrightness > 0.0f ? this.mMaxPanelBrightness : 4095.0f;
        return brightness / maxPanel;
    }
}
