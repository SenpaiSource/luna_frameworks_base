package com.android.server.display.feature.ambience;

import android.common.OplusFeatureCache;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.display.DisplayManagerInternal;
import android.hardware.display.OplusDisplayManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.util.SparseArray;
import com.android.server.display.DisplayPowerController;
import com.android.server.display.config.global.ComfortableBrightnessConfig;
import com.android.server.display.config.global.OplusDisplayGlobalConfig;
import com.android.server.display.feature.ambience.events.delegate.SensorDelegate;
import com.android.server.display.feature.ambience.events.event.TimeEvent;
import com.android.server.display.feature.ambience.events.utils.PhoneState;
import com.android.server.display.feature.ambience.processes.process.LuxDataProcess;
import com.android.server.display.feature.ambience.processes.process.LuxPostProcessStrategy;
import com.android.server.display.feature.ambience.processes.process.LuxQueueProcess;
import com.android.server.display.feature.ambience.processes.process.StabilityProcess;
import com.android.server.display.feature.ambience.scenes.OplusBrightnessSceneAdapter;
import com.android.server.display.feature.ambience.scenes.scene.ErraticScene;
import com.android.server.display.feature.ambience.scenes.scene.StableScene;
import com.android.server.display.feature.ambience.scenes.utils.AmbientData;
import com.android.server.display.feature.ambience.utils.AmbienceLog;
import com.android.server.display.feature.ambience.utils.OplusLimitQueue;
import com.android.server.display.feature.ambience.utils.OplusLuxResult;
import com.android.server.display.feature.ambience.utils.OplusPair;
import com.android.server.display.feature.power.OplusProximitySensorScreenOnOff;
import com.android.server.display.marvels.module.IORBrightnessMarvelsDataCollector;
import com.android.server.display.util.AutoReleaseReadWriteLock;
import com.android.server.display.util.OplusDisplayState;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.CopyOnWriteArrayList;

/* JADX INFO: loaded from: /run/media/sathiya/data/scratch/ambience.dex */
public class OplusBrightnessAmbientAdapter implements OplusBrightnessSceneAdapter.AmbienceListener, OplusDisplayGlobalConfig.IGlobalConfigListener {
    private static final int DEEP_SLEEP_MODE_ON = 1;
    private static final int DEEP_SLEEP_STATUS_URI_DEFAULT = 0;
    private static final float DEFAULT_BRIGHT_DRAG_RESET_FEELING_DELTA_THRESHOLD = 1.5f;
    private static final float DEFAULT_DARK_DRAG_RESET_FEELING_DELTA_THRESHOLD = 1.78f;
    private static final float DEFAULT_DOWN_DRAG_RESET_FEELING_COEFFICIENT = 2.0f;
    private static final float DEFAULT_MINIUM_DRAG_RESET_FEELING_DELTA_THRESHOLD = 0.89f;
    private static final int GAME_MODE_DEFAULT = 0;
    private static final int GAME_MODE_ON = 1;
    private static final int INVALID_VALUE = -1;
    private static final int MSG_CYCLE_SEND_NEWEST_WEIGHT_LUX = 5468;
    private static final int MSG_FIRST_INDEX = 5467;
    private static final String RESET_AMBIENT_ADAPTER_REASON_MODE_SWITCH = "ModeSwitch";
    private static final String TAG = "OplusBrightnessAmbientAdapter";
    private OplusLuxResult mCurrentLuxResult;
    private OplusLuxResult mLastUpdateResult;
    private LuxPostProcessStrategy mLuxPostProcessStrategy;
    private LuxQueueProcess mLuxQueueProcess;
    private OplusBrightnessSceneAdapter mSceneAdapter;
    private StabilityProcess mStabilityProcess;
    private static final String SYSTEM_FOLDING_MODE_KEYS = "oplus_system_folding_mode";
    private static final Uri SYSTEM_FOLDING_MODE_URI = Settings.Global.getUriFor(SYSTEM_FOLDING_MODE_KEYS);
    private static final String DEEP_SLEEP_STATUS = "oplus_deep_sleep_status";
    public static final Uri DEEP_SLEEP_STATUS_URI = Settings.System.getUriFor(DEEP_SLEEP_STATUS);
    private static final String GAME_MODE_KEY = "debug_gamemode_value";
    private static final Uri GAME_MODE_URI = Settings.Global.getUriFor(GAME_MODE_KEY);
    private static boolean mIsBootComplete = false;
    private static boolean mIsAutomaticMode = false;
    private static boolean mIsManualAutomaticMode = false;
    private static boolean mIsDeepSleep = false;
    private static boolean mIsFolded = false;
    private static boolean mIsDragging = false;
    private static boolean mIsAnimating = false;
    private static boolean mIsForceUpdateResult = false;
    private static boolean mIsNeedResetSpline = false;
    private static boolean mIsProximityPowerOff = false;
    private static long mScreenOnTime = TimeEvent.getSystemMillisTime();
    private static volatile OplusBrightnessAmbientAdapter sInstance = null;
    private boolean mIsChangeToAutomaticMode = false;
    private boolean mIsAutomaticModeForRateChange = false;
    private boolean mIsFast = false;
    private boolean mIsGameMode = false;
    private Context mContext = null;
    private AmbientAdapterHandler mAmbientAdapterHandler = null;
    private AmbientAdapterSettingsObserver mAmbientAdapterObserver = null;
    private final CopyOnWriteArrayList<Listener> mLuxChangeResultListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<AmbientResetListener> mResetDragInfoListeners = new CopyOnWriteArrayList<>();
    private AutoReleaseReadWriteLock mLock = new AutoReleaseReadWriteLock();
    private OplusLuxResult mLuxResult = null;
    private volatile float mWeightLuxInDrag = SensorDelegate.DEFAULT_PROXIMITY_NEAR_CHECK_VALUE;

    public interface AmbientResetListener {
        void resetDragInfo();
    }

    public static OplusBrightnessAmbientAdapter getInstance() {
        if (sInstance == null) {
            synchronized (OplusBrightnessAmbientAdapter.class) {
                if (sInstance == null) {
                    sInstance = new OplusBrightnessAmbientAdapter();
                }
            }
        }
        return sInstance;
    }

    public void init(Context context, Handler handler, DisplayPowerController dpc) {
        SparseArray<DisplayPowerController> list = new SparseArray<>();
        if (dpc != null) {
            list.put(0, dpc);
        }
        init(context, handler, list);
    }

    public void init(Context context, Handler handler, SparseArray<DisplayPowerController> dpcList) {
        AmbienceLog.f(TAG, "OplusBrightnessAmbientAdapter init+++");
        this.mContext = context;
        this.mAmbientAdapterHandler = new AmbientAdapterHandler(handler.getLooper());
        this.mSceneAdapter = new OplusBrightnessSceneAdapter(this.mContext, handler, this);
        this.mSceneAdapter.init();
        this.mLuxQueueProcess = new LuxQueueProcess(TAG, this.mSceneAdapter);
        this.mStabilityProcess = new StabilityProcess(TAG, this.mSceneAdapter, dpcList);
        this.mLuxPostProcessStrategy = new LuxPostProcessStrategy(TAG, this.mSceneAdapter);
        OplusDisplayGlobalConfig.getInstance().register(this);
        this.mAmbientAdapterObserver = new AmbientAdapterSettingsObserver();
        com.android.server.display.util.OplusDisplayBrightnessBroadcastReceiver.getInstance().init(context, handler);
        AmbienceLog.f(TAG, "OplusBrightnessAmbientAdapter init---");
    }

    public void setSensorManager(SensorManager sensorManager, Sensor sensor) {
        try {
            this.mSceneAdapter.setSensorManager(sensorManager, sensor);
            if (this.mContext != null) {
                brightnessModeChanged(Settings.System.getIntForUser(this.mContext.getContentResolver(), "screen_brightness_mode", 0, -2));
            }
        } catch (Throwable t) {
            AmbienceLog.e(TAG, "setSensorManager failed: " + t);
        }
    }

    public void addListener(Listener listener) {
        this.mLuxChangeResultListeners.add(listener);
    }

    public void addAmbientResetListener(AmbientResetListener listener) {
        this.mResetDragInfoListeners.add(listener);
    }

    public void removeListener(Listener listener) {
        this.mLuxChangeResultListeners.remove(listener);
    }

    public void removeAmbientResetListener(AmbientResetListener listener) {
        this.mResetDragInfoListeners.remove(listener);
    }

    public void onBootCompleted() {
        if (this.mAmbientAdapterHandler != null) {
            this.mAmbientAdapterHandler.post(this::oplusLambda$onBootCompleted$0);
            this.mAmbientAdapterObserver.register(this.mContext.getContentResolver());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void oplusLambda$onBootCompleted$0() {
        AmbienceLog.f(TAG, "onBootCompleted");
        this.mSceneAdapter.onBootCompleted();
        AmbienceLog.onBootCompleted();
        this.mStabilityProcess.onBootCompleted();
        mIsForceUpdateResult = true;
        mIsBootComplete = true;
    }

    private void brightnessModeChanged(int newMode) {
        brightnessModeChanged(newMode == 1);
    }

    public void brightnessModeChanged(final boolean isAuto) {
        if (this.mAmbientAdapterHandler != null) {
            this.mAmbientAdapterHandler.post(() -> {
                try {
                    oplusLambda$brightnessModeChanged$1(isAuto);
                } catch (Throwable t) {
                    AmbienceLog.e(TAG, "brightnessModeChanged error: " + t);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void oplusLambda$brightnessModeChanged$1(boolean isAuto) {
        if (Boolean.logicalXor(isAuto, mIsAutomaticMode)) {
            AmbienceLog.f(TAG, "onBrightnessSettingModeChanged " + mIsAutomaticMode + "--->" + isAuto + "; mIsAnimating = " + mIsAnimating + "; mIsFast = " + this.mIsFast);
            mIsAutomaticMode = isAuto;
            resetAmbientAdapter(RESET_AMBIENT_ADAPTER_REASON_MODE_SWITCH);
            this.mLuxPostProcessStrategy.brightnessModeChanged(mIsAutomaticMode);
            this.mSceneAdapter.brightnessModeChanged(mIsAutomaticMode);
            adapterSwitch(mIsAutomaticMode);
            this.mIsChangeToAutomaticMode = isAuto;
            this.mIsAutomaticModeForRateChange = isAuto;
            this.mSceneAdapter.setFastAnimationWithReset(this.mIsChangeToAutomaticMode && mIsAnimating && this.mIsFast);
        }
    }

    public void onDeepSleepModeChanged(int newMode) {
        deepSleepModeChanged(newMode);
    }

    private void deepSleepModeChanged(int newMode) {
        deepSleepModeChanged(newMode == 1);
    }

    private ComfortableBrightnessConfig getConfig() {
        return OplusDisplayGlobalConfig.getInstance().getComfortableBrightnessConfig();
    }

    private void deepSleepModeChanged(boolean isDeepSleep) {
        if (Boolean.logicalXor(isDeepSleep, mIsDeepSleep)) {
            AmbienceLog.f(TAG, "onDeepSleepModeChanged " + mIsDeepSleep + "--->" + isDeepSleep);
            mIsDeepSleep = isDeepSleep;
            this.mSceneAdapter.deepSleepModeChanged(isDeepSleep);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onGameModeChanged(int newMode) {
        boolean isGameMode = newMode == 1;
        if (Boolean.logicalXor(isGameMode, this.mIsGameMode)) {
            AmbienceLog.f(TAG, "onGameModeChanged " + this.mIsGameMode + "--->" + isGameMode);
            this.mIsGameMode = isGameMode;
            this.mSceneAdapter.onGameModeChanged(isGameMode);
        }
    }

    public void onDisplayFoldedStateChange(boolean isFolded) {
        if (Boolean.logicalXor(isFolded, mIsFolded)) {
            AmbienceLog.f(TAG, "onDisplayFoldedStateChange " + mIsFolded + "--->" + isFolded);
            mIsFolded = isFolded;
            this.mSceneAdapter.onDisplayFoldedStateChange(isFolded);
            OplusFeatureCache.getOrCreate(IORBrightnessMarvelsDataCollector.DEFAULT, new Object[0]).onDisplayFoldedStateChange(isFolded);
        }
    }

    public void setManualAutomaticMode(final boolean isAuto) {
        this.mAmbientAdapterHandler.postDelayed(() -> oplusLambda$setManualAutomaticMode$2(isAuto), TimeEvent.DEFAULT_TIME_TWO_SECONDS);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void oplusLambda$setManualAutomaticMode$2(boolean isAuto) {
        if (Boolean.logicalXor(isAuto, mIsManualAutomaticMode)) {
            AmbienceLog.f(TAG, "setManualAutomaticMode " + mIsManualAutomaticMode + "--->" + isAuto + " ,mIsAutomaticMode = " + mIsAutomaticMode);
            mIsManualAutomaticMode = isAuto;
            if (!mIsAutomaticMode) {
                this.mSceneAdapter.putToWork(mIsManualAutomaticMode);
            }
        }
    }

    public void onScreenEvent(final int displayId, final boolean isScreenOn, boolean isFolding, DisplayManagerInternal.DisplayPowerRequest request) {
        AmbienceLog.f(TAG, "onScreenEvent displayId:" + displayId + " isScreenOn:" + isScreenOn + " isFolding:" + isFolding + " mIsProximityPowerOff:" + mIsProximityPowerOff + " mIsNeedResetSpline:" + mIsNeedResetSpline);
        this.mAmbientAdapterHandler.post(() -> oplusLambda$onScreenEvent$4(isScreenOn, displayId));
        this.mSceneAdapter.onScreenEvent(displayId, isScreenOn, isFolding, request);
        this.mLuxPostProcessStrategy.onScreenEvent(displayId, isScreenOn, isFolding, request);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void oplusLambda$onScreenEvent$4(boolean isScreenOn, int displayId) {
        if (isScreenOn) {
            mScreenOnTime = TimeEvent.getSystemMillisTime();
            if (!mIsNeedResetSpline && !mIsProximityPowerOff) {
                this.mLastUpdateResult = null;
                return;
            }
            return;
        }
        mIsProximityPowerOff = OplusProximitySensorScreenOnOff.getInstance().isProximityPositive();
        AmbienceLog.logic(TAG, () -> OplusBrightnessAmbientAdapter.mIsProximityPowerOff, "display:" + displayId + " PROXIMITY_SCREEN_OFF...");
    }

    public static boolean isProximityPowerOff() {
        return mIsProximityPowerOff;
    }

    public static boolean isNeedResetSpline() {
        return mIsNeedResetSpline;
    }

    public static long getScreenOnPastTime() {
        return TimeEvent.getSystemMillisTime() - mScreenOnTime;
    }

    public void setDragging(boolean dragging) {
        if (dragging) {
            this.mAmbientAdapterHandler.post(this::oplusLambda$setDragging$5);
        }
        if (Boolean.logicalXor(dragging, mIsDragging)) {
            AmbienceLog.d(TAG, "drag state change:" + mIsDragging + " -> " + dragging);
            mIsDragging = dragging;
            this.mSceneAdapter.setDragging(dragging);
        }
    }

    public boolean isDragging() {
        return mIsDragging;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void oplusLambda$setDragging$5() {
        this.mWeightLuxInDrag = getNewestEnvironmentWeightLux();
    }

    public void notifyDragInfo(float lux, float nit, float defaultNit, float currentNit) {
        AmbienceLog.f(TAG, "DargInfo:" + lux + " " + nit + " " + defaultNit + " " + currentNit);
        OplusPair<Float, Float> dragFeeling = this.mStabilityProcess.notifyDragInfo(lux, nit, defaultNit);
        float checkThreshold = getDragResetCheckThreshold(lux, nit > defaultNit);
        if (Math.abs(((Float) dragFeeling.first).floatValue() - ((Float) dragFeeling.second).floatValue()) > checkThreshold) {
            mIsNeedResetSpline = true;
        } else {
            mIsNeedResetSpline = false;
        }
        AmbienceLog.logic(TAG, new AmbienceLog.ILogPrintLogic() { // from class: com.android.server.display.feature.ambience.OplusBrightnessAmbientAdapter$$ExternalSyntheticLambda3
            @Override // com.android.server.display.feature.ambience.utils.AmbienceLog.ILogPrintLogic
            public final boolean isLogPrint() {
                return OplusBrightnessAmbientAdapter.mIsNeedResetSpline;
            }
        }, "drag is abnormal, reset spline later, check threshold:" + checkThreshold, "drag is normal, nothing to do, check threshold:" + checkThreshold);
        if (this.mLastUpdateResult != null) {
            this.mLastUpdateResult.setAutomaticLux(lux);
            handleLuxPostProcess(this.mLastUpdateResult);
            AmbienceLog.d(TAG, "update result:" + this.mLastUpdateResult);
        }
        if (!Float.isNaN(currentNit)) {
            this.mLuxPostProcessStrategy.notifyDragInfo(lux, nit, currentNit);
        }
    }

    private float getDragResetCheckThreshold(float lux, boolean isUp) {
        float checkThreshold;
        if (StabilityProcess.isDarkEnvironmentLux(lux)) {
            checkThreshold = (((-0.89f) / StabilityProcess.getDefaultDarkEnvironmentLux()) * lux) + DEFAULT_DARK_DRAG_RESET_FEELING_DELTA_THRESHOLD;
        } else if (StabilityProcess.isOutDoorEnvironmentLux(lux)) {
            checkThreshold = DEFAULT_MINIUM_DRAG_RESET_FEELING_DELTA_THRESHOLD;
        } else {
            checkThreshold = (((-0.61f) / (StabilityProcess.getDefaultOutDoorEnvironmentLux() - StabilityProcess.getDefaultDarkEnvironmentLux())) * (lux - StabilityProcess.getDefaultDarkEnvironmentLux())) + DEFAULT_BRIGHT_DRAG_RESET_FEELING_DELTA_THRESHOLD;
        }
        if (!isUp) {
            return checkThreshold / DEFAULT_DOWN_DRAG_RESET_FEELING_COEFFICIENT;
        }
        return checkThreshold;
    }

    public void setAnimating(boolean animating, boolean isPrimaryAnimator, boolean isFast) {
        if (isPrimaryAnimator) {
            AmbienceLog.f(TAG, "setAnimating: from " + mIsAnimating + " to " + animating);
            mIsAnimating = animating;
            this.mIsFast = isFast;
            this.mSceneAdapter.setFastAnimationWithReset(this.mIsChangeToAutomaticMode && animating && isFast);
            this.mIsChangeToAutomaticMode = false;
        }
    }

    public void onRateChange(boolean isFast) {
        this.mIsFast = isFast;
        this.mSceneAdapter.setFastAnimationWithReset(this.mIsAutomaticModeForRateChange && mIsAnimating && isFast);
        this.mIsAutomaticModeForRateChange = false;
    }

    public float getNewestEnvironmentLux() {
        OplusLimitQueue<Float> realtimeLuxLimitQueue = this.mSceneAdapter.getRealtimeLuxLimitQueue();
        return realtimeLuxLimitQueue.isEmpty() ? SensorDelegate.DEFAULT_PROXIMITY_NEAR_CHECK_VALUE : realtimeLuxLimitQueue.getLast().floatValue();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public float getNewestEnvironmentWeightLux() {
        OplusLimitQueue<Float> updateLuxLimitQueue = this.mSceneAdapter.getWeightLuxLimitQueue();
        if (!updateLuxLimitQueue.isEmpty()) {
            float weightLux = updateLuxLimitQueue.getLast().floatValue();
            return weightLux;
        }
        if (this.mLastUpdateResult == null) {
            return SensorDelegate.DEFAULT_PROXIMITY_NEAR_CHECK_VALUE;
        }
        float lastWeightLux = this.mLastUpdateResult.getWeightLux();
        float lastAutomaticLux = this.mLastUpdateResult.getAutomaticLux();
        float lastFinalLux = this.mLastUpdateResult.getFinalLux();
        if (!Float.isNaN(lastWeightLux)) {
            return lastWeightLux;
        }
        if (!Float.isNaN(lastAutomaticLux)) {
            return lastAutomaticLux;
        }
        if (Float.isNaN(lastFinalLux)) {
            return SensorDelegate.DEFAULT_PROXIMITY_NEAR_CHECK_VALUE;
        }
        return lastFinalLux;
    }

    public StabilityProcess getStabilityProcess() {
        return this.mStabilityProcess;
    }

    public LuxPostProcessStrategy getLuxPostProcessStrategy() {
        return this.mLuxPostProcessStrategy;
    }

    public OplusBrightnessSceneAdapter getSceneAdapter() {
        return this.mSceneAdapter;
    }

    public ArrayList<Float> getArc() {
        ArrayList<Float> arc = new ArrayList<>();
        LinkedList<Float> luxList = this.mSceneAdapter.getWeightLuxLimitQueue().getLinkedListByPastTime(TimeEvent.DEFAULT_TIME_HALF_MINUTE);
        Iterator<Float> it = luxList.iterator();
        while (it.hasNext()) {
            float lux = it.next().floatValue();
            arc.add(Float.valueOf(lux));
        }
        return arc;
    }

    public HashMap<String, String> getArc2() {
        Float f;
        HashMap<String, String> result = new HashMap<>();
        if (this.mCurrentLuxResult != null && this.mLastUpdateResult != null) {
            float currentBrightness = OplusDisplayManager.getInstance().getCurrentScreenBrightness(0);
            result.put("Level", String.valueOf(currentBrightness));
            result.put("Nit", String.valueOf(OplusDisplayManager.getInstance().getNitFromBrightness(0, currentBrightness)));
            result.put("StableLux", String.valueOf(this.mLastUpdateResult.getFinalLux()));
            result.put("ShakeRange", this.mCurrentLuxResult.getStabilityProcess().getLargeChangeLimit().toString());
            result.put("Process", this.mLastUpdateResult.getChangeReasonProcess().toString());
            result.put("Scene", this.mCurrentLuxResult.getScene().getSceneName());
            result.put("ReactAccelerate", String.valueOf(this.mLastUpdateResult.getAccelerateScale()));
            if (this.mLastUpdateResult.getBrightnessChangeDirection() > 0) {
                f = (Float) this.mLastUpdateResult.getAnimatingAcceleration().first;
            } else {
                f = (Float) this.mLastUpdateResult.getAnimatingAcceleration().second;
            }
            result.put("AnimatAccelerate", String.valueOf(f));
            result.putAll(this.mCurrentLuxResult.getPhoneState().getPhoneState());
        }
        return result;
    }

    public void resetAmbientAdapter(String reason) {
        AmbienceLog.logic(TAG, this::oplusLambda$resetAmbientAdapter$7, "resetAmbientAdapter reason: " + reason, "boot is not completed yet, nothing to reset.");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ boolean oplusLambda$resetAmbientAdapter$7() {
        if (!mIsBootComplete) {
            return false;
        }
        setDragging(false);
        mIsNeedResetSpline = false;
        resetDragInfo();
        return true;
    }

    private void adapterSwitch(boolean isOn) {
        AmbienceLog.d(TAG, "adapterSwitch: " + isOn);
        if (!isOn) {
            this.mAmbientAdapterHandler.postDelayed(this::oplusLambda$adapterSwitch$8, TimeEvent.DEFAULT_TIME_FIVE_SECONDS);
        } else {
            mIsForceUpdateResult = true;
        }
        this.mSceneAdapter.putToWork(isOn);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void oplusLambda$adapterSwitch$8() {
        if (!mIsAutomaticMode && this.mLastUpdateResult != null && this.mLastUpdateResult.getUpdatePastTime() > TimeEvent.DEFAULT_TIME_FIVE_SECONDS) {
            AmbienceLog.d(TAG, "reset Last Result");
            this.mLastUpdateResult = null;
        }
    }

    public float getWeightLuxInDrag() {
        return this.mWeightLuxInDrag;
    }

    @Override // com.android.server.display.feature.ambience.scenes.OplusBrightnessSceneAdapter.AmbienceListener
    public void onAmbienceChanged(AmbientData data) {
        if (this.mStabilityProcess == null || this.mLuxPostProcessStrategy == null) {
            return;
        }
        OplusLuxResult preResult = this.mLastUpdateResult;
        OplusLuxResult luxResult = new OplusLuxResult(data, this.mStabilityProcess, preResult, mIsForceUpdateResult);
        if (!luxResult.isInitChange()) {
            this.mStabilityProcess.updateRangeStrategy(luxResult);
            handleLuxQueueProcess(luxResult);
            handleLuxDataProcess(luxResult);
            handleStabilityProcess(luxResult);
        }
        handleLuxPostProcess(luxResult);
        if (luxResult.isNeedUpdate() || luxResult.isNeedSync()) {
            this.mLastUpdateResult = luxResult;
            mIsForceUpdateResult = false;
            mIsProximityPowerOff = false;
            this.mSceneAdapter.updateLuxChange(luxResult.isStabilityProcessSuccess() ? luxResult.getAutomaticLux() : luxResult.getLastAutomaticLux(), true);
            onLuxResultChange(luxResult);
        } else {
            if (mIsProximityPowerOff) {
                onLuxResultChange(this.mLastUpdateResult);
            }
            if (luxResult.isLuxDataProcessSuccess()) {
                this.mSceneAdapter.updateLuxChange(luxResult.getWeightLux(), false);
            }
        }
        this.mCurrentLuxResult = luxResult;
    }

    public void onSensorRegisterStatusChange(boolean register) {
        for (Listener listener : this.mLuxChangeResultListeners) {
            listener.onSensorRegisterStatusChange(register);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ OplusLuxResult oplusLambda$getLuxResult$9() {
        return this.mLuxResult;
    }

    public OplusLuxResult getLuxResult() {
        return (OplusLuxResult) this.mLock.executeReadOperation(this::oplusLambda$getLuxResult$9);
    }

    private void onLuxResultChange(final OplusLuxResult result) {
        AmbienceLog.d(TAG, "onLuxResultChange=" + result.getLastFinalLux());
        this.mLock.executeWriteOperation(() -> oplusLambda$onLuxResultChange$10(result));
        for (Listener listener : this.mLuxChangeResultListeners) {
            listener.onLuxResultChange(result);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void oplusLambda$onLuxResultChange$10(OplusLuxResult result) {
        this.mLuxResult = result;
    }

    private void resetDragInfo() {
        AmbienceLog.d(TAG, "resetDragInfo");
        this.mStabilityProcess.resetDragInfo();
        for (AmbientResetListener listener : this.mResetDragInfoListeners) {
            listener.resetDragInfo();
        }
    }

    private void handleLuxQueueProcess(OplusLuxResult luxResult) {
        LinkedList<Float> resultList;
        float accelerateScale = this.mStabilityProcess.getDefaultAccelerateScale();
        if (luxResult.getScene().getSceneConfig().getReactionAccelerateSwitch()) {
            LinkedList<Float> accelerateList = this.mSceneAdapter.getRealtimeLuxLimitQueue().getLinkedListByCount(this.mStabilityProcess.getDefaultAccelerateCheckSize());
            float accelerateLux = LuxDataProcess.findWeightingValue(accelerateList, this.mStabilityProcess.getDefaultAccelerateCalculateWeight());
            accelerateScale = this.mStabilityProcess.getAccelerateScale(accelerateLux);
        }
        luxResult.setAccelerateScale(accelerateScale);
        if (luxResult.isResetForceUpdate()) {
            resultList = new LinkedList<>();
            resultList.add(Float.valueOf(getNewestEnvironmentLux()));
        } else {
            resultList = this.mLuxQueueProcess.getUpdateBackwardList(luxResult);
        }
        if (resultList != null && !resultList.isEmpty()) {
            luxResult.setCalculateLuxQueue(resultList);
            luxResult.setAmbientLightDirection(this.mLuxQueueProcess.getDirection());
        }
    }

    private void handleLuxDataProcess(OplusLuxResult luxResult) {
        if (luxResult.isLuxQueueProcessSuccess()) {
            float weightLux = LuxDataProcess.findWeightingValue(luxResult.getCalculateLuxQueue());
            PhoneState state = luxResult.getPhoneState();
            if (state.isCompleteData() && state.getProximityEvent().isNear() && weightLux <= luxResult.getLastFinalLux()) {
                AmbienceLog.logic(TAG, new AmbienceLog.ILogPrintLogic() { // from class: com.android.server.display.feature.ambience.OplusBrightnessAmbientAdapter$$ExternalSyntheticLambda8
                    @Override // com.android.server.display.feature.ambience.utils.AmbienceLog.ILogPrintLogic
                    public final boolean isLogPrint() {
                        return AmbienceLog.isLogOn;
                    }
                }, "don`t change Lux because system has been locked by proximity.");
            } else {
                luxResult.setWeightLux(weightLux);
            }
        }
    }

    private void handleStabilityProcess(OplusLuxResult luxResult) {
        if (luxResult.isLuxDataProcessSuccess() || luxResult.isResetForceUpdate()) {
            float resultLux = Float.NaN;
            float weightLux = luxResult.getWeightLux();
            float calculateLux = weightLux;
            if (getConfig() != null && getConfig().mErraticSceneLuxFilterSupport && ErraticScene.isSameScene(luxResult.getScene()) && !luxResult.isAccelerateState()) {
                long checkTime = getScreenOnPastTime();
                long lastTargetDuration = this.mSceneAdapter.getTargetLuxLimitQueue().getLastDataDuration();
                if (this.mSceneAdapter.getTargetLuxLimitQueue().getLastDataDuration() != -1) {
                    checkTime = Math.min(checkTime, lastTargetDuration);
                }
                LinkedList<Float> historyUpdateList = this.mSceneAdapter.getWeightLuxLimitQueue().getLinkedListByPastTimeBasedOnCount(checkTime, 3, false);
                historyUpdateList.add(Float.valueOf(calculateLux));
                calculateLux = LuxDataProcess.findWeightingValue(historyUpdateList, luxResult.getScene().getSceneConfig().getLuxCalculateOneTimeFilterWeight());
            }
            if (Float.isNaN(resultLux) && this.mStabilityProcess.isLargeChange(calculateLux)) {
                resultLux = calculateLux;
                luxResult.setChangeReason(OplusLuxResult.REASON.LargeChange);
            }
            if (Float.isNaN(resultLux)) {
                int backwardIndex = luxResult.getCalculateLuxQueue().size();
                float auxScore = (float) OplusLimitQueue.getSum(this.mSceneAdapter.getLuxSourceLimitQueue().getLinkedListByCount(backwardIndex));
                if (auxScore != -1.0f && auxScore >= backwardIndex * this.mStabilityProcess.getDefaultAuxDataSourceProportion() && this.mStabilityProcess.isLargeChangeForSpecialLight(calculateLux)) {
                    resultLux = calculateLux;
                    luxResult.setChangeReason(OplusLuxResult.REASON.AuxLargeChange);
                }
            }
            if (Float.isNaN(resultLux)) {
                if (this.mStabilityProcess.isSmallChange(calculateLux)) {
                    if (luxResult.isResetForceUpdate()) {
                        resultLux = weightLux;
                    } else if (mIsAnimating) {
                        if (this.mLastUpdateResult != null && this.mLastUpdateResult.getAmbientLightDirection() != luxResult.getAmbientLightDirection()) {
                            resultLux = LuxDataProcess.findWeightingValue(luxResult.getLastAutomaticLux(), calculateLux);
                            luxResult.setChangeReason(OplusLuxResult.REASON.RecoverChange);
                        }
                    } else if (StableScene.isSameScene(luxResult.getScene()) || (getConfig() != null && getConfig().mAllSceneLuxSmallChangeSupport)) {
                        LinkedList<Float> updateList = this.mSceneAdapter.getWeightLuxLimitQueue().getLinkedListByPastTime(StableScene.DEFAULT_STABLE_SCENE_QUEUE_FORCE_UPDATE_RECALL_TIME);
                        if (!updateList.isEmpty()) {
                            updateList.add(Float.valueOf(calculateLux));
                            OplusPair<Float, Float> smallRange = this.mStabilityProcess.getSmallChangeLimit();
                            if (OplusLimitQueue.isOutOfMaxLimit(updateList, ((Float) smallRange.second).floatValue()) || OplusLimitQueue.isOutOfMinLimit(updateList, ((Float) smallRange.first).floatValue())) {
                                resultLux = (float) OplusLimitQueue.getAverage(updateList);
                                luxResult.setChangeReason(OplusLuxResult.REASON.StableForceChange);
                            }
                        }
                    }
                } else if (luxResult.isResetForceUpdate()) {
                    resultLux = luxResult.getLastAutomaticLux();
                }
            }
            if (!Float.isNaN(resultLux)) {
                luxResult.setAutomaticLux(resultLux);
            }
        }
    }

    private void handleLuxPostProcess(OplusLuxResult luxResult) {
        if (this.mLastUpdateResult == null && !luxResult.isInitChange()) {
            AmbienceLog.w(TAG, "brightness process lack of initialization data, ignore updates.");
            return;
        }
        luxResult.setLuxPostProcessMonitoring(false);
        float motionLux = this.mLuxPostProcessStrategy.getMotionModeLux(luxResult);
        if (!Float.isNaN(motionLux)) {
            luxResult.setFinalLux(motionLux);
            luxResult.setChangeReason(OplusLuxResult.REASON.MotionMode);
        }
        float dayNightLux = this.mLuxPostProcessStrategy.getDayNightModeLux(luxResult);
        if (!Float.isNaN(dayNightLux)) {
            luxResult.setFinalLux(dayNightLux);
            luxResult.setChangeReason(OplusLuxResult.REASON.DayNightMode);
        }
        float interimLux = this.mLuxPostProcessStrategy.getInterimModeLux(luxResult);
        if (!Float.isNaN(interimLux)) {
            luxResult.setFinalLux(interimLux);
            luxResult.setChangeReason(OplusLuxResult.REASON.InterimMode);
        }
        float darkestLux = this.mLuxPostProcessStrategy.getDarkestModeLux(luxResult);
        if (!Float.isNaN(darkestLux)) {
            luxResult.setFinalLux(darkestLux);
            luxResult.setChangeReason(OplusLuxResult.REASON.DarkestMode);
        }
        if (!this.mAmbientAdapterHandler.hasMessages(MSG_CYCLE_SEND_NEWEST_WEIGHT_LUX)) {
            Message msg = this.mAmbientAdapterHandler.obtainMessage(MSG_CYCLE_SEND_NEWEST_WEIGHT_LUX);
            this.mAmbientAdapterHandler.sendMessageDelayed(msg, TimeEvent.DEFAULT_TIME_TWO_SECONDS);
        }
    }

    public void onRusConfigLoadSuccess() {
        this.mSceneAdapter.applyRusData();
    }

    private class AmbientAdapterSettingsObserver extends ContentObserver {
        public AmbientAdapterSettingsObserver() {
            super(OplusBrightnessAmbientAdapter.this.mAmbientAdapterHandler);
        }

        public void register(ContentResolver contentResolver) {
            contentResolver.registerContentObserver(OplusBrightnessAmbientAdapter.DEEP_SLEEP_STATUS_URI, false, this, -1);
            contentResolver.registerContentObserver(OplusBrightnessAmbientAdapter.SYSTEM_FOLDING_MODE_URI, false, this, -1);
            contentResolver.registerContentObserver(OplusBrightnessAmbientAdapter.GAME_MODE_URI, false, this, -1);
        }

        public void unregister(ContentResolver contentResolver) {
            contentResolver.unregisterContentObserver(this);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            if (OplusBrightnessAmbientAdapter.DEEP_SLEEP_STATUS_URI.equals(uri)) {
                int deepSleepStatus = Settings.System.getIntForUser(OplusBrightnessAmbientAdapter.this.mContext.getContentResolver(), OplusBrightnessAmbientAdapter.DEEP_SLEEP_STATUS, 0, -2);
                OplusBrightnessAmbientAdapter.this.onDeepSleepModeChanged(deepSleepStatus);
            } else if (OplusBrightnessAmbientAdapter.SYSTEM_FOLDING_MODE_URI.equals(uri)) {
                int state = Settings.Global.getInt(OplusBrightnessAmbientAdapter.this.mContext.getContentResolver(), OplusBrightnessAmbientAdapter.SYSTEM_FOLDING_MODE_KEYS, -1);
                OplusBrightnessAmbientAdapter.this.onDisplayFoldedStateChange(state == 0);
            } else if (OplusBrightnessAmbientAdapter.GAME_MODE_URI.equals(uri)) {
                int gameMode = Settings.Global.getInt(OplusBrightnessAmbientAdapter.this.mContext.getContentResolver(), OplusBrightnessAmbientAdapter.GAME_MODE_KEY, 0);
                OplusBrightnessAmbientAdapter.this.onGameModeChanged(gameMode);
                AmbienceLog.d(OplusBrightnessAmbientAdapter.TAG, "onChange gameMode = " + gameMode);
            }
        }
    }

    private class AmbientAdapterHandler extends Handler {
        public AmbientAdapterHandler(Looper looper) {
            super(looper);
            AmbienceLog.f(OplusBrightnessAmbientAdapter.TAG, "AmbientAdapterHandler init");
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case OplusBrightnessAmbientAdapter.MSG_CYCLE_SEND_NEWEST_WEIGHT_LUX /* 5468 */:
                    if (OplusDisplayState.getInstance().isScreenOn()) {
                        float weightLux = OplusBrightnessAmbientAdapter.this.getNewestEnvironmentWeightLux();
                        OplusFeatureCache.getOrCreate(IORBrightnessMarvelsDataCollector.DEFAULT, new Object[0]).setStableLux(weightLux);
                    }
                    break;
            }
        }
    }

    public interface Listener {
        void onLuxResultChange(OplusLuxResult oplusLuxResult);

        default void onSensorRegisterStatusChange(boolean register) {
        }
    }

    public void setIrOn() {
        if (this.mSceneAdapter != null) {
            this.mSceneAdapter.setIrOn();
        }
    }
}
