package com.android.server.display.feature.ambience.processes.process;

import android.common.OplusFeatureCache;
import android.os.OplusPropertyList;
import android.os.SystemProperties;
import android.util.MathUtils;
import android.util.Pair;
import android.util.SparseArray;
import com.android.server.content.IOplusFeatureConfigManagerInternal;
import com.android.server.display.DisplayPowerController;
import com.android.server.display.IDisplayBrightnessModelExt;
import com.android.server.display.config.global.ComfortableBrightnessConfig;
import com.android.server.display.config.global.OplusDisplayGlobalConfig;
import com.android.server.display.feature.ambience.events.delegate.DeepThinkerDelegate;
import com.android.server.display.feature.ambience.events.delegate.SensorDelegate;
import com.android.server.display.feature.ambience.events.event.TimeEvent;
import com.android.server.display.feature.ambience.scenes.OplusBrightnessSceneAdapter;
import com.android.server.display.feature.ambience.scenes.scene.ErraticScene;
import com.android.server.display.feature.ambience.scenes.utils.SceneBase;
import com.android.server.display.feature.ambience.scenes.utils.SceneConfig;
import com.android.server.display.feature.ambience.utils.AmbienceLog;
import com.android.server.display.feature.ambience.utils.BrightnessFeelingConverter;
import com.android.server.display.feature.ambience.utils.OplusLimitQueue;
import com.android.server.display.feature.ambience.utils.OplusLuxResult;
import com.android.server.display.feature.ambience.utils.OplusPair;
import java.util.Optional;

/* JADX INFO: loaded from: /run/media/sathiya/data/scratch/ambience.dex */
public class StabilityProcess {
    private static final float DEFAULT_AUX_DATA_SOURCE_PROPORTION = 0.5f;
    private static final float DEFAULT_DARK_ENVIRONMENT_SPECIAL_RANG_THRESHOLD = 2.0f;
    private static final float DEFAULT_DARK_ENVIRONMENT_THRESHOLD = 36.0f;
    private static final float DEFAULT_ERRATIC_SCENE_CHANGE_LUX_CONVERT_COEFFICIENT = 0.9f;
    private static final float DEFAULT_ERRATIC_SCENE_RIGHT_MIN_LUX_COEFFICIENT = 0.5f;
    private static final float DEFAULT_OUTDOOR_ENVIRONMENT_THRESHOLD = 5000.0f;
    private static final float DEFAULT_SHAKE_RANGE_MAX_LUX = 500000.0f;
    private static final String DISABLE = "0";
    private static final String ENABLE = "1";
    private static final float LARGE_CHANGE_MAX_GAP_LUX_RIGHT = 2000.0f;
    private static final float LARGE_CHANGE_WEIGHT = 0.6f;
    private static final String LOW_LIGHT_UNDER_SCREEN_LIGHT_SENSOR = "persist.sys.oplus.lowlightreport";
    private static final String LUX_SMALL_DEBOUNCE_EXPAND_SUPPORT = "oplus.software.display.lux_small_debounce_expand_support";
    private static final float[] SMALL_CHANGE_REGION_SPECIAL_LUX_THRESHOLD = {20.0f, 25.0f, 30.0f, 40.0f};
    private static final float[] SMALL_CHANGE_REGION_SPECIAL_PERCENT_CONSTANT_LEFT = {0.1066f, 0.0842f, 0.0896f};
    private static final float[] SMALL_CHANGE_REGION_SPECIAL_PERCENT_CONSTANT_RIGHT = {0.1429f, 0.1656f, 0.1603f};
    private static final float SMALL_CHANGE_WEIGHT = 0.4f;
    private final String TAG;
    private ComfortableBrightnessConfig mComfortableBrightnessConfig;
    private SparseArray<DisplayPowerController> mDpcList;
    private OplusBrightnessSceneAdapter mSceneAdapter;
    public final float[] STABLE_RIGHT_MIN_LUX_NORMAL_LIGHT = {5.01f, 3.01f};
    public final float[] STABLE_RIGHT_MIN_LUX_UNDER_SCREEN_LIGHT = {7.01f, 4.2f};
    public final float[] STABLE_RIGHT_MIN_LUX_UNDER_SCREEN_LIGHT_OLD_PROJECT = {10.01f, 7.01f};
    private final int INVALID_VALUE = -1;
    private final float[] LARGE_CHANGE_REGION_LEFT_COEFFICIENT = {4.0f, 6.0f, 1.6f, DEFAULT_DARK_ENVIRONMENT_SPECIAL_RANG_THRESHOLD};
    private final float[] LARGE_CHANGE_REGION_LEFT_CONSTANT = {SensorDelegate.DEFAULT_PROXIMITY_NEAR_CHECK_VALUE, 2.5f, -135.0f, SensorDelegate.DEFAULT_PROXIMITY_NEAR_CHECK_VALUE};
    private final float[] LARGE_CHANGE_REGION_RIGHT_COEFFICIENT = {2.5f, 10.0f, DEFAULT_DARK_ENVIRONMENT_SPECIAL_RANG_THRESHOLD, 1.5f};
    private final float[] LARGE_CHANGE_REGION_RIGHT_CONSTANT = {SensorDelegate.DEFAULT_PROXIMITY_NEAR_CHECK_VALUE, -150.0f, 130.0f, 380.0f};
    private final float[] SMALL_CHANGE_REGION_PERCENT_CONSTANT = {0.125f, 0.2f};
    private final float[] SMALL_CHANGE_REGION_LUX_THRESHOLD = {DEFAULT_DARK_ENVIRONMENT_SPECIAL_RANG_THRESHOLD, 4.0f, 6.0f, 8.0f, 10.0f, 15.0f, 20.0f, DEFAULT_OUTDOOR_ENVIRONMENT_THRESHOLD};
    private final float[] SMALL_CHANGE_REGION_PERCENT_CONSTANT_LEFT = {0.2853f, 0.2778f, 0.2593f, 0.25f, 0.2444f, 0.1926f, 0.1778f, 0.125f, 0.2f};
    private final float[] SMALL_CHANGE_REGION_PERCENT_CONSTANT_RIGHT = {0.3138f, 0.2778f, 0.2593f, 0.25f, 0.2444f, 0.1926f, 0.1778f, 0.125f, 0.2f};
    private OplusPair<Float, Float> mMainLargeRangePair = null;
    private OplusPair<Float, Float> mAuxLargeRangePair = null;
    private OplusPair<Float, Float> mMainSmallRangePair = null;
    private OplusPair<Float, Float> mHBMChangeRangePair = null;
    private SceneBase mCurrentScene = null;
    private Accelerator mAccelerator = null;
    private float mCurrentAutomaticLux = -1.0f;
    private float mShakeRangeDragCoefficient = Float.NaN;
    private boolean mIsNeedForceUpdateRange = false;
    private boolean mLuxSmallDebounceExpandSupport = false;
    public float mStableRightMinLux = 5.01f;
    public float mStableSmallRightMinLux = 3.01f;
    private final float[] LEFT_REGION_CHECK_NODE = {30.0f, 300.0f, 1080.0f, DEFAULT_SHAKE_RANGE_MAX_LUX};
    private final float[] RIGHT_REGION_CHECK_NODE = {20.0f, 35.0f, 500.0f, DEFAULT_SHAKE_RANGE_MAX_LUX};

    public StabilityProcess(String tag, OplusBrightnessSceneAdapter adapter, SparseArray<DisplayPowerController> dpcList) {
        this.mComfortableBrightnessConfig = null;
        this.mDpcList = null;
        this.mSceneAdapter = null;
        this.TAG = tag + "[StabilityProcess]";
        this.mDpcList = dpcList;
        this.mSceneAdapter = adapter;
        this.mComfortableBrightnessConfig = OplusDisplayGlobalConfig.getInstance().getComfortableBrightnessConfig();
    }

    public void updateRangeStrategy(OplusLuxResult luxResult) {
        float lux = luxResult.getLastAutomaticLux();
        if (Float.isNaN(lux)) {
            lux = luxResult.getLastFinalLux();
            AmbienceLog.f(this.TAG, "last automatic lux is NaN, get last final lux:" + lux);
        }
        SceneBase scene = luxResult.getScene();
        boolean isLuxChange = lux != this.mCurrentAutomaticLux;
        boolean isSceneChange = (scene == null || this.mCurrentScene == null || scene.getSceneName().equals(this.mCurrentScene.getSceneName())) ? false : true;
        boolean isForceUpdate = this.mIsNeedForceUpdateRange;
        if ((isLuxChange || isSceneChange || isForceUpdate) && scene != null) {
            this.mIsNeedForceUpdateRange = false;
            this.mCurrentAutomaticLux = Float.isNaN(lux) ? luxResult.getRealTimeLux() : lux;
            this.mCurrentScene = scene;
            this.mMainLargeRangePair = getLargeChangeLimit(this.mCurrentAutomaticLux);
            this.mAuxLargeRangePair = getLargeChangeLimitForSpecialLight(this.mCurrentAutomaticLux);
            this.mMainSmallRangePair = getSmallChangeLimit(this.mMainLargeRangePair);
            this.mHBMChangeRangePair = getHBMChangeLimit(this.mCurrentAutomaticLux);
            String str = this.TAG;
            StringBuilder sbAppend = new StringBuilder().append("updateRangeStrategy: reason[");
            String str2 = DeepThinkerDelegate.InOutDoorImpl.Constants.EMPTY;
            StringBuilder sbAppend2 = sbAppend.append(isLuxChange ? "LuxChange " : DeepThinkerDelegate.InOutDoorImpl.Constants.EMPTY).append(isSceneChange ? "SceneChange " : DeepThinkerDelegate.InOutDoorImpl.Constants.EMPTY);
            if (isForceUpdate) {
                str2 = "ForceUpdate ";
            }
            AmbienceLog.f(str, sbAppend2.append(str2).append("] baseLux:").append(this.mCurrentAutomaticLux).append(DeepThinkerDelegate.InOutDoorImpl.Constants.COMMA).append(toString()).toString());
        }
    }

    public OplusPair<Float, Float> getLargeChangeLimit() {
        return this.mMainLargeRangePair;
    }

    public OplusPair<Float, Float> getLargeChangeLimitForSpecialLight() {
        return this.mAuxLargeRangePair;
    }

    public OplusPair<Float, Float> getHBMChangeLimit() {
        return this.mHBMChangeRangePair;
    }

    public OplusPair<Float, Float> getSmallChangeLimit() {
        return this.mMainSmallRangePair;
    }

    public float getDefaultAccelerateScale() {
        return 1.0f;
    }

    public int getDefaultAccelerateCheckSize() {
        return 2;
    }

    public float getDefaultAccelerateCalculateWeight() {
        return 0.8f;
    }

    public float getDefaultAuxDataSourceProportion() {
        return 0.5f;
    }

    public static float getDefaultErraticSceneChangeLuxConvertCoefficient() {
        return DEFAULT_ERRATIC_SCENE_CHANGE_LUX_CONVERT_COEFFICIENT;
    }

    public static float getDefaultDarkEnvironmentLux() {
        return DEFAULT_DARK_ENVIRONMENT_THRESHOLD;
    }

    public static boolean isDarkEnvironmentLux(float lux) {
        return DEFAULT_DARK_ENVIRONMENT_THRESHOLD >= lux;
    }

    public static float getDefaultOutDoorEnvironmentLux() {
        return DEFAULT_OUTDOOR_ENVIRONMENT_THRESHOLD;
    }

    public static boolean isOutDoorEnvironmentLux(float lux) {
        return DEFAULT_OUTDOOR_ENVIRONMENT_THRESHOLD >= lux;
    }

    public float getAccelerateScale() {
        if (this.mAccelerator != null) {
            return this.mAccelerator.getAccelerateScale();
        }
        return 1.0f;
    }

    public float getAccelerateScale(float verifyLux) {
        this.mAccelerator = new Accelerator(this, verifyLux);
        return this.mAccelerator.getAccelerateScale();
    }

    public float getAccelerateScale(float verifyLux, SceneBase scene) {
        this.mAccelerator = new Accelerator(verifyLux, scene);
        return this.mAccelerator.getAccelerateScale();
    }

    public OplusPair<Float, Float> notifyDragInfo(float lux, float nit, float defaultNit) {
        double currentFeeling = BrightnessFeelingConverter.getFeelingFromNit(nit);
        double defaultFeeling = BrightnessFeelingConverter.getFeelingFromNit(defaultNit);
        if (nit > defaultNit) {
            double feelingScale = currentFeeling / defaultFeeling;
            if (feelingScale > 1.0d) {
                this.mShakeRangeDragCoefficient = (float) feelingScale;
            } else {
                this.mShakeRangeDragCoefficient = Float.NaN;
            }
        } else {
            this.mShakeRangeDragCoefficient = Float.NaN;
        }
        forceUpdateShakeRange();
        AmbienceLog.f(this.TAG, "lux:" + lux + " nit:" + defaultNit + " -> " + nit + " feeling:" + defaultFeeling + " -> " + currentFeeling + " Coefficient:" + this.mShakeRangeDragCoefficient);
        return OplusPair.create("dragFeeling", Float.valueOf((float) currentFeeling), Float.valueOf((float) defaultFeeling));
    }

    public void resetDragInfo() {
        this.mShakeRangeDragCoefficient = Float.NaN;
        forceUpdateShakeRange();
    }

    public float getStableRightMinLux() {
        return this.mStableRightMinLux;
    }

    public float getStableSmallRightMinLux() {
        return this.mStableSmallRightMinLux;
    }

    public void onBootCompleted() {
        boolean isUnderScreenLight = (OplusPropertyList.SCREEN_UNDERLIGHTSENSOR_REGION.equals(DISABLE) && OplusPropertyList.UNDERSCREEN_LIGHTSENSOR_SUPPORT.equals(DISABLE)) ? false : true;
        boolean isHighPrecisionUnderScreenLight = SystemProperties.getInt(LOW_LIGHT_UNDER_SCREEN_LIGHT_SENSOR, 1) == 1;
        float[] minChangeLux = this.STABLE_RIGHT_MIN_LUX_NORMAL_LIGHT;
        if (isUnderScreenLight) {
            if (isHighPrecisionUnderScreenLight) {
                minChangeLux = this.STABLE_RIGHT_MIN_LUX_UNDER_SCREEN_LIGHT;
            } else {
                minChangeLux = this.STABLE_RIGHT_MIN_LUX_UNDER_SCREEN_LIGHT_OLD_PROJECT;
            }
        }
        try {
            this.mLuxSmallDebounceExpandSupport = OplusFeatureCache.getOrCreate(IOplusFeatureConfigManagerInternal.DEFAULT, new Object[0]).hasFeature(LUX_SMALL_DEBOUNCE_EXPAND_SUPPORT);
        } catch (Throwable t) {
            AmbienceLog.e(this.TAG, "Failed to check LUX_SMALL_DEBOUNCE_EXPAND_SUPPORT: " + t);
            this.mLuxSmallDebounceExpandSupport = false;
        }
        this.mStableRightMinLux = minChangeLux[0];
        this.mStableSmallRightMinLux = minChangeLux[1];
        AmbienceLog.i(this.TAG, "isUnderScreenLight:" + isUnderScreenLight + " isHighPrecisionUnderScreenLight:" + isHighPrecisionUnderScreenLight + " min limit[" + this.mStableRightMinLux + DeepThinkerDelegate.InOutDoorImpl.Constants.COMMA + this.mStableSmallRightMinLux + "]");
    }

    public void forceUpdateShakeRange() {
        this.mIsNeedForceUpdateRange = true;
    }

    public boolean isLargeChange(float weightLux) {
        return this.mMainLargeRangePair == null || Float.isNaN(((Float) this.mMainLargeRangePair.first).floatValue()) || Float.isNaN(((Float) this.mMainLargeRangePair.second).floatValue()) || weightLux <= ((Float) this.mMainLargeRangePair.first).floatValue() || weightLux >= ((Float) this.mMainLargeRangePair.second).floatValue();
    }

    public boolean isSmallChange(float weightLux) {
        return this.mMainSmallRangePair == null || Float.isNaN(((Float) this.mMainSmallRangePair.first).floatValue()) || Float.isNaN(((Float) this.mMainSmallRangePair.second).floatValue()) || weightLux <= ((Float) this.mMainSmallRangePair.first).floatValue() || weightLux >= ((Float) this.mMainSmallRangePair.second).floatValue();
    }

    public boolean isLargeChangeForSpecialLight(float weightLux) {
        return this.mAuxLargeRangePair == null || Float.isNaN(((Float) this.mAuxLargeRangePair.first).floatValue()) || Float.isNaN(((Float) this.mAuxLargeRangePair.second).floatValue()) || weightLux <= ((Float) this.mAuxLargeRangePair.first).floatValue() || weightLux >= ((Float) this.mAuxLargeRangePair.second).floatValue();
    }

    public boolean isHBMChange(float weightLux) {
        if (this.mHBMChangeRangePair == null) {
            return false;
        }
        return weightLux < ((Float) this.mHBMChangeRangePair.first).floatValue() || weightLux > ((Float) this.mHBMChangeRangePair.second).floatValue();
    }

    public boolean isHBMMode() {
        return this.mHBMChangeRangePair != null && ((Float) this.mHBMChangeRangePair.first).floatValue() > SensorDelegate.DEFAULT_PROXIMITY_NEAR_CHECK_VALUE;
    }

    private boolean isErraticSceneLargeRangeStrategyReduceNotSupported() {
        return this.mComfortableBrightnessConfig != null && this.mComfortableBrightnessConfig.mErraticSceneLargeRangeStrategyReduceNotSupport;
    }

    public String toString() {
        return " scene:" + this.mCurrentScene.getSceneName() + " " + this.mMainLargeRangePair + " " + this.mAuxLargeRangePair + " " + this.mMainSmallRangePair + " " + this.mHBMChangeRangePair;
    }

    public OplusPair<Float, Float> getLargeChangeLimit(float targetLux) {
        return OplusPair.create("large", Float.valueOf(getLargeChangeValueLeft(targetLux)), Float.valueOf(getLargeChangeValueRight(targetLux)));
    }

    private OplusPair<Float, Float> getHBMChangeLimit(float targetLux) {
        Pair<Float, Float> range;
        float leftLux = Float.NaN;
        float rightLux = Float.NaN;
        if (this.mDpcList != null) {
            for (int i = 0; i < this.mDpcList.size(); i++) {
                IDisplayBrightnessModelExt model = null;
                DisplayPowerController dpc = this.mDpcList.get(i);
                if (dpc != null) {
                    model = dpc.getDisplayBrightnessModel();
                    if (model == null) {
                        model = new IDisplayBrightnessModelExt() {
                            @Override
                            public Pair<Float, Float> getHBMRangeLux(float lux) {
                                if (lux >= 10000.0f) {
                                    return new Pair<>(5000.0f, 10000.0f);
                                }
                                return null;
                            }
                        };
                    }
                }
                if (model != null && (range = model.getHBMRangeLux(targetLux)) != null) {
                    float leftLux2 = Float.isNaN(leftLux) ? ((Float) range.first).floatValue() : Math.min(leftLux, ((Float) range.first).floatValue());
                    rightLux = Float.isNaN(rightLux) ? ((Float) range.second).floatValue() : Math.max(rightLux, ((Float) range.second).floatValue());
                    leftLux = leftLux2;
                }
            }
        }
        if (Float.isNaN(leftLux) || Float.isNaN(rightLux)) {
            return null;
        }
        OplusPair<Float, Float> hbmChangeLimit = new OplusPair<>("hbm", Float.valueOf(leftLux), Float.valueOf(rightLux));
        return hbmChangeLimit;
    }

    private OplusPair<Float, Float> getLargeChangeLimitForSpecialLight(float targetLux) {
        return OplusPair.create("largeForAux", Float.valueOf(getLargeChangeValueLeftForSpecialLight(targetLux)), Float.valueOf(getLargeChangeValueRightForSpecialLight(targetLux)));
    }

    public OplusPair<Float, Float> getSmallChangeLimit(OplusPair<Float, Float> largeRangePair) {
        return OplusPair.create("small", Float.valueOf(getSmallChangeValueLeft(largeRangePair, this.mCurrentAutomaticLux)), Float.valueOf(getSmallChangeValueRight(largeRangePair, this.mCurrentAutomaticLux)));
    }

    public OplusPair<Float, Float> getSmallChangeLimit(OplusPair<Float, Float> largeRangePair, float curLux) {
        return OplusPair.create("small", Float.valueOf(getSmallChangeValueLeft(largeRangePair, curLux)), Float.valueOf(getSmallChangeValueRight(largeRangePair, curLux)));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public float getLargeChangeValueLeft(float targetLux) {
        return getLargeChangeValueLeft(targetLux, this.mCurrentScene);
    }

    private float getLargeChangeValueLeft(float targetLux, SceneBase scene) {
        float largeChangeX = targetLux;
        int idx = findReginIdx(targetLux, this.LEFT_REGION_CHECK_NODE);
        if (idx >= 0 && idx < this.LEFT_REGION_CHECK_NODE.length) {
            largeChangeX = (largeChangeX / this.LARGE_CHANGE_REGION_LEFT_COEFFICIENT[idx]) + this.LARGE_CHANGE_REGION_LEFT_CONSTANT[idx];
        } else {
            AmbienceLog.i(this.TAG, "not find left large change value");
        }
        float sceneDownScale = scene.getSceneConfig().getShakeRangeMainSensorDownWeight();
        float largeChangeX2 = Math.max(((1.0f - sceneDownScale) * targetLux) + (largeChangeX * sceneDownScale), SensorDelegate.DEFAULT_PROXIMITY_NEAR_CHECK_VALUE);
        if (this.mComfortableBrightnessConfig != null && this.mComfortableBrightnessConfig.mWeakLightSpecialShakeRangeSupport && targetLux <= DEFAULT_DARK_ENVIRONMENT_SPECIAL_RANG_THRESHOLD && OplusLimitQueue.getSum(this.mSceneAdapter.getWeightLuxLimitQueue().getLinkedListByPastTime(TimeEvent.DEFAULT_TIME_TEN_SECONDS)) > 0.0d) {
            return -1.0f;
        }
        return largeChangeX2;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public float getLargeChangeValueRight(float targetLux) {
        return getLargeChangeValueRight(targetLux, this.mCurrentScene);
    }

    private float getLargeChangeValueRight(float targetLux, SceneBase scene) {
        float largeChangeX = targetLux;
        int idx = findReginIdx(targetLux, this.RIGHT_REGION_CHECK_NODE);
        if (idx >= 0 && idx < this.RIGHT_REGION_CHECK_NODE.length) {
            largeChangeX = (this.LARGE_CHANGE_REGION_RIGHT_COEFFICIENT[idx] * largeChangeX) + this.LARGE_CHANGE_REGION_RIGHT_CONSTANT[idx];
        } else {
            AmbienceLog.i(this.TAG, "not find right large change value");
        }
        float largeChangeX2 = Math.min(largeChangeX, LARGE_CHANGE_MAX_GAP_LUX_RIGHT + targetLux);
        float sceneUpScale = scene.getSceneConfig().getShakeRangeMainSensorUpWeight();
        float erraticSceneCoefficient = 0.5f;
        if (isErraticSceneLargeRangeStrategyReduceNotSupported()) {
            erraticSceneCoefficient = 1.0f;
        }
        float largeChangeX3 = Math.max(((1.0f - sceneUpScale) * targetLux) + (largeChangeX2 * sceneUpScale), getStableRightMinLux() * (ErraticScene.isSameScene(scene) ? erraticSceneCoefficient : 1.0f));
        if (!Float.isNaN(this.mShakeRangeDragCoefficient)) {
            return Math.max(((1.0f - this.mShakeRangeDragCoefficient) * targetLux) + (this.mShakeRangeDragCoefficient * largeChangeX3), SensorDelegate.DEFAULT_PROXIMITY_NEAR_CHECK_VALUE);
        }
        return largeChangeX3;
    }

    private float getLargeChangeValueLeftForSpecialLight(float targetLux) {
        return getLargeChangeValueLeftForSpecialLight(targetLux, this.mCurrentScene);
    }

    private float getLargeChangeValueLeftForSpecialLight(float targetLux, SceneBase scene) {
        float specialLightLargeChangeX = targetLux;
        int idx = findReginIdx(targetLux, this.LEFT_REGION_CHECK_NODE);
        if (idx >= 0 && idx < this.LEFT_REGION_CHECK_NODE.length) {
            specialLightLargeChangeX = (specialLightLargeChangeX / this.LARGE_CHANGE_REGION_LEFT_COEFFICIENT[idx]) + this.LARGE_CHANGE_REGION_LEFT_CONSTANT[idx];
        } else {
            AmbienceLog.i(this.TAG, "not find left large change value");
        }
        float sceneDownScale = scene.getSceneConfig().getShakeRangeAuxSensorDownWeight();
        float specialLightLargeChangeX2 = Math.max(((1.0f - sceneDownScale) * targetLux) + (specialLightLargeChangeX * sceneDownScale), SensorDelegate.DEFAULT_PROXIMITY_NEAR_CHECK_VALUE);
        if (this.mComfortableBrightnessConfig != null && this.mComfortableBrightnessConfig.mWeakLightSpecialShakeRangeSupport && targetLux <= DEFAULT_DARK_ENVIRONMENT_SPECIAL_RANG_THRESHOLD && OplusLimitQueue.getSum(this.mSceneAdapter.getWeightLuxLimitQueue().getLinkedListByPastTime(TimeEvent.DEFAULT_TIME_TEN_SECONDS)) > 0.0d) {
            return -1.0f;
        }
        return specialLightLargeChangeX2;
    }

    private float getLargeChangeValueRightForSpecialLight(float targetLux) {
        return getLargeChangeValueRightForSpecialLight(targetLux, this.mCurrentScene);
    }

    private float getLargeChangeValueRightForSpecialLight(float targetLux, SceneBase scene) {
        float specialLightLargeChangeX = targetLux;
        int idx = findReginIdx(targetLux, this.RIGHT_REGION_CHECK_NODE);
        if (idx >= 0 && idx < this.RIGHT_REGION_CHECK_NODE.length) {
            specialLightLargeChangeX = (this.LARGE_CHANGE_REGION_RIGHT_COEFFICIENT[idx] * specialLightLargeChangeX) + this.LARGE_CHANGE_REGION_RIGHT_CONSTANT[idx];
        } else {
            AmbienceLog.i(this.TAG, "not find right large change value");
        }
        float specialLightLargeChangeX2 = Math.min(specialLightLargeChangeX, LARGE_CHANGE_MAX_GAP_LUX_RIGHT + targetLux);
        float sceneUpScale = scene.getSceneConfig().getShakeRangeAuxSensorUpWeight();
        return Math.max(((1.0f - sceneUpScale) * targetLux) + (specialLightLargeChangeX2 * sceneUpScale), this.mCurrentScene.getSceneConfig().getDualLightMinLuxRange());
    }

    private float getSmallChangeValueLeft(OplusPair<Float, Float> largeRangePair, float curLux) {
        float smallChangeX;
        float largeChangeX = ((Float) largeRangePair.second).floatValue() - ((Float) largeRangePair.first).floatValue();
        if (this.mLuxSmallDebounceExpandSupport) {
            if (curLux <= this.SMALL_CHANGE_REGION_LUX_THRESHOLD[0]) {
                smallChangeX = curLux - (this.SMALL_CHANGE_REGION_PERCENT_CONSTANT_LEFT[0] * largeChangeX);
            } else if (curLux <= this.SMALL_CHANGE_REGION_LUX_THRESHOLD[1]) {
                smallChangeX = curLux - (this.SMALL_CHANGE_REGION_PERCENT_CONSTANT_LEFT[1] * largeChangeX);
            } else if (curLux <= this.SMALL_CHANGE_REGION_LUX_THRESHOLD[2]) {
                smallChangeX = curLux - (this.SMALL_CHANGE_REGION_PERCENT_CONSTANT_LEFT[2] * largeChangeX);
            } else if (curLux <= this.SMALL_CHANGE_REGION_LUX_THRESHOLD[3]) {
                smallChangeX = curLux - (this.SMALL_CHANGE_REGION_PERCENT_CONSTANT_LEFT[3] * largeChangeX);
            } else if (curLux <= this.SMALL_CHANGE_REGION_LUX_THRESHOLD[4]) {
                smallChangeX = curLux - (this.SMALL_CHANGE_REGION_PERCENT_CONSTANT_LEFT[4] * largeChangeX);
            } else if (curLux <= this.SMALL_CHANGE_REGION_LUX_THRESHOLD[5]) {
                smallChangeX = curLux - (this.SMALL_CHANGE_REGION_PERCENT_CONSTANT_LEFT[5] * largeChangeX);
            } else if (curLux <= this.SMALL_CHANGE_REGION_LUX_THRESHOLD[6]) {
                smallChangeX = curLux - (this.SMALL_CHANGE_REGION_PERCENT_CONSTANT_LEFT[6] * largeChangeX);
            } else if (curLux <= this.SMALL_CHANGE_REGION_LUX_THRESHOLD[7]) {
                smallChangeX = curLux - (this.SMALL_CHANGE_REGION_PERCENT_CONSTANT_LEFT[7] * largeChangeX);
            } else {
                smallChangeX = curLux - (this.SMALL_CHANGE_REGION_PERCENT_CONSTANT_LEFT[8] * largeChangeX);
            }
        } else if (curLux > DEFAULT_OUTDOOR_ENVIRONMENT_THRESHOLD) {
            smallChangeX = curLux - (this.SMALL_CHANGE_REGION_PERCENT_CONSTANT[1] * largeChangeX);
        } else {
            smallChangeX = curLux - (this.SMALL_CHANGE_REGION_PERCENT_CONSTANT[0] * largeChangeX);
        }
        if (curLux > SMALL_CHANGE_REGION_SPECIAL_LUX_THRESHOLD[0] && curLux <= SMALL_CHANGE_REGION_SPECIAL_LUX_THRESHOLD[1]) {
            smallChangeX = curLux - (SMALL_CHANGE_REGION_SPECIAL_PERCENT_CONSTANT_LEFT[0] * largeChangeX);
        } else if (curLux > SMALL_CHANGE_REGION_SPECIAL_LUX_THRESHOLD[1] && curLux <= SMALL_CHANGE_REGION_SPECIAL_LUX_THRESHOLD[2]) {
            smallChangeX = curLux - (SMALL_CHANGE_REGION_SPECIAL_PERCENT_CONSTANT_LEFT[1] * largeChangeX);
        } else if (curLux > SMALL_CHANGE_REGION_SPECIAL_LUX_THRESHOLD[2] && curLux <= SMALL_CHANGE_REGION_SPECIAL_LUX_THRESHOLD[3]) {
            smallChangeX = curLux - (SMALL_CHANGE_REGION_SPECIAL_PERCENT_CONSTANT_LEFT[2] * largeChangeX);
        }
        if (isErraticSceneLargeRangeStrategyReduceNotSupported()) {
            smallChangeX = (LARGE_CHANGE_WEIGHT * ((Float) largeRangePair.first).floatValue()) + (SMALL_CHANGE_WEIGHT * smallChangeX);
        }
        if (smallChangeX < ((Float) largeRangePair.first).floatValue()) {
            float smallChangeX2 = ((Float) largeRangePair.first).floatValue();
            return smallChangeX2;
        }
        return smallChangeX;
    }

    private float getSmallChangeValueRight(OplusPair<Float, Float> largeRangePair, float curLux) {
        float smallChangeX;
        float largeChangeX = ((Float) largeRangePair.second).floatValue() - ((Float) largeRangePair.first).floatValue();
        if (this.mLuxSmallDebounceExpandSupport) {
            if (curLux <= this.SMALL_CHANGE_REGION_LUX_THRESHOLD[0]) {
                smallChangeX = (this.SMALL_CHANGE_REGION_PERCENT_CONSTANT_RIGHT[0] * largeChangeX) + curLux;
            } else if (curLux <= this.SMALL_CHANGE_REGION_LUX_THRESHOLD[1]) {
                smallChangeX = (this.SMALL_CHANGE_REGION_PERCENT_CONSTANT_RIGHT[1] * largeChangeX) + curLux;
            } else if (curLux <= this.SMALL_CHANGE_REGION_LUX_THRESHOLD[2]) {
                smallChangeX = (this.SMALL_CHANGE_REGION_PERCENT_CONSTANT_RIGHT[2] * largeChangeX) + curLux;
            } else if (curLux <= this.SMALL_CHANGE_REGION_LUX_THRESHOLD[3]) {
                smallChangeX = (this.SMALL_CHANGE_REGION_PERCENT_CONSTANT_RIGHT[3] * largeChangeX) + curLux;
            } else if (curLux <= this.SMALL_CHANGE_REGION_LUX_THRESHOLD[4]) {
                smallChangeX = (this.SMALL_CHANGE_REGION_PERCENT_CONSTANT_RIGHT[4] * largeChangeX) + curLux;
            } else if (curLux <= this.SMALL_CHANGE_REGION_LUX_THRESHOLD[5]) {
                smallChangeX = (this.SMALL_CHANGE_REGION_PERCENT_CONSTANT_RIGHT[5] * largeChangeX) + curLux;
            } else if (curLux <= this.SMALL_CHANGE_REGION_LUX_THRESHOLD[6]) {
                smallChangeX = (this.SMALL_CHANGE_REGION_PERCENT_CONSTANT_RIGHT[6] * largeChangeX) + curLux;
            } else if (curLux <= this.SMALL_CHANGE_REGION_LUX_THRESHOLD[7]) {
                smallChangeX = (this.SMALL_CHANGE_REGION_PERCENT_CONSTANT_RIGHT[7] * largeChangeX) + curLux;
            } else {
                smallChangeX = (this.SMALL_CHANGE_REGION_PERCENT_CONSTANT_RIGHT[8] * largeChangeX) + curLux;
            }
        } else if (curLux > DEFAULT_OUTDOOR_ENVIRONMENT_THRESHOLD) {
            smallChangeX = (this.SMALL_CHANGE_REGION_PERCENT_CONSTANT[1] * largeChangeX) + curLux;
        } else {
            smallChangeX = (this.SMALL_CHANGE_REGION_PERCENT_CONSTANT[0] * largeChangeX) + curLux;
        }
        if (curLux > SMALL_CHANGE_REGION_SPECIAL_LUX_THRESHOLD[0] && curLux <= SMALL_CHANGE_REGION_SPECIAL_LUX_THRESHOLD[1]) {
            smallChangeX = curLux + (SMALL_CHANGE_REGION_SPECIAL_PERCENT_CONSTANT_RIGHT[0] * largeChangeX);
        } else if (curLux > SMALL_CHANGE_REGION_SPECIAL_LUX_THRESHOLD[1] && curLux <= SMALL_CHANGE_REGION_SPECIAL_LUX_THRESHOLD[2]) {
            smallChangeX = curLux + (SMALL_CHANGE_REGION_SPECIAL_PERCENT_CONSTANT_RIGHT[1] * largeChangeX);
        } else if (curLux > SMALL_CHANGE_REGION_SPECIAL_LUX_THRESHOLD[2] && curLux <= SMALL_CHANGE_REGION_SPECIAL_LUX_THRESHOLD[3]) {
            smallChangeX = curLux + (SMALL_CHANGE_REGION_SPECIAL_PERCENT_CONSTANT_RIGHT[2] * largeChangeX);
        }
        if (isErraticSceneLargeRangeStrategyReduceNotSupported()) {
            smallChangeX = (LARGE_CHANGE_WEIGHT * ((Float) largeRangePair.second).floatValue()) + (SMALL_CHANGE_WEIGHT * smallChangeX);
        }
        float smallChangeX2 = Math.max(getStableSmallRightMinLux(), smallChangeX);
        if (smallChangeX2 > ((Float) largeRangePair.second).floatValue()) {
            float smallChangeX3 = ((Float) largeRangePair.second).floatValue();
            return smallChangeX3;
        }
        return smallChangeX2;
    }

    private int findReginIdx(float lux, float[] array) {
        if (array == null || array.length == 0) {
            return -1;
        }
        int idx = 0;
        while (idx < array.length && lux - array[idx] > SensorDelegate.DEFAULT_PROXIMITY_NEAR_CHECK_VALUE) {
            idx++;
        }
        return Math.min(idx, array.length - 1);
    }

    private class Accelerator {
        private static final int ACCELERATE_COUNT_MAX = 10;
        public static final float DEFAULT_ACCELERATE_REALTIME_QUEUE_CALCULATE_WEIGHT = 0.8f;
        public static final int DEFAULT_ACCELERATE_REALTIME_QUEUE_CHECK_SIZE = 2;
        private static final float DEFAULT_ACCELERATE_SCALE = 1.0f;
        private static final float DOWN_DIRECTION_POINT_CHECK_LIMIT = 1.0f;
        private float accelerateScale;

        public Accelerator(StabilityProcess stabilityProcess, float verifyLux) {
            this(verifyLux, stabilityProcess.mCurrentScene);
        }

        public Accelerator(float verifyLux, SceneBase scene) {
            this.accelerateScale = 1.0f;
            if (!Float.isNaN(verifyLux) && scene != null) {
                SceneConfig sceneConfig = scene.getSceneConfig();
                if (sceneConfig.getReactionAccelerateSwitch() && StabilityProcess.this.mMainLargeRangePair != null && verifyLux != MathUtils.constrain(verifyLux, ((Float) StabilityProcess.this.mMainLargeRangePair.first).floatValue(), ((Float) StabilityProcess.this.mMainLargeRangePair.second).floatValue())) {
                    boolean isUp = verifyLux - StabilityProcess.this.mCurrentAutomaticLux > SensorDelegate.DEFAULT_PROXIMITY_NEAR_CHECK_VALUE;
                    int AccelerateCount = calculateAccelerateCount(isUp, StabilityProcess.this.mCurrentAutomaticLux, verifyLux, 0);
                    float directionWeight = isUp ? sceneConfig.getReactionAccelerateUpWeight() : sceneConfig.getReactionAccelerateDownWeight();
                    int countOffset = isUp ? sceneConfig.getReactionAccelerateUpOffset() : sceneConfig.getReactionAccelerateDownOffset();
                    this.accelerateScale = (float) Math.pow(directionWeight, Math.max(AccelerateCount - countOffset, SensorDelegate.DEFAULT_PROXIMITY_NEAR_CHECK_VALUE));
                    this.accelerateScale = MathUtils.constrain(this.accelerateScale, sceneConfig.getReactionAccelerateScaleMinBounds(), sceneConfig.getReactionAccelerateScaleMaxBounds());
                }
            }
        }

        private int calculateAccelerateCount(boolean isUp, float currentLux, float verifyLux, int count) {
            StabilityProcess stabilityProcess = StabilityProcess.this;
            float limitLux = isUp ? stabilityProcess.getLargeChangeValueRight(currentLux) : stabilityProcess.getLargeChangeValueLeft(currentLux);
            boolean isOverRange = false;
            if (!isUp ? verifyLux < limitLux : verifyLux > limitLux) {
                isOverRange = true;
            }
            if (count >= ACCELERATE_COUNT_MAX || ((currentLux < 1.0f && !isUp) || !isOverRange)) {
                return count;
            }
            return calculateAccelerateCount(isUp, limitLux, verifyLux, count + 1);
        }

        public float getAccelerateScale() {
            return this.accelerateScale;
        }
    }
}
