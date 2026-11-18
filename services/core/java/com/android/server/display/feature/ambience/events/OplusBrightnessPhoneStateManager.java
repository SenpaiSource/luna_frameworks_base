package com.android.server.display.feature.ambience.events;

import android.common.OplusFeatureCache;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraManager;
import android.hardware.display.DisplayManagerInternal;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import com.android.server.display.config.global.ComfortableBrightnessConfig;
import com.android.server.display.config.global.OplusDisplayGlobalConfig;
import com.android.server.display.feature.ambience.OplusBrightnessAmbientAdapter;
import com.android.server.display.feature.ambience.events.delegate.DeepThinkerDelegate;
import com.android.server.display.feature.ambience.events.delegate.NetworkDelegate;
import com.android.server.display.feature.ambience.events.delegate.SensorDelegate;
import com.android.server.display.feature.ambience.events.event.DarkModeEvent;
import com.android.server.display.feature.ambience.events.event.InOutDoorEvent;
import com.android.server.display.feature.ambience.events.event.LuxEvent;
import com.android.server.display.feature.ambience.events.event.MotionEvent;
import com.android.server.display.feature.ambience.events.event.NetworkEvent;
import com.android.server.display.feature.ambience.events.event.ProximityEvent;
import com.android.server.display.feature.ambience.events.event.TimeEvent;
import com.android.server.display.feature.ambience.events.utils.EventBase;
import com.android.server.display.feature.ambience.events.utils.EventResult;
import com.android.server.display.feature.ambience.events.utils.PhoneState;
import com.android.server.display.feature.ambience.processes.process.StabilityProcess;
import com.android.server.display.feature.ambience.scenes.scene.ErraticScene;
import com.android.server.display.feature.ambience.scenes.utils.SceneBase;
import com.android.server.display.feature.ambience.utils.AmbienceLog;
import com.android.server.display.feature.stat.TrackManager;
import com.android.server.display.feature.viewer.BrightnessRecorder;
import com.android.server.display.feature.viewer.BrightnessViewer;
import com.android.server.display.marvels.module.IORBrightnessMarvelsDataCollector;
import com.android.server.display.test.impl.OplusBrightnessDebugCenter;
import com.android.server.display.util.OplusDisplayState;

/* JADX INFO: loaded from: /run/media/sathiya/data/scratch/ambience.dex */
public class OplusBrightnessPhoneStateManager {
    private static final String CAMERA_ID_FOR_TORCH = "0";
    private static final long DEFAULT_RESET_IR_STATE_TIME = 3000;
    private static final long DEFAULT_STATE_CHANGED_CALLBACK_TIMEOUT = 3000;
    private static final long DEFAULT_TORCH_EXIT_DEBOUNCE_DELAY_TIME = 1000;
    private static final int MSG_CYCLE_UPDATE_STATUS = 5468;
    private static final int MSG_FIRST_INDEX = 5467;
    private static final int MSG_RESET_IR_STATE = 5471;
    private static final int MSG_UPDATE_IN_OUT_DOOR_STATE = 5469;
    private static final int MSG_UPDATE_TORCH_STATE = 5470;
    private static final int OBMS_VERIFY_NUMBER = 80261364;
    private static final String TAG = "OplusBrightnessPhoneStateManager";
    private Context mContext;
    private boolean mIsInitSuccess;
    private PhoneStateHandler mPhoneStateHandler;
    private Handler mSmartHandler;
    private final Object mMotionChangeFlagObject = new Object();
    private final Object mNetworkChangeFlagObject = new Object();
    private final Object mLuxChangeFlagObject = new Object();
    private boolean mIsBootComplete = false;
    private boolean mIsRegisterSuccess = false;
    private boolean mIsDragging = false;
    private boolean mIsDeepSleep = false;
    private boolean mIsFolded = false;
    private boolean mIsFastAnimatingWithReset = false;
    private boolean mInOutDoorUpdateRunning = false;
    private volatile boolean mIsTorchOn = false;
    private boolean mIsIrOn = false;
    private boolean mIsUntrustedState = false;
    private long mLastBrightnessChangeTime = 0;
    private boolean mIsAutomaticMode = false;
    private int mSkipLuxCount = 0;
    private int mSkipLuxCountDefault = 3;
    private PhoneStateListener mPhoneStateListener = null;
    private LuxEvent.LuxEventResult mLastMainEventResult = null;
    private LuxEvent.LuxEventResult mLastAuxEventResult = null;
    private LuxEvent.LuxEventResult mLastLuxEventResult = null;
    private DeepThinkerDelegate mDeepThinkerManagerDelegate = DeepThinkerDelegate.getInstance();
    private NetworkDelegate mNetworkDelegate = NetworkDelegate.getInstance();
    private DarkModeEvent mDarkModeEvent = DarkModeEvent.getInstance();
    private SensorDelegate mSensorDelegate = SensorDelegate.getInstance();
    private InOutDoorEvent mInOutDoorEvent = InOutDoorEvent.getInstance();
    private LuxEvent mLuxEvent = LuxEvent.getInstance();
    private MotionEvent mMotionEvent = MotionEvent.getInstance();
    private NetworkEvent mNetworkEvent = NetworkEvent.getInstance();
    private ProximityEvent mProximityEvent = ProximityEvent.getInstance();
    private TimeEvent mTimeEvent = TimeEvent.getInstance();
    private PhoneState mPhoneState = new PhoneState();
    private EventBase.IEventListener mMotionListener = this::oplusLambda$new$6;
    private EventBase.IEventListener mDarkModeListener = this::oplusLambda$new$7;
    private EventBase.IEventListener mTimeListener = this::oplusLambda$new$8;
    private EventBase.IEventListener mNetworkEventListener = this::oplusLambda$new$9;
    private EventBase.IEventListener mLuxEventListener = this::oplusLambda$new$11;
    private EventBase.IEventListener mProximityEventListener = this::oplusLambda$new$12;
    private EventBase.IEventListener mInOutDoorEventListener = this::oplusLambda$new$13;
    private final CameraManager.TorchCallback mTorchCallback = new CameraManager.TorchCallback() { // from class: com.android.server.display.feature.ambience.events.OplusBrightnessPhoneStateManager.2
        @Override // android.hardware.camera2.CameraManager.TorchCallback
        public void onTorchModeUnavailable(String cameraId) {
            AmbienceLog.f(OplusBrightnessPhoneStateManager.TAG, "onTorchModeUnavailable, id:" + cameraId);
            if (cameraId != null && cameraId.equals(OplusBrightnessPhoneStateManager.CAMERA_ID_FOR_TORCH)) {
                OplusBrightnessPhoneStateManager.this.mPhoneStateHandler.removeMessages(OplusBrightnessPhoneStateManager.MSG_UPDATE_TORCH_STATE);
                OplusBrightnessPhoneStateManager.this.setTorchModeEnabled(true);
            }
        }

        @Override // android.hardware.camera2.CameraManager.TorchCallback
        public void onTorchModeChanged(String cameraId, boolean enabled) {
            AmbienceLog.f(OplusBrightnessPhoneStateManager.TAG, "onTorchModeChanged, id:" + cameraId + " enabled:" + enabled);
            OplusBrightnessPhoneStateManager.this.mPhoneStateHandler.removeMessages(OplusBrightnessPhoneStateManager.MSG_UPDATE_TORCH_STATE);
            if (enabled) {
                OplusBrightnessPhoneStateManager.this.setTorchModeEnabled(true);
                return;
            }
            Message msg = OplusBrightnessPhoneStateManager.this.mPhoneStateHandler.obtainMessage(OplusBrightnessPhoneStateManager.MSG_UPDATE_TORCH_STATE);
            msg.obj = Boolean.valueOf(enabled);
            OplusBrightnessPhoneStateManager.this.mPhoneStateHandler.sendMessageDelayed(msg, 1000L);
        }
    };

    public interface PhoneStateListener {
        void onPhoneStateChanged(PhoneState phoneState);
    }

    public OplusBrightnessPhoneStateManager(Context context, Handler handler) {
        this.mIsInitSuccess = false;
        this.mContext = null;
        this.mSmartHandler = null;
        this.mPhoneStateHandler = null;
        AmbienceLog.f(TAG, "OplusBrightnessPhoneStateManager init+++");
        this.mContext = context;
        this.mSmartHandler = handler;
        this.mPhoneStateHandler = new PhoneStateHandler(handler.getLooper());
        this.mLuxEvent.init(this.mContext, this.mPhoneStateHandler);
        this.mDeepThinkerManagerDelegate.init(this.mContext, this.mPhoneStateHandler);
        this.mNetworkDelegate.init(this.mContext, this.mPhoneStateHandler);
        this.mDarkModeEvent.init(this.mContext, this.mPhoneStateHandler);
        this.mSensorDelegate.init(this.mContext, this.mPhoneStateHandler);
        this.mInOutDoorEvent.init(this.mContext, this.mPhoneStateHandler);
        this.mMotionEvent.init(this.mContext, this.mPhoneStateHandler);
        this.mNetworkEvent.init(this.mContext, this.mPhoneStateHandler);
        this.mProximityEvent.init(this.mContext, this.mPhoneStateHandler);
        this.mTimeEvent.init(this.mContext, this.mPhoneStateHandler);
        this.mPhoneState.clearData();
        this.mIsInitSuccess = true;
        AmbienceLog.f(TAG, "OplusBrightnessPhoneStateManager init---");
    }

    private void registerEvents(boolean register) {
        this.mPhoneState.clearData();
        clearLuxDataAsync();
        if (register) {
            this.mPhoneStateHandler.post(this::oplusLambda$registerEvents$0);
        } else {
            this.mPhoneStateHandler.post(this::oplusLambda$registerEvents$1);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void oplusLambda$registerEvents$0() {
        this.mLuxEvent.register(this.mLuxEventListener);
        if (getConfig() != null && getConfig().mMotionModeSupport) {
            this.mMotionEvent.register(this.mMotionListener);
        }
        this.mDarkModeEvent.register(this.mDarkModeListener);
        this.mTimeEvent.register(this.mTimeListener);
        this.mNetworkEvent.register(this.mNetworkEventListener);
        this.mProximityEvent.register(chooseProximitySensor(), this.mProximityEventListener);
        if (getConfig() != null && getConfig().mInOutDoorEventSupport) {
            this.mInOutDoorEvent.register(this.mInOutDoorEventListener);
        }
        this.mIsRegisterSuccess = true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void oplusLambda$registerEvents$1() {
        this.mLuxEvent.unregister(this.mLuxEventListener);
        if (getConfig() != null && getConfig().mMotionModeSupport) {
            this.mMotionEvent.unregister(this.mMotionListener);
        }
        this.mDarkModeEvent.unregister(this.mDarkModeListener);
        this.mTimeEvent.unregister(this.mTimeListener);
        this.mNetworkEvent.unregister(this.mNetworkEventListener);
        this.mProximityEvent.unregister(this.mProximityEventListener);
        if (getConfig() != null && getConfig().mInOutDoorEventSupport) {
            this.mInOutDoorEvent.unregister(this.mInOutDoorEventListener);
        }
        this.mIsRegisterSuccess = false;
    }

    public void onBootCompleted() {
        this.mPhoneStateHandler.post(this::oplusLambda$onBootCompleted$2);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void oplusLambda$onBootCompleted$2() {
        AmbienceLog.f(TAG, "onBootCompleted");
        this.mLuxEvent.onBootCompleted();
        this.mDeepThinkerManagerDelegate.onBootCompleted();
        this.mNetworkDelegate.onBootCompleted();
        this.mDarkModeEvent.onBootCompleted();
        this.mSensorDelegate.onBootCompleted();
        this.mInOutDoorEvent.onBootCompleted();
        this.mMotionEvent.onBootCompleted();
        this.mNetworkEvent.onBootCompleted();
        this.mProximityEvent.onBootCompleted();
        this.mTimeEvent.onBootCompleted();
        registerTorchCallback();
        registerDebugCallback();
        this.mIsBootComplete = true;
    }

    private void registerTorchCallback() {
        if (this.mSensorDelegate.isDualLightSensorSupport()) {
            CameraManager cm = (CameraManager) this.mContext.getSystemService("camera");
            if (cm != null) {
                cm.registerTorchCallback(this.mTorchCallback, this.mPhoneStateHandler);
            } else {
                AmbienceLog.e(TAG, "registerTorchCallback fail, CameraManager is null.");
            }
        }
    }

    /* JADX INFO: renamed from: com.android.server.display.feature.ambience.events.OplusBrightnessPhoneStateManager$1, reason: invalid class name */
    class AnonymousClass1 implements OplusBrightnessDebugCenter.IOplusDebugListener {
        boolean isDebug = false;

        AnonymousClass1() {
        }

        public void onEventChange(EventResult result) {
            if (!this.isDebug) {
                OplusBrightnessPhoneStateManager.this.mPhoneStateHandler.post(this::oplusLambda$onEventChange$0);
            }
            this.isDebug = true;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void oplusLambda$onEventChange$0() {
            OplusBrightnessPhoneStateManager.this.mLastMainEventResult = null;
            OplusBrightnessPhoneStateManager.this.mLastAuxEventResult = null;
        }

        public void onSceneChange(SceneBase scene) {
        }

        public void onDebugFinish() {
            OplusBrightnessPhoneStateManager.this.mPhoneStateHandler.post(this::oplusLambda$onDebugFinish$1);
            this.isDebug = false;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void oplusLambda$onDebugFinish$1() {
            OplusBrightnessPhoneStateManager.this.mLastMainEventResult = null;
            OplusBrightnessPhoneStateManager.this.mLastAuxEventResult = null;
        }
    }

    private void registerDebugCallback() {
        OplusBrightnessDebugCenter.getInstance().registerDebugListener(OplusBrightnessDebugCenter.EVENT.LUX, new AnonymousClass1(), OBMS_VERIFY_NUMBER);
    }

    public void onScreenEvent(int displayId, boolean isScreenOn, boolean isFolding, DisplayManagerInternal.DisplayPowerRequest request) {
        this.mLastBrightnessChangeTime = TimeEvent.getSystemMillisTime();
        if (isScreenOn) {
            if (this.mIsRegisterSuccess && this.mLuxEvent.isRegistered()) {
                AmbienceLog.f(TAG, "screen on first lux event:" + this.mLastLuxEventResult);
                if (this.mPhoneState.setLuxEvent(this.mLastLuxEventResult)) {
                    this.mPhoneStateHandler.post(this::oplusLambda$onScreenEvent$3);
                }
            }
        } else {
            clearLuxDataAsync();
        }
        if (!isFolding) {
            updateSensorSwitch();
        }
        this.mLuxEvent.onScreenEvent(displayId, isScreenOn);
        this.mMotionEvent.onScreenEvent(displayId, isScreenOn);
        this.mDarkModeEvent.onScreenEvent(displayId, isScreenOn);
        this.mTimeEvent.onScreenEvent(displayId, isScreenOn);
        this.mNetworkEvent.onScreenEvent(displayId, isScreenOn);
        this.mProximityEvent.onScreenEvent(displayId, isScreenOn);
        this.mInOutDoorEvent.onScreenEvent(displayId, isScreenOn);
    }

    public void applyRusData() {
        if (this.mIsRegisterSuccess) {
            if (getConfig() != null && !getConfig().mMotionModeSupport) {
                this.mMotionEvent.unregister(this.mMotionListener);
            } else {
                this.mMotionEvent.register(this.mMotionListener);
            }
        }
    }

    public void brightnessModeChanged(boolean isAuto) {
        this.mIsAutomaticMode = isAuto;
    }

    public void deepSleepModeChanged(boolean isDeepSleep) {
        if (Boolean.logicalXor(isDeepSleep, this.mIsDeepSleep)) {
            this.mIsDeepSleep = isDeepSleep;
            updateSensorSwitch();
        }
    }

    public void onDisplayFoldedStateChange(boolean isFolded) {
        if (Boolean.logicalXor(isFolded, this.mIsFolded)) {
            AmbienceLog.f(TAG, "onDisplayFoldedStateChange " + this.mIsFolded + "--->" + isFolded);
            this.mIsFolded = isFolded;
            updateTorchModeLuxReliability();
            if (getConfig() != null && getConfig().mSecondaryProximitySensorSupport) {
                this.mPhoneState.setProximityEvent(null);
                this.mProximityEvent.unregister(this.mProximityEventListener);
                this.mProximityEvent.register(chooseProximitySensor(), this.mProximityEventListener);
            }
        }
    }

    private void updateSensorSwitch() {
        if (this.mIsRegisterSuccess) {
            this.mPhoneStateHandler.post(this::oplusLambda$updateSensorSwitch$4);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void oplusLambda$updateSensorSwitch$4() {
        if (OplusDisplayState.getInstance().isScreenOn()) {
            this.mLuxEvent.register(this.mLuxEventListener);
            this.mProximityEvent.register(chooseProximitySensor(), this.mProximityEventListener);
            return;
        }
        if (getConfig() != null && getConfig().mLightSensorAlwaysOnSupport && !this.mIsDeepSleep && this.mIsAutomaticMode) {
            this.mLuxEvent.unregister(LuxEvent.SENSOR_TYPE.AUX_LIGHT, this.mLuxEventListener);
        } else {
            this.mLuxEvent.unregister(this.mLuxEventListener);
        }
        if (!OplusBrightnessAmbientAdapter.isProximityPowerOff()) {
            this.mProximityEvent.unregister(this.mProximityEventListener);
        }
    }

    private ProximityEvent.SENSOR_TYPE chooseProximitySensor() {
        if (getConfig() != null && getConfig().mSecondaryProximitySensorSupport && this.mIsFolded) {
            return ProximityEvent.SENSOR_TYPE.SUB_PROXIMITY;
        }
        if (getConfig() != null && getConfig().mAutoModeProximitySensorSupport) {
            return ProximityEvent.SENSOR_TYPE.PROXIMITY;
        }
        return null;
    }

    private void clearLuxDataAsync() {
        this.mPhoneStateHandler.post(this::oplusLambda$clearLuxDataAsync$5);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void oplusLambda$clearLuxDataAsync$5() {
        this.mLastMainEventResult = null;
        this.mLastAuxEventResult = null;
        this.mLastLuxEventResult = null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void oplusLambda$new$6(EventResult result) {
        MotionEvent.MotionEventResult motionResult = (MotionEvent.MotionEventResult) result;
        updateInOutDoorState(this.mMotionChangeFlagObject);
        if (this.mPhoneState.setMotionEvent(motionResult)) {
            oplusLambda$onScreenEvent$3();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void oplusLambda$new$7(EventResult result) {
        DarkModeEvent.DarkModeEventResult darkModeResult = (DarkModeEvent.DarkModeEventResult) result;
        if (this.mPhoneState.setDarkModeEvent(darkModeResult)) {
            oplusLambda$onScreenEvent$3();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void oplusLambda$new$8(EventResult result) {
        if (result == null) {
            return;
        }
        TimeEvent.TimeEventResult timeResult = (TimeEvent.TimeEventResult) result;
        if (!timeResult.objectIsNull() && this.mPhoneState.setTimeEvent(timeResult)) {
            oplusLambda$onScreenEvent$3();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void oplusLambda$new$9(EventResult result) {
        NetworkEvent.NetworkEventResult networkResult = (NetworkEvent.NetworkEventResult) result;
        updateInOutDoorState(this.mNetworkChangeFlagObject);
        if (this.mPhoneState.setNetworkEvent(networkResult)) {
            oplusLambda$onScreenEvent$3();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void oplusLambda$new$11(EventResult result) {
        LuxEvent.LuxEventResult tempLuxResult;
        if (result == null) {
            return;
        }
        LuxEvent.LuxEventResult sensorResult = (LuxEvent.LuxEventResult) result;
        if (!this.mIsFastAnimatingWithReset && this.mSkipLuxCount != 0) {
            this.mSkipLuxCount--;
            AmbienceLog.f(TAG, "skip lux , mSkipLuxCount = " + this.mSkipLuxCount);
            return;
        }
        if ((sensorResult.objectIsNull() && !sensorResult.isDebugResult()) || this.mIsDragging || this.mIsFastAnimatingWithReset || (tempLuxResult = assessLuxEffectiveness(sensorResult)) == null) {
            return;
        }
        this.mLastLuxEventResult = tempLuxResult;
        if (OplusDisplayState.getInstance().isScreenOn() && this.mPhoneState.setLuxEvent(this.mLastLuxEventResult)) {
            AmbienceLog.logic(TAG, this::oplusLambda$new$10, sensorResult + " -> " + this.mLastLuxEventResult);
            oplusLambda$onScreenEvent$3();
            if (this.mLastLuxEventResult != null) {
                int lux = (int) this.mLastLuxEventResult.getLux();
                TrackManager.getInstance().setLux(lux);
                BrightnessViewer.getInstance().setLux(lux);
                BrightnessRecorder.getInstance().updateRecorderLux(lux);
                OplusFeatureCache.getOrCreate(IORBrightnessMarvelsDataCollector.DEFAULT, new Object[0]).setLux(lux);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ boolean oplusLambda$new$10() {
        if (this.mLastLuxEventResult != null) {
            return TimeEvent.getSystemMillisTime() - this.mLastBrightnessChangeTime < TimeEvent.DEFAULT_TIME_TWO_SECONDS || AmbienceLog.isLogOn;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void oplusLambda$new$12(EventResult result) {
        ProximityEvent.ProximityEventResult sensorResult = (ProximityEvent.ProximityEventResult) result;
        if (this.mPhoneState.setProximityEvent(sensorResult)) {
            oplusLambda$onScreenEvent$3();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void oplusLambda$new$13(EventResult result) {
        InOutDoorEvent.InOutDoorEventResult inOutDoorResult = (InOutDoorEvent.InOutDoorEventResult) result;
        if (this.mPhoneState.setInOutDoorEvent(inOutDoorResult)) {
            oplusLambda$onScreenEvent$3();
        }
    }

    public void setSensorManager(SensorManager mgr, Sensor sensor) {
        this.mSensorDelegate.setSensorManager(mgr, sensor);
        if (getConfig() != null) {
            getConfig().updateConfigForOldProject();
        }
    }

    public void registerListener(PhoneStateListener listener) {
        this.mPhoneStateListener = listener;
        boolean isRegister = this.mPhoneStateListener != null;
        if (Boolean.logicalXor(this.mIsRegisterSuccess, isRegister)) {
            AmbienceLog.f(TAG, "registerListener: " + isRegister + " lastRegister:" + this.mIsRegisterSuccess);
            registerEvents(isRegister);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX INFO: renamed from: sendPhoneStateCallback, reason: merged with bridge method [inline-methods] */
    public void oplusLambda$onScreenEvent$3() {
        this.mPhoneStateHandler.removeMessages(MSG_CYCLE_UPDATE_STATUS);
        if (this.mPhoneStateListener != null) {
            if (this.mPhoneState.getMotionEvent() == null && getConfig() != null && !getConfig().mMotionModeSupport) {
                this.mPhoneState.setMotionEvent(new MotionEvent.MotionEventResult(MotionEvent.MotionEventResult.ACTIVITY.MOTIONLESS.name(), MotionEvent.MotionEventResult.ACTIVITY.MOTIONLESS.ordinal()));
            }
            if (this.mPhoneState.getProximityEvent() == null && getConfig() != null && !getConfig().mAutoModeProximitySensorSupport) {
                this.mPhoneState.setProximityEvent(new ProximityEvent.ProximityEventResult(ProximityEvent.ProximityEventResult.PROXIMITY.FAR.name(), ProximityEvent.ProximityEventResult.PROXIMITY.FAR.ordinal()));
            }
            if (this.mPhoneState.getInOutDoorEvent() == null && getConfig() != null && !getConfig().mInOutDoorEventSupport) {
                this.mPhoneState.setInOutDoorEvent(new InOutDoorEvent.InOutDoorEventResult(InOutDoorEvent.InOutDoorEventResult.INOUT.UNCERTAIN.name(), InOutDoorEvent.InOutDoorEventResult.INOUT.UNCERTAIN.ordinal()));
            }
            this.mPhoneStateListener.onPhoneStateChanged(new PhoneState(this.mPhoneState));
            this.mPhoneState.callbackEnd();
            this.mPhoneStateHandler.sendEmptyMessageDelayed(MSG_CYCLE_UPDATE_STATUS, ErraticScene.DEFAULT_CALCULATE_HISTORY_UPDATE_TIME);
        }
    }

    private boolean shouldUseLastMainSensorResult(LuxEvent.LuxEventResult result) {
        ProximityEvent.ProximityEventResult proximityEvent;
        if (result == null || (proximityEvent = this.mPhoneState.getProximityEvent()) == null || !proximityEvent.isNear()) {
            return false;
        }
        return this.mLastMainEventResult == null || result.getLux() < this.mLastMainEventResult.getLux();
    }

    private LuxEvent.LuxEventResult assessLuxEffectiveness(LuxEvent.LuxEventResult result) {
        if (result == null) {
            AmbienceLog.f(TAG, "assessLuxEffectiveness result is null");
            return null;
        }
        if (result.isMainSensorData()) {
            if (this.mIsUntrustedState || shouldUseLastMainSensorResult(result)) {
                if (this.mLastMainEventResult == null) {
                    AmbienceLog.f(TAG, "assessLuxEffectiveness lastMainEventResult is null");
                    return null;
                }
                result = this.mLastMainEventResult;
            }
            this.mLastMainEventResult = result;
            if (this.mLastAuxEventResult != null) {
                int compare = Float.compare(result.getLux(), this.mLastAuxEventResult.getLux());
                if (compare < 0) {
                    return null;
                }
                if (compare == 0 && result.getLux() > SensorDelegate.DEFAULT_PROXIMITY_NEAR_CHECK_VALUE) {
                    return this.mLastAuxEventResult;
                }
            }
        } else if (result.isAuxSensorData()) {
            if (this.mIsTorchOn || this.mIsIrOn) {
                return this.mLastLuxEventResult;
            }
            this.mLastAuxEventResult = result;
            if (this.mLastMainEventResult != null) {
                if (result.getLux() < this.mLastMainEventResult.getLux()) {
                    return null;
                }
            } else if (this.mLastLuxEventResult == null) {
                return null;
            }
        }
        return result;
    }

    public void updateLuxChange(float lux, boolean isTargetChange) {
        if (isTargetChange) {
            long currentTime = TimeEvent.getSystemMillisTime();
            if (StabilityProcess.isDarkEnvironmentLux(lux)) {
                updateInOutDoorState(this.mLuxChangeFlagObject);
            }
            this.mLastBrightnessChangeTime = currentTime;
        }
    }

    public void setDragging(boolean dragging) {
        this.mIsDragging = dragging;
    }

    public void setFastAnimationWithReset(boolean enable) {
        this.mIsFastAnimatingWithReset = enable;
        if (enable) {
            this.mSkipLuxCount = this.mSkipLuxCountDefault;
        }
    }

    private void updateInOutDoorState(Object reason) {
        if (getConfig() == null || !getConfig().mInOutDoorEventSupport) {
            return;
        }
        if (this.mPhoneStateHandler.hasMessages(MSG_UPDATE_IN_OUT_DOOR_STATE)) {
            if (!this.mInOutDoorUpdateRunning && !this.mPhoneStateHandler.hasEqualMessages(MSG_UPDATE_IN_OUT_DOOR_STATE, reason)) {
                this.mPhoneStateHandler.removeMessages(MSG_UPDATE_IN_OUT_DOOR_STATE);
                long delay = 0;
                if (reason != null) {
                    if (reason.equals(this.mLuxChangeFlagObject) || reason.equals(this.mMotionChangeFlagObject)) {
                        delay = TimeEvent.DEFAULT_TIME_HALF_MINUTE;
                    } else if (reason.equals(this.mNetworkChangeFlagObject)) {
                        delay = 1000;
                    }
                }
                this.mInOutDoorUpdateRunning = true;
                this.mPhoneStateHandler.sendEmptyMessageDelayed(MSG_UPDATE_IN_OUT_DOOR_STATE, delay);
                AmbienceLog.f(TAG, "InOutDoor last reason: " + getUpdateInOutDoorReasonName(reason) + " delay:" + delay);
                return;
            }
            return;
        }
        Message msg = this.mPhoneStateHandler.obtainMessage(MSG_UPDATE_IN_OUT_DOOR_STATE);
        msg.obj = reason;
        this.mPhoneStateHandler.sendMessageDelayed(msg, TimeEvent.DEFAULT_TIME_HALF_HOUR);
        AmbienceLog.f(TAG, "InOutDoor first reason: " + getUpdateInOutDoorReasonName(reason));
    }

    private String getUpdateInOutDoorReasonName(Object reason) {
        if (reason != null) {
            if (reason.equals(this.mLuxChangeFlagObject)) {
                return "lux";
            }
            if (reason.equals(this.mMotionChangeFlagObject)) {
                return "motion";
            }
            if (reason.equals(this.mNetworkChangeFlagObject)) {
                return "network";
            }
            return "unknown";
        }
        return "null";
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setTorchModeEnabled(boolean isOn) {
        AmbienceLog.f(TAG, "Torch state change " + this.mIsTorchOn + " -> " + isOn);
        this.mIsTorchOn = isOn;
        this.mLastAuxEventResult = null;
        updateTorchModeLuxReliability();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setIrEnabled(boolean isOn) {
        AmbienceLog.f(TAG, "Ir state change " + this.mIsIrOn + " -> " + isOn);
        this.mIsIrOn = isOn;
        this.mLastAuxEventResult = null;
    }

    private void updateTorchModeLuxReliability() {
        boolean result = false;
        if (this.mIsTorchOn && OplusDisplayGlobalConfig.getInstance().hasRemapDisable() && this.mIsFolded) {
            result = true;
        }
        this.mIsUntrustedState = result;
        AmbienceLog.f(TAG, "update Lux reliability, isTrust:" + (!this.mIsUntrustedState));
    }

    private ComfortableBrightnessConfig getConfig() {
        return OplusDisplayGlobalConfig.getInstance().getComfortableBrightnessConfig();
    }

    private class PhoneStateHandler extends Handler {
        public PhoneStateHandler(Looper looper) {
            super(looper);
            AmbienceLog.f(OplusBrightnessPhoneStateManager.TAG, "PhoneStateHandler init");
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case OplusBrightnessPhoneStateManager.MSG_CYCLE_UPDATE_STATUS /* 5468 */:
                    if (OplusBrightnessPhoneStateManager.this.mPhoneState.isAuxiliaryEventChanged()) {
                        OplusBrightnessPhoneStateManager.this.oplusLambda$onScreenEvent$3();
                    }
                    break;
                case OplusBrightnessPhoneStateManager.MSG_UPDATE_IN_OUT_DOOR_STATE /* 5469 */:
                    OplusBrightnessPhoneStateManager.this.mInOutDoorEvent.requestInOutDoorState();
                    OplusBrightnessPhoneStateManager.this.mInOutDoorUpdateRunning = false;
                    break;
                case OplusBrightnessPhoneStateManager.MSG_UPDATE_TORCH_STATE /* 5470 */:
                    OplusBrightnessPhoneStateManager.this.setTorchModeEnabled(((Boolean) msg.obj).booleanValue());
                    break;
                case OplusBrightnessPhoneStateManager.MSG_RESET_IR_STATE /* 5471 */:
                    OplusBrightnessPhoneStateManager.this.setIrEnabled(false);
                    break;
            }
        }
    }

    public void setIrOn() {
        AmbienceLog.f(TAG, "setIrOn");
        setIrEnabled(true);
        Message msg = this.mPhoneStateHandler.obtainMessage(MSG_RESET_IR_STATE);
        this.mPhoneStateHandler.removeMessages(MSG_RESET_IR_STATE);
        this.mPhoneStateHandler.sendMessageDelayed(msg, ErraticScene.DEFAULT_CALCULATE_HISTORY_UPDATE_TIME);
    }
}
