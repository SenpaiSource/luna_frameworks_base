package com.android.server.display.model;

import android.content.Context;
import android.os.OplusPropertyList;
import android.os.SystemProperties;
import android.util.IndentingPrintWriter;
import android.util.Pair;
import android.util.Slog;
import com.android.server.display.IDisplayBrightnessModelExt;
import com.android.server.display.config.device.CurveConfig;
import com.android.server.display.config.device.OplusDisplayDeviceConfig;
import com.android.server.display.config.global.OplusDisplayGlobalConfig;
import com.android.server.display.util.AutoReleaseReadWriteLock;
import com.android.server.display.util.OplusBrightnessPoint;
import com.android.server.display.util.OplusBrightnessUtils;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/* JADX INFO: loaded from: /run/media/sathiya/data/scratch/dex_extract/smali_oplus1/com/android/server/display/model/OplusDisplayBrightnessModel.smali */
public class OplusDisplayBrightnessModel implements IDisplayBrightnessModelExt {
    public static final float DEFAULT_DELTA = 2.0f;
    public static final String DISABLE = "0";
    public static final String ENABLE = "1";
    public static final String LOW_LIGHT_UNDER_SCREEN_LIGHT_SENSOR = "persist.sys.oplus.lowlightreport";
    public static final int PANEL_BACKLIGHT = 1;
    public static final int PANEL_LEVEL = 0;
    public static final int PANEL_NIT = 2;
    public static final int PANEL_PCC = 3;
    public static final float ULTRA_EXP_DOWN_OFFSET = 0.92f;
    public static final float ULTRA_EXP_UP_OFFSET = 0.8f;
    public float DEFAULT_BRIGHTNESSS_LUX;
    public float DEFAULT_BRIGHTNESSS_NIT;
    public float MINUM_BRIGHTNESSS_NIT;
    public float mAdjfreqForAnimation;
    public int mBrightnessRampRateEdrEnter;
    public int mBrightnessRampRateEdrExit;
    public int mBrightnessRampRateFast;
    public int mBrightnessRampRateGame;
    public int mBrightnessRampRateHdrVisionEnter;
    public int mBrightnessRampRateScreenOn;
    public int mBrightnessRampRateSlow;
    public int mBrightnessRampRateSunlightIn;
    public int mBrightnessRampRateSunlightOut;
    public int mBrightnessRampRateWindowEnter;
    public int mBrightnessRampRateWindowExit;
    public float mDarkLuxThreshold;
    public float mDefaultBrightness;
    public float mDeltaDownScale;
    public float mDeltaUpScale;
    public float mDragExpandMultipleLeft;
    public float mDragExpandMultipleRight;
    public OplusDisplayGlobalConfig mGlobalConfig;
    public float mHBMScale;
    public float mHighBrightnessPercent;
    public float mHightScale;
    public final boolean mIsAccelerateScaleNotSupport;
    public final boolean mIsApolloSupport;
    public final boolean mIsSilkySupport;
    public final boolean mIsSlopeRMSupport;
    public float mLastCurY;
    public float mLeftScaleInDarkEnv;
    private AutoReleaseReadWriteLock mLock;
    public float mLowBrightnessPercent;
    public float mLowDownScale;
    public float mLowHbmScale;
    public final float mLowLightBrightnessNit;
    public final int mLowLightBrightnessRate;
    public float mLowScale;
    public float mLowUpScale;
    public float mLowestArea;
    public float mLowestScale;
    public float[] mLuxs;
    public final int mManualRateMultiplier;
    public float mMaxBrightness;
    public float mMaxBrightnessChange;
    public float mMaxConfigBrightness;
    public float mMaxPanelBrightness;
    public float mMinBrightness;
    public float mMinBrightnessChange;
    public float mMinConfigBrightness;
    public int mModelType;
    public float[] mNits;
    public float mNormalMaxNit;
    public int mNormalMaxScale;
    private OplusDisplayGlobalHBM mOplusDisplayGlobalHBM;
    public float[] mPanelBacklights;
    public float[] mPanelBacklightsPWM;
    public float[] mPanelBacklightsSPD;
    public float[] mPanelBrightness;
    public float[] mPanelBrightnessPWM;
    public float[] mPanelBrightnessSPD;
    public float[] mPanelNits;
    public float[] mPanelNitsPWM;
    public float[] mPanelNitsSPD;
    public float[] mPanelPcc;
    public long mPhysicalDisplayId;
    private HashMap<Float, float[]> mRegionRateMap;
    public float mRightScaleInDarkEnv;
    public float mSilky2wCurrY;
    public final int mSilky2wRate;
    public float mSpeedMultipleInDistance;
    public OplusDisplaySplineManager mSplineManager;
    public float mStableSmallChangeScaleInDarkEnv;
    public float mStableSmallChangeTargetPercent;
    public float mTotalBrightness;
    public static float[] BRIGHTNESS_REGION_SLOPE = {0.55f, 0.75f, 0.85f, 1.0f, 1.2f, 2.0f, 2.5f, 3.0f, 4.0f};
    public static String sExponentialBrightness = OplusPropertyList.RO_BRIGHTNESS_MODE;
    public final String TAG = "OplusDisplayBrightnessModel";
    public final float FLOAT_EPSILON = 1.0E-6f;
    public final float FLOAT_EPSILON_MULT = 1.01f;
    public final int FLOAT_MULT_COE = 100000;
    public final int BRIGHTNESS_ON = 255;
    public final float ZERO = 0.0f;
    public final float FLOAT_ONE = 1.0f;
    public final float DEFAULT_PCC = 1.0f;
    public final int mRefreshRate60 = 60;
    public final int mRefreshRate90 = 90;
    public final int mRefreshRate120 = 120;
    public final float REGION_SLOPE_RM_0 = 0.7f;
    public final float REGION_SLOPE_RM_2 = 0.85f;
    public final int[] brightnessRegion = {4, 400, 700, 1000, 2000};
    public final float[] brightnessRegionDelta = {0.25f, 0.35f, 0.7f, 1.0f};

    public OplusDisplayBrightnessModel(OplusDisplayDeviceConfig cfg, long physicalDisplayId, int type) {
        this.mIsSlopeRMSupport = SystemProperties.getInt("ro.display.brightness.region.slope.rm", 0) > 0;
        this.mIsApolloSupport = (SystemProperties.getInt("persist.brightness.apollo.usehwc", 0) | SystemProperties.getInt("persist.brightness.apollo", 0)) > 0;
        this.mIsSilkySupport = (SystemProperties.getInt("persist.vendor.sys.pq.disp.silky.brightness", 0) | SystemProperties.getInt("persist.vendor.sys.pq.disp.ccorr.silky.brightness", 0)) > 0;
        this.mLowLightBrightnessRate = SystemProperties.getInt("ro.oplus.display.brightness.lowlight.brightness_rate", 0);
        this.mSilky2wRate = SystemProperties.getInt("ro.oplus.display.brightness.silky.2w_rate", 0);
        this.mManualRateMultiplier = SystemProperties.getInt("ro.oplus.display.brightness.manual.rate_multiplier", 1);
        this.mLowLightBrightnessNit = Float.parseFloat(SystemProperties.get("ro.oplus.display.brightness.lowlight.brightnessNit", "12.0"));
        this.mIsAccelerateScaleNotSupport = SystemProperties.getInt("ro.display.brightness.accelerate.scale.notsupport", 0) > 0;
        this.mSilky2wCurrY = 5.28f;
        this.mDarkLuxThreshold = 36.0f;
        this.mLeftScaleInDarkEnv = 1.5f;
        this.mRightScaleInDarkEnv = 1.5f;
        this.mStableSmallChangeScaleInDarkEnv = 1.5f;
        this.DEFAULT_BRIGHTNESSS_LUX = 700.0f;
        this.DEFAULT_BRIGHTNESSS_NIT = 135.0f;
        this.MINUM_BRIGHTNESSS_NIT = 2.5f;
        this.mDefaultBrightness = 102.0f;
        this.mMaxBrightness = 2047.0f;
        this.mMaxConfigBrightness = 2047.0f;
        this.mNormalMaxNit = 800.0f;
        this.mMaxPanelBrightness = 4095.0f;
        this.mTotalBrightness = 4095.0f;
        this.mMinBrightness = 160.0f;
        this.mMinConfigBrightness = 160.0f;
        this.mDeltaUpScale = 0.5f;
        this.mDeltaDownScale = 0.15f;
        this.mMinBrightnessChange = 0.5f;
        this.mMaxBrightnessChange = 4.0f;
        this.mHighBrightnessPercent = 0.6f;
        this.mHightScale = 2.0f;
        this.mHBMScale = 8.0f;
        this.mLowBrightnessPercent = 0.15f;
        this.mLowScale = 3.5f;
        this.mLowUpScale = 1.5f;
        this.mLowHbmScale = 1.68f;
        this.mLowestArea = 50.0f;
        this.mLowestScale = 3.0f;
        this.mLowDownScale = 0.125f;
        this.mSpeedMultipleInDistance = 0.1f;
        this.mNormalMaxScale = 1000;
        this.mStableSmallChangeTargetPercent = 0.1f;
        this.mDragExpandMultipleRight = 1.3f;
        this.mDragExpandMultipleLeft = 1.8f;
        this.mLastCurY = 0.0f;
        this.mBrightnessRampRateFast = SystemProperties.getInt("ro.oplus.display.brightness.fast_rate", 200);
        this.mBrightnessRampRateScreenOn = 120;
        this.mBrightnessRampRateSlow = 40;
        this.mBrightnessRampRateSunlightIn = 200;
        this.mBrightnessRampRateSunlightOut = 38;
        this.mBrightnessRampRateWindowExit = 109;
        this.mBrightnessRampRateWindowEnter = 80;
        this.mBrightnessRampRateGame = 1;
        this.mBrightnessRampRateEdrEnter = 255;
        this.mBrightnessRampRateEdrExit = 47;
        this.mBrightnessRampRateHdrVisionEnter = 94;
        this.mAdjfreqForAnimation = 0.0f;
        this.mPhysicalDisplayId = 0L;
        this.mModelType = 0;
        this.mSplineManager = null;
        this.mGlobalConfig = null;
        this.mRegionRateMap = new HashMap<>();
        this.mOplusDisplayGlobalHBM = null;
        this.mLock = new AutoReleaseReadWriteLock();
        if (cfg == null) {
            Slog.e("OplusDisplayBrightnessModel", "OplusDisplayDeviceConfig is null!!!");
            return;
        }
        CurveConfig curveConfig = cfg.getCurveConfig();
        this.mGlobalConfig = OplusDisplayGlobalConfig.getInstance();
        this.mPhysicalDisplayId = physicalDisplayId;
        this.mModelType = type;
        this.mLuxs = new float[curveConfig.getDisplayBrightnessLuxTable().size()];
        this.mNits = new float[curveConfig.getDisplayBrightnessNitTable().size()];
        this.mPanelNits = new float[curveConfig.getDisplayPanelBrightnessTable().size()];
        this.mPanelBrightness = new float[curveConfig.getDisplayPanelBrightnessTable().size()];
        this.mPanelBacklights = new float[curveConfig.getDisplayPanelBrightnessTable().size()];
        this.mPanelPcc = new float[curveConfig.getDisplayPanelBrightnessTable().size()];
        this.mPanelNitsPWM = new float[curveConfig.getDisplayPanelBrightnessTablePWM().size()];
        this.mPanelBrightnessPWM = new float[curveConfig.getDisplayPanelBrightnessTablePWM().size()];
        this.mPanelBacklightsPWM = new float[curveConfig.getDisplayPanelBrightnessTablePWM().size()];
        this.mPanelNitsSPD = new float[curveConfig.getDisplayPanelBrightnessTableSPD().size()];
        this.mPanelBrightnessSPD = new float[curveConfig.getDisplayPanelBrightnessTableSPD().size()];
        this.mPanelBacklightsSPD = new float[curveConfig.getDisplayPanelBrightnessTableSPD().size()];
        OplusBrightnessUtils.traversalMaps(curveConfig.getDisplayBrightnessLuxTable(), this.mLuxs);
        OplusBrightnessUtils.traversalMaps(curveConfig.getDisplayBrightnessNitTable(), this.mNits);
        curveConfig.traversalPanelMaps(this.mPanelBrightness, 0);
        curveConfig.traversalPanelMaps(this.mPanelBacklights, 1);
        curveConfig.traversalPanelMaps(this.mPanelPcc, 3);
        sortMapsByValuesIncrease(curveConfig.getDisplayPanelBrightnessTable(), this.mPanelNits);
        curveConfig.traversalPanelMapsPWM(this.mPanelBrightnessPWM, 0);
        curveConfig.traversalPanelMapsPWM(this.mPanelBacklightsPWM, 1);
        sortMapsByValuesIncrease(curveConfig.getDisplayPanelBrightnessTablePWM(), this.mPanelNitsPWM);
        curveConfig.traversalPanelMapsSPD(this.mPanelBrightnessSPD, 0);
        curveConfig.traversalPanelMapsSPD(this.mPanelBacklightsSPD, 1);
        sortMapsByValuesIncrease(curveConfig.getDisplayPanelBrightnessTableSPD(), this.mPanelNitsSPD);
        if (this.mGlobalConfig.getBrightnessRegionSlopes() != null && BRIGHTNESS_REGION_SLOPE.length == this.mGlobalConfig.getBrightnessRegionSlopes().length) {
            BRIGHTNESS_REGION_SLOPE = this.mGlobalConfig.getBrightnessRegionSlopes();
        }
        if (this.mGlobalConfig.geRegionRateMap().size() != 0) {
            this.mRegionRateMap = this.mGlobalConfig.geRegionRateMap();
        }
        sExponentialBrightness = this.mIsSilkySupport ? ENABLE : sExponentialBrightness;
        SystemProperties.set("persist.sys.oplus.display.brightness.mode", sExponentialBrightness);
        this.mSplineManager = new OplusDisplaySplineManager(this.mPhysicalDisplayId);
        if (this.mSplineManager != null) {
            this.mSplineManager.createSplines(this);
            OplusSpline tmpSplineNit = this.mSplineManager.getCurve("nits");
            OplusSpline tmpSplineBrightness = this.mSplineManager.getCurve("backlights");
            if (tmpSplineNit != null && tmpSplineBrightness != null) {
                this.mDefaultBrightness = tmpSplineNit.interpolate(this.DEFAULT_BRIGHTNESSS_NIT);
                this.mGlobalConfig.transformAppInfo(tmpSplineBrightness, tmpSplineNit);
            }
        }
        this.mMinBrightness = curveConfig.getMinLevel();
        this.mMaxBrightness = curveConfig.getMaxLevel();
        this.mMinConfigBrightness = curveConfig.getMinLevel();
        this.mMaxConfigBrightness = curveConfig.getMaxLevel();
        this.mNormalMaxNit = getNitFromBrightness(this.mMaxBrightness);
        this.mTotalBrightness = this.mPanelBrightness.length > 0 ? this.mPanelBrightness[this.mPanelBrightness.length - 1] : this.mMaxBrightness;
        this.mMaxPanelBrightness = this.mPanelBacklights.length > 0 ? this.mPanelBacklights[this.mPanelBacklights.length - 1] : this.mMaxBrightness;
        initRate(this.mMaxBrightness);
        this.mOplusDisplayGlobalHBM = crateHbmIfNeed(cfg);
    }

    private OplusDisplayGlobalHBM crateHbmIfNeed(OplusDisplayDeviceConfig cfg) {
        if (cfg.getCurveConfig().getDisplayBrightnessHbmDateTable() != null && !cfg.getCurveConfig().getDisplayBrightnessHbmDateTable().isEmpty()) {
            return new OplusDisplayGlobalHBM(cfg, this);
        }
        return null;
    }

    private void initRate(float normalMaxBrihgtness) {
        if (normalMaxBrihgtness > 255.0f) {
            int multi = ((int) (1.0f + normalMaxBrihgtness)) / 256;
            this.mBrightnessRampRateFast *= multi;
            this.mBrightnessRampRateSlow *= multi;
            this.mBrightnessRampRateScreenOn *= multi;
            this.mBrightnessRampRateSunlightIn *= multi;
            this.mBrightnessRampRateSunlightOut *= multi;
            this.mBrightnessRampRateWindowExit *= multi;
            this.mBrightnessRampRateWindowEnter *= multi;
            this.mBrightnessRampRateGame *= multi;
            this.mBrightnessRampRateEdrEnter *= multi;
            this.mBrightnessRampRateEdrExit *= multi;
            this.mBrightnessRampRateHdrVisionEnter *= multi;
        }
    }

    public void onRusApply(OplusDisplayDeviceConfig rusConfig) {
        CurveConfig rusCurveConfig = rusConfig.getCurveConfig();
        final float[] rusLuxs = new float[rusCurveConfig.getDisplayBrightnessLuxTable().size()];
        final float[] rusNits = new float[rusCurveConfig.getDisplayBrightnessNitTable().size()];
        OplusBrightnessUtils.traversalMaps(rusCurveConfig.getDisplayBrightnessLuxTable(), rusLuxs);
        OplusBrightnessUtils.traversalMaps(rusCurveConfig.getDisplayBrightnessNitTable(), rusNits);
        if (this.mSplineManager.updateLuxNitSplineSuccess(rusLuxs, rusNits)) {
            this.mLock.executeWriteOperation(() -> {
                this.mLuxs = rusLuxs;
                this.mNits = rusNits;
            });
            Slog.d("OplusDisplayBrightnessModel", "onRusApply success");
        }
        if (this.mOplusDisplayGlobalHBM != null) {
            this.mOplusDisplayGlobalHBM.onRusApply(rusConfig);
        }
    }

    public int getBrightnessModelType() {
        return this.mModelType;
    }

    public int getBrightnessRampRateSunlightIn() {
        return this.mBrightnessRampRateSunlightIn;
    }

    public int getBrightnessRampRateSunlightOut() {
        return this.mBrightnessRampRateSunlightOut;
    }

    public void drag(float x, float y) {
        if (this.mSplineManager != null) {
            this.mSplineManager.drag(x, y);
        }
    }

    public void resetSpline() {
        if (this.mSplineManager != null) {
            this.mSplineManager.reset();
        }
    }

    public boolean isDragSpline() {
        return this.mSplineManager.isDragSpline();
    }

    public int getBrightnessRampRateFast() {
        return this.mBrightnessRampRateFast;
    }

    public int getBrightnessRampRateFastManual() {
        return this.mBrightnessRampRateFast * this.mManualRateMultiplier;
    }

    public int getBrightnessRampRateSlow() {
        return this.mBrightnessRampRateSlow;
    }

    public int getBrightnessRampRateGame() {
        return this.mBrightnessRampRateGame;
    }

    public int getBrightnessRampRateWindowExit() {
        return this.mBrightnessRampRateWindowExit;
    }

    public int getBrightnessRampRateWindowEnter() {
        return this.mBrightnessRampRateWindowEnter;
    }

    public int getBrightnessRampRateEdrEnter() {
        return this.mBrightnessRampRateEdrEnter;
    }

    public int getBrightnessRampRateEdrExit() {
        return this.mBrightnessRampRateEdrExit;
    }

    public int getBrightnessRampRateHdrVisionEnter() {
        return this.mBrightnessRampRateHdrVisionEnter;
    }

    public Pair<Float, Float> getHBMRangeLux(float lux) {
        if (this.mOplusDisplayGlobalHBM != null) {
            return this.mOplusDisplayGlobalHBM.getHBMRangeLux(lux);
        }
        return null;
    }

    public float getShakeRangeMaxLux() {
        return ((Float) this.mLock.executeReadOperation(() -> this.mLuxs[this.mLuxs.length - 1])).floatValue();
    }

    public int getBrightnessRampRateBoost() {
        return this.mBrightnessRampRateScreenOn;
    }

    public float getValueByCurve(String name, float key) {
        if (name == null || name.isEmpty()) {
            OplusSpline curve = this.mSplineManager.getCurve();
            if (curve == null) {
                return Float.NaN;
            }
            float nit = curve.interpolate(key);
            if (this.mOplusDisplayGlobalHBM != null) {
                return Math.max(nit, this.mOplusDisplayGlobalHBM.getHbmNit(key));
            }
            return nit;
        }
        OplusSpline curve = this.mSplineManager.getCurve(name);
        if (curve == null) {
            return Float.NaN;
        }
        return curve.interpolate(key);
    }

    public boolean isCurveValid(String name) {
        if (this.mSplineManager.getCurve(name) != null) {
            return true;
        }
        return false;
    }

    public void onBootPhase(Context context, int phase, boolean autoMode) {
    }

    public void dump(PrintWriter pw) {
        IndentingPrintWriter ipw = new IndentingPrintWriter(pw, "    ");
        ipw.increaseIndent();
        ipw.println("BrightnessModel:");
        ipw.increaseIndent();
        ipw.println("mPhysicalDisplayId=" + this.mPhysicalDisplayId);
        ipw.println("mMinBrightness=" + getMinBrightness());
        ipw.println("mMaxBrightness=" + getMaxBrightness());
        ipw.println("mNormalMaxNit=" + this.mNormalMaxNit);
        ipw.println("mTotalBrightness=" + this.mTotalBrightness);
        ipw.println("mMaxPanelBrightness=" + this.mMaxPanelBrightness);
        ipw.println("rateFast=" + this.mBrightnessRampRateFast);
        ipw.println("rateSlow=" + this.mBrightnessRampRateSlow);
        ipw.println("rateScreenOn=" + this.mBrightnessRampRateScreenOn);
        ipw.println("rateEdrEnterDef=" + this.mBrightnessRampRateEdrEnter);
        ipw.println("rateEdrExitDef=" + this.mBrightnessRampRateEdrExit);
        ipw.decreaseIndent();
        ipw.decreaseIndent();
    }

    public float getDefaultBrightness() {
        return this.mDefaultBrightness;
    }

    public float getMaxBrightness() {
        return ((Float) this.mLock.executeReadOperation(() -> this.mMaxBrightness)).floatValue();
    }

    public void setMaxBrightness(final float val) {
        this.mLock.executeWriteOperation(() -> this.mMaxBrightness = val);
    }

    public float getMaxConfigBrightness() {
        return this.mMaxConfigBrightness;
    }

    public float getNormalMaxNit() {
        return this.mNormalMaxNit;
    }

    public float getMaxPanelBrightness() {
        return this.mMaxPanelBrightness;
    }

    public float getMaxPanelNit() {
        if (this.mPanelNits != null && this.mPanelNits.length > 0) {
            return this.mPanelNits[this.mPanelNits.length - 1];
        }
        return 1.0f;
    }

    public float getMinPanelNit() {
        if (this.mPanelNits != null && this.mPanelNits.length > 0) {
            return this.mPanelNits[0];
        }
        return 1.0f;
    }

    public float getTotalBrightness() {
        return this.mTotalBrightness;
    }

    public float clampBrightness(float brightnessState) {
        return Math.min(this.mTotalBrightness, Math.max(brightnessState, getMinBrightness()));
    }

    public float clampNit(float nit) {
        return Math.min(getMaxPanelNit(), Math.max(nit, getMinPanelNit()));
    }

    public final float getNitFromBrightness(float brightness) {
        return this.mSplineManager.getCurve("backlights").interpolate(brightness);
    }

    public float getBrightnessFromNit(float nit) {
        return this.mSplineManager.getCurve("nits").interpolate(nit);
    }

    public OplusBrightnessPoint getMaxBrightnessPoint() {
        float maxLux = ((Float) this.mLock.executeReadOperation(() -> (this.mLuxs != null && this.mLuxs.length > 0) ? this.mLuxs[this.mLuxs.length - 1] : 10000.0f)).floatValue();
        float maxNit = (this.mPanelNits != null && this.mPanelNits.length > 0) ? this.mPanelNits[Math.min(Math.max((int) this.mMaxBrightness, 0), this.mPanelNits.length - 1)] : 800.0f;
        return new OplusBrightnessPoint(maxLux, maxNit, this.mMaxBrightness);
    }

    public float getBacklightFromBrightness(float brightness) {
        return brightness;
    }

    public float getBacklightFromNit(float nit) {
        float ret = (getBrightnessFromNit(nit) * 255.0f) / getMaxBrightness();
        return Math.min(ret, 255.0f);
    }

    public float getColorFromBrightness(float brightness) {
        return 1.0f;
    }

    public float getMinBrightness() {
        return ((Float) this.mLock.executeReadOperation(() -> this.mMinBrightness)).floatValue();
    }

    public void setMinBrightness(final float val) {
        this.mLock.executeWriteOperation(() -> this.mMinBrightness = val);
    }

    public float getMinConfigBrightness() {
        return this.mMinConfigBrightness;
    }

    public void changeSplines(boolean pwmEnable) {
        if (this.mSplineManager != null) {
            this.mSplineManager.changeSplines(pwmEnable);
        }
    }

    public void changeSinglePulseSplines(boolean change) {
        if (this.mSplineManager != null) {
            this.mSplineManager.changeSinglePulseSplines(change);
        }
    }

    public OplusBrightnessPoint getDefaultTargetPoint() {
        return new OplusBrightnessPoint(this.DEFAULT_BRIGHTNESSS_LUX, this.DEFAULT_BRIGHTNESSS_NIT, this.mDefaultBrightness);
    }

    public float getDarkLuxThreshold() {
        return this.mDarkLuxThreshold;
    }

    public float getLeftScaleInDarkEnv() {
        return this.mLeftScaleInDarkEnv;
    }

    public float getRightScaleInDarkEnv() {
        return this.mRightScaleInDarkEnv;
    }

    public float getStableSmallChangeScaleInDarkEnv() {
        return this.mStableSmallChangeScaleInDarkEnv;
    }

    public OplusDisplayGlobalHBM getOplusDisplayGlobalHBM() {
        return this.mOplusDisplayGlobalHBM;
    }

    public Pair<float[], float[]> getLuxSplineArrays() {
        return (Pair) this.mLock.executeReadOperation(() -> Pair.create(this.mLuxs, this.mNits));
    }

    public long getPhysicalDisplayId() {
        return this.mPhysicalDisplayId;
    }

    public boolean printAutoLuxIntervalForMarvels(String path) {
        if (this.mSplineManager != null) {
            return this.mSplineManager.printAutoLuxIntervalForMarvels(path);
        }
        return false;
    }

    private void sortMapsByValuesIncrease(HashMap<Integer, float[]> map, float[] array) {
        List<Map.Entry<Integer, float[]>> mapEntrylists = new ArrayList<>(map.entrySet());
        Collections.sort(mapEntrylists, (o1, o2) -> {
            int x = (int) (o1.getValue()[2] * 100000.0f);
            int y = (int) (o2.getValue()[2] * 100000.0f);
            return x - y;
        });
        float beginValue = -1.0f;
        int beginIndex = -1;
        for (int i = 1; i < mapEntrylists.size(); i++) {
            try {
                float front = mapEntrylists.get(i - 1).getValue()[2];
                float current = mapEntrylists.get(i).getValue()[2];
                if (current - front > 0.0f || i >= mapEntrylists.size() - 1) {
                    if (beginIndex >= 0) {
                        if (current <= beginValue) {
                            beginIndex--;
                            beginValue = mapEntrylists.get(beginIndex).getValue()[2];
                            Slog.d("OplusDisplayBrightnessModel", "sortMapsByValuesIncrease--final data, reset BI:" + beginIndex + " BV--" + beginValue);
                        }
                        float gap = (current - beginValue) / (i - beginIndex);
                        Slog.d("OplusDisplayBrightnessModel", "sortMapsByValuesIncrease--BV:" + beginValue + ", CV:" + current + ", G:" + gap);
                        for (int j = beginIndex + 1; j < i; j++) {
                            array[j] = mapEntrylists.get(j - 1).getValue()[2] + gap;
                            Slog.d("OplusDisplayBrightnessModel", "sortMapsByValuesIncrease--Array set [" + j + "," + array[j] + "]");
                        }
                        beginValue = -1.0f;
                        beginIndex = -1;
                    } else if (i == mapEntrylists.size() - 1) {
                        float lastValue = mapEntrylists.get(i).getValue()[2];
                        float beforeLastValue = mapEntrylists.get(i - 1).getValue()[2];
                        if (lastValue == beforeLastValue) {
                            float begin = mapEntrylists.get(i - 2).getValue()[2];
                            array[i - 1] = (begin + lastValue) / 2.0f;
                            Slog.d("OplusDisplayBrightnessModel", "sortMapsByValuesIncrease--last data repeat, Array set [" + (i - 1) + "," + array[i - 1] + "]");
                        }
                    }
                } else if (beginIndex < 0) {
                    beginValue = front;
                    beginIndex = i - 1;
                    Slog.d("OplusDisplayBrightnessModel", "sortMapsByValuesIncrease--BI:" + beginIndex + " BV--" + beginValue);
                }
                array[i] = mapEntrylists.get(i).getValue()[2];
            } catch (ArrayIndexOutOfBoundsException e) {
                Slog.e("OplusDisplayBrightnessModel", "sortMapsByValuesIncrease ArrayIndexOutOfBoundsException:" + e.getMessage());
                return;
            }
        }
    }

    public float getCurrentRegionRate(float targetY, float currY, float nextChange) {
        if (this.mRegionRateMap.size() == 0) {
            return nextChange;
        }
        for (Map.Entry<Float, float[]> entry : this.mRegionRateMap.entrySet()) {
            float rate = entry.getKey().floatValue();
            float[] regionLevels = entry.getValue();
            boolean isDown = false;
            boolean isUp = regionLevels[0] < currY && currY < regionLevels[1] && currY < targetY;
            if (regionLevels[1] < currY && currY < regionLevels[0] && currY > targetY) {
                isDown = true;
            }
            if (isUp || isDown) {
                return rate;
            }
        }
        return nextChange;
    }
}
