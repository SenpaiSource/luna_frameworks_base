package com.android.server.display.feature.ambience.events.delegate;

import android.R;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import com.android.server.display.feature.ambience.events.event.LuxEvent;
import com.android.server.display.feature.ambience.events.event.ProximityEvent;
import com.android.server.display.feature.ambience.utils.AmbienceLog;
import com.oplus.content.OplusFeatureConfigManager;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/* JADX INFO: loaded from: /run/media/sathiya/data/scratch/ambience.dex */
public class SensorDelegate {
    private static final int DEFAULT_NUMBER_ZERO = 0;
    public static final float DEFAULT_PROXIMITY_NEAR_CHECK_VALUE = 0.0f;
    private static final String MAIN_RGB_LIGHT_SENSOR_NAME = "qti.sensor.bri_light";
    private static final int MSG_FIRST_INDEX = 5467;
    private static final int MSG_REGISTER_SENSOR_EVENT = 5468;
    private static final int SUB_INFRARED_PROXIMITY_TYPE = 65641;
    private static final String TAG = "SensorDelegate";
    private String mAuxLightSensorName;
    private Sensor mAuxSensor;
    private Context mContext;
    private boolean mIsAuxLightSensorActive;
    private boolean mIsBootComplete;
    private boolean mIsDualLightSensorSupport;
    private boolean mIsMainLightSensorActive;
    private boolean mIsProximitySensorActive;
    private boolean mIsRGBMainLightSensor;
    private boolean mIsSensorManagerReady;
    private boolean mIsSubProximitySensorActive;
    private boolean mIsSubProximitySupport;
    private String mMainLightSensorName;
    private Sensor mMainSensor;
    private Sensor mProximitySensor;
    private String mProximitySensorName;
    private ConcurrentHashMap<SENSOR_TYPE, ConcurrentHashMap<SensorEventListener, Boolean>> mRegisteredSensorListenerMap;
    private SensorDelegateHandler mSensorDelegateHandler;
    private SensorManager mSensorManager;
    private Sensor mSubProximitySensor;
    private String mSubProximitySensorName;

    public enum SENSOR_TYPE {
        MAIN_LIGHT,
        AUX_LIGHT,
        PROXIMITY,
        SUB_PROXIMITY
    }

    private static class SensorDelegateHolder {
        private static final SensorDelegate INSTANCE = new SensorDelegate();

        private SensorDelegateHolder() {
        }
    }

    public static final SensorDelegate getInstance() {
        return SensorDelegateHolder.INSTANCE;
    }

    private SensorDelegate() {
        this.mIsBootComplete = false;
        this.mIsSensorManagerReady = false;
        this.mIsRGBMainLightSensor = false;
        this.mIsDualLightSensorSupport = false;
        this.mIsSubProximitySupport = false;
        this.mIsMainLightSensorActive = false;
        this.mIsAuxLightSensorActive = false;
        this.mIsProximitySensorActive = false;
        this.mIsSubProximitySensorActive = false;
        this.mMainLightSensorName = null;
        this.mAuxLightSensorName = null;
        this.mProximitySensorName = null;
        this.mSubProximitySensorName = null;
        this.mContext = null;
        this.mSensorDelegateHandler = null;
        this.mSensorManager = null;
        this.mMainSensor = null;
        this.mAuxSensor = null;
        this.mProximitySensor = null;
        this.mSubProximitySensor = null;
        this.mRegisteredSensorListenerMap = new ConcurrentHashMap<>();
        this.mRegisteredSensorListenerMap.clear();
        for (SENSOR_TYPE type : SENSOR_TYPE.values()) {
            this.mRegisteredSensorListenerMap.put(type, new ConcurrentHashMap<>());
        }
    }

    public void init(Context context, Handler handler) {
        AmbienceLog.f(TAG, "SensorDelegate init");
        this.mContext = context;
        this.mSensorDelegateHandler = new SensorDelegateHandler(handler.getLooper());
    }

    public void onBootCompleted() {
        AmbienceLog.f(TAG, "onBootCompleted");
        this.mIsBootComplete = true;
    }

    public void setSensorManager(SensorManager sensorManager, Sensor sensor) {
        this.mSensorManager = sensorManager;
        if (this.mSensorManager != null) {
            if (sensor != null) {
                this.mMainSensor = sensor;
            }
            this.mProximitySensor = this.mSensorManager.getDefaultSensor(8);
            boolean hasDualLight = false;
            boolean hasSecondaryProx = false;
            try {
                hasDualLight = OplusFeatureConfigManager.getInstance().hasFeature("oplus.software.display.dual_lightsensor_support");
                hasSecondaryProx = OplusFeatureConfigManager.getInstance().hasFeature("oplus.software.display.secondary_proximity_enable");
            } catch (Throwable t) {
                AmbienceLog.e(TAG, "Failed to query OplusFeatureConfigManager: " + t);
            }
            if (hasDualLight) {
                this.mIsDualLightSensorSupport = true;
            }
            if (hasSecondaryProx) {
                if (this.mContext != null) {
                    this.mSubProximitySensor = this.mSensorManager.getDefaultSensor(SUB_INFRARED_PROXIMITY_TYPE, true);
                } else {
                    AmbienceLog.e(TAG, "set subProximity sensor fail, Context is null");
                }
                if (this.mSubProximitySensor != null) {
                    this.mIsSubProximitySupport = true;
                }
            }
            this.mMainLightSensorName = this.mMainSensor != null ? this.mMainSensor.getStringType() : "ARC";
            this.mAuxLightSensorName = this.mAuxSensor != null ? this.mAuxSensor.getStringType() : "ARC";
            this.mProximitySensorName = this.mProximitySensor != null ? this.mProximitySensor.getStringType() : "ARC";
            this.mSubProximitySensorName = this.mSubProximitySensor != null ? this.mSubProximitySensor.getStringType() : "ARC";
            this.mIsSensorManagerReady = true;
            AmbienceLog.f(TAG, "SensorManager ready, mainLight:" + this.mMainLightSensorName + " auxLight:" + this.mAuxLightSensorName + " proximity:" + this.mProximitySensorName + " subProximity:" + this.mSubProximitySensorName);
            if (this.mMainSensor != null) {
                this.mSensorDelegateHandler.post(this::oplusLambda$setSensorManager$0);
            }
            return;
        }
        AmbienceLog.e(TAG, "setSensorManager fail, SensorManager is null");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void oplusLambda$setSensorManager$0() {
        registerSensorEvent(true);
    }

    private Sensor findMainLightSensor(String name) {
        Sensor mainSensor = null;
        for (Sensor sensor : this.mSensorManager.getSensorList(-1)) {
            if (sensor.getStringType().equals(name)) {
                mainSensor = sensor;
            } else if (sensor.getStringType().equals(MAIN_RGB_LIGHT_SENSOR_NAME)) {
                mainSensor = sensor;
                this.mIsRGBMainLightSensor = true;
                AmbienceLog.i(TAG, "main light sensor change to qti.sensor.bri_light");
                break;
            }
        }
        if (mainSensor == null) {
            AmbienceLog.e(TAG, "[!!]cannot find sensor " + name);
        }
        return mainSensor;
    }

    private Sensor findAuxLightSensor(String name) {
        Sensor auxSensor = null;
        for (Sensor sensor : this.mSensorManager.getSensorList(-1)) {
            if (sensor.getStringType().equals(name)) {
                auxSensor = sensor;
                break;
            }
        }
        if (auxSensor == null) {
            AmbienceLog.e(TAG, "[!!]cannot find sensor " + name);
        }
        return auxSensor;
    }

    private class RegisterConfig {
        public final boolean mIsRegister;
        public final SensorEventListener mListener;
        public final SENSOR_TYPE mType;

        public RegisterConfig(SensorDelegate sensorDelegate, SensorEventListener listener, SENSOR_TYPE type, boolean isRegister) {
            this.mListener = listener;
            this.mType = type;
            this.mIsRegister = isRegister;
        }
    }

    public boolean registerEventCallback(SensorEventListener listener, SENSOR_TYPE type, Object caller) {
        if (caller == null || (!(caller instanceof LuxEvent) && !(caller instanceof ProximityEvent))) {
            AmbienceLog.e(TAG, "don't support other package use, please access class LuxEvent or ProximityEvent");
            return false;
        }
        if (listener == null) {
            AmbienceLog.e(TAG, "listener is null, please check code");
            return false;
        }
        RegisterConfig config = new RegisterConfig(this, listener, type, true);
        return sendRegisterMessage(config);
    }

    public boolean unregisterEventCallback(SENSOR_TYPE type, Object caller) {
        if (caller == null || (!(caller instanceof LuxEvent) && !(caller instanceof ProximityEvent))) {
            AmbienceLog.e(TAG, "don't support other package use, please access class LuxEvent or ProximityEvent");
            return false;
        }
        RegisterConfig config = new RegisterConfig(this, null, type, false);
        return sendRegisterMessage(config);
    }

    private boolean sendRegisterMessage(RegisterConfig config) {
        Message msg = this.mSensorDelegateHandler.obtainMessage(MSG_REGISTER_SENSOR_EVENT);
        msg.obj = config;
        msg.sendToTarget();
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void prepareRegisterSensorEvent(RegisterConfig config) {
        SENSOR_TYPE type = config.mType;
        if (config.mIsRegister) {
            if (this.mRegisteredSensorListenerMap.get(type).isEmpty()) {
                final SensorEventListener listener = config.mListener;
                if (listener != null) {
                    this.mRegisteredSensorListenerMap.get(type).put(new SensorEventListener() {
                        @Override // android.hardware.SensorEventListener
                        public void onSensorChanged(SensorEvent event) {
                            listener.onSensorChanged(event);
                        }

                        @Override // android.hardware.SensorEventListener
                        public void onAccuracyChanged(Sensor sensor, int accuracy) {
                            listener.onAccuracyChanged(sensor, accuracy);
                        }
                    }, true);
                } else {
                    AmbienceLog.e(TAG, "RegisterConfig listener is null, please check code");
                }
            } else {
                AmbienceLog.f(TAG, "current type " + type.name() + " has been registered, not to repeat operate");
            }
        } else if (!this.mRegisteredSensorListenerMap.get(type).isEmpty()) {
            this.mRegisteredSensorListenerMap.get(type).replaceAll(SensorDelegate::oplusLambda$prepareRegisterSensorEvent$1);
        } else {
            AmbienceLog.f(TAG, "current type " + type.name() + " not registered, not to repeat operate");
        }
        if (this.mIsSensorManagerReady) {
            registerSensorEvent(config.mIsRegister);
        } else {
            AmbienceLog.e(TAG, "prepareRegisterSensorEvent fail, the component is not ready");
        }
    }

    static /* synthetic */ Boolean oplusLambda$prepareRegisterSensorEvent$1(SensorEventListener key, Boolean value) {
        return true;
    }

    private void registerSensorEvent(final boolean isRegister) {
        if (this.mSensorManager == null) {
            AmbienceLog.e(TAG, "registerSensorEvent fail, SensorManager is null");
            return;
        }
        this.mRegisteredSensorListenerMap.forEach((obj, obj2) -> oplusLambda$registerSensorEvent$4(isRegister, (SensorDelegate.SENSOR_TYPE) obj, (ConcurrentHashMap) obj2));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void oplusLambda$registerSensorEvent$4(final boolean isRegister, final SENSOR_TYPE sensorType, ConcurrentHashMap sensorState) {
        if (!sensorState.isEmpty()) {
            sensorState.forEach((obj, obj2) -> oplusLambda$registerSensorEvent$2(isRegister, sensorType, (SensorEventListener) obj, (Boolean) obj2));
            if (sensorState.containsValue(true)) {
                if (isRegister) {
                    sensorState.replaceAll((k, v) -> oplusLambda$registerSensorEvent$3((SensorEventListener) k, (Boolean) v));
                } else {
                    sensorState.clear();
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void oplusLambda$registerSensorEvent$2(boolean isRegister, SENSOR_TYPE sensorType, SensorEventListener listener, Boolean prepare) {
        if (listener != null && prepare.booleanValue()) {
            boolean result = false;
            if (!isRegister) {
                if (sensorType == SENSOR_TYPE.AUX_LIGHT && !this.mIsDualLightSensorSupport) {
                    result = false;
                    AmbienceLog.d(TAG, "not support dual light sensor");
                } else if (sensorType == SENSOR_TYPE.SUB_PROXIMITY && !this.mIsSubProximitySupport) {
                    result = false;
                    AmbienceLog.d(TAG, "not support sub proximity sensor");
                } else {
                    this.mSensorManager.unregisterListener(listener);
                    result = true;
                    updateSensorActiveState(sensorType, false);
                }
            } else {
                switch (sensorType) {
                    case MAIN_LIGHT:
                        if (this.mMainSensor == null) {
                            AmbienceLog.e(TAG, "registerSensorEvent fail, the main sensor is null");
                        } else {
                            result = this.mSensorManager.registerListener(listener, this.mMainSensor, 3, this.mSensorDelegateHandler);
                            updateSensorActiveState(sensorType, result);
                        }
                        break;
                    case AUX_LIGHT:
                        if (!this.mIsDualLightSensorSupport || this.mAuxSensor == null) {
                            AmbienceLog.e(TAG, "registerSensorEvent fail, the aux sensor is null, dual Light support:" + this.mIsDualLightSensorSupport);
                        } else {
                            result = this.mSensorManager.registerListener(listener, this.mAuxSensor, 3, this.mSensorDelegateHandler);
                            updateSensorActiveState(sensorType, result);
                        }
                        break;
                    case PROXIMITY:
                        if (this.mProximitySensor != null) {
                            result = this.mSensorManager.registerListener(listener, this.mProximitySensor, 3, this.mSensorDelegateHandler);
                            updateSensorActiveState(sensorType, result);
                        }
                        break;
                    case SUB_PROXIMITY:
                        if (this.mSubProximitySensor != null) {
                            result = this.mSensorManager.registerListener(listener, this.mSubProximitySensor, 3, this.mSensorDelegateHandler);
                            updateSensorActiveState(sensorType, result);
                        }
                        break;
                }
            }
            AmbienceLog.f(TAG, sensorType.name() + (isRegister ? " registerListener" : " unregisterListener") + (result ? " success" : " fail"));
        }
    }

    static /* synthetic */ Boolean oplusLambda$registerSensorEvent$3(SensorEventListener key, Boolean value) {
        return false;
    }

    public boolean isRGBMainLightSensor() {
        return this.mIsRGBMainLightSensor;
    }

    public boolean isDualLightSensorSupport() {
        return this.mIsDualLightSensorSupport;
    }

    public boolean isSubProximitySupport() {
        return this.mIsSubProximitySupport;
    }

    public String getMainLightSensorName() {
        return this.mMainLightSensorName;
    }

    public String getAuxLightSensorName() {
        return this.mAuxLightSensorName;
    }

    public String getProximitySensorName() {
        return this.mProximitySensorName;
    }

    public String getSubProximitySensorName() {
        return this.mSubProximitySensorName;
    }

    private void updateSensorActiveState(SENSOR_TYPE sensorType, boolean isActive) {
        switch (sensorType) {
            case MAIN_LIGHT:
                this.mIsMainLightSensorActive = isActive;
                break;
            case AUX_LIGHT:
                this.mIsAuxLightSensorActive = isActive;
                break;
            case PROXIMITY:
                this.mIsProximitySensorActive = isActive;
                break;
            case SUB_PROXIMITY:
                this.mIsSubProximitySensorActive = isActive;
                break;
        }
        AmbienceLog.d(TAG, "updateSensorActiveState: " + sensorType + " " + isActive);
    }

    private class SensorDelegateHandler extends Handler {
        public SensorDelegateHandler(Looper looper) {
            super(looper);
            AmbienceLog.f(SensorDelegate.TAG, "SensorDelegateHandler init");
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SensorDelegate.MSG_REGISTER_SENSOR_EVENT /* 5468 */:
                    SensorDelegate.this.prepareRegisterSensorEvent((RegisterConfig) msg.obj);
                    break;
            }
        }
    }
}
