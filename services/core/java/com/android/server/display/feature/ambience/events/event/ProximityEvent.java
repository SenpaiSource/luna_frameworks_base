package com.android.server.display.feature.ambience.events.event;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import com.android.server.display.feature.ambience.events.delegate.DeepThinkerDelegate;
import com.android.server.display.feature.ambience.events.delegate.SensorDelegate;
import com.android.server.display.feature.ambience.events.utils.EventBase;
import com.android.server.display.feature.ambience.events.utils.EventResult;
import com.android.server.display.feature.ambience.utils.AmbienceLog;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/* JADX INFO: loaded from: /run/media/sathiya/data/scratch/ambience.dex */
public class ProximityEvent extends EventBase {
    private static final float INVALID_VALUE = -1.0f;
    private static final int MSG_DEBUG_CHANGE = 5470;
    private static final int MSG_DEBUG_FINISH = 5471;
    private static final int MSG_FIRST_INDEX = 5467;
    private static final int MSG_PROXIMITY_CHANGED = 5469;
    private static final int MSG_REGISTER_CALLBACK = 5468;
    private static final long PROXIMITY_DEBOUNCE_CHECK_TIMEOUT = 1000;
    private static final String TAG = "ProximityEvent";
    private Context mContext;
    private boolean mIsBootComplete;
    private ProximityEventHandler mProximityEventHandler;
    private final SensorEventListener mProximityEventListener;
    private final List<EventBase.IEventListener> mProximityEventListeners;
    private ProximitySensor mProximitySensor;
    private final SensorEventListener mSubProximityEventListener;
    private final List<EventBase.IEventListener> mSubProximityEventListeners;
    private ProximitySensor mSubProximitySensor;

    public enum SENSOR_TYPE {
        PROXIMITY,
        SUB_PROXIMITY
    }

    private static class ProximityEventHolder {
        private static final ProximityEvent INSTANCE = new ProximityEvent();

        private ProximityEventHolder() {
        }
    }

    public static final ProximityEvent getInstance() {
        return ProximityEventHolder.INSTANCE;
    }

    private ProximityEvent() {
        super(TAG);
        this.mProximityEventListeners = new CopyOnWriteArrayList();
        this.mSubProximityEventListeners = new CopyOnWriteArrayList();
        this.mIsBootComplete = false;
        this.mContext = null;
        this.mProximityEventHandler = null;
        this.mProximitySensor = null;
        this.mSubProximitySensor = null;
        this.mProximityEventListener = new SensorEventListener() { // from class: com.android.server.display.feature.ambience.events.event.ProximityEvent.1
            @Override // android.hardware.SensorEventListener
            public void onSensorChanged(SensorEvent event) {
                if (event != null) {
                    Message msg = ProximityEvent.this.mProximityEventHandler.obtainMessage(ProximityEvent.MSG_PROXIMITY_CHANGED);
                    msg.obj = ProximityEvent.this.ParseSensorEventResult(event);
                    ProximityEvent.this.mProximityEventHandler.removeMessages(ProximityEvent.MSG_PROXIMITY_CHANGED);
                    Long delayTime = 0L;
                    if (ProximityEventResult.PROXIMITY.FAR.ordinal() == ((ProximityEventResult) msg.obj).getCurrentEventId()) {
                        delayTime = 1000L;
                    }
                    ProximityEvent.this.mProximityEventHandler.sendMessageDelayed(msg, delayTime.longValue());
                }
            }

            @Override // android.hardware.SensorEventListener
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };
        this.mSubProximityEventListener = new SensorEventListener() { // from class: com.android.server.display.feature.ambience.events.event.ProximityEvent.2
            @Override // android.hardware.SensorEventListener
            public void onSensorChanged(SensorEvent event) {
                if (event != null) {
                    Message msg = ProximityEvent.this.mProximityEventHandler.obtainMessage(ProximityEvent.MSG_PROXIMITY_CHANGED);
                    msg.obj = ProximityEvent.this.ParseSensorEventResult(event);
                    ProximityEvent.this.mProximityEventHandler.removeMessages(ProximityEvent.MSG_PROXIMITY_CHANGED);
                    Long delayTime = 0L;
                    if (ProximityEventResult.PROXIMITY.FAR.ordinal() == ((ProximityEventResult) msg.obj).getCurrentEventId()) {
                        delayTime = 1000L;
                    }
                    ProximityEvent.this.mProximityEventHandler.sendMessageDelayed(msg, delayTime.longValue());
                }
            }

            @Override // android.hardware.SensorEventListener
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };
        this.mProximitySensor = new ProximitySensor(SENSOR_TYPE.PROXIMITY, this.mProximityEventListener);
        this.mSubProximitySensor = new ProximitySensor(SENSOR_TYPE.SUB_PROXIMITY, this.mSubProximityEventListener);
    }

    public void init(Context context, Handler handler) {
        AmbienceLog.f(TAG, "ProximityEvent init");
        this.mContext = context;
        this.mProximityEventHandler = new ProximityEventHandler(handler.getLooper());
    }

    @Override // com.android.server.display.feature.ambience.events.utils.EventBase
    public void onBootCompleted() {
        AmbienceLog.f(TAG, "onBootCompleted");
        this.mIsBootComplete = true;
    }

    @Override // com.android.server.display.feature.ambience.events.utils.EventBase
    public int register(EventBase.IEventListener listener) {
        return register(SENSOR_TYPE.PROXIMITY, listener);
    }

    public int register(SENSOR_TYPE type, EventBase.IEventListener listener) {
        return updateLightSensorListener(type, listener, true) | updateLightSensorEnabled();
    }

    @Override // com.android.server.display.feature.ambience.events.utils.EventBase
    public int unregister(EventBase.IEventListener listener) {
        return unregister(SENSOR_TYPE.PROXIMITY, listener) | unregister(SENSOR_TYPE.SUB_PROXIMITY, listener);
    }

    public int unregister(SENSOR_TYPE type, EventBase.IEventListener listener) {
        return updateLightSensorListener(type, listener, false) | updateLightSensorEnabled();
    }

    private int updateLightSensorListener(SENSOR_TYPE type, EventBase.IEventListener listener, boolean isRegister) {
        if (type == null || listener == null) {
            return 2;
        }
        if (type.ordinal() == SENSOR_TYPE.PROXIMITY.ordinal()) {
            if (isRegister) {
                if (this.mProximityEventListeners.contains(listener)) {
                    return 2;
                }
                int result = this.mProximityEventListeners.add(listener) ? 1 : 2;
                AmbienceLog.d(TAG, "add proximity Listener:" + listener);
                return result;
            }
            if (!this.mProximityEventListeners.contains(listener)) {
                return 2;
            }
            int result2 = this.mProximityEventListeners.remove(listener) ? 1 : 2;
            AmbienceLog.d(TAG, "remove proximity Listener:" + listener);
            return result2;
        }
        if (type.ordinal() != SENSOR_TYPE.SUB_PROXIMITY.ordinal()) {
            return 2;
        }
        if (isRegister) {
            if (this.mSubProximityEventListeners.contains(listener)) {
                return 2;
            }
            int result3 = this.mSubProximityEventListeners.add(listener) ? 1 : 2;
            AmbienceLog.d(TAG, "add sub-proximity Listener:" + listener);
            return result3;
        }
        if (!this.mSubProximityEventListeners.contains(listener)) {
            return 2;
        }
        int result4 = this.mSubProximityEventListeners.remove(listener) ? 1 : 2;
        AmbienceLog.d(TAG, "remove sub-proximity Listener:" + listener);
        return result4;
    }

    private int updateLightSensorEnabled() {
        int result = 1;
        boolean proximityPrepare = !this.mProximityEventListeners.isEmpty();
        boolean subProximityPrepare = !this.mSubProximityEventListeners.isEmpty();
        if (Boolean.logicalXor(this.mProximitySensor.isRegistered(), proximityPrepare)) {
            AmbienceLog.f(TAG, "proximity sensor enable:" + proximityPrepare);
            if (proximityPrepare) {
                result = this.mProximitySensor.register();
            } else {
                result = this.mProximitySensor.unregister();
            }
        }
        if (Boolean.logicalXor(this.mSubProximitySensor.isRegistered(), subProximityPrepare)) {
            AmbienceLog.f(TAG, "sub-proximity sensor enable:" + subProximityPrepare);
            if (subProximityPrepare) {
                int result2 = this.mSubProximitySensor.register();
                return result2;
            }
            int result3 = this.mSubProximitySensor.unregister();
            return result3;
        }
        return result;
    }

    @Override // com.android.server.display.feature.ambience.events.utils.EventBase
    public void sendRegisterCallback() {
        if ((this.mEventResult == null || !this.mProximitySensor.isRegistered()) && !this.mIsArcDebugMode) {
            this.mEventResult = new ProximityEventResult(ProximityEventResult.PROXIMITY.FAR.name(), ProximityEventResult.PROXIMITY.FAR.ordinal());
        }
        this.mProximityEventHandler.obtainMessage(MSG_REGISTER_CALLBACK).sendToTarget();
    }

    @Override // com.android.server.display.feature.ambience.events.utils.EventBase
    public boolean sendEventCallBack(EventResult result) {
        if (result != null) {
            ProximityEventResult proxResult = (ProximityEventResult) result;
            List<EventBase.IEventListener> listeners = this.mProximityEventListeners;
            if (proxResult.isSubProximityData()) {
                listeners = this.mSubProximityEventListeners;
            }
            for (EventBase.IEventListener listener : listeners) {
                if (listener != null) {
                    listener.onEventChange(proxResult);
                }
            }
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public ProximityEventResult ParseSensorEventResult(SensorEvent event) {
        ProximityEventResult.PROXIMITY proximity;
        ProximityEventResult result = (ProximityEventResult) this.mEventResult;
        if (event != null) {
            if (event.values[0] == SensorDelegate.DEFAULT_PROXIMITY_NEAR_CHECK_VALUE) {
                proximity = ProximityEventResult.PROXIMITY.NEAR;
            } else {
                proximity = ProximityEventResult.PROXIMITY.FAR;
            }
            ProximityEventResult result2 = new ProximityEventResult(proximity.name(), proximity.ordinal());
            result2.obj = event;
            return result2;
        }
        return result;
    }

    public ProximityEventResult createSimulateProximityEventResult(ProximityEventResult.PROXIMITY type) {
        return new ProximityEventResult(type.name(), type.ordinal(), this.mIsArcDebugMode);
    }

    public static class ProximityEventResult extends EventResult {

        public enum PROXIMITY {
            NEAR,
            FAR
        }

        public ProximityEventResult(String eventType, int eventId) {
            super(eventType, eventId);
        }

        public ProximityEventResult(String eventType, int eventId, boolean isDebug) {
            super(eventType, eventId);
            this.isDebugResult = isDebug;
        }

        private ProximityEventResult(ProximityEventResult result) {
            this(result.getCurrentEventType(), result.getCurrentEventId());
            this.obj = result.obj;
            this.isDebugResult = result.isDebugResult;
        }

        public static ProximityEventResult copyFrom(ProximityEventResult result) {
            if (result == null) {
                return null;
            }
            return new ProximityEventResult(result);
        }

        public String getProximityName() {
            if (objectIsNull()) {
                return DeepThinkerDelegate.InOutDoorImpl.Constants.EMPTY;
            }
            SensorEvent event = (SensorEvent) this.obj;
            String name = event.sensor.getStringType();
            return name;
        }

        public boolean isMainProximityData() {
            return getProximityName().equals(SensorDelegate.getInstance().getProximitySensorName());
        }

        public boolean isSubProximityData() {
            return getProximityName().equals(SensorDelegate.getInstance().getSubProximitySensorName());
        }

        public boolean isNear() {
            return getCurrentEventId() == PROXIMITY.NEAR.ordinal();
        }

        @Override // com.android.server.display.feature.ambience.events.utils.EventResult
        public String toString() {
            StringBuffer sb = new StringBuffer(ProximityEvent.TAG);
            sb.append("{");
            sb.append("event:").append(getCurrentEventType());
            sb.append(" ");
            sb.append("id:").append(getCurrentEventId());
            sb.append("}");
            return sb.toString();
        }
    }

    private class ProximityEventHandler extends Handler {
        private final String TAG;

        public ProximityEventHandler(Looper looper) {
            super(looper);
            this.TAG = "ProximityEventHandler";
            AmbienceLog.f("ProximityEventHandler", "ProximityEventHandler init");
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case ProximityEvent.MSG_REGISTER_CALLBACK /* 5468 */:
                    if (ProximityEvent.this.mEventResult != null) {
                        AmbienceLog.d("ProximityEventHandler", "MSG_REGISTER_CALLBACK, event = " + ProximityEvent.this.mEventResult);
                        ProximityEvent.this.sendEventCallBack(ProximityEvent.this.mEventResult);
                    }
                    break;
                case ProximityEvent.MSG_PROXIMITY_CHANGED /* 5469 */:
                    ProximityEventResult proximity = (ProximityEventResult) msg.obj;
                    if ((ProximityEvent.this.mEventResult != null && ProximityEvent.this.mEventResult.getCurrentEventId() != proximity.getCurrentEventId()) || ProximityEvent.this.mEventResult == null) {
                        ProximityEvent.this.mLastEventResult = ProximityEvent.this.mEventResult;
                        ProximityEvent.this.mEventResult = proximity;
                        if (!ProximityEvent.this.mIsArcDebugMode) {
                            AmbienceLog.f("ProximityEventHandler", "MSG_PROXIMITY_CHANGED, event = " + ProximityEvent.this.mEventResult);
                            ProximityEvent.this.sendEventCallBack(ProximityEvent.this.mEventResult);
                        }
                        break;
                    }
                    break;
                case ProximityEvent.MSG_DEBUG_CHANGE /* 5470 */:
                    ProximityEvent.this.mEventResult = (EventResult) msg.obj;
                    ProximityEvent.this.sendEventCallBack(ProximityEvent.this.mEventResult);
                    break;
                case ProximityEvent.MSG_DEBUG_FINISH /* 5471 */:
                    ProximityEvent.this.mIsArcDebugMode = false;
                    ProximityEvent.this.resetResultData();
                    ProximityEvent.this.sendRegisterCallback();
                    break;
            }
        }
    }

    @Override // com.android.server.display.feature.ambience.events.utils.EventBase
    public void onEventChange(EventResult result) {
        AmbienceLog.d(TAG, "onEventChange:" + result);
        this.mIsArcDebugMode = true;
        Message msg = this.mProximityEventHandler.obtainMessage(MSG_DEBUG_CHANGE);
        msg.obj = result;
        msg.sendToTarget();
    }

    @Override // com.android.server.display.feature.ambience.events.utils.EventBase
    public void onDebugFinish() {
        AmbienceLog.d(TAG, "onDebugFinish");
        this.mProximityEventHandler.obtainMessage(MSG_DEBUG_FINISH).sendToTarget();
    }

    private class ProximitySensor {
        private final SensorEventListener listener;
        private boolean mIsRegistered = false;
        private final SensorDelegate.SENSOR_TYPE sensorType;

        public ProximitySensor(SENSOR_TYPE sensorType, SensorEventListener listener) {
            this.listener = listener;
            this.sensorType = sensorType == SENSOR_TYPE.PROXIMITY ? SensorDelegate.SENSOR_TYPE.PROXIMITY : SensorDelegate.SENSOR_TYPE.SUB_PROXIMITY;
        }

        public int register() {
            ProximityEvent.this.sendRegisterCallback();
            if (this.mIsRegistered) {
                return 1;
            }
            try {
                this.mIsRegistered = SensorDelegate.getInstance().registerEventCallback(this.listener, this.sensorType, ProximityEvent.getInstance());
                AmbienceLog.f(ProximityEvent.TAG, "register " + this.sensorType.name() + " result:" + this.mIsRegistered);
            } catch (Exception e) {
                AmbienceLog.e(ProximityEvent.TAG, "Oops! Exception on register: " + this.sensorType.name() + " --- " + e.getMessage());
            }
            return this.mIsRegistered ? 1 : 4;
        }

        public int unregister() {
            if (!this.mIsRegistered) {
                return 1;
            }
            boolean isUnregisterSuccess = false;
            try {
                isUnregisterSuccess = SensorDelegate.getInstance().unregisterEventCallback(this.sensorType, ProximityEvent.getInstance());
                AmbienceLog.f(ProximityEvent.TAG, "unregister " + this.sensorType.name() + " result:" + this.mIsRegistered);
            } catch (Exception e) {
                AmbienceLog.e(ProximityEvent.TAG, "Oops! Exception on register: " + this.sensorType.name() + " --- " + e.getMessage());
            }
            this.mIsRegistered = !isUnregisterSuccess;
            ProximityEvent.this.resetResultData();
            return isUnregisterSuccess ? 1 : 16;
        }

        public boolean isRegistered() {
            return this.mIsRegistered;
        }
    }
}
