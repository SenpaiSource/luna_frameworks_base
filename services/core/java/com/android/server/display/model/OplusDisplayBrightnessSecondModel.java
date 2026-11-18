package com.android.server.display.model;

import android.content.ContentResolver;
import android.content.Context;
import android.hardware.display.OplusDisplayManager;
import android.provider.Settings;
import android.util.Slog;
import com.android.server.display.config.device.OplusDisplayDeviceConfig;
import java.util.Locale;

/* JADX INFO: loaded from: /run/media/sathiya/data/scratch/dex_extract/smali_oplus1/com/android/server/display/model/OplusDisplayBrightnessSecondModel.smali */
public class OplusDisplayBrightnessSecondModel extends OplusDisplayBrightnessModel {
    private static final int FRAME_MS_DEFAULT = 16;
    private static final int FRAME_MS_HIGH = 8;
    private static final float FRAME_RATE_DEFAULT = 0.625f;
    private static final float FRAME_RATE_HIGH = 0.3125f;
    private static final int MULTIPLE_FRAME_MS = 1000;
    private static final float[] NORMAL_NIT = {5.0f, 30.0f, 50.0f};
    private static final String SCREEN_BRIGHTNESS_SECOND_REGION = "screen_brightness_second_region";
    private static final String TAG = "BrightnessSecondModel";

    public OplusDisplayBrightnessSecondModel(OplusDisplayDeviceConfig config, long physicalId, int type) {
        super(config, physicalId, type);
        String info = String.format(Locale.US, "id=%d,min=%f,max=%f,def=%f,total=%f", Long.valueOf(this.mPhysicalDisplayId), Float.valueOf(this.mMinBrightness), Float.valueOf(this.mMaxBrightness), Float.valueOf(this.mDefaultBrightness), Float.valueOf(this.mTotalBrightness));
        Slog.i(TAG, "BrightnessConfig[" + info + "]");
    }

    private void init(Context context) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < NORMAL_NIT.length; i++) {
            float nit = NORMAL_NIT[i];
            float lev = OplusDisplayManager.getInstance().getBrightnessFromNit(0, nit);
            sb.append("[").append(nit).append(",").append(lev).append("]");
            if (i != NORMAL_NIT.length - 1) {
                sb.append(",");
            }
        }
        String value = sb.toString();
        ContentResolver resolver = context.getContentResolver();
        Settings.System.putStringForUser(resolver, SCREEN_BRIGHTNESS_SECOND_REGION, value, -2);
    }

    private float getDesireNit(float nit) {
        float tmpNit = nit;
        float mid = nit;
        int length = NORMAL_NIT.length;
        int index = nit >= NORMAL_NIT[0] ? -1 : 0;
        if (index != 0 && nit < NORMAL_NIT[length - 1]) {
            for (int i = 0; i < length - 1; i++) {
                if (nit >= NORMAL_NIT[i] && nit <= NORMAL_NIT[i + 1]) {
                    index = i;
                    break;
                }
            }
        }
        if (index != -1) {
            mid = (NORMAL_NIT[index] + NORMAL_NIT[index + 1]) / 2.0f;
            tmpNit = nit < mid ? NORMAL_NIT[index] : NORMAL_NIT[index + 1];
        }
        if (nit != tmpNit) {
            Slog.d(TAG, "desireNit changed:" + nit + "->" + tmpNit + " index=" + index + " mid=" + mid);
        }
        return tmpNit;
    }

    public void onBootPhase(Context context, int phase, boolean autoMode) {
        if (phase == 500) {
            init(context);
        }
    }

    public float getBacklightFromBrightness(float brightness) {
        if (brightness < 0.0f) {
            return 0.0f;
        }
        float backlight = brightness;
        float maxPanel = this.mMaxPanelBrightness > 0.0f ? this.mMaxPanelBrightness : 4095.0f;
        if (this.mPanelBacklights != null && this.mPanelBacklights.length > 0) {
            if (brightness > this.mPanelBacklights.length - 1) {
                return 1.0f;
            }
            if (brightness > 1.0f) {
                backlight = this.mPanelBacklights[Math.min((int) brightness, this.mPanelBacklights.length - 1)];
            }
        }
        return backlight / maxPanel;
    }

    public float clampNit(float nit) {
        return getDesireNit(nit);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("BrightnessConfig[");
        sb.append("id=").append(this.mPhysicalDisplayId).append(",");
        sb.append("min=").append(this.mMinBrightness).append(",");
        sb.append("max=").append(this.mMaxBrightness).append(",");
        sb.append("def=").append(this.mDefaultBrightness).append(",");
        sb.append("total=").append(this.mTotalBrightness).append(",");
        sb.append("]");
        return sb.toString();
    }
}
