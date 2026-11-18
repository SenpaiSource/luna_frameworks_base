package com.android.server.display.feature.ambience.events.event;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.OplusPropertyList;
import com.android.server.display.config.global.OplusDisplayGlobalConfig;
import com.android.server.display.feature.ambience.events.delegate.SensorDelegate;
import com.android.server.display.feature.ambience.events.utils.EventBase;
import com.android.server.display.feature.ambience.events.utils.EventResult;
import com.android.server.display.feature.ambience.utils.AmbienceLog;
import com.android.server.display.util.OplusBrightnessUtils;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/* JADX INFO: loaded from: /run/media/sathiya/data/scratch/ambience.dex */
public class LuxEvent extends EventBase {
    private static final float[] BOOST_FLOOR_FOR_AUX;
    private static final float[] BOOST_RATION_FOR_AUX;
    private static final float COE1_THRESHOLD_FOR_AUX = 200.0f;
    private static final float COMPLX_PARAM_FOR_AUX = 4000.0f;
    private static final float[] LINEAR_RATION_FOR_AUX;
    private static final float[] LUX_AREAS;
    private static final float[] LUX_THRESH_FOR_AUX;
    private static final float[] MAIN_LUX_AREAS_FOR_AUX;
    private static final float MAX_LIMIT_FOR_AUX = 0.65f;
    private static final int MSG_DEBUG_CHANGE = 5470;
    private static final int MSG_DEBUG_FINISH = 5471;
    private static final int MSG_FIRST_INDEX = 5467;
    private static final int MSG_LIGHT_CHANGED = 5469;
    private static final int MSG_REGISTER_CALLBACK = 5468;
    private static final String TAG = "LuxEvent";
    private static final float mAccuracyOffset = 0.2f;
    private LightSensor mAuxLightSensor;
    private final SensorEventListener mAuxLuxEventListener;
    private final List<EventBase.IEventListener> mAuxLuxEventListeners;
    private Context mContext;
    private boolean mIsBootComplete;
    private boolean mIsRegistered;
    private LuxEventHandler mLuxEventHandler;
    private LightSensor mMainLightSensor;
    private final SensorEventListener mMainLuxEventListener;
    private final List<EventBase.IEventListener> mMainLuxEventListeners;
    private static final boolean isUnderScreenLight = !OplusPropertyList.SCREEN_UNDERLIGHTSENSOR_REGION.equals("0");
    private static final float[] LUX_AREAS_ADJ = {SensorDelegate.DEFAULT_PROXIMITY_NEAR_CHECK_VALUE, SensorDelegate.DEFAULT_PROXIMITY_NEAR_CHECK_VALUE, SensorDelegate.DEFAULT_PROXIMITY_NEAR_CHECK_VALUE, 0.3f, 0.043f, 0.94f, 1.08f};
    private static final float[] HIGH_PRECISION_SENSOR_LUX_AREAS = {SensorDelegate.DEFAULT_PROXIMITY_NEAR_CHECK_VALUE, 4.0f, 10.0f, 30.0f, 500.0f, 1000.0f, 7500.0f};
    private static final float[] UNDER_SCREEN_OPTICS_SENSOR_LUX_AREAS = {SensorDelegate.DEFAULT_PROXIMITY_NEAR_CHECK_VALUE, 4.5f, 10.0f, 30.0f, 500.0f, 1000.0f, 7500.0f};

    public enum SENSOR_TYPE {
        MAIN_LIGHT,
        AUX_LIGHT
    }

    static {
        LUX_AREAS = isUnderScreenLight ? UNDER_SCREEN_OPTICS_SENSOR_LUX_AREAS : HIGH_PRECISION_SENSOR_LUX_AREAS;
        MAIN_LUX_AREAS_FOR_AUX = new float[]{4.0f, 10.0f, 30.0f, 60.0f, 95.0f};
        LUX_THRESH_FOR_AUX = new float[]{SensorDelegate.DEFAULT_PROXIMITY_NEAR_CHECK_VALUE, 4.0f, 10.0f, 30.0f, 60.0f};
        LINEAR_RATION_FOR_AUX = new float[]{0.25f, 0.25f, 0.25f, 0.25f, 0.3f};
        BOOST_RATION_FOR_AUX = new float[]{16.0f, 16.0f, 32.0f, 64.0f, 128.0f};
        BOOST_FLOOR_FOR_AUX = new float[]{4.0f, 4.0f, 30.0f, 60.0f, 95.0f};
    }

    private static class LuxEventHolder {
        private static final LuxEvent INSTANCE = new LuxEvent();

        private LuxEventHolder() {
        }
    }

    public static final LuxEvent getInstance() {
        return LuxEventHolder.INSTANCE;
    }

    private LuxEvent() {
        super(TAG);
        this.mMainLuxEventListeners = new CopyOnWriteArrayList();
        this.mAuxLuxEventListeners = new CopyOnWriteArrayList();
        this.mIsRegistered = false;
        this.mIsBootComplete = false;
        this.mContext = null;
        this.mLuxEventHandler = null;
        this.mMainLightSensor = null;
        this.mAuxLightSensor = null;
        this.mMainLuxEventListener = new SensorEventListener() { // from class: com.android.server.display.feature.ambience.events.event.LuxEvent.1
            @Override // android.hardware.SensorEventListener
            public void onSensorChanged(SensorEvent event) {
                if (event != null && !LuxEvent.this.mIsArcDebugMode) {
                    Message msg = LuxEvent.this.mLuxEventHandler.obtainMessage(LuxEvent.MSG_LIGHT_CHANGED);
                    msg.obj = new SensorEvent(event.sensor, event.accuracy, event.timestamp, event.values);
                    msg.sendToTarget();
                }
            }

            @Override // android.hardware.SensorEventListener
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };
        this.mAuxLuxEventListener = new SensorEventListener() { // from class: com.android.server.display.feature.ambience.events.event.LuxEvent.2
            @Override // android.hardware.SensorEventListener
            public void onSensorChanged(SensorEvent event) {
                if (event != null && !LuxEvent.this.mIsArcDebugMode) {
                    Message msg = LuxEvent.this.mLuxEventHandler.obtainMessage(LuxEvent.MSG_LIGHT_CHANGED);
                    msg.obj = new SensorEvent(event.sensor, event.accuracy, event.timestamp, event.values);
                    msg.sendToTarget();
                }
            }

            @Override // android.hardware.SensorEventListener
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };
        this.mMainLightSensor = new LightSensor(SENSOR_TYPE.MAIN_LIGHT, this.mMainLuxEventListener);
        this.mAuxLightSensor = new LightSensor(SENSOR_TYPE.AUX_LIGHT, this.mAuxLuxEventListener);
    }

    public void init(Context context, Handler handler) {
        AmbienceLog.f(TAG, "LuxEvent init");
        this.mContext = context;
        this.mLuxEventHandler = new LuxEventHandler(handler.getLooper());
    }

    @Override // com.android.server.display.feature.ambience.events.utils.EventBase
    public void onBootCompleted() {
        AmbienceLog.f(TAG, "onBootCompleted");
        this.mIsBootComplete = true;
    }

    @Override // com.android.server.display.feature.ambience.events.utils.EventBase
    public int register(EventBase.IEventListener listener) {
        return register(SENSOR_TYPE.MAIN_LIGHT, listener) | register(SENSOR_TYPE.AUX_LIGHT, listener);
    }

    public int register(SENSOR_TYPE type, EventBase.IEventListener listener) {
        return updateLightSensorListener(type, listener, true) | updateLightSensorEnabled();
    }

    @Override // com.android.server.display.feature.ambience.events.utils.EventBase
    public int unregister(EventBase.IEventListener listener) {
        return unregister(SENSOR_TYPE.MAIN_LIGHT, listener) | unregister(SENSOR_TYPE.AUX_LIGHT, listener);
    }

    public int unregister(SENSOR_TYPE type, EventBase.IEventListener listener) {
        return updateLightSensorListener(type, listener, false) | updateLightSensorEnabled();
    }

    public boolean isUnderScreenLight() {
        return isUnderScreenLight;
    }

    private int updateLightSensorListener(SENSOR_TYPE type, EventBase.IEventListener listener, boolean isRegister) {
        if (type == null || listener == null) {
            return 2;
        }
        if (type.ordinal() == SENSOR_TYPE.MAIN_LIGHT.ordinal()) {
            if (isRegister) {
                if (this.mMainLuxEventListeners.contains(listener)) {
                    return 2;
                }
                int result = this.mMainLuxEventListeners.add(listener) ? 1 : 2;
                AmbienceLog.d(TAG, "add main light Listener:" + listener);
                return result;
            }
            if (!this.mMainLuxEventListeners.contains(listener)) {
                return 2;
            }
            int result2 = this.mMainLuxEventListeners.remove(listener) ? 1 : 2;
            AmbienceLog.d(TAG, "remove main light Listener:" + listener);
            return result2;
        }
        if (type.ordinal() != SENSOR_TYPE.AUX_LIGHT.ordinal()) {
            return 2;
        }
        if (isRegister) {
            if (this.mAuxLuxEventListeners.contains(listener)) {
                return 2;
            }
            int result3 = this.mAuxLuxEventListeners.add(listener) ? 1 : 2;
            AmbienceLog.d(TAG, "add aux light Listener:" + listener);
            return result3;
        }
        if (!this.mAuxLuxEventListeners.contains(listener)) {
            return 2;
        }
        int result4 = this.mAuxLuxEventListeners.remove(listener) ? 1 : 2;
        AmbienceLog.d(TAG, "remove aux light Listener:" + listener);
        return result4;
    }

    private int updateLightSensorEnabled() {
        int result = 1;
        boolean mainLightPrepare = !this.mMainLuxEventListeners.isEmpty();
        boolean auxLightPrepare = !this.mAuxLuxEventListeners.isEmpty();
        if (Boolean.logicalXor(this.mMainLightSensor.isRegistered(), mainLightPrepare)) {
            AmbienceLog.f(TAG, "main light sensor enable:" + mainLightPrepare);
            if (mainLightPrepare) {
                result = this.mMainLightSensor.register();
            } else {
                result = this.mMainLightSensor.unregister();
            }
        }
        if (Boolean.logicalXor(this.mAuxLightSensor.isRegistered(), auxLightPrepare)) {
            AmbienceLog.f(TAG, "aux light sensor enable:" + auxLightPrepare);
            if (auxLightPrepare) {
                int result2 = this.mAuxLightSensor.register();
                return result2;
            }
            int result3 = this.mAuxLightSensor.unregister();
            return result3;
        }
        return result;
    }

    @Override // com.android.server.display.feature.ambience.events.utils.EventBase
    public void sendRegisterCallback() {
    }

    @Override // com.android.server.display.feature.ambience.events.utils.EventBase
    public boolean sendEventCallBack(EventResult result) {
        if (result == null) {
            return false;
        }
        List<EventBase.IEventListener> listeners = this.mMainLuxEventListeners;
        if (result.getCurrentEventId() == LuxEventResult.LIGHT.AUX_LUX.ordinal()) {
            listeners = this.mAuxLuxEventListeners;
        }
        for (EventBase.IEventListener listener : listeners) {
            if (listener != null) {
                listener.onEventChange(result);
            }
        }
        return true;
    }

    public boolean isRegistered() {
        return this.mMainLightSensor.isRegistered() || this.mAuxLightSensor.isRegistered();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public LuxEventResult ParseSensorEventResult(SensorEvent event) {
        LuxEventResult result = (LuxEventResult) this.mEventResult;
        if (event != null) {
            LuxEventResult.LIGHT light = LuxEventResult.LIGHT.MAIN_LUX;
            if (SensorDelegate.getInstance().getAuxLightSensorName().equals(event.sensor.getStringType())) {
                light = LuxEventResult.LIGHT.AUX_LUX;
            }
            LuxEventResult result2 = new LuxEventResult(light.name(), light.ordinal());
            result2.obj = event;
            return result2;
        }
        return result;
    }

    public LuxEventResult createSimulateLuxEventResult(float lux, LuxEventResult.LIGHT type) {
        return new LuxEventResult(type.name(), type.ordinal(), lux, this.mIsArcDebugMode);
    }

    public static final class LuxEventResult extends EventResult {
        public static final float INVALID_VALUE = -1.0f;
        private static volatile float sMainLux = -1.0f;
        private long mEventTime;
        private float mIRRatio;
        private float mLux;

        public enum LIGHT {
            MAIN_LUX,
            AUX_LUX,
            IR_RATIO,
            RGB
        }

        public LuxEventResult(String eventType, int eventId) {
            super(eventType, eventId);
            this.mLux = -1.0f;
            this.mIRRatio = -1.0f;
            this.mEventTime = TimeEvent.getSystemMillisTime();
        }

        public LuxEventResult(String eventType, int eventId, float lux, boolean isDebug) {
            super(eventType, eventId);
            this.mLux = -1.0f;
            this.mIRRatio = -1.0f;
            this.mEventTime = TimeEvent.getSystemMillisTime();
            this.mLux = lux;
            this.isDebugResult = isDebug;
        }

        private LuxEventResult(LuxEventResult result) {
            this(result.getCurrentEventType(), result.getCurrentEventId());
            this.obj = result.obj;
            this.isDebugResult = result.isDebugResult;
            this.mLux = result.getLux();
            this.mIRRatio = result.getIRRatio();
            this.mEventTime = result.getEventTime();
        }

        public static LuxEventResult copyFrom(LuxEventResult result) {
            if (result == null) {
                return null;
            }
            return new LuxEventResult(result);
        }

        public static void updateOriginalMainLux(float lux) {
            sMainLux = lux;
        }

        public static float getOriginalMainLux() {
            return sMainLux;
        }

        public float getLux() {
            if (this.mLux == -1.0f) {
                updateLux();
            }
            if (this.isDebugResult) {
                return calculateLux(this.mLux);
            }
            return this.mLux;
        }

        public float getOriginalLux() {
            if (!objectIsNull()) {
                return ((SensorEvent) this.obj).values[0];
            }
            return getLux();
        }

        public float getIRRatio() {
            if (this.mIRRatio == -1.0f && !objectIsNull()) {
                SensorEvent event = (SensorEvent) this.obj;
                this.mIRRatio = event.values[6];
            }
            return this.mIRRatio;
        }

        public long getEventTime() {
            return this.mEventTime;
        }

        private void updateLux() {
            if (!objectIsNull()) {
                SensorEvent event = (SensorEvent) this.obj;
                this.mLux = calculateLux(event.values[0]);
            }
        }

        private float calculateLux(float originLux) {
            if (isMainSensorData()) {
                updateOriginalMainLux(originLux);
                return originLux;
            }
            if (!isAuxSensorData()) {
                return -1.0f;
            }
            if (!OplusDisplayGlobalConfig.getInstance().isDualLightSensorFusionSupport()) {
                if (OplusBrightnessUtils.isRM()) {
                    float lux = calculateAuxLuxRM(originLux);
                    return lux;
                }
                if (originLux < LuxEvent.LUX_AREAS[0]) {
                    return -1.0f;
                }
                if (originLux < LuxEvent.LUX_AREAS[1]) {
                    return originLux;
                }
                if (originLux < LuxEvent.LUX_AREAS[2]) {
                    float lux2 = LuxEvent.LUX_AREAS[1];
                    return lux2;
                }
                if (originLux < LuxEvent.LUX_AREAS[3]) {
                    float lux3 = LuxEvent.LUX_AREAS[1] + (LuxEvent.LUX_AREAS_ADJ[3] * (originLux - LuxEvent.LUX_AREAS[2]));
                    return lux3;
                }
                if (originLux < LuxEvent.LUX_AREAS[4]) {
                    float lux4 = LuxEvent.LUX_AREAS[2] + (LuxEvent.LUX_AREAS_ADJ[4] * (originLux - LuxEvent.LUX_AREAS[3]));
                    return lux4;
                }
                if (originLux < LuxEvent.LUX_AREAS[5]) {
                    float lux5 = LuxEvent.LUX_AREAS[3] + (LuxEvent.LUX_AREAS_ADJ[5] * (originLux - LuxEvent.LUX_AREAS[4]));
                    return lux5;
                }
                if (originLux < LuxEvent.LUX_AREAS[6]) {
                    float lux6 = LuxEvent.LUX_AREAS[4] + (LuxEvent.LUX_AREAS_ADJ[6] * (originLux - LuxEvent.LUX_AREAS[5]));
                    return lux6;
                }
                return originLux;
            }
            float lux7 = calculateAuxLux(getOriginalMainLux(), originLux);
            return lux7;
        }

        private float calculateAuxLuxRM(float auxLux) {
            if (auxLux <= LuxEvent.LUX_AREAS[3]) {
                return auxLux;
            }
            if (auxLux < LuxEvent.LUX_AREAS[4]) {
                float resultAux = LuxEvent.LUX_AREAS[2] + (LuxEvent.LUX_AREAS_ADJ[4] * (auxLux - LuxEvent.LUX_AREAS[3]));
                return resultAux > LuxEvent.LUX_AREAS[3] ? resultAux : LuxEvent.LUX_AREAS[3];
            }
            if (auxLux < LuxEvent.LUX_AREAS[5]) {
                return LuxEvent.LUX_AREAS[3] + (LuxEvent.LUX_AREAS_ADJ[5] * (auxLux - LuxEvent.LUX_AREAS[4]));
            }
            if (auxLux < LuxEvent.LUX_AREAS[6]) {
                return LuxEvent.LUX_AREAS[4] + (LuxEvent.LUX_AREAS_ADJ[6] * (auxLux - LuxEvent.LUX_AREAS[5]));
            }
            return auxLux;
        }

        public boolean isInvalidLux() {
            return getLux() == -1.0f;
        }

        public boolean isMainSensorData() {
            return getCurrentEventId() == LIGHT.MAIN_LUX.ordinal();
        }

        public boolean isAuxSensorData() {
            return getCurrentEventId() == LIGHT.AUX_LUX.ordinal();
        }

        public float calculateAuxLux(float mainLux, float auxLux) {
            if (mainLux > LuxEvent.MAIN_LUX_AREAS_FOR_AUX[LuxEvent.MAIN_LUX_AREAS_FOR_AUX.length - 1]) {
                return auxLux > LuxEvent.COE1_THRESHOLD_FOR_AUX ? LuxEvent.MAX_LIMIT_FOR_AUX * auxLux : auxLux;
            }
            if (auxLux <= mainLux) {
                return auxLux;
            }
            int idx = 0;
            while (idx < LuxEvent.MAIN_LUX_AREAS_FOR_AUX.length && mainLux > LuxEvent.MAIN_LUX_AREAS_FOR_AUX[idx]) {
                idx++;
            }
            if (idx == LuxEvent.MAIN_LUX_AREAS_FOR_AUX.length) {
                return auxLux;
            }
            float threshold = LuxEvent.LUX_THRESH_FOR_AUX[idx];
            if (auxLux <= threshold) {
                return auxLux;
            }
            float linearComplexAux = LuxEvent.LINEAR_RATION_FOR_AUX[idx] * (auxLux - threshold);
            float exponentComplexAux = (LuxEvent.BOOST_RATION_FOR_AUX[idx] * ((float) Math.exp(auxLux / LuxEvent.COMPLX_PARAM_FOR_AUX))) + LuxEvent.BOOST_FLOOR_FOR_AUX[idx];
            return Math.min(linearComplexAux, exponentComplexAux) + mainLux;
        }

        @Override // com.android.server.display.feature.ambience.events.utils.EventResult
        public String toString() {
            StringBuffer sb = new StringBuffer(LuxEvent.TAG);
            sb.append("{");
            sb.append("event:").append(getCurrentEventType());
            sb.append(" ");
            sb.append("lux:").append(getLux());
            if (AmbienceLog.isLogOn) {
                sb.append(" ");
                sb.append("IR:").append(getIRRatio());
                sb.append(" ");
                sb.append("time:").append(getEventTime());
                sb.append(" ");
                sb.append("debug:").append(this.isDebugResult);
            }
            sb.append("}");
            return sb.toString();
        }
    }

    private class LuxEventHandler extends Handler {
        private final String TAG;

        public LuxEventHandler(Looper looper) {
            super(looper);
            this.TAG = "LuxEventHandler";
            AmbienceLog.f("LuxEventHandler", "LuxEventHandler init");
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case LuxEvent.MSG_LIGHT_CHANGED /* 5469 */:
                    LuxEvent.this.mLastEventResult = LuxEvent.this.mEventResult;
                    LuxEvent.this.mEventResult = LuxEvent.this.ParseSensorEventResult((SensorEvent) msg.obj);
                    if (!LuxEvent.this.mIsArcDebugMode) {
                        LuxEvent.this.sendEventCallBack(LuxEvent.this.mEventResult);
                    }
                    break;
                case LuxEvent.MSG_DEBUG_CHANGE /* 5470 */:
                    LuxEvent.this.mEventResult = (EventResult) msg.obj;
                    LuxEvent.this.sendEventCallBack(LuxEvent.this.mEventResult);
                    break;
                case LuxEvent.MSG_DEBUG_FINISH /* 5471 */:
                    LuxEvent.this.mIsArcDebugMode = false;
                    LuxEvent.this.sendRegisterCallback();
                    break;
            }
        }
    }

    @Override // com.android.server.display.feature.ambience.events.utils.EventBase
    public void onEventChange(EventResult result) {
        this.mIsArcDebugMode = true;
        Message msg = this.mLuxEventHandler.obtainMessage(MSG_DEBUG_CHANGE);
        msg.obj = result;
        msg.sendToTarget();
    }

    @Override // com.android.server.display.feature.ambience.events.utils.EventBase
    public void onDebugFinish() {
        AmbienceLog.d(TAG, "onDebugFinish");
        this.mLuxEventHandler.obtainMessage(MSG_DEBUG_FINISH).sendToTarget();
    }

    private class LightSensor {
        private final SensorEventListener listener;
        private boolean mIsRegistered = false;
        private final SensorDelegate.SENSOR_TYPE sensorType;

        public LightSensor(SENSOR_TYPE sensorType, SensorEventListener listener) {
            this.listener = listener;
            this.sensorType = sensorType == SENSOR_TYPE.MAIN_LIGHT ? SensorDelegate.SENSOR_TYPE.MAIN_LIGHT : SensorDelegate.SENSOR_TYPE.AUX_LIGHT;
        }

        public int register() {
            if (this.mIsRegistered) {
                return 1;
            }
            try {
                this.mIsRegistered = SensorDelegate.getInstance().registerEventCallback(this.listener, this.sensorType, LuxEvent.getInstance());
                AmbienceLog.f(LuxEvent.TAG, "register " + this.sensorType.name() + " result:" + this.mIsRegistered);
            } catch (Exception e) {
                AmbienceLog.e(LuxEvent.TAG, "Oops! Exception on register: " + this.sensorType.name() + " --- " + e.getMessage());
            }
            return this.mIsRegistered ? 1 : 4;
        }

        public int unregister() {
            if (!this.mIsRegistered) {
                return 1;
            }
            boolean isUnregisterSuccess = false;
            try {
                isUnregisterSuccess = SensorDelegate.getInstance().unregisterEventCallback(this.sensorType, LuxEvent.getInstance());
                AmbienceLog.f(LuxEvent.TAG, "unregister " + this.sensorType.name() + " result:" + this.mIsRegistered);
            } catch (Exception e) {
                AmbienceLog.e(LuxEvent.TAG, "Oops! Exception on register: " + this.sensorType.name() + " --- " + e.getMessage());
            }
            this.mIsRegistered = !isUnregisterSuccess;
            LuxEvent.this.resetResultData();
            return isUnregisterSuccess ? 1 : 16;
        }

        public boolean isRegistered() {
            return this.mIsRegistered;
        }
    }
}
