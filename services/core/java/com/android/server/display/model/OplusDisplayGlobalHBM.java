package com.android.server.display.model;

import android.util.Pair;
import com.android.server.display.config.device.OplusDisplayDeviceConfig;
import com.android.server.display.feature.ambience.utils.AmbienceLog;
import java.util.ArrayList;
import java.util.HashMap;

/* JADX INFO: loaded from: /run/media/sathiya/data/scratch/dex_extract/oplus-services/classes.dex */
public class OplusDisplayGlobalHBM {
    private static final int ARRAY_SIZE_OFFSET = 1;
    private static final int ARRAY_START_INDEX = 0;
    private static final int DEFAULT_EVOLUTION_HBM_CONFIG_SIZE = 4;
    private static final int DEFAULT_HBM_CONFIG_SIZE = 2;
    private static final float DEFAULT_HBM_NIT = 0.0f;
    private static final String TAG = "OplusDisplayGlobalHBM";
    private OplusDisplayBrightnessModel mBrightnessModel;
    private ArrayList<HbmConfig> mConfigs = new ArrayList<>();
    private HbmConfig mCurrentHbmConfig = null;
    private Pair<Float, Float> mCurrentHbmPoint = null;

    public OplusDisplayGlobalHBM(OplusDisplayDeviceConfig config, OplusDisplayBrightnessModel model) {
        this.mBrightnessModel = null;
        this.mBrightnessModel = model;
        loadDisplayBrightnessConfig(config);
    }

    public Pair<Float, Float> getHBMRangeLux(float lux) {
        HbmConfig pendingHbmConfig = null;
        for (HbmConfig config : this.mConfigs) {
            if (lux >= config.getEnterLux()) {
                pendingHbmConfig = (HbmConfig) config.clone();
            }
        }
        if (pendingHbmConfig != null) {
            float gap = pendingHbmConfig.getGapLux();
            float enterLux = (gap > 0.0f)
                    ? ((gap + lux) - ((lux - pendingHbmConfig.getEnterLux()) % gap))
                    : (lux + 1000.0f);
            float exitLux = pendingHbmConfig.getExitLux();
            Pair<Float, Float> HBMRange = new Pair<>(Float.valueOf(exitLux), Float.valueOf(enterLux));
            return HBMRange;
        }
        if (this.mConfigs.isEmpty()) {
            return null;
        }
        Pair<Float, Float> HBMRange2 = new Pair<>(Float.valueOf(0.0f), Float.valueOf(this.mConfigs.get(0).getEnterLux()));
        return HBMRange2;
    }

    public void onRusApply(OplusDisplayDeviceConfig rusConfig) {
        AmbienceLog.d(TAG, "onRusApply");
        ArrayList<HbmConfig> rusHbmConfigs = new ArrayList<>();
        loadDisplayBrightnessConfig(rusConfig, rusHbmConfigs);
        this.mConfigs = rusHbmConfigs;
    }

    private void loadDisplayBrightnessConfig(OplusDisplayDeviceConfig config) {
        loadDisplayBrightnessConfig(config, this.mConfigs);
    }

    private void loadDisplayBrightnessConfig(OplusDisplayDeviceConfig config, ArrayList<HbmConfig> hbmConfigs) {
        if (config != null) {
            HashMap<Integer, Float[]> hbmArray = config.getCurveConfig().getDisplayBrightnessHbmDateTable();
            int displayId = config.getDisplayId();
            for (int i = 0; i < hbmArray.size(); i++) {
                Float[] data = hbmArray.get(Integer.valueOf(i));
                if (data != null) {
                    if (data.length == 4) {
                        if (displayId == 1) {
                            addHbmConfig(data[0].floatValue(), data[1].floatValue(), data[2].floatValue(), data[3].floatValue(), displayId, hbmConfigs);
                        } else {
                            addHbmConfig(data[0].floatValue(), data[1].floatValue(), data[2].floatValue(), data[3].floatValue(), hbmConfigs);
                        }
                    } else if (data.length == 2) {
                        addHbmConfig(data[0].floatValue(), data[1].floatValue(), hbmConfigs);
                    }
                }
            }
        }
    }

    public boolean isHbmMode() {
        return (this.mCurrentHbmConfig == null || this.mCurrentHbmPoint == null) ? false : true;
    }

    private void addHbmConfig(float lux, float nit, ArrayList<HbmConfig> hbmConfigs) {
        addHbmConfig(new HbmConfig(lux, nit), 0, hbmConfigs);
    }

    private void addHbmConfig(float enter, float exit, float gapLux, float gapNit, ArrayList<HbmConfig> hbmConfigs) {
        addHbmConfig(new HbmConfig(enter, exit, gapLux, gapNit), 0, hbmConfigs);
    }

    private void addHbmConfig(float enter, float exit, float gapLux, float gapNit, int displayId, ArrayList<HbmConfig> hbmConfigs) {
        addHbmConfig(new HbmConfig(enter, exit, gapLux, gapNit), displayId, hbmConfigs);
    }

    private synchronized void addHbmConfig(HbmConfig config, int displayId, ArrayList<HbmConfig> hbmConfigs) {
        try {
            if (config == null) {
                AmbienceLog.e(TAG, "HbmConfig is null!");
                return;
            }
            if (config.getBaseNit() == -1.0f) {
                if (hbmConfigs.isEmpty()) {
                    hbmConfigs.clear();
                    if (this.mBrightnessModel != null) {
                        Pair<float[], float[]> splinePair = this.mBrightnessModel.getLuxSplineArrays();
                        if (splinePair != null) {
                            float[] normalSplineNits = (float[]) splinePair.second;
                            if (normalSplineNits.length > 1) {
                                config.setBaseNit(normalSplineNits[normalSplineNits.length - 1]);
                            }
                        }
                    }
                } else {
                    HbmConfig lastConfig = hbmConfigs.get(hbmConfigs.size() - 1);
                    if (lastConfig.getEnterLux() < config.getEnterLux()) {
                        config.updateBaseNitFromLastConfig(lastConfig);
                    } else {
                        AmbienceLog.e(TAG, "the curve does not meet monotonic conditions!");
                        return;
                    }
                }
            }
            if (config.isValidConfig()) {
                hbmConfigs.add(config);
                AmbienceLog.d(TAG, "add Hbm Config success:" + config);
            } else {
                AmbienceLog.e(TAG, "add Hbm Config fail:" + config);
            }
        } catch (Throwable th) {
            throw th;
        }
    }

    public float getHbmNit(float currentLux) {
        float lux = Math.round(currentLux);
        float currentNit = 0.0f;
        if (this.mCurrentHbmConfig == null && !this.mConfigs.isEmpty() && this.mConfigs.get(0).getEnterLux() > lux) {
            return 0.0f;
        }
        HbmConfig pendingHbmConfig = null;
        for (HbmConfig config : this.mConfigs) {
            if (lux < config.getEnterLux()) {
                break;
            }
            pendingHbmConfig = (HbmConfig) config.clone();
        }
        float pendingNit = pendingHbmConfig != null ? pendingHbmConfig.getHbmNit(lux) : 0.0f;
        if (this.mCurrentHbmConfig != null && this.mCurrentHbmConfig.compare(pendingHbmConfig)) {
            currentNit = this.mCurrentHbmConfig.getHbmNit(lux);
        }
        float finalNit = Math.max(pendingNit, currentNit);
        updateCurrentHbmConfig(lux, finalNit, pendingHbmConfig);
        return finalNit;
    }

    private void updateCurrentHbmConfig(float lux, float nit, HbmConfig pending) {
        if (nit != 0.0f) {
            if (this.mCurrentHbmConfig == null) {
                this.mCurrentHbmConfig = pending;
                AmbienceLog.d(TAG, "updateCurrentHbmConfig normal -> hbm, " + this.mCurrentHbmConfig);
            } else if (pending != null && pending.getHbmNit(lux) == nit && !pending.equals(this.mCurrentHbmConfig)) {
                this.mCurrentHbmConfig = pending;
                AmbienceLog.d(TAG, "updateCurrentHbmConfig hbm -> hbm, " + this.mCurrentHbmConfig);
            }
            Pair<Float, Float> result = Pair.create(Float.valueOf(lux), Float.valueOf(nit));
            if (this.mCurrentHbmPoint == null) {
                AmbienceLog.d(TAG, "updateCurrentHbmConfig: " + result);
            } else if (((Float) this.mCurrentHbmPoint.second).floatValue() != nit) {
                AmbienceLog.d(TAG, "updateCurrentHbmConfig: " + this.mCurrentHbmPoint + " -> " + result);
            }
            this.mCurrentHbmPoint = result;
            return;
        }
        this.mCurrentHbmPoint = null;
        this.mCurrentHbmConfig = null;
        AmbienceLog.d(TAG, "updateCurrentHbmConfig hbm -> normal");
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("OplusDisplayGlobalHBM: ").append("{");
        for (HbmConfig config : this.mConfigs) {
            sb.append(config).append(",");
        }
        sb.append("}");
        return sb.toString();
    }

    class HbmConfig implements Cloneable {
        private static final float BAD_VALUE = -1.0f;
        private static final float DEFAULT_GAP_LUX = 10000.0f;
        private static final float DEFAULT_GAP_NIT = 0.0f;
        private static final String TAG = "HbmConfig";
        private float mBaseNit;
        private float mEnterLux;
        private float mExitLux;
        private float mGapLux;
        private float mGapNit;

        public HbmConfig(float lux, float nit) {
            this.mEnterLux = -1.0f;
            this.mExitLux = -1.0f;
            this.mBaseNit = -1.0f;
            this.mGapLux = -1.0f;
            this.mGapNit = -1.0f;
            this.mEnterLux = lux;
            this.mExitLux = lux;
            this.mBaseNit = nit;
            this.mGapLux = DEFAULT_GAP_LUX;
            this.mGapNit = 0.0f;
        }

        public HbmConfig(float enter, float exit, float gapLux, float gapNit) {
            this.mEnterLux = -1.0f;
            this.mExitLux = -1.0f;
            this.mBaseNit = -1.0f;
            this.mGapLux = -1.0f;
            this.mGapNit = -1.0f;
            this.mEnterLux = enter;
            this.mExitLux = exit;
            this.mGapLux = gapLux;
            this.mGapNit = gapNit;
        }

        public Object clone() {
            try {
                HbmConfig con = (HbmConfig) super.clone();
                return con;
            } catch (CloneNotSupportedException e) {
                AmbienceLog.e(TAG, "[CloneNotSupportedException]clone error:" + e.getMessage());
                return null;
            }
        }

        public void setBaseNit(float nit) {
            this.mBaseNit = nit;
        }

        public float getEnterLux() {
            return this.mEnterLux;
        }

        public float getExitLux() {
            return this.mExitLux;
        }

        public float getGapLux() {
            return this.mGapLux;
        }

        public float getGapNit() {
            return this.mGapNit;
        }

        public float getBaseNit() {
            return this.mBaseNit;
        }

        public boolean isValidConfig() {
            return (getEnterLux() == -1.0f || getExitLux() == -1.0f || getGapLux() == -1.0f || getGapNit() == -1.0f || getBaseNit() == -1.0f) ? false : true;
        }

        public void updateBaseNitFromLastConfig(HbmConfig lastConfig) {
            float lastEnterLux = lastConfig.getEnterLux();
            float lastBaseNit = lastConfig.getBaseNit();
            float lastGapLux = lastConfig.getGapLux();
            float lastGapNit = lastConfig.getGapNit();
            setBaseNit((((getEnterLux() - lastEnterLux) / lastGapLux) * lastGapNit) + lastBaseNit);
        }

        public float getHbmNit(float lux) {
            if (lux < getExitLux()) {
                return 0.0f;
            }
            if (lux < getEnterLux()) {
                if (OplusDisplayGlobalHBM.this.mCurrentHbmPoint != null) {
                    return ((Float) OplusDisplayGlobalHBM.this.mCurrentHbmPoint.second).floatValue();
                }
                return 0.0f;
            }
            float finalNit = getBaseNit() + (getGapNit() * ((int) Math.floor((lux - getEnterLux()) / getGapLux())));
            if (equals(OplusDisplayGlobalHBM.this.mCurrentHbmConfig)) {
                return Math.max(((Float) OplusDisplayGlobalHBM.this.mCurrentHbmPoint.second).floatValue(), finalNit);
            }
            return finalNit;
        }

        public boolean compare(HbmConfig config) {
            return config == null || getEnterLux() >= config.getEnterLux();
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(TAG).append("[");
            sb.append("EnterLux").append("=").append(getEnterLux()).append(",");
            sb.append("ExitLux").append("=").append(getExitLux()).append(",");
            sb.append("GapLux").append("=").append(getGapLux()).append(",");
            sb.append("GapNit").append("=").append(getGapNit()).append(",");
            sb.append("BaseNit").append("=").append(getBaseNit());
            sb.append("]");
            return sb.toString();
        }

        public boolean equals(Object obj) {
            if (obj == null || !(obj instanceof HbmConfig)) {
                return false;
            }
            HbmConfig other = (HbmConfig) obj;
            return getEnterLux() == other.getEnterLux() && getExitLux() == other.getExitLux() && getGapLux() == other.getGapLux() && getGapNit() == other.getGapNit() && getBaseNit() == other.getBaseNit();
        }

        public int hashCode() {
            return (int) (getBaseNit() + getEnterLux() + getExitLux() + (getGapLux() * getGapNit()));
        }
    }
}
