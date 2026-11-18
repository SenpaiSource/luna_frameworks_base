package com.android.server.display.model;

import android.util.Slog;
import com.android.server.display.config.device.OplusDisplayDeviceConfig;
import java.util.HashMap;

/* JADX INFO: loaded from: /run/media/sathiya/data/scratch/dex_extract/smali_oplus1/com/android/server/display/model/OplusDisplayBrightnessModeStrategy.smali */
public class OplusDisplayBrightnessModeStrategy {
    public static final int CHECK_DOUBLE = 2;
    public static final int CHECK_POINT = 10;
    public static final float PCC_DEFAULT = 1.0f;
    public static final String TAG = "OplusDisplayBrightnessModeStrategy";

    private static boolean isSecondaryLuxSpline(OplusDisplayDeviceConfig config) {
        HashMap<Integer, float[]> tmpNitTable = config.getCurveConfig().getDisplayPanelBrightnessTable();
        int displayId = config.getDisplayId();
        if (displayId != 1 || tmpNitTable == null || tmpNitTable.size() <= 20) {
            return false;
        }
        float[] row10 = tmpNitTable.get(10);
        float[] row20 = tmpNitTable.get(20);
        if (row10 == null || row20 == null || row10.length <= 2 || row20.length <= 2) {
            return false;
        }
        boolean isSecondary = row10[2] > 0.0f && row20[2] > 0.0f;
        return isSecondary;
    }

    private static int sniffDisplayBrightnessModel(OplusDisplayDeviceConfig config) {
        if (config == null) {
            return 0;
        }
        HashMap<Integer, float[]> tmpBrightnessTable = config.getCurveConfig().getDisplayPanelBrightnessTable();
        if (tmpBrightnessTable != null && tmpBrightnessTable.size() > 0) {
            float[] row10 = tmpBrightnessTable.get(10);
            if (row10 != null && row10.length > 3 && row10[3] != 1.0f) {
                return 2;
            }
        }
        if (isSecondaryLuxSpline(config)) {
            return 3;
        }
        if (tmpBrightnessTable != null && tmpBrightnessTable.size() > 0) {
            float[] row10 = tmpBrightnessTable.get(10);
            float[] row20 = tmpBrightnessTable.get(20);
            if (row10 != null && row20 != null && row10.length > 2 && row20.length > 2) {
                if (row10[2] > 0.0f && row20[2] > 0.0f && row20[2] - row10[2] < 1.0f) {
                    return 1;
                }
            }
        }
        return 0;
    }

    public static OplusDisplayBrightnessModel createDisplayBrightnessModel(OplusDisplayDeviceConfig config, long physicalDisplayId) {
        int score = sniffDisplayBrightnessModel(config);
        Slog.i(TAG, "score = " + score + " physicalDisplayId=" + physicalDisplayId);
        switch (score) {
            case 0:
                return new OplusDisplayBrightnessLinearModel(config, physicalDisplayId, score);
            case 1:
                return new OplusDisplayBrightnessExponentModel(config, physicalDisplayId, score);
            case CHECK_DOUBLE /* 2 */:
                return new OplusDisplayBrightnessApolloModel(config, physicalDisplayId, score);
            case 3:
                return new OplusDisplayBrightnessSecondModel(config, physicalDisplayId, score);
            default:
                return new OplusDisplayBrightnessLinearModel(config, physicalDisplayId, score);
        }
    }
}
