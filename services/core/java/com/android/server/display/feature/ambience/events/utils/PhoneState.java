package com.android.server.display.feature.ambience.events.utils;

import com.android.server.display.feature.ambience.events.event.DarkModeEvent;
import com.android.server.display.feature.ambience.events.event.InOutDoorEvent;
import com.android.server.display.feature.ambience.events.event.LuxEvent;
import com.android.server.display.feature.ambience.events.event.MotionEvent;
import com.android.server.display.feature.ambience.events.event.NetworkEvent;
import com.android.server.display.feature.ambience.events.event.ProximityEvent;
import com.android.server.display.feature.ambience.events.event.TimeEvent;
import java.util.HashMap;

/* JADX INFO: loaded from: /run/media/sathiya/data/scratch/ambience.dex */
public class PhoneState {
    private static final long DEFAULT_AUXILIARY_EVENT_CHANGE_CALLBACK_MAX_COUNT = 2;
    private final String TAG;
    private int mAuxiliaryEventChangeCount;
    private long mCallbackTime;
    private DarkModeEvent.DarkModeEventResult mDarkModeEvent;
    private InOutDoorEvent.InOutDoorEventResult mInOutDoorEvent;
    private LuxEvent.LuxEventResult mLuxEvent;
    private MotionEvent.MotionEventResult mMotionEvent;
    private NetworkEvent.NetworkEventResult mNetworkEvent;
    private ProximityEvent.ProximityEventResult mProximityEvent;
    private TimeEvent.TimeEventResult mTimeEvent;

    public PhoneState() {
        this.TAG = "PhoneState";
        this.mCallbackTime = 0L;
        this.mAuxiliaryEventChangeCount = 0;
        this.mDarkModeEvent = null;
        this.mInOutDoorEvent = null;
        this.mLuxEvent = null;
        this.mMotionEvent = null;
        this.mNetworkEvent = null;
        this.mProximityEvent = null;
        this.mTimeEvent = null;
    }

    public PhoneState(PhoneState state) {
        this.TAG = "PhoneState";
        this.mCallbackTime = 0L;
        this.mAuxiliaryEventChangeCount = 0;
        this.mDarkModeEvent = null;
        this.mInOutDoorEvent = null;
        this.mLuxEvent = null;
        this.mMotionEvent = null;
        this.mNetworkEvent = null;
        this.mProximityEvent = null;
        this.mTimeEvent = null;
        this.mCallbackTime = TimeEvent.getSystemMillisTime();
        this.mAuxiliaryEventChangeCount = state.mAuxiliaryEventChangeCount;
        this.mDarkModeEvent = DarkModeEvent.DarkModeEventResult.copyFrom(state.mDarkModeEvent);
        this.mInOutDoorEvent = InOutDoorEvent.InOutDoorEventResult.copyFrom(state.mInOutDoorEvent);
        this.mLuxEvent = LuxEvent.LuxEventResult.copyFrom(state.mLuxEvent);
        this.mMotionEvent = MotionEvent.MotionEventResult.copyFrom(state.mMotionEvent);
        this.mNetworkEvent = NetworkEvent.NetworkEventResult.copyFrom(state.mNetworkEvent);
        this.mProximityEvent = ProximityEvent.ProximityEventResult.copyFrom(state.mProximityEvent);
        this.mTimeEvent = TimeEvent.TimeEventResult.copyFrom(state.mTimeEvent);
    }

    public boolean setEvent(EventResult event, Class clazz) {
        if (clazz.equals(LuxEvent.class)) {
            this.mLuxEvent = (LuxEvent.LuxEventResult) event;
        } else if (clazz.equals(TimeEvent.class)) {
            this.mTimeEvent = (TimeEvent.TimeEventResult) event;
            this.mAuxiliaryEventChangeCount++;
        } else if (clazz.equals(ProximityEvent.class)) {
            this.mProximityEvent = (ProximityEvent.ProximityEventResult) event;
            this.mAuxiliaryEventChangeCount++;
        } else if (clazz.equals(MotionEvent.class)) {
            this.mMotionEvent = (MotionEvent.MotionEventResult) event;
            this.mAuxiliaryEventChangeCount++;
        } else if (clazz.equals(NetworkEvent.class)) {
            this.mNetworkEvent = (NetworkEvent.NetworkEventResult) event;
            this.mAuxiliaryEventChangeCount++;
        } else if (clazz.equals(InOutDoorEvent.class)) {
            this.mInOutDoorEvent = (InOutDoorEvent.InOutDoorEventResult) event;
            this.mAuxiliaryEventChangeCount++;
        } else if (clazz.equals(DarkModeEvent.class)) {
            this.mDarkModeEvent = (DarkModeEvent.DarkModeEventResult) event;
            this.mAuxiliaryEventChangeCount++;
        }
        return isNeedUpdateState();
    }

    public boolean setDarkModeEvent(DarkModeEvent.DarkModeEventResult event) {
        this.mDarkModeEvent = event;
        this.mAuxiliaryEventChangeCount++;
        return isNeedUpdateState();
    }

    public DarkModeEvent.DarkModeEventResult getDarkModeEvent() {
        return this.mDarkModeEvent;
    }

    public boolean setInOutDoorEvent(InOutDoorEvent.InOutDoorEventResult event) {
        this.mInOutDoorEvent = event;
        this.mAuxiliaryEventChangeCount++;
        return isNeedUpdateState();
    }

    public InOutDoorEvent.InOutDoorEventResult getInOutDoorEvent() {
        return this.mInOutDoorEvent;
    }

    public boolean setLuxEvent(LuxEvent.LuxEventResult event) {
        this.mLuxEvent = event;
        return isNeedUpdateState();
    }

    public LuxEvent.LuxEventResult getLuxEvent() {
        return this.mLuxEvent;
    }

    public boolean setMotionEvent(MotionEvent.MotionEventResult event) {
        this.mMotionEvent = event;
        this.mAuxiliaryEventChangeCount++;
        return isNeedUpdateState();
    }

    public MotionEvent.MotionEventResult getMotionEvent() {
        return this.mMotionEvent;
    }

    public boolean setNetworkEvent(NetworkEvent.NetworkEventResult event) {
        this.mNetworkEvent = event;
        this.mAuxiliaryEventChangeCount++;
        return isNeedUpdateState();
    }

    public NetworkEvent.NetworkEventResult getNetworkEvent() {
        return this.mNetworkEvent;
    }

    public boolean setProximityEvent(ProximityEvent.ProximityEventResult event) {
        this.mProximityEvent = event;
        this.mAuxiliaryEventChangeCount++;
        return isNeedUpdateState();
    }

    public ProximityEvent.ProximityEventResult getProximityEvent() {
        return this.mProximityEvent;
    }

    public boolean setTimeEvent(TimeEvent.TimeEventResult event) {
        this.mTimeEvent = event;
        this.mAuxiliaryEventChangeCount++;
        return isNeedUpdateState();
    }

    public TimeEvent.TimeEventResult getTimeEvent() {
        return this.mTimeEvent;
    }

    public boolean isCompleteData() {
        return (this.mDarkModeEvent == null || this.mInOutDoorEvent == null || this.mLuxEvent == null || this.mMotionEvent == null || this.mNetworkEvent == null || this.mProximityEvent == null || this.mTimeEvent == null) ? false : true;
    }

    public boolean isImportantDataExist() {
        return this.mLuxEvent != null;
    }

    public boolean isNeedUpdateState() {
        return isImportantDataExist() || ((long) this.mAuxiliaryEventChangeCount) >= DEFAULT_AUXILIARY_EVENT_CHANGE_CALLBACK_MAX_COUNT;
    }

    public boolean isAuxiliaryEventChanged() {
        return this.mAuxiliaryEventChangeCount > 0;
    }

    public boolean isOnlyLuxChanged() {
        return !isAuxiliaryEventChanged() && isImportantDataExist();
    }

    public void clearData() {
        this.mDarkModeEvent = null;
        this.mInOutDoorEvent = null;
        this.mLuxEvent = null;
        this.mMotionEvent = null;
        this.mNetworkEvent = null;
        this.mProximityEvent = null;
        this.mTimeEvent = null;
    }

    public void callbackEnd() {
        this.mLuxEvent = null;
        this.mAuxiliaryEventChangeCount = 0;
    }

    public HashMap<String, String> getPhoneState() {
        HashMap<String, String> result = new HashMap<>();
        if (this.mTimeEvent != null && !this.mTimeEvent.objectIsNull()) {
            result.put("Time", this.mTimeEvent.getNormalizationTimeFormat());
        }
        if (this.mLuxEvent != null && (!this.mLuxEvent.objectIsNull() || this.mLuxEvent.isDebugResult())) {
            result.put("Light", this.mLuxEvent.getCurrentEventType() + "[" + this.mLuxEvent.getLux() + "]");
        }
        if (this.mProximityEvent != null) {
            result.put("Proximity", this.mProximityEvent.getCurrentEventType());
        }
        if (this.mMotionEvent != null) {
            result.put("Motion", this.mMotionEvent.getCurrentEventType());
        }
        if (this.mInOutDoorEvent != null) {
            result.put("InOutDoor", this.mInOutDoorEvent.getCurrentEventType());
        }
        if (this.mDarkModeEvent != null) {
            result.put("DarkMode", this.mDarkModeEvent.getCurrentEventType());
        }
        if (this.mNetworkEvent != null) {
            result.put("Network", this.mNetworkEvent.getCurrentEventType());
        }
        return result;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("PhoneState");
        sb.append("{");
        if (this.mTimeEvent != null && !this.mTimeEvent.objectIsNull()) {
            sb.append(this.mTimeEvent).append("-");
            sb.append(this.mTimeEvent.getNormalizationTimeFormat()).append(", ");
        }
        if (this.mLuxEvent != null && (!this.mLuxEvent.objectIsNull() || this.mLuxEvent.isDebugResult())) {
            sb.append(this.mLuxEvent).append(", ");
        }
        if (this.mProximityEvent != null) {
            sb.append(this.mProximityEvent).append(", ");
        }
        if (this.mMotionEvent != null) {
            sb.append(this.mMotionEvent).append(", ");
        }
        if (this.mInOutDoorEvent != null) {
            sb.append(this.mInOutDoorEvent).append(", ");
        }
        if (this.mDarkModeEvent != null) {
            sb.append(this.mDarkModeEvent).append(", ");
        }
        if (this.mNetworkEvent != null) {
            sb.append(this.mNetworkEvent);
        }
        sb.append("}");
        return sb.toString();
    }
}
