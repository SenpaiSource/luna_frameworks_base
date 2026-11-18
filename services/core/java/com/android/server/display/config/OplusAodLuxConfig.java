package com.android.server.display.config;

import android.os.SystemProperties;
import android.util.Slog;
import com.android.server.display.feature.ambience.utils.AmbienceLog;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import org.json.JSONObject;

/**
 * Manages AOD lux banding thresholds and configuration parsed from lux_aod.json.
 * Avalon / OnePlus Nord 4 defines:
 *   thrd-low: 12 lux
 *   thrd-high: 50 lux
 *   deploy-multi-aod: 1
 */
public class OplusAodLuxConfig {
    private static final String TAG = "OplusAodLuxConfig";
    private static volatile OplusAodLuxConfig sInstance = null;

    private static final String[] CANDIDATE_PATHS = new String[]{
        "/odm/etc/sensor/config/lux_aod.json",
        "/vendor/etc/sensor/config/lux_aod.json",
        "/my_product/vendor/etc/sensor/config/lux_aod.json",
        "/system_ext/etc/sensor/config/lux_aod.json",
        "/system/etc/sensor/config/lux_aod.json"
    };

    private float mThrdLow = 12.0f;
    private float mThrdHigh = 50.0f;
    private int mDeployMultiAod = 1;
    private boolean mLoaded = false;

    public static OplusAodLuxConfig getInstance() {
        if (sInstance == null) {
            synchronized (OplusAodLuxConfig.class) {
                if (sInstance == null) {
                    sInstance = new OplusAodLuxConfig();
                    sInstance.loadConfig();
                }
            }
        }
        return sInstance;
    }

    private void loadConfig() {
        for (String path : CANDIDATE_PATHS) {
            File file = new File(path);
            if (file.exists() && file.canRead()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    JSONObject root = new JSONObject(sb.toString());
                    if (root.has("lux_aod")) {
                        JSONObject luxAod = root.getJSONObject("lux_aod");
                        if (luxAod.has("thrd-low")) {
                            mThrdLow = com.android.server.display.util.OplusBrightnessUtils.parseFloatSafe(
                                    luxAod.getJSONObject("thrd-low").optString("data", "12"), 12.0f);
                        }
                        if (luxAod.has("thrd-high")) {
                            mThrdHigh = com.android.server.display.util.OplusBrightnessUtils.parseFloatSafe(
                                    luxAod.getJSONObject("thrd-high").optString("data", "50"), 50.0f);
                        }
                        if (luxAod.has("deploy-multi-aod")) {
                            mDeployMultiAod = com.android.server.display.util.OplusBrightnessUtils.parseIntSafe(
                                    luxAod.getJSONObject("deploy-multi-aod").optString("data", "1"), 1);
                        }
                        mLoaded = true;
                        AmbienceLog.i(TAG, "Loaded lux_aod from " + path + ": low=" + mThrdLow + ", high=" + mThrdHigh + ", multi=" + mDeployMultiAod);
                        return;
                    }
                } catch (Exception e) {
                    AmbienceLog.e(TAG, "Failed to parse lux_aod from " + path + ": " + e.getMessage());
                }
            }
        }
        AmbienceLog.i(TAG, "Using default avalon lux_aod config: low=" + mThrdLow + ", high=" + mThrdHigh + ", multi=" + mDeployMultiAod);
    }

    public float getThrdLow() {
        return mThrdLow;
    }

    public float getThrdHigh() {
        return mThrdHigh;
    }

    public int getDeployMultiAod() {
        return mDeployMultiAod;
    }

    public boolean isLoaded() {
        return mLoaded;
    }

    /**
     * Maps ambient lux into one of three stock ColorOS AOD operational tiers:
     * Band 1: Low band ([0, 12) lux) - dark environment
     * Band 2: Mid band ([12, 50) lux) - standard indoor
     * Band 3: High band (>= 50 lux) - bright office / daylight
     */
    public int getAodTier(float lux) {
        if (lux < mThrdLow) {
            return 1;
        } else if (lux < mThrdHigh) {
            return 2;
        } else {
            return 3;
        }
    }
}
