package com.android.server.display.feature.ambience.scenes;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.display.DisplayManagerInternal;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import com.android.server.display.feature.ambience.OplusBrightnessAmbientAdapter;
import com.android.server.display.feature.ambience.events.OplusBrightnessPhoneStateManager;
import com.android.server.display.feature.ambience.events.delegate.SensorDelegate;
import com.android.server.display.feature.ambience.events.event.TimeEvent;
import com.android.server.display.feature.ambience.events.utils.EventResult;
import com.android.server.display.feature.ambience.events.utils.PhoneState;
import com.android.server.display.feature.ambience.processes.process.LuxPostProcessStrategy;
import com.android.server.display.feature.ambience.processes.process.StabilityProcess;
import com.android.server.display.feature.ambience.scenes.scene.ErraticScene;
import com.android.server.display.feature.ambience.scenes.scene.NormalScene;
import com.android.server.display.feature.ambience.scenes.scene.StableScene;
import com.android.server.display.feature.ambience.scenes.utils.AmbientData;
import com.android.server.display.feature.ambience.scenes.utils.SceneBase;
import com.android.server.display.feature.ambience.utils.AmbienceLog;
import com.android.server.display.feature.ambience.utils.OplusLimitQueue;
import com.android.server.display.test.impl.OplusBrightnessDebugCenter;
import com.android.server.power.OplusScreenOffTorchHelper;
import java.util.HashMap;
import java.util.function.BiConsumer;

/* JADX INFO: loaded from: /run/media/sathiya/data/scratch/ambience.dex */
public class OplusBrightnessSceneAdapter implements OplusBrightnessPhoneStateManager.PhoneStateListener, OplusBrightnessDebugCenter.IOplusDebugListener {
    public static final String DEFAULT_LUX_SOURCE_QUEUE_KEY = "lux-source";
    private static final long DEFAULT_LUX_SOURCE_RECORD_TIME = 30000;
    public static final String DEFAULT_REALTIME_LUX_QUEUE_KEY = "realtime";
    private static final long DEFAULT_REALTIME_LUX_RECORD_TIME = 5000;
    public static final String DEFAULT_TARGET_LUX_QUEUE_KEY = "target";
    private static final long DEFAULT_TARGET_LUX_RECORD_TIME = 30000;
    public static final String DEFAULT_UPDATE_LUX_QUEUE_KEY = "update";
    private static final long DEFAULT_UPDATE_LUX_RECORD_TIME = 60000;
    private static final int OBMS_VERIFY_NUMBER = 80261364;
    private static final String TAG = "OplusBrightnessSceneAdapter";
    private AmbienceListener mAmbienceListener;
    private final HashMap<String, OplusLimitQueue> mAmbienceLuxQueueMap;
    private OplusBrightnessAmbientAdapter mAmbientAdapter;
    private Context mContext;
    private SceneBase mCurrentScene;
    private boolean mIsArcDebugMode;
    private boolean mIsDeepSleep;
    private boolean mIsFolded;
    private volatile boolean mIsGameMode;
    private boolean mIsNeedRefreshScore;
    private final OplusLimitQueue<Float> mLuxSourceLimitQueue;
    private OplusBrightnessPhoneStateManager mPhoneStateManager;
    private Handler mSmartHandler;
    private final OplusLimitQueue<Float> mTargetLuxLimitQueue;
    private final OplusLimitQueue<Float> mWeightLuxLimitQueue;
    private final NormalScene mNormalScene = new NormalScene(this);
    private final ErraticScene mErraticScene = new ErraticScene(this);
    private final StableScene mStableScene = new StableScene(this);
    private final OplusLimitQueue<Float> mRealtimeLuxLimitQueue = new OplusLimitQueue<>(null, 5000L);

    public interface AmbienceListener {
        void onAmbienceChanged(AmbientData ambientData);
    }

    public OplusBrightnessSceneAdapter(Context context, Handler handler, OplusBrightnessAmbientAdapter adapter) {
        Long lValueOf = Long.valueOf(TimeEvent.DEFAULT_TIME_HALF_MINUTE);
        this.mLuxSourceLimitQueue = new OplusLimitQueue<>(null, lValueOf);
        this.mWeightLuxLimitQueue = new OplusLimitQueue<>(null, 60000L);
        this.mTargetLuxLimitQueue = new OplusLimitQueue<>(null, lValueOf);
        this.mAmbienceLuxQueueMap = new HashMap<>();
        this.mIsDeepSleep = false;
        this.mIsFolded = false;
        this.mIsNeedRefreshScore = false;
        this.mIsArcDebugMode = false;
        this.mIsGameMode = false;
        this.mContext = null;
        this.mSmartHandler = null;
        this.mPhoneStateManager = null;
        this.mAmbientAdapter = null;
        this.mAmbienceListener = null;
        this.mCurrentScene = null;
        AmbienceLog.f(TAG, "OplusBrightnessSceneAdapter init+++");
        this.mContext = context;
        this.mSmartHandler = handler;
        this.mAmbientAdapter = adapter;
        this.mAmbienceListener = adapter;
        this.mPhoneStateManager = new OplusBrightnessPhoneStateManager(this.mContext, this.mSmartHandler);
        this.mAmbienceLuxQueueMap.put(DEFAULT_REALTIME_LUX_QUEUE_KEY, this.mRealtimeLuxLimitQueue);
        this.mAmbienceLuxQueueMap.put(DEFAULT_LUX_SOURCE_QUEUE_KEY, this.mLuxSourceLimitQueue);
        this.mAmbienceLuxQueueMap.put(DEFAULT_UPDATE_LUX_QUEUE_KEY, this.mWeightLuxLimitQueue);
        this.mAmbienceLuxQueueMap.put(DEFAULT_TARGET_LUX_QUEUE_KEY, this.mTargetLuxLimitQueue);
        OplusBrightnessDebugCenter.getInstance().registerDebugListener(OplusBrightnessDebugCenter.EVENT.SCENE, this, OBMS_VERIFY_NUMBER);
        AmbienceLog.f(TAG, "OplusBrightnessSceneAdapter init---");
    }

    public void init() {
        this.mNormalScene.init();
        this.mErraticScene.init();
        this.mStableScene.init();
    }

    public void onBootCompleted() {
        AmbienceLog.f(TAG, "onBootCompleted");
        this.mPhoneStateManager.onBootCompleted();
    }

    public void setSensorManager(SensorManager sensorManager, Sensor sensor) {
        this.mPhoneStateManager.setSensorManager(sensorManager, sensor);
    }

    public void onScreenEvent(int displayId, boolean isScreenOn, boolean isFolding, DisplayManagerInternal.DisplayPowerRequest request) {
        this.mPhoneStateManager.onScreenEvent(displayId, isScreenOn, isFolding, request);
        if (!this.mIsArcDebugMode) {
            this.mCurrentScene = this.mNormalScene;
        }
        this.mIsNeedRefreshScore = true;
    }

    public void brightnessModeChanged(boolean isAuto) {
        this.mPhoneStateManager.brightnessModeChanged(isAuto);
    }

    public void onDisplayFoldedStateChange(boolean isFolded) {
        if (Boolean.logicalXor(isFolded, this.mIsFolded)) {
            this.mIsFolded = isFolded;
            this.mPhoneStateManager.onDisplayFoldedStateChange(isFolded);
        }
    }

    public void putToWork(boolean work) {
        AmbienceLog.f(TAG, "putToWork:" + work);
        if (work) {
            if (!this.mIsArcDebugMode) {
                this.mCurrentScene = this.mNormalScene;
            }
            this.mPhoneStateManager.registerListener(this);
        } else {
            this.mPhoneStateManager.registerListener(null);
            clearLuxQueueMap();
        }
        this.mAmbientAdapter.onSensorRegisterStatusChange(work);
    }

    public void deepSleepModeChanged(boolean isDeepSleep) {
        if (Boolean.logicalXor(isDeepSleep, this.mIsDeepSleep)) {
            this.mIsDeepSleep = isDeepSleep;
            this.mPhoneStateManager.deepSleepModeChanged(isDeepSleep);
        }
    }

    public void onGameModeChanged(boolean isGameMode) {
        this.mIsGameMode = isGameMode;
    }

    public boolean getGameMode() {
        return this.mIsGameMode;
    }

    public boolean isFlashLightOn() {
        if (this.mContext == null) {
            return false;
        }
        return OplusScreenOffTorchHelper.getInstance(this.mContext).isFlashLightOn();
    }

    public void clearLuxQueueMap() {
        this.mAmbienceLuxQueueMap.forEach(new BiConsumer() { // from class: com.android.server.display.feature.ambience.scenes.OplusBrightnessSceneAdapter$$ExternalSyntheticLambda1
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                OplusBrightnessSceneAdapter.oplusLambda$clearLuxQueueMap$0((String) obj, (OplusLimitQueue) obj2);
            }
        });
    }

    static /* synthetic */ void oplusLambda$clearLuxQueueMap$0(String key, OplusLimitQueue value) {
        value.clear();
        AmbienceLog.d(TAG, key.toString() + " queue is cleared...");
    }

    public void setDragging(boolean dragging) {
        this.mPhoneStateManager.setDragging(dragging);
    }

    public void setFastAnimationWithReset(boolean enable) {
        this.mPhoneStateManager.setFastAnimationWithReset(enable);
    }

    public void applyRusData() {
        this.mPhoneStateManager.applyRusData();
    }

    public ErraticScene getErraticScene() {
        return this.mErraticScene;
    }

    public NormalScene getNormalScene() {
        return this.mNormalScene;
    }

    public StableScene getStableScene() {
        return this.mStableScene;
    }

    public SceneBase getCurrentScene() {
        return this.mCurrentScene;
    }

    public HashMap<String, OplusLimitQueue> getAmbienceLuxQueueMap() {
        return this.mAmbienceLuxQueueMap;
    }

    public OplusLimitQueue<Float> getRealtimeLuxLimitQueue() {
        return this.mRealtimeLuxLimitQueue;
    }

    public OplusLimitQueue<Float> getWeightLuxLimitQueue() {
        return this.mWeightLuxLimitQueue;
    }

    public OplusLimitQueue<Float> getTargetLuxLimitQueue() {
        return this.mTargetLuxLimitQueue;
    }

    public OplusLimitQueue<Float> getLuxSourceLimitQueue() {
        return this.mLuxSourceLimitQueue;
    }

    public StabilityProcess getStabilityProcess() {
        return this.mAmbientAdapter.getStabilityProcess();
    }

    public LuxPostProcessStrategy getLuxPostProcessStrategy() {
        return this.mAmbientAdapter.getLuxPostProcessStrategy();
    }

    private void matchScene(PhoneState state) {
        if (!this.mIsArcDebugMode) {
            if (!state.isOnlyLuxChanged() || this.mIsNeedRefreshScore) {
                final String lastSceneName = this.mCurrentScene != null ? this.mCurrentScene.getSceneName() : "undefined";
                int erraticScore = this.mErraticScene.sceneScore(state, this.mAmbienceLuxQueueMap, this.mCurrentScene);
                int stableScore = this.mStableScene.sceneScore(state, this.mAmbienceLuxQueueMap, this.mCurrentScene);
                int normalScore = this.mNormalScene.sceneScore(state, this.mAmbienceLuxQueueMap, this.mCurrentScene);
                int finalScore = Math.max(Math.max(erraticScore, stableScore), normalScore);
                if (!this.mIsArcDebugMode) {
                    if (finalScore == normalScore) {
                        this.mCurrentScene = this.mNormalScene;
                    } else if (finalScore == erraticScore) {
                        this.mCurrentScene = this.mErraticScene;
                    } else if (finalScore == stableScore) {
                        this.mCurrentScene = this.mStableScene;
                    } else {
                        this.mCurrentScene = this.mNormalScene;
                    }
                }
                final String CurrentSceneName = this.mCurrentScene.getSceneName();
                AmbienceLog.logic(TAG, new AmbienceLog.ILogPrintLogic() { // from class: com.android.server.display.feature.ambience.scenes.OplusBrightnessSceneAdapter$$ExternalSyntheticLambda0
                    @Override // com.android.server.display.feature.ambience.utils.AmbienceLog.ILogPrintLogic
                    public final boolean isLogPrint() {
                        return OplusBrightnessSceneAdapter.oplusLambda$matchScene$1(lastSceneName, CurrentSceneName);
                    }
                }, "Arc scene change!!! " + lastSceneName + " -> " + CurrentSceneName + " [erraticScore:" + erraticScore + ", stableScore:" + stableScore + "] isLuxRefresh:" + this.mIsNeedRefreshScore);
                this.mIsNeedRefreshScore = false;
            }
        }
    }

    static /* synthetic */ boolean oplusLambda$matchScene$1(String lastSceneName, String CurrentSceneName) {
        return !lastSceneName.equals(CurrentSceneName);
    }

    private void sendAmbientDateCallback(AmbientData data) {
        if (this.mAmbienceListener != null && data != null) {
            this.mAmbienceListener.onAmbienceChanged(data);
        }
    }

    public void updateLuxChange(float lux, boolean isTargetChange) {
        if (!Float.isFinite(lux)) {
            AmbienceLog.e(TAG, "updateLuxChange is fail, lux:" + lux + " isTargetChange:" + isTargetChange);
            return;
        }
        if (isTargetChange) {
            this.mWeightLuxLimitQueue.add(Float.valueOf(lux));
            this.mTargetLuxLimitQueue.add(Float.valueOf(lux));
        } else if (!this.mWeightLuxLimitQueue.isEmpty() && this.mWeightLuxLimitQueue.getLastDataDuration() >= 1000) {
            this.mWeightLuxLimitQueue.add(Float.valueOf(lux));
        }
        this.mIsNeedRefreshScore = isTargetChange || getStabilityProcess().isSmallChange(lux);
        this.mPhoneStateManager.updateLuxChange(lux, isTargetChange);
    }

    @Override // com.android.server.display.feature.ambience.events.OplusBrightnessPhoneStateManager.PhoneStateListener
    public void onPhoneStateChanged(PhoneState state) {
        matchScene(state);
        if (state.isImportantDataExist()) {
            this.mRealtimeLuxLimitQueue.add(Float.valueOf(state.getLuxEvent().getLux()));
            this.mLuxSourceLimitQueue.add(Float.valueOf(state.getLuxEvent().isMainSensorData() ? SensorDelegate.DEFAULT_PROXIMITY_NEAR_CHECK_VALUE : 1.0f));
            sendAmbientDateCallback(new AmbientData(state, this.mCurrentScene));
        }
    }

    private class SceneAdapterHandler extends Handler {
        public SceneAdapterHandler(OplusBrightnessSceneAdapter oplusBrightnessSceneAdapter, Looper looper) {
            super(looper);
            AmbienceLog.f(OplusBrightnessSceneAdapter.TAG, "SceneAdapterHandler init");
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            int i = msg.what;
        }
    }

    public void onEventChange(EventResult result) {
    }

    public void onSceneChange(SceneBase scene) {
        if (scene != null) {
            AmbienceLog.d(TAG, "onSceneChange! " + this.mCurrentScene + " -> " + scene);
            this.mIsArcDebugMode = true;
            this.mCurrentScene = scene;
        }
    }

    public void onDebugFinish() {
        this.mCurrentScene = null;
        this.mIsArcDebugMode = false;
        this.mIsNeedRefreshScore = true;
    }

    public void setIrOn() {
        if (this.mPhoneStateManager != null) {
            this.mPhoneStateManager.setIrOn();
        }
    }
}
