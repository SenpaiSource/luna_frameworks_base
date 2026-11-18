package com.android.server.display.feature.ambience.utils;

import com.android.server.display.feature.ambience.events.delegate.SensorDelegate;
import com.android.server.display.feature.ambience.events.event.TimeEvent;
import com.android.server.display.feature.ambience.events.utils.PhoneState;
import com.android.server.display.feature.ambience.processes.process.StabilityProcess;
import com.android.server.display.feature.ambience.scenes.utils.AmbientData;
import com.android.server.display.feature.ambience.scenes.utils.SceneBase;
import com.android.server.display.feature.ambience.scenes.utils.SceneConfig;
import java.util.LinkedList;

/* JADX INFO: loaded from: /run/media/sathiya/data/scratch/ambience.dex */
public final class OplusLuxResult {
    private final AmbientData mAmbientData;
    private boolean mIsLuxPostProcessMonitoring;
    private final boolean mIsSceneChange;
    private final float mLastAutomaticLux;
    private final float mLastFinalLux;
    private final long mLastResultTime;
    private final PhoneState mPhoneState;
    private final SceneBase mScene;
    private final StabilityProcess mStabilityProcess;
    private float mAccelerateScale = 1.0f;
    private int mAmbientLightDirection = 0;
    private LinkedList<Float> mCalculateLuxQueue = null;
    private float mWeightLux = Float.NaN;
    private float mAutomaticLux = Float.NaN;
    private float mFinalLux = Float.NaN;
    private int mBrightnessChangeDirection = 0;
    private LinkedList<String> mChangeReasonProcess = new LinkedList<>();
    private final long mResultTime = System.currentTimeMillis();

    public enum REASON {
        InitChange,
        LargeChange,
        AuxLargeChange,
        StableForceChange,
        RecoverChange,
        HBMChange,
        ErraticStagnantChange,
        InterimMode,
        DayNightMode,
        DarkestMode,
        MotionMode,
        SyncUpdate,
        ResetForceUpdate
    }

    public OplusLuxResult(AmbientData data, StabilityProcess stabilityProcess, OplusLuxResult lastResult, boolean isReset) {
        this.mIsLuxPostProcessMonitoring = false;
        this.mAmbientData = data;
        this.mPhoneState = data.getPhoneState();
        this.mScene = data.getScene();
        this.mStabilityProcess = stabilityProcess;
        if (lastResult != null) {
            this.mLastFinalLux = lastResult.isNeedUpdate() ? lastResult.getFinalLux() : lastResult.getLastFinalLux();
            this.mLastAutomaticLux = lastResult.isStabilityProcessSuccess() ? lastResult.getAutomaticLux() : lastResult.getLastAutomaticLux();
            this.mIsSceneChange = !this.mScene.getSceneName().equals(lastResult.getScene().getSceneName());
            this.mIsLuxPostProcessMonitoring = lastResult.isLuxPostProcessMonitoring();
            this.mLastResultTime = lastResult.getResultTime();
        } else {
            this.mLastFinalLux = Float.NaN;
            this.mLastAutomaticLux = Float.NaN;
            this.mIsSceneChange = true;
            this.mIsLuxPostProcessMonitoring = false;
            this.mLastResultTime = this.mResultTime;
            if (this.mPhoneState.isImportantDataExist()) {
                setAutomaticLux(this.mPhoneState.getLuxEvent().getOriginalLux());
                setChangeReason(REASON.InitChange);
            }
        }
        if (isReset) {
            setChangeReason(REASON.ResetForceUpdate);
        }
    }

    public AmbientData getAmbientData() {
        return this.mAmbientData;
    }

    public PhoneState getPhoneState() {
        return this.mPhoneState;
    }

    public SceneBase getScene() {
        return this.mScene;
    }

    public StabilityProcess getStabilityProcess() {
        return this.mStabilityProcess;
    }

    public float getRealTimeLux() {
        if (this.mPhoneState.isImportantDataExist()) {
            return this.mPhoneState.getLuxEvent().getLux();
        }
        return Float.NaN;
    }

    public float getLastFinalLux() {
        return this.mLastFinalLux;
    }

    public float getLastAutomaticLux() {
        return this.mLastAutomaticLux;
    }

    public void setAccelerateScale(float scale) {
        this.mAccelerateScale = scale;
    }

    public float getAccelerateScale() {
        return this.mAccelerateScale;
    }

    public boolean isAccelerateState() {
        return this.mAccelerateScale > 1.0f;
    }

    public void setCalculateLuxQueue(LinkedList<Float> List) {
        this.mCalculateLuxQueue = List;
    }

    public LinkedList<Float> getCalculateLuxQueue() {
        return this.mCalculateLuxQueue;
    }

    public void setAmbientLightDirection(int direction) {
        this.mAmbientLightDirection = direction;
    }

    public int getAmbientLightDirection() {
        return this.mAmbientLightDirection;
    }

    public int getBrightnessChangeDirection() {
        return this.mBrightnessChangeDirection;
    }

    public boolean isLuxQueueProcessSuccess() {
        return (this.mCalculateLuxQueue == null || this.mCalculateLuxQueue.isEmpty()) ? false : true;
    }

    public void setWeightLux(float lux) {
        this.mWeightLux = lux;
    }

    public float getWeightLux() {
        return this.mWeightLux;
    }

    public boolean isLuxDataProcessSuccess() {
        return (Float.isNaN(this.mWeightLux) || this.mWeightLux < SensorDelegate.DEFAULT_PROXIMITY_NEAR_CHECK_VALUE || Float.compare(this.mLastAutomaticLux, this.mWeightLux) == 0) ? false : true;
    }

    public final void setAutomaticLux(float lux) {
        if (!Float.isNaN(lux)) {
            this.mAutomaticLux = lux;
            setFinalLux(lux);
        }
    }

    public float getAutomaticLux() {
        return this.mAutomaticLux;
    }

    public void setFinalLux(float lux) {
        this.mFinalLux = lux;
        if (!Float.isNaN(this.mLastFinalLux)) {
            this.mBrightnessChangeDirection = Float.compare(lux, this.mLastFinalLux);
        }
    }

    public float getFinalLux() {
        return this.mFinalLux;
    }

    public OplusPair<Float, Float> getAnimatingAcceleration() {
        SceneConfig config = this.mScene.getSceneConfig();
        float defaultScale = this.mStabilityProcess.getDefaultAccelerateScale();
        if (config.getAnimationAccelerateSwitch()) {
            float upWeight = config.getAnimationAccelerateUpWeight();
            float downWeight = config.getAnimationAccelerateDownWeight();
            if (upWeight < SensorDelegate.DEFAULT_PROXIMITY_NEAR_CHECK_VALUE || downWeight < SensorDelegate.DEFAULT_PROXIMITY_NEAR_CHECK_VALUE) {
                upWeight = this.mAccelerateScale;
                downWeight = this.mAccelerateScale;
            }
            if (!isAccelerateState() && this.mPhoneState.isCompleteData()) {
                if (this.mPhoneState.getMotionEvent().isFoot()) {
                    downWeight /= 1.2f;
                }
                if (this.mPhoneState.getMotionEvent().isTransportation()) {
                    downWeight /= 1.5f;
                }
            }
            if (isLuxDataProcessSuccess() && this.mWeightLux > 100.0f) {
                upWeight = Math.max(upWeight, defaultScale);
                downWeight = Math.max(downWeight, defaultScale);
            }
            return OplusPair.create("AnimatingAcceleration", Float.valueOf(Math.min(upWeight, 2.0f)), Float.valueOf(Math.min(downWeight, 2.0f)));
        }
        return OplusPair.create("AnimatingAcceleration", Float.valueOf(defaultScale), Float.valueOf(defaultScale));
    }

    public boolean isStabilityProcessSuccess() {
        return !Float.isNaN(this.mAutomaticLux) && this.mAutomaticLux >= SensorDelegate.DEFAULT_PROXIMITY_NEAR_CHECK_VALUE;
    }

    public final void setChangeReason(REASON reason) {
        this.mChangeReasonProcess.add(reason.name());
    }

    public LinkedList<String> getChangeReasonProcess() {
        return this.mChangeReasonProcess;
    }

    public boolean isInitChange() {
        return this.mChangeReasonProcess.contains(REASON.InitChange.name());
    }

    public boolean isResetForceUpdate() {
        return this.mChangeReasonProcess.contains(REASON.ResetForceUpdate.name());
    }

    public void setLuxPostProcessMonitoring(boolean monitoring) {
        this.mIsLuxPostProcessMonitoring = monitoring;
    }

    public boolean isLuxPostProcessMonitoring() {
        return this.mIsLuxPostProcessMonitoring;
    }

    public boolean isNeedUpdate() {
        if (!Float.isNaN(this.mFinalLux)) {
            if (Float.compare(this.mFinalLux, this.mLastFinalLux) != 0 || isInitChange() || isResetForceUpdate()) {
                return true;
            }
            return false;
        }
        return false;
    }

    public boolean isNeedSync() {
        boolean needSyncUpdate = false;
        if (Float.isNaN(this.mFinalLux)) {
            needSyncUpdate = (Float.isNaN(this.mLastFinalLux) || Float.isNaN(this.mLastAutomaticLux) || this.mLastFinalLux == this.mLastAutomaticLux) ? false : true;
            this.mFinalLux = Float.isNaN(this.mWeightLux) ? this.mLastAutomaticLux : this.mWeightLux;
            setChangeReason(REASON.SyncUpdate);
        }
        return needSyncUpdate;
    }

    public boolean isDangerLux(float lux) {
        if (isStabilityProcessSuccess()) {
            if (this.mAutomaticLux <= lux) {
                return true;
            }
        } else if (this.mLastAutomaticLux <= lux) {
            return true;
        }
        return false;
    }

    public boolean isSceneChange() {
        return this.mIsSceneChange;
    }

    public long getResultTime() {
        return this.mResultTime;
    }

    public long getUpdateIntervalTime() {
        return this.mResultTime - this.mLastResultTime;
    }

    public long getUpdatePastTime() {
        return TimeEvent.getSystemMillisTime() - this.mResultTime;
    }

    public String getDirectionString(int direction) {
        if (direction == 1) {
            return "Brightening";
        }
        if (direction == -1) {
            return "Darkening";
        }
        return "Stable";
    }

    public String toString() {
        return "finalLux:" + this.mFinalLux + ", weightLux:" + this.mWeightLux + ", automaticLux:" + this.mAutomaticLux + ", lastFinalLux:" + this.mLastFinalLux + ", lastAutomaticLux:" + this.mLastAutomaticLux + ", deviceDirection:" + getDirectionString(this.mBrightnessChangeDirection) + ", environmentDirection:" + getDirectionString(this.mAmbientLightDirection) + ", scene:" + this.mScene.getSceneName() + ", accelerateScale:" + this.mAccelerateScale + ", updateProcess:" + this.mChangeReasonProcess + ", luxQueue:" + this.mCalculateLuxQueue;
    }
}
