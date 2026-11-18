package com.android.server.display.feature.ambience.scenes.utils;

import android.util.ArrayMap;
import com.android.server.display.feature.ambience.events.delegate.DeepThinkerDelegate;
import com.android.server.display.feature.ambience.events.event.TimeEvent;
import com.android.server.display.feature.ambience.events.utils.EventBase;
import com.android.server.display.feature.ambience.utils.AmbienceLog;
import java.util.Map;

/* JADX INFO: loaded from: /run/media/sathiya/data/scratch/ambience.dex */
public class SceneConfig {
    public static final int INVALID_VALUE = -1;
    public static final String TAG = "SceneConfig";
    private int mLuxQueueUpTrendLuxCount = 4;
    private long mLuxQueueUpTrendLuxTime = 800;
    private int mLuxQueueDownTrendLuxCount = 6;
    private long mLuxQueueDownTrendLuxTime = 1200;
    private float mLuxCalculateOneTimeFilterWeight = 1.0f;
    private float mShakeRangeMainSensorUpWeight = 1.0f;
    private float mShakeRangeMainSensorDownWeight = 1.0f;
    private float mShakeRangeAuxSensorUpWeight = 1.0f;
    private float mShakeRangeAuxSensorDownWeight = 1.0f;
    private boolean mReactionAccelerateSwitch = true;
    private float mReactionAccelerateUpWeight = 1.5f;
    private int mReactionAccelerateUpOffset = 1;
    private float mReactionAccelerateDownWeight = 1.08f;
    private int mReactionAccelerateDownOffset = 3;
    private float mReactionAccelerateScaleMinBounds = 1.0f;
    private float mReactionAccelerateScaleMaxBounds = 3.333f;
    private boolean mAnimationAccelerateSwitch = true;
    private float mAnimationAccelerateUpWeight = -1.0f;
    private float mAnimationAccelerateDownWeight = -1.0f;
    private int mDayNightModeBeginTime = 73000;
    private int mDayNightModeEndTime = 210000;
    private long mDayNightModeDuration = 45000;
    private float mDayNightModeLuxThreshold = 1.5f;
    private float mInterimModeLuxThreshold = 1.0f;
    private long mInterimModeDuration = 12000;
    private float mMotionModeInTransportationLuxThreshold = 4.0f;
    private float mMotionModeOnFootLuxThreshold = 2.5f;
    private long mDarknessModeDuration = TimeEvent.DEFAULT_TIME_TWO_MINUTE;
    private float mDualLightMinLuxRange = 2.0f;

    public void setLuxQueueUpTrendLuxCount(int luxQueueUpTrendLuxCount) {
        this.mLuxQueueUpTrendLuxCount = luxQueueUpTrendLuxCount;
    }

    public int getLuxQueueUpTrendLuxCount() {
        return this.mLuxQueueUpTrendLuxCount;
    }

    public void setLuxQueueUpTrendLuxTime(long luxQueueUpTrendLuxTime) {
        this.mLuxQueueUpTrendLuxTime = luxQueueUpTrendLuxTime;
    }

    public long getLuxQueueUpTrendLuxTime() {
        return this.mLuxQueueUpTrendLuxTime;
    }

    public void setLuxQueueDownTrendLuxCount(int luxQueueDownTrendLuxCount) {
        this.mLuxQueueDownTrendLuxCount = luxQueueDownTrendLuxCount;
    }

    public int getLuxQueueDownTrendLuxCount() {
        return this.mLuxQueueDownTrendLuxCount;
    }

    public void setLuxQueueDownTrendLuxTime(long luxQueueDownTrendLuxTime) {
        this.mLuxQueueDownTrendLuxTime = luxQueueDownTrendLuxTime;
    }

    public long getLuxQueueDownTrendLuxTime() {
        return this.mLuxQueueDownTrendLuxTime;
    }

    public void setLuxCalculateOneTimeFilterWeight(float luxCalculateOneTimeFilterWeight) {
        this.mLuxCalculateOneTimeFilterWeight = luxCalculateOneTimeFilterWeight;
    }

    public float getLuxCalculateOneTimeFilterWeight() {
        return this.mLuxCalculateOneTimeFilterWeight;
    }

    public void setShakeRangeMainSensorUpWeight(float shakeRangeMainSensorUpWeight) {
        this.mShakeRangeMainSensorUpWeight = shakeRangeMainSensorUpWeight;
    }

    public float getShakeRangeMainSensorUpWeight() {
        return this.mShakeRangeMainSensorUpWeight;
    }

    public void setShakeRangeMainSensorDownWeight(float shakeRangeMainSensorDownWeight) {
        this.mShakeRangeMainSensorDownWeight = shakeRangeMainSensorDownWeight;
    }

    public float getShakeRangeMainSensorDownWeight() {
        return this.mShakeRangeMainSensorDownWeight;
    }

    public void setShakeRangeAuxSensorUpWeight(float shakeRangeAuxSensorUpWeight) {
        this.mShakeRangeAuxSensorUpWeight = shakeRangeAuxSensorUpWeight;
    }

    public float getShakeRangeAuxSensorUpWeight() {
        return this.mShakeRangeAuxSensorUpWeight;
    }

    public void setShakeRangeAuxSensorDownWeight(float shakeRangeAuxSensorDownWeight) {
        this.mShakeRangeAuxSensorDownWeight = shakeRangeAuxSensorDownWeight;
    }

    public float getShakeRangeAuxSensorDownWeight() {
        return this.mShakeRangeAuxSensorDownWeight;
    }

    public void setReactionAccelerateSwitch(boolean reactionAccelerateSwitch) {
        this.mReactionAccelerateSwitch = reactionAccelerateSwitch;
    }

    public boolean getReactionAccelerateSwitch() {
        return this.mReactionAccelerateSwitch;
    }

    public void setReactionAccelerateUpWeight(float reactionAccelerateUpWeight) {
        this.mReactionAccelerateUpWeight = reactionAccelerateUpWeight;
    }

    public float getReactionAccelerateUpWeight() {
        return this.mReactionAccelerateUpWeight;
    }

    public void setReactionAccelerateUpOffset(int reactionAccelerateUpOffset) {
        this.mReactionAccelerateUpOffset = reactionAccelerateUpOffset;
    }

    public int getReactionAccelerateUpOffset() {
        return this.mReactionAccelerateUpOffset;
    }

    public void setReactionAccelerateDownWeight(float reactionAccelerateDownWeight) {
        this.mReactionAccelerateDownWeight = reactionAccelerateDownWeight;
    }

    public float getReactionAccelerateDownWeight() {
        return this.mReactionAccelerateDownWeight;
    }

    public void setReactionAccelerateDownOffset(int reactionAccelerateDownOffset) {
        this.mReactionAccelerateDownOffset = reactionAccelerateDownOffset;
    }

    public int getReactionAccelerateDownOffset() {
        return this.mReactionAccelerateDownOffset;
    }

    public void setReactionAccelerateScaleMinBounds(float scale) {
        this.mReactionAccelerateScaleMinBounds = scale;
    }

    public float getReactionAccelerateScaleMinBounds() {
        return this.mReactionAccelerateScaleMinBounds;
    }

    public void setReactionAccelerateScaleMaxBounds(float scale) {
        this.mReactionAccelerateScaleMaxBounds = scale;
    }

    public float getReactionAccelerateScaleMaxBounds() {
        return this.mReactionAccelerateScaleMaxBounds;
    }

    public void setAnimationAccelerateSwitch(boolean animationAccelerateSwitch) {
        this.mAnimationAccelerateSwitch = animationAccelerateSwitch;
    }

    public boolean getAnimationAccelerateSwitch() {
        return this.mAnimationAccelerateSwitch;
    }

    public void setAnimationAccelerateUpWeight(float animationAccelerateUpWeight) {
        this.mAnimationAccelerateUpWeight = animationAccelerateUpWeight;
    }

    public float getAnimationAccelerateUpWeight() {
        return this.mAnimationAccelerateUpWeight;
    }

    public void setAnimationAccelerateDownWeight(float animationAccelerateDownWeight) {
        this.mAnimationAccelerateDownWeight = animationAccelerateDownWeight;
    }

    public float getAnimationAccelerateDownWeight() {
        return this.mAnimationAccelerateDownWeight;
    }

    public void setDayNightModeBeginTime(int dayNightModeBeginTime) {
        this.mDayNightModeBeginTime = dayNightModeBeginTime;
    }

    public int getDayNightModeBeginTime() {
        return this.mDayNightModeBeginTime;
    }

    public void setDayNightModeEndTime(int dayNightModeEndTime) {
        this.mDayNightModeEndTime = dayNightModeEndTime;
    }

    public int getDayNightModeEndTime() {
        return this.mDayNightModeEndTime;
    }

    public void setDayNightModeDuration(long dayNightModeDuration) {
        this.mDayNightModeDuration = dayNightModeDuration;
    }

    public long getDayNightModeDuration() {
        return this.mDayNightModeDuration;
    }

    public void setDayNightModeLuxThreshold(float dayNightModeLuxThreshold) {
        this.mDayNightModeLuxThreshold = dayNightModeLuxThreshold;
    }

    public float getDayNightModeLuxThreshold() {
        return this.mDayNightModeLuxThreshold;
    }

    public void setInterimModeLuxThreshold(float interimModeLuxThreshold) {
        this.mInterimModeLuxThreshold = interimModeLuxThreshold;
    }

    public float getInterimModeLuxThreshold() {
        return this.mInterimModeLuxThreshold;
    }

    public void setInterimModeDuration(long interimModeDuration) {
        this.mInterimModeDuration = interimModeDuration;
    }

    public long getInterimModeDuration() {
        return this.mInterimModeDuration;
    }

    public void setMotionModeInTransportationLuxThreshold(float motionModeInTransportationLuxThreshold) {
        this.mMotionModeInTransportationLuxThreshold = motionModeInTransportationLuxThreshold;
    }

    public float getMotionModeInTransportationLuxThreshold() {
        return this.mMotionModeInTransportationLuxThreshold;
    }

    public void setMotionModeOnFootLuxThreshold(float motionModeOnFootLuxThreshold) {
        this.mMotionModeOnFootLuxThreshold = motionModeOnFootLuxThreshold;
    }

    public float getMotionModeOnFootLuxThreshold() {
        return this.mMotionModeOnFootLuxThreshold;
    }

    public void setDarknessModeDuration(long darknessModeDuration) {
        this.mDarknessModeDuration = darknessModeDuration;
    }

    public long getDarknessModeDuration() {
        return this.mDarknessModeDuration;
    }

    public void setDualLightMinLuxRange(float range) {
        this.mDualLightMinLuxRange = range;
    }

    public float getDualLightMinLuxRange() {
        return this.mDualLightMinLuxRange;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code duplicated, block: B:95:0x0174  */
    public void updateKeyValueConfig(ArrayMap<String, String> config) {
        byte b;
        for (Map.Entry<String, String> entry : config.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            switch (key.hashCode()) {
                case -2064702388:
                    if (key.equals("LuxCalculateOneTimeFilterWeight")) {
                        b = 4;
                    } else {
                        b = -1;
                    }
                    break;
                case -1713715933:
                    if (key.equals("ReactionAccelerateScaleMinBounds")) {
                        b = 14;
                    } else {
                        b = -1;
                    }
                    break;
                case -1561841498:
                    if (key.equals("DualLightMinLuxRange")) {
                        b = 28;
                    } else {
                        b = -1;
                    }
                    break;
                case -1496350919:
                    if (key.equals("LuxQueueDownTrendLuxCount")) {
                        b = 2;
                    } else {
                        b = -1;
                    }
                    break;
                case -1446801797:
                    if (key.equals("ReactionAccelerateDownOffset")) {
                        b = 13;
                    } else {
                        b = -1;
                    }
                    break;
                case -1306035873:
                    if (key.equals("InterimModeLuxThreshold")) {
                        b = 23;
                    } else {
                        b = -1;
                    }
                    break;
                case -1294444663:
                    if (key.equals("DayNightModeEndTime")) {
                        b = 20;
                    } else {
                        b = -1;
                    }
                    break;
                case -1218614176:
                    if (key.equals("ReactionAccelerateDownWeight")) {
                        b = 12;
                    } else {
                        b = -1;
                    }
                    break;
                case -1198869381:
                    if (key.equals("DayNightModeLuxThreshold")) {
                        b = 22;
                    } else {
                        b = -1;
                    }
                    break;
                case -1121164265:
                    if (key.equals("DayNightModeBeginTime")) {
                        b = 19;
                    } else {
                        b = -1;
                    }
                    break;
                case -1081939139:
                    if (key.equals("ShakeRangeMainSensorUpWeight")) {
                        b = 5;
                    } else {
                        b = -1;
                    }
                    break;
                case -885192589:
                    if (key.equals("DayNightModeDuration")) {
                        b = 21;
                    } else {
                        b = -1;
                    }
                    break;
                case -879052957:
                    if (key.equals("LuxQueueDownTrendLuxTime")) {
                        b = 3;
                    } else {
                        b = -1;
                    }
                    break;
                case -692406908:
                    if (key.equals("ShakeRangeMainSensorDownWeight")) {
                        b = 6;
                    } else {
                        b = -1;
                    }
                    break;
                case -427807652:
                    if (key.equals("LuxQueueUpTrendLuxTime")) {
                        b = 1;
                    } else {
                        b = -1;
                    }
                    break;
                case -392648352:
                    if (key.equals("LuxQueueUpTrendLuxCount")) {
                        b = 0;
                    } else {
                        b = -1;
                    }
                    break;
                case -186874078:
                    if (key.equals("MotionModeOnFootLuxThreshold")) {
                        b = 26;
                    } else {
                        b = -1;
                    }
                    break;
                case -71265062:
                    if (key.equals("ReactionAccelerateSwitch")) {
                        b = 9;
                    } else {
                        b = -1;
                    }
                    break;
                case -45522714:
                    if (key.equals("MotionModeInTransportationLuxThreshold")) {
                        b = 25;
                    } else {
                        b = -1;
                    }
                    break;
                case 145040356:
                    if (key.equals("DarknessModeDuration")) {
                        b = 27;
                    } else {
                        b = -1;
                    }
                    break;
                case 201493633:
                    if (key.equals("ShakeRangeAuxSensorDownWeight")) {
                        b = 8;
                    } else {
                        b = -1;
                    }
                    break;
                case 501112186:
                    if (key.equals("ShakeRangeAuxSensorUpWeight")) {
                        b = 7;
                    } else {
                        b = -1;
                    }
                    break;
                case 900194135:
                    if (key.equals("InterimModeDuration")) {
                        b = 24;
                    } else {
                        b = -1;
                    }
                    break;
                case 1044630324:
                    if (key.equals("ReactionAccelerateUpOffset")) {
                        b = 11;
                    } else {
                        b = -1;
                    }
                    break;
                case 1272817945:
                    if (key.equals("ReactionAccelerateUpWeight")) {
                        b = 10;
                    } else {
                        b = -1;
                    }
                    break;
                case 1504228724:
                    if (key.equals("AnimationAccelerateUpWeight")) {
                        b = 17;
                    } else {
                        b = -1;
                    }
                    break;
                case 1808772789:
                    if (key.equals("ReactionAccelerateScaleMaxBounds")) {
                        b = 15;
                    } else {
                        b = -1;
                    }
                    break;
                case 2047409141:
                    if (key.equals("AnimationAccelerateSwitch")) {
                        b = 16;
                    } else {
                        b = -1;
                    }
                    break;
                case 2123812347:
                    if (key.equals("AnimationAccelerateDownWeight")) {
                        b = 18;
                    } else {
                        b = -1;
                    }
                    break;
                default:
                    b = -1;
                    break;
            }
            switch (b) {
                case 0:
                    setLuxQueueUpTrendLuxCount(parseString2Int(value, this.mLuxQueueUpTrendLuxCount));
                    break;
                case 1:
                    setLuxQueueUpTrendLuxTime(parseString2Long(value, this.mLuxQueueUpTrendLuxTime));
                    break;
                case 2:
                    setLuxQueueDownTrendLuxCount(parseString2Int(value, this.mLuxQueueDownTrendLuxCount));
                    break;
                case 3:
                    setLuxQueueDownTrendLuxTime(parseString2Long(value, this.mLuxQueueDownTrendLuxTime));
                    break;
                case 4:
                    setLuxCalculateOneTimeFilterWeight(parseString2Float(value, this.mLuxCalculateOneTimeFilterWeight));
                    break;
                case DeepThinkerDelegate.InOutDoorImpl.Constants.NRE_AVG_GPGSV /* 5 */:
                    setShakeRangeMainSensorUpWeight(parseString2Float(value, this.mShakeRangeMainSensorUpWeight));
                    break;
                case DeepThinkerDelegate.InOutDoorImpl.Constants.FEATURE_SIZE /* 6 */:
                    setShakeRangeMainSensorDownWeight(parseString2Float(value, this.mShakeRangeMainSensorDownWeight));
                    break;
                case 7:
                    setShakeRangeAuxSensorUpWeight(parseString2Float(value, this.mShakeRangeAuxSensorUpWeight));
                    break;
                case EventBase.OPERATE_REMOVE_ARRAY_LIST_FAIL_CODE /* 8 */:
                    setShakeRangeAuxSensorDownWeight(parseString2Float(value, this.mShakeRangeAuxSensorDownWeight));
                    break;
                case 9:
                    setReactionAccelerateSwitch(parseString2Boolean(value, this.mReactionAccelerateSwitch));
                    break;
                case 10:
                    setReactionAccelerateUpWeight(parseString2Float(value, this.mReactionAccelerateUpWeight));
                    break;
                case 11:
                    setReactionAccelerateUpOffset(parseString2Int(value, this.mReactionAccelerateUpOffset));
                    break;
                case 12:
                    setReactionAccelerateDownWeight(parseString2Float(value, this.mReactionAccelerateDownWeight));
                    break;
                case 13:
                    setReactionAccelerateDownOffset(parseString2Int(value, this.mReactionAccelerateDownOffset));
                    break;
                case 14:
                    setReactionAccelerateScaleMinBounds(parseString2Float(value, this.mReactionAccelerateScaleMinBounds));
                    break;
                case 15:
                    setReactionAccelerateScaleMaxBounds(parseString2Float(value, this.mReactionAccelerateScaleMaxBounds));
                    break;
                case EventBase.OPERATE_UNREGISTER_FAIL_CODE /* 16 */:
                    setAnimationAccelerateSwitch(parseString2Boolean(value, this.mAnimationAccelerateSwitch));
                    break;
                case 17:
                    setAnimationAccelerateUpWeight(parseString2Float(value, this.mAnimationAccelerateUpWeight));
                    break;
                case 18:
                    setAnimationAccelerateDownWeight(parseString2Float(value, this.mAnimationAccelerateDownWeight));
                    break;
                case 19:
                    setDayNightModeBeginTime(parseString2Int(value, this.mDayNightModeBeginTime));
                    break;
                case 20:
                    setDayNightModeEndTime(parseString2Int(value, this.mDayNightModeEndTime));
                    break;
                case 21:
                    setDayNightModeDuration(parseString2Long(value, this.mDayNightModeDuration));
                    break;
                case 22:
                    setDayNightModeLuxThreshold(parseString2Float(value, this.mDayNightModeLuxThreshold));
                    break;
                case 23:
                    setInterimModeLuxThreshold(parseString2Float(value, this.mInterimModeLuxThreshold));
                    break;
                case 24:
                    setInterimModeDuration(parseString2Long(value, this.mInterimModeDuration));
                    break;
                case 25:
                    setMotionModeInTransportationLuxThreshold(parseString2Float(value, this.mMotionModeInTransportationLuxThreshold));
                    break;
                case 26:
                    setMotionModeOnFootLuxThreshold(parseString2Float(value, this.mMotionModeOnFootLuxThreshold));
                    break;
                case 27:
                    setDarknessModeDuration(parseString2Long(value, this.mDarknessModeDuration));
                    break;
                case 28:
                    setDualLightMinLuxRange(parseString2Float(value, this.mDualLightMinLuxRange));
                    break;
                default:
                    AmbienceLog.e(TAG, "update config error:" + key + " ->" + value);
                    break;
            }
            AmbienceLog.f(TAG, "update config:" + key + " ->" + value);
        }
    }

    public int parseString2Int(String value, int def) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            AmbienceLog.e(TAG, "[NumberFormatException] parseString2Int error, value:" + value + " err:" + e.getMessage());
            return def;
        }
    }

    public float parseString2Float(String value, float def) {
        try {
            float ret = Float.parseFloat(value);
            if (Float.isFinite(ret)) {
                return ret;
            }
            return -1.0f;
        } catch (NumberFormatException e) {
            AmbienceLog.e(TAG, "[NumberFormatException] parseString2Float error, value:" + value + " err:" + e.getMessage());
            return def;
        }
    }

    public long parseString2Long(String value, long def) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            AmbienceLog.e(TAG, "[NumberFormatException] parseString2Long error, value:" + value + " err:" + e.getMessage());
            return def;
        }
    }

    public boolean parseString2Boolean(String value, boolean def) {
        try {
            return Boolean.parseBoolean(value);
        } catch (NumberFormatException e) {
            AmbienceLog.e(DeepThinkerDelegate.InOutDoorImpl.Constants.EMPTY, "[NumberFormatException] parseString2Boolean error, value:" + value + " err:" + e.getMessage());
            return def;
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("SceneConfig:");
        sb.append("{");
        sb.append("LuxQueueUpTrendLuxCount:").append(this.mLuxQueueUpTrendLuxCount).append(DeepThinkerDelegate.InOutDoorImpl.Constants.COMMA);
        sb.append("LuxQueueUpTrendLuxTime:").append(this.mLuxQueueUpTrendLuxTime).append(DeepThinkerDelegate.InOutDoorImpl.Constants.COMMA);
        sb.append("LuxQueueDownTrendLuxCount:").append(this.mLuxQueueDownTrendLuxCount).append(DeepThinkerDelegate.InOutDoorImpl.Constants.COMMA);
        sb.append("LuxQueueDownTrendLuxTime:").append(this.mLuxQueueDownTrendLuxTime).append(DeepThinkerDelegate.InOutDoorImpl.Constants.COMMA);
        sb.append("LuxCalculateOneTimeFilterWeight:").append(this.mLuxCalculateOneTimeFilterWeight).append(DeepThinkerDelegate.InOutDoorImpl.Constants.COMMA);
        sb.append("ShakeRangeMainSensorUpWeight:").append(this.mShakeRangeMainSensorUpWeight).append(DeepThinkerDelegate.InOutDoorImpl.Constants.COMMA);
        sb.append("ShakeRangeMainSensorDownWeight:").append(this.mShakeRangeMainSensorDownWeight).append(DeepThinkerDelegate.InOutDoorImpl.Constants.COMMA);
        sb.append("ShakeRangeAuxSensorUpWeight:").append(this.mShakeRangeAuxSensorUpWeight).append(DeepThinkerDelegate.InOutDoorImpl.Constants.COMMA);
        sb.append("ShakeRangeAuxSensorDownWeight:").append(this.mShakeRangeAuxSensorDownWeight).append(DeepThinkerDelegate.InOutDoorImpl.Constants.COMMA);
        sb.append("ReactionAccelerateSwitch:").append(this.mReactionAccelerateSwitch).append(DeepThinkerDelegate.InOutDoorImpl.Constants.COMMA);
        sb.append("ReactionAccelerateUpWeight:").append(this.mReactionAccelerateUpWeight).append(DeepThinkerDelegate.InOutDoorImpl.Constants.COMMA);
        sb.append("ReactionAccelerateUpOffset:").append(this.mReactionAccelerateUpOffset).append(DeepThinkerDelegate.InOutDoorImpl.Constants.COMMA);
        sb.append("ReactionAccelerateDownWeight:").append(this.mReactionAccelerateDownWeight).append(DeepThinkerDelegate.InOutDoorImpl.Constants.COMMA);
        sb.append("ReactionAccelerateDownOffset:").append(this.mReactionAccelerateDownOffset).append(DeepThinkerDelegate.InOutDoorImpl.Constants.COMMA);
        sb.append("ReactionAccelerateScaleMinBounds:").append(this.mReactionAccelerateScaleMinBounds).append(DeepThinkerDelegate.InOutDoorImpl.Constants.COMMA);
        sb.append("ReactionAccelerateScaleMaxBounds:").append(this.mReactionAccelerateScaleMaxBounds).append(DeepThinkerDelegate.InOutDoorImpl.Constants.COMMA);
        sb.append("AnimationAccelerateSwitch:").append(this.mAnimationAccelerateSwitch).append(DeepThinkerDelegate.InOutDoorImpl.Constants.COMMA);
        sb.append("AnimationAccelerateUpWeight:").append(this.mAnimationAccelerateUpWeight).append(DeepThinkerDelegate.InOutDoorImpl.Constants.COMMA);
        sb.append("AnimationAccelerateDownWeight:").append(this.mAnimationAccelerateDownWeight).append(DeepThinkerDelegate.InOutDoorImpl.Constants.COMMA);
        sb.append("DayNightModeBeginTime:").append(this.mDayNightModeBeginTime).append(DeepThinkerDelegate.InOutDoorImpl.Constants.COMMA);
        sb.append("DayNightModeEndTime:").append(this.mDayNightModeEndTime).append(DeepThinkerDelegate.InOutDoorImpl.Constants.COMMA);
        sb.append("DayNightModeDuration:").append(this.mDayNightModeDuration).append(DeepThinkerDelegate.InOutDoorImpl.Constants.COMMA);
        sb.append("DayNightModeLuxThreshold:").append(this.mDayNightModeLuxThreshold).append(DeepThinkerDelegate.InOutDoorImpl.Constants.COMMA);
        sb.append("InterimModeLuxThreshold:").append(this.mInterimModeLuxThreshold).append(DeepThinkerDelegate.InOutDoorImpl.Constants.COMMA);
        sb.append("InterimModeDuration:").append(this.mInterimModeDuration).append(DeepThinkerDelegate.InOutDoorImpl.Constants.COMMA);
        sb.append("MotionModeInTransportationLuxThreshold:").append(this.mMotionModeInTransportationLuxThreshold).append(DeepThinkerDelegate.InOutDoorImpl.Constants.COMMA);
        sb.append("MotionModeOnFootLuxThreshold:").append(this.mMotionModeOnFootLuxThreshold).append(DeepThinkerDelegate.InOutDoorImpl.Constants.COMMA);
        sb.append("DarknessModeDuration:").append(this.mDarknessModeDuration).append(DeepThinkerDelegate.InOutDoorImpl.Constants.COMMA);
        sb.append("DualLightMinLuxRange:").append(this.mDualLightMinLuxRange);
        sb.append("}");
        return sb.toString();
    }

    private class DefaultConfig {
        private static final float ANIMATION_ACCELERATE_DOWN_WEIGHT = -1.0f;
        private static final boolean ANIMATION_ACCELERATE_SWITCH = true;
        private static final float ANIMATION_ACCELERATE_UP_WEIGHT = -1.0f;
        private static final long DARKNESS_MODE_DURATION = 120000;
        private static final int DAY_NIGHT_MODE_BEGIN_TIME = 73000;
        private static final long DAY_NIGHT_MODE_DURATION = 45000;
        private static final int DAY_NIGHT_MODE_END_TIME = 210000;
        private static final float DAY_NIGHT_MODE_LUX_THRESHOLD = 1.5f;
        private static final float DUAL_LIGHT_MIN_LUX_RANGE = 2.0f;
        private static final long INTERIM_MODE_DURATION = 12000;
        private static final float INTERIM_MODE_LUX_THRESHOLD = 1.0f;
        private static final float LUX_CALCULATE_ONE_TIME_FILTER_WEIGHT = 1.0f;
        private static final int LUX_QUEUE_DOWN_TREND_LUX_COUNT = 6;
        private static final long LUX_QUEUE_DOWN_TREND_LUX_TIME = 1200;
        private static final int LUX_QUEUE_UP_TREND_LUX_COUNT = 4;
        private static final long LUX_QUEUE_UP_TREND_LUX_TIME = 800;
        private static final float MOTION_MODE_IN_TRANSPORTATION_LUX_THRESHOLD = 4.0f;
        private static final float MOTION_MODE_ON_FOOT_LUX_THRESHOLD = 2.5f;
        private static final int REACTION_ACCELERATE_DOWN_OFFSET = 3;
        private static final float REACTION_ACCELERATE_DOWN_WEIGHT = 1.08f;
        private static final float REACTION_ACCELERATE_SCALE_MAX_BOUNDS = 3.333f;
        private static final float REACTION_ACCELERATE_SCALE_MIN_BOUNDS = 1.0f;
        private static final boolean REACTION_ACCELERATE_SWITCH = true;
        private static final int REACTION_ACCELERATE_UP_OFFSET = 1;
        private static final float REACTION_ACCELERATE_UP_WEIGHT = 1.5f;
        private static final float SHAKE_RANGE_AUX_SENSOR_DOWN_WEIGHT = 1.0f;
        private static final float SHAKE_RANGE_AUX_SENSOR_UP_WEIGHT = 1.0f;
        private static final float SHAKE_RANGE_MAIN_SENSOR_DOWN_WEIGHT = 1.0f;
        private static final float SHAKE_RANGE_MAIN_SENSOR_UP_WEIGHT = 1.0f;

        private DefaultConfig(SceneConfig sceneConfig) {
        }
    }
}
