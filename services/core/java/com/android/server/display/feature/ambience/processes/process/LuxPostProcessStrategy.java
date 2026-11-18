package com.android.server.display.feature.ambience.processes.process;

import android.hardware.display.DisplayManagerInternal;
import com.android.server.display.config.global.ComfortableBrightnessConfig;
import com.android.server.display.config.global.OplusDisplayGlobalConfig;
import com.android.server.display.feature.ambience.events.delegate.SensorDelegate;
import com.android.server.display.feature.ambience.events.event.InOutDoorEvent;
import com.android.server.display.feature.ambience.events.event.MotionEvent;
import com.android.server.display.feature.ambience.events.event.NetworkEvent;
import com.android.server.display.feature.ambience.events.event.TimeEvent;
import com.android.server.display.feature.ambience.events.utils.PhoneState;
import com.android.server.display.feature.ambience.scenes.OplusBrightnessSceneAdapter;
import com.android.server.display.feature.ambience.scenes.scene.StableScene;
import com.android.server.display.feature.ambience.scenes.utils.SceneBase;
import com.android.server.display.feature.ambience.scenes.utils.SceneConfig;
import com.android.server.display.feature.ambience.utils.AmbienceLog;
import com.android.server.display.feature.ambience.utils.OplusLimitQueue;
import com.android.server.display.feature.ambience.utils.OplusLuxResult;
import java.util.LinkedList;

/* JADX INFO: loaded from: /run/media/sathiya/data/scratch/ambience.dex */
public class LuxPostProcessStrategy {
    private final String TAG;
    private ComfortableBrightnessConfig mComfortableBrightnessConfig;
    private DarkestMode mDarkestMode;
    private DayNightMode mDayNightMode;
    private InterimMode mInterimMode;
    private MotionMode mMotionMode;
    private OplusBrightnessSceneAdapter mSceneAdapter;
    private final int INVALID_VALUE = -1;
    private final int DEFAULT_RESET_REASON_POWER_OFF = 0;
    private final int DEFAULT_RESET_REASON_MODE_SWITCH = 1;
    private boolean mIsChangeToAutoMode = false;

    public LuxPostProcessStrategy(String tag, OplusBrightnessSceneAdapter adapter) {
        this.mSceneAdapter = null;
        this.mComfortableBrightnessConfig = null;
        this.mInterimMode = null;
        this.mDayNightMode = null;
        this.mDarkestMode = null;
        this.mMotionMode = null;
        this.TAG = tag + "[LuxPostProcessStrategy]";
        this.mSceneAdapter = adapter;
        this.mInterimMode = new InterimMode();
        this.mDayNightMode = new DayNightMode();
        this.mDarkestMode = new DarkestMode();
        this.mMotionMode = new MotionMode();
        this.mComfortableBrightnessConfig = OplusDisplayGlobalConfig.getInstance().getComfortableBrightnessConfig();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public float getCurrentCheckLux(OplusLuxResult luxResult) {
        if (luxResult == null) {
            return Float.NaN;
        }
        float resultLux = luxResult.getFinalLux();
        if (Float.isNaN(resultLux)) {
            float resultLux2 = luxResult.getLastAutomaticLux();
            if (Float.isNaN(resultLux2)) {
                return luxResult.getRealTimeLux();
            }
            return resultLux2;
        }
        return resultLux;
    }

    public void onScreenEvent(int displayId, boolean isScreenOn, boolean isFolding, DisplayManagerInternal.DisplayPowerRequest request) {
        if (!isScreenOn) {
            resetAll(0);
        }
    }

    public void brightnessModeChanged(boolean isAuto) {
        this.mIsChangeToAutoMode = isAuto;
        if (!isAuto) {
            resetAll(1);
        }
    }

    public void notifyDragInfo(float lux, float dragNit, float currentNit) {
        this.mInterimMode.notifyDragInfo(lux, dragNit, currentNit);
        this.mDayNightMode.notifyDragInfo(lux, dragNit, currentNit);
        this.mDarkestMode.notifyDragInfo(lux, dragNit, currentNit);
        this.mMotionMode.notifyDragInfo(lux, dragNit, currentNit);
    }

    private void resetAll(int reason) {
        this.mInterimMode.reset(reason);
        this.mDayNightMode.reset(reason);
        this.mDarkestMode.reset(reason);
        this.mMotionMode.reset(reason);
    }

    public float getInterimModeLux(OplusLuxResult luxResult) {
        return this.mInterimMode.getInterimModeLux(luxResult);
    }

    public float getDayNightModeLux(OplusLuxResult luxResult) {
        return this.mDayNightMode.getDayNightModeLux(luxResult);
    }

    public boolean isDayMode(long normalizationTime, SceneConfig sceneConfig) {
        return this.mDayNightMode.isBrightMode(normalizationTime, sceneConfig);
    }

    public float getDarkestModeLux(OplusLuxResult luxResult) {
        return this.mDarkestMode.getDarkestModeLux(luxResult);
    }

    public float getMotionModeLux(OplusLuxResult luxResult) {
        return this.mMotionMode.getMotionModeLux(luxResult);
    }

    /* JADX INFO: Access modifiers changed from: private */
    class InterimMode {
        private final float DEFAULT_STABLE_ENVIRONMENT_STANDARD_DIVINATION_LIMIT = 0.01f;
        private final int DEFAULT_INTERIM_MODE_MONITOR_SIZE = 20;
        private OplusLimitQueue<Float> mInterimModeList = new OplusLimitQueue<>(20, null);
        private SceneBase mInterimModeScene = null;
        private boolean isTakeEffect = false;
        private long mHateDargTime = -1;
        private final float INTERIM_CHECK_LUX = 2.0f;

        public InterimMode() {
            this.mInterimModeList.clear();
        }

        public float getInterimModeLux(OplusLuxResult luxResult) {
            if (LuxPostProcessStrategy.this.mComfortableBrightnessConfig != null && LuxPostProcessStrategy.this.mComfortableBrightnessConfig.mInterimModeSupport && luxResult != null && luxResult.getPhoneState().isCompleteData()) {
                if (luxResult.isNeedUpdate() && !Float.isNaN(luxResult.getWeightLux())) {
                    this.mInterimModeList.add(Float.valueOf(luxResult.getWeightLux()));
                } else if (this.mInterimModeList.isEmpty()) {
                    this.mInterimModeList.add(Float.valueOf(luxResult.getLastFinalLux()));
                }
                if (this.mInterimModeScene == null && !luxResult.isNeedUpdate()) {
                    return Float.NaN;
                }
                if (luxResult.isDangerLux(this.INTERIM_CHECK_LUX)) {
                    SceneBase scene = this.mInterimModeScene != null ? this.mInterimModeScene : luxResult.getScene();
                    SceneConfig sceneConfig = scene.getSceneConfig();
                    float luxThreshold = sceneConfig.getInterimModeLuxThreshold();
                    long interimModeDuration = sceneConfig.getInterimModeDuration();
                    float checkLux = LuxPostProcessStrategy.this.getCurrentCheckLux(luxResult);
                    this.mInterimModeScene = null;
                    if (checkLux <= luxThreshold && !luxResult.isInitChange() && !LuxPostProcessStrategy.this.mIsChangeToAutoMode) {
                        LinkedList<Float> interimList = this.mInterimModeList.getLinkedListByPastTime(interimModeDuration);
                        if (interimList.isEmpty()) {
                            interimList = this.mInterimModeList.getLinkedListByCount(1);
                        }
                        if (isForceRespondInterim(luxResult) || OplusLimitQueue.getStandardDeviation(interimList) > 0.009999999776482582d) {
                            this.isTakeEffect = true;
                            luxResult.setLuxPostProcessMonitoring(true);
                            this.mInterimModeScene = scene;
                            return Math.max(checkLux, luxThreshold);
                        }
                        this.isTakeEffect = false;
                        luxResult.setAutomaticLux(luxResult.getWeightLux());
                    }
                }
                LuxPostProcessStrategy.this.mIsChangeToAutoMode = false;
            }
            this.isTakeEffect = false;
            return Float.NaN;
        }

        public void reset(int reason) {
            this.mInterimModeScene = null;
            if (reason == 1) {
                this.mHateDargTime = -1L;
            }
            if (reason == 0 && this.mHateDargTime > 0 && TimeEvent.getSystemMillisTime() - this.mHateDargTime > TimeEvent.DEFAULT_TIME_TWO_HOUR) {
                this.mHateDargTime = -1L;
                AmbienceLog.f(LuxPostProcessStrategy.this.TAG, "reset interim hate drag");
            }
        }

        public void notifyDragInfo(float lux, float dragNit, float currentNit) {
            if (dragNit < currentNit) {
                if (this.isTakeEffect) {
                    AmbienceLog.f(LuxPostProcessStrategy.this.TAG, "hate interim");
                    this.mHateDargTime = TimeEvent.getSystemMillisTime();
                    return;
                }
                return;
            }
            this.mHateDargTime = -1L;
        }

        public boolean isTakeEffect() {
            return this.isTakeEffect;
        }

        private boolean isForceRespondInterim(final OplusLuxResult luxResult) {
            PhoneState state = luxResult.getPhoneState();
            if (state != null && state.isCompleteData() && state.getInOutDoorEvent().isOutDoor() && !state.getNetworkEvent().isWifiConnected() && this.mHateDargTime < 0) {
                AmbienceLog.logic(LuxPostProcessStrategy.this.TAG, new AmbienceLog.ILogPrintLogic() { // from class: com.android.server.display.feature.ambience.processes.process.LuxPostProcessStrategy$InterimMode$$ExternalSyntheticLambda0
                    @Override // com.android.server.display.feature.ambience.utils.AmbienceLog.ILogPrintLogic
                    public final boolean isLogPrint() {
                        return LuxPostProcessStrategy.InterimMode.oplusLambda$isForceRespondInterim$0(luxResult);
                    }
                }, "isForceRespondInterim: outDoor");
                return true;
            }
            return false;
        }

        static /* synthetic */ boolean oplusLambda$isForceRespondInterim$0(OplusLuxResult luxResult) {
            return AmbienceLog.isLogOn && luxResult.isStabilityProcessSuccess();
        }
    }

    private class DayNightMode {
        private SceneBase mDayNightModeScene = null;
        private long mDayNightModeTime = -1;
        private long mHateDargTime = -1;
        private boolean isTakeEffect = false;
        private boolean isBrightMode = false;
        private final float DAY_NIGHT_CHECK_LUX = 5.0f;

        public DayNightMode() {
        }

        public float getDayNightModeLux(OplusLuxResult luxResult) {
            SceneBase scene;
            if (LuxPostProcessStrategy.this.mComfortableBrightnessConfig != null && LuxPostProcessStrategy.this.mComfortableBrightnessConfig.mDayNightModeSupport && luxResult != null && luxResult.getPhoneState().isCompleteData()) {
                if (this.mDayNightModeScene == null && !luxResult.isNeedUpdate()) {
                    return Float.NaN;
                }
                if (this.mDayNightModeScene != null) {
                    scene = this.mDayNightModeScene;
                } else {
                    scene = luxResult.getScene();
                }
                SceneBase scene2 = scene;
                SceneConfig sceneConfig = scene2.getSceneConfig();
                long dayNightModeDuration = sceneConfig.getDayNightModeDuration();
                float dayNightModeLuxThreshold = sceneConfig.getDayNightModeLuxThreshold();
                int currentTime = luxResult.getPhoneState().getTimeEvent().getNormalizationTime();
                float checkLux = LuxPostProcessStrategy.this.getCurrentCheckLux(luxResult);
                this.mDayNightModeScene = null;
                if (isRespondDayNightMode(currentTime, sceneConfig, luxResult, checkLux <= dayNightModeLuxThreshold)) {
                    if (this.mDayNightModeTime == -1) {
                        this.mDayNightModeTime = System.currentTimeMillis();
                    }
                    if (System.currentTimeMillis() - this.mDayNightModeTime > dayNightModeDuration) {
                        this.isTakeEffect = false;
                        return checkLux;
                    }
                    if (!StableScene.isSameScene(luxResult.getScene())) {
                        this.mDayNightModeTime = -1L;
                    }
                    this.isTakeEffect = true;
                    luxResult.setLuxPostProcessMonitoring(true);
                    this.mDayNightModeScene = scene2;
                    return Math.max(dayNightModeLuxThreshold, checkLux);
                }
            }
            this.isTakeEffect = false;
            this.mDayNightModeScene = null;
            this.mDayNightModeTime = -1L;
            return Float.NaN;
        }

        private boolean isRespondDayNightMode(long normalizationTime, SceneConfig sceneConfig, OplusLuxResult luxResult, boolean thresholdLimit) {
            boolean result = false;
            boolean isCurrentBright = isBrightMode(normalizationTime, sceneConfig);
            boolean isDayNightSwitch = isDayNightSwitch(isCurrentBright);
            if (thresholdLimit) {
                if (isCurrentBright) {
                    result = (isDayNightSwitch || LuxPostProcessStrategy.this.mIsChangeToAutoMode) ? false : true;
                } else if (luxResult.isInitChange()) {
                    this.isBrightMode = isCurrentBright;
                    result = false;
                } else {
                    result = isDayNightSwitch;
                }
            } else {
                this.isBrightMode = isCurrentBright;
            }
            return result && this.mHateDargTime < 0;
        }

        public boolean isBrightMode(long normalizationTime, SceneConfig sceneConfig) {
            if (sceneConfig != null) {
                return normalizationTime >= ((long) sceneConfig.getDayNightModeBeginTime()) && normalizationTime <= ((long) sceneConfig.getDayNightModeEndTime());
            }
            AmbienceLog.e(LuxPostProcessStrategy.this.TAG, "sceneConfig is null!");
            return false;
        }

        public boolean isDayNightSwitch(boolean currentMode) {
            return Boolean.logicalXor(this.isBrightMode, currentMode);
        }

        public void reset(int reason) {
            if (this.isTakeEffect) {
                this.mDayNightModeScene = null;
                this.mDayNightModeTime = -1L;
            }
            if (reason == 1) {
                this.mDayNightModeScene = null;
                this.mDayNightModeTime = -1L;
                this.mHateDargTime = -1L;
            }
            if (reason == 0 && this.mHateDargTime > 0 && !this.isBrightMode) {
                AmbienceLog.f(LuxPostProcessStrategy.this.TAG, "reset dayNight hate drag");
                this.mHateDargTime = -1L;
            }
        }

        public void notifyDragInfo(float lux, float dragNit, float currentNit) {
            if (dragNit >= currentNit) {
                if (lux > this.DAY_NIGHT_CHECK_LUX) {
                    this.mHateDargTime = -1L;
                }
            } else if (this.isTakeEffect) {
                AmbienceLog.f(LuxPostProcessStrategy.this.TAG, "hate dayNight");
                this.mHateDargTime = TimeEvent.getSystemMillisTime();
            }
        }

        public boolean isTakeEffect() {
            return this.isTakeEffect;
        }
    }

    private class DarkestMode {
        private static final float AVERAGE_LUX_THRESHOLD = 1.0f;
        private final float DARKEST_CHECK_LUX = SensorDelegate.DEFAULT_PROXIMITY_NEAR_CHECK_VALUE;
        private final float DARKEST_LUX = -1.0f;
        private SceneBase mDarkestModeScene = null;
        private long mDarkestModeTime = -1;
        private boolean isTakeEffect = false;

        public DarkestMode() {
        }

        public float getDarkestModeLux(OplusLuxResult luxResult) {
            if (LuxPostProcessStrategy.this.mComfortableBrightnessConfig != null && LuxPostProcessStrategy.this.mComfortableBrightnessConfig.mDarkestModeSupport && luxResult != null && luxResult.getPhoneState().isCompleteData()) {
                float realtimeQueueAverageLux = (float) OplusLimitQueue.getAverage(LuxPostProcessStrategy.this.mSceneAdapter.getRealtimeLuxLimitQueue().getLinkedList());
                if ((this.mDarkestModeScene == null && !luxResult.isNeedUpdate() && realtimeQueueAverageLux >= AVERAGE_LUX_THRESHOLD) || LuxPostProcessStrategy.this.mSceneAdapter.getGameMode() || LuxPostProcessStrategy.this.mSceneAdapter.isFlashLightOn()) {
                    this.isTakeEffect = false;
                    this.mDarkestModeTime = -1L;
                    return Float.NaN;
                }
                if (luxResult.isDangerLux(SensorDelegate.DEFAULT_PROXIMITY_NEAR_CHECK_VALUE) || (realtimeQueueAverageLux != -1.0f && realtimeQueueAverageLux < AVERAGE_LUX_THRESHOLD)) {
                    SceneBase scene = this.mDarkestModeScene != null ? this.mDarkestModeScene : luxResult.getScene();
                    SceneConfig sceneConfig = scene.getSceneConfig();
                    long DarkestModeDuration = sceneConfig.getDarknessModeDuration();
                    float checkLux = LuxPostProcessStrategy.this.getCurrentCheckLux(luxResult);
                    this.mDarkestModeScene = scene;
                    if ((luxResult.isNeedUpdate() ? luxResult.getFinalLux() : luxResult.getLastFinalLux()) <= SensorDelegate.DEFAULT_PROXIMITY_NEAR_CHECK_VALUE && luxResult.getRealTimeLux() <= SensorDelegate.DEFAULT_PROXIMITY_NEAR_CHECK_VALUE) {
                        if (this.mDarkestModeTime == -1) {
                            this.mDarkestModeTime = System.currentTimeMillis();
                        }
                        if (System.currentTimeMillis() - this.mDarkestModeTime >= DarkestModeDuration) {
                            this.isTakeEffect = true;
                            return Math.min(-1.0f, checkLux);
                        }
                        this.isTakeEffect = false;
                        luxResult.setLuxPostProcessMonitoring(true);
                        return Float.NaN;
                    }
                }
            }
            this.isTakeEffect = false;
            this.mDarkestModeScene = null;
            this.mDarkestModeTime = -1L;
            return Float.NaN;
        }

        public void reset(int reason) {
            if (reason == 1 || !this.isTakeEffect) {
                this.mDarkestModeScene = null;
                this.mDarkestModeTime = -1L;
            }
        }

        public void notifyDragInfo(float lux, float dragNit, float currentNit) {
        }

        public boolean isTakeEffect() {
            return this.isTakeEffect;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    class MotionMode {
        private final long DEFAULT_MOTION_EXIT_DELAY_TIME = TimeEvent.DEFAULT_TIME_ONE_MINUTE;
        private final float DEFAULT_MOTION_EXIT_INTERIM_LUX_COEFFICIENT = 1.0f;
        private long mMotionModeTime = -1;
        private long mHateDargTime = -1;
        private float mCurrentMotionLux = Float.NaN;
        private boolean isTakeEffect = false;

        public MotionMode() {
        }

        public float getMotionModeLux(OplusLuxResult luxResult) {
            if (LuxPostProcessStrategy.this.mComfortableBrightnessConfig != null && LuxPostProcessStrategy.this.mComfortableBrightnessConfig.mMotionModeSupport && luxResult != null && luxResult.getPhoneState().isCompleteData()) {
                if (isRespondMotion(luxResult)) {
                    SceneBase scene = luxResult.getScene();
                    SceneConfig sceneConfig = scene.getSceneConfig();
                    int motionEventId = luxResult.getPhoneState().getMotionEvent().getCurrentEventId();
                    float checkLux = LuxPostProcessStrategy.this.getCurrentCheckLux(luxResult);
                    float motionLux = SensorDelegate.DEFAULT_PROXIMITY_NEAR_CHECK_VALUE;
                    if (motionEventId == MotionEvent.MotionEventResult.ACTIVITY.WALKING.ordinal() || motionEventId == MotionEvent.MotionEventResult.ACTIVITY.RUNNING.ordinal() || motionEventId == MotionEvent.MotionEventResult.ACTIVITY.FOOT.ordinal()) {
                        motionLux = sceneConfig.getMotionModeOnFootLuxThreshold();
                    } else if (motionEventId == MotionEvent.MotionEventResult.ACTIVITY.TRANSPORTATION.ordinal()) {
                        motionLux = sceneConfig.getMotionModeInTransportationLuxThreshold();
                    }
                    this.isTakeEffect = true;
                    this.mMotionModeTime = System.currentTimeMillis();
                    if (luxResult.isStabilityProcessSuccess()) {
                        this.mCurrentMotionLux = motionLux;
                        return motionLux + checkLux;
                    }
                    if (motionLux >= checkLux) {
                        this.mCurrentMotionLux = motionLux;
                        return motionLux + checkLux;
                    }
                    return this.mCurrentMotionLux + checkLux;
                }
                if (this.mMotionModeTime != -1) {
                    float checkLux2 = LuxPostProcessStrategy.this.getCurrentCheckLux(luxResult);
                    if (checkLux2 <= this.mCurrentMotionLux && System.currentTimeMillis() - this.mMotionModeTime < TimeEvent.DEFAULT_TIME_ONE_MINUTE) {
                        luxResult.setLuxPostProcessMonitoring(true);
                        if (!Float.isNaN(this.mCurrentMotionLux)) {
                            if (luxResult.isStabilityProcessSuccess() && this.mCurrentMotionLux + checkLux2 != luxResult.getLastFinalLux()) {
                                initializationState();
                                return Float.NaN;
                            }
                            return Math.max(checkLux2, this.mCurrentMotionLux * 1.0f);
                        }
                    } else {
                        initializationState();
                        return checkLux2;
                    }
                }
            }
            initializationState();
            return Float.NaN;
        }

        public void reset(int reason) {
            if (reason == 1) {
                initializationState();
                this.mHateDargTime = -1L;
            }
            if (reason == 0 && this.mHateDargTime > 0) {
                if (!InOutDoorEvent.getInstance().isOutDoor() || NetworkEvent.getInstance().isWifiConnected()) {
                    AmbienceLog.f(LuxPostProcessStrategy.this.TAG, "reset motion hate drag");
                    this.mHateDargTime = -1L;
                }
            }
        }

        public void notifyDragInfo(float lux, float dragNit, float currentNit) {
            if (dragNit < currentNit) {
                if (this.isTakeEffect) {
                    AmbienceLog.f(LuxPostProcessStrategy.this.TAG, "hate motion");
                    this.mHateDargTime = TimeEvent.getSystemMillisTime();
                    initializationState();
                    return;
                }
                return;
            }
            this.mHateDargTime = -1L;
        }

        public boolean isTakeEffect() {
            return this.isTakeEffect;
        }

        private void initializationState() {
            this.isTakeEffect = false;
            this.mMotionModeTime = -1L;
            this.mCurrentMotionLux = Float.NaN;
        }

        private boolean isRespondMotion(final OplusLuxResult luxResult) {
            PhoneState state = luxResult.getPhoneState();
            if (state == null) {
                return false;
            }
            MotionEvent.MotionEventResult motionEvent = state.getMotionEvent();
            if (motionEvent != null && motionEvent.isMotionless()) {
                return false;
            }
            NetworkEvent.NetworkEventResult networkEvent = state.getNetworkEvent();
            if (networkEvent != null && networkEvent.isWifiConnected()) {
                AmbienceLog.logic(LuxPostProcessStrategy.this.TAG, new AmbienceLog.ILogPrintLogic() { // from class: com.android.server.display.feature.ambience.processes.process.LuxPostProcessStrategy$MotionMode$$ExternalSyntheticLambda0
                    @Override // com.android.server.display.feature.ambience.utils.AmbienceLog.ILogPrintLogic
                    public final boolean isLogPrint() {
                        return luxResult.isStabilityProcessSuccess();
                    }
                }, "isRespondMotion: network limit.");
                return false;
            }
            InOutDoorEvent.InOutDoorEventResult inOutDoorEvent = state.getInOutDoorEvent();
            if (inOutDoorEvent != null && inOutDoorEvent.isInDoor()) {
                AmbienceLog.logic(LuxPostProcessStrategy.this.TAG, new AmbienceLog.ILogPrintLogic() { // from class: com.android.server.display.feature.ambience.processes.process.LuxPostProcessStrategy$MotionMode$$ExternalSyntheticLambda1
                    @Override // com.android.server.display.feature.ambience.utils.AmbienceLog.ILogPrintLogic
                    public final boolean isLogPrint() {
                        return luxResult.isStabilityProcessSuccess();
                    }
                }, "isRespondMotion: inOutDoor limit.");
                return false;
            }
            if (LuxPostProcessStrategy.this.mDarkestMode.isTakeEffect()) {
                AmbienceLog.logic(LuxPostProcessStrategy.this.TAG, new AmbienceLog.ILogPrintLogic() { // from class: com.android.server.display.feature.ambience.processes.process.LuxPostProcessStrategy$MotionMode$$ExternalSyntheticLambda2
                    @Override // com.android.server.display.feature.ambience.utils.AmbienceLog.ILogPrintLogic
                    public final boolean isLogPrint() {
                        return luxResult.isStabilityProcessSuccess();
                    }
                }, "isRespondMotion: darkestMode limit.");
                return false;
            }
            if (this.mHateDargTime > 0) {
                AmbienceLog.logic(LuxPostProcessStrategy.this.TAG, new AmbienceLog.ILogPrintLogic() { // from class: com.android.server.display.feature.ambience.processes.process.LuxPostProcessStrategy$MotionMode$$ExternalSyntheticLambda3
                    @Override // com.android.server.display.feature.ambience.utils.AmbienceLog.ILogPrintLogic
                    public final boolean isLogPrint() {
                        return luxResult.isStabilityProcessSuccess();
                    }
                }, "isRespondMotion: user hate limit.");
                return false;
            }
            return true;
        }
    }
}
