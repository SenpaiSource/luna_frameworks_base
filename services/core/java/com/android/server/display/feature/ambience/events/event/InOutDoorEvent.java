package com.android.server.display.feature.ambience.events.event;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import com.android.server.display.feature.ambience.events.delegate.DeepThinkerDelegate;
import com.android.server.display.feature.ambience.events.utils.EventBase;
import com.android.server.display.feature.ambience.events.utils.EventResult;
import com.android.server.display.feature.ambience.utils.AmbienceLog;

/* JADX INFO: loaded from: /run/media/sathiya/data/scratch/ambience.dex */
public class InOutDoorEvent extends EventBase implements DeepThinkerDelegate.DeepThinkerDelegateInOutDoorCallBack {
    private static final long DEFAULT_IN_OUT_DOOR_CHANGED_MESSAGE_DELAY_TIME = 0;
    private static final int MSG_DEBUG_CHANGE = 5470;
    private static final int MSG_DEBUG_FINISH = 5471;
    private static final int MSG_FIRST_INDEX = 5467;
    private static final int MSG_IN_OUT_DOOR_CHANGED = 5469;
    private static final int MSG_REGISTER_CALLBACK = 5468;
    private static final String TAG = "InOutDoorEvent";
    private Context mContext;
    private InOutDoorEventHandler mInOutDoorEventHandler;
    private boolean mIsBootComplete;
    private boolean mIsRegistered;

    private static class InOutDoorEventHolder {
        private static final InOutDoorEvent INSTANCE = new InOutDoorEvent();

        private InOutDoorEventHolder() {
        }
    }

    public static final InOutDoorEvent getInstance() {
        return InOutDoorEventHolder.INSTANCE;
    }

    private InOutDoorEvent() {
        super(TAG);
        this.mIsRegistered = false;
        this.mIsBootComplete = false;
        this.mContext = null;
        this.mInOutDoorEventHandler = null;
    }

    public void init(Context context, Handler handler) {
        AmbienceLog.f(TAG, "InOutDoorEvent init");
        this.mContext = context;
        this.mInOutDoorEventHandler = new InOutDoorEventHandler(handler.getLooper());
    }

    public void requestInOutDoorState() {
        DeepThinkerDelegate.getInstance().requestInOutDoorCallBack(this, this);
    }

    @Override // com.android.server.display.feature.ambience.events.utils.EventBase
    public void onBootCompleted() {
        AmbienceLog.f(TAG, "onBootCompleted");
        this.mIsBootComplete = true;
    }

    @Override // com.android.server.display.feature.ambience.events.utils.EventBase
    public int register(EventBase.IEventListener listener) {
        return addListener(listener) | register();
    }

    private int register() {
        sendRegisterCallback();
        if (this.mIsRegistered) {
            return 1;
        }
        try {
            this.mIsRegistered = DeepThinkerDelegate.getInstance().requestInOutDoorCallBack(this, this);
            AmbienceLog.f(TAG, "register inOutDoor event result:" + this.mIsRegistered);
        } catch (Exception e) {
            AmbienceLog.e(TAG, "Oops! Exception on register: " + e.getMessage());
        }
        return this.mIsRegistered ? 1 : 4;
    }

    @Override // com.android.server.display.feature.ambience.events.utils.EventBase
    public int unregister(EventBase.IEventListener listener) {
        return removeListener(listener) | unregister();
    }

    private int unregister() {
        if (!isListenerEmpty() || !this.mIsRegistered) {
            return 1;
        }
        boolean isUnregisterSuccess = false;
        try {
            isUnregisterSuccess = DeepThinkerDelegate.getInstance().requestInOutDoorCallBack(null, this);
            this.mInOutDoorEventHandler.obtainMessage(MSG_DEBUG_FINISH).sendToTarget();
            AmbienceLog.f(TAG, "unregister inOutDoor event result:" + isUnregisterSuccess);
        } catch (Exception e) {
            AmbienceLog.e(TAG, "Oops! Exception on register: " + e.getMessage());
        }
        this.mIsRegistered = !isUnregisterSuccess;
        resetResultData();
        return isUnregisterSuccess ? 1 : 16;
    }

    @Override // com.android.server.display.feature.ambience.events.utils.EventBase
    public void sendRegisterCallback() {
        if (this.mEventResult == null || !this.mIsRegistered) {
            this.mEventResult = new InOutDoorEventResult(InOutDoorEventResult.INOUT.UNCERTAIN.name(), InOutDoorEventResult.INOUT.UNCERTAIN.ordinal());
        }
        this.mInOutDoorEventHandler.obtainMessage(MSG_REGISTER_CALLBACK).sendToTarget();
    }

    @Override // com.android.server.display.feature.ambience.events.delegate.DeepThinkerDelegate.DeepThinkerDelegateInOutDoorCallBack
    public void onInOutDoorStateChanged(int inOutDoorState) {
        Message msg = this.mInOutDoorEventHandler.obtainMessage(MSG_IN_OUT_DOOR_CHANGED);
        switch (inOutDoorState) {
            case 1:
                msg.obj = InOutDoorEventResult.INOUT.IN_DOOR;
                break;
            case 2:
                msg.obj = InOutDoorEventResult.INOUT.OUT_DOOR;
                break;
            default:
                msg.obj = InOutDoorEventResult.INOUT.UNCERTAIN;
                break;
        }
        this.mInOutDoorEventHandler.removeMessages(MSG_IN_OUT_DOOR_CHANGED);
        this.mInOutDoorEventHandler.sendMessageDelayed(msg, DEFAULT_IN_OUT_DOOR_CHANGED_MESSAGE_DELAY_TIME);
    }

    public boolean isOutDoor() {
        if (this.mEventResult != null) {
            return ((InOutDoorEventResult) this.mEventResult).isOutDoor();
        }
        return false;
    }

    public static class InOutDoorEventResult extends EventResult {

        public enum INOUT {
            IN_DOOR,
            OUT_DOOR,
            UNCERTAIN
        }

        public InOutDoorEventResult(String eventType, int eventId) {
            super(eventType, eventId);
        }

        private InOutDoorEventResult(InOutDoorEventResult result) {
            this(result.getCurrentEventType(), result.getCurrentEventId());
            this.obj = result.obj;
            this.isDebugResult = result.isDebugResult;
        }

        public static InOutDoorEventResult copyFrom(InOutDoorEventResult result) {
            if (result == null) {
                return null;
            }
            return new InOutDoorEventResult(result);
        }

        public boolean isOutDoor() {
            return getCurrentEventId() == INOUT.OUT_DOOR.ordinal();
        }

        public boolean isInDoor() {
            return getCurrentEventId() == INOUT.IN_DOOR.ordinal();
        }

        @Override // com.android.server.display.feature.ambience.events.utils.EventResult
        public String toString() {
            StringBuffer sb = new StringBuffer(InOutDoorEvent.TAG);
            sb.append("{");
            sb.append("event:").append(getCurrentEventType());
            sb.append(" ");
            sb.append("id:").append(getCurrentEventId());
            sb.append("}");
            return sb.toString();
        }
    }

    private class InOutDoorEventHandler extends Handler {
        private final String TAG;

        public InOutDoorEventHandler(Looper looper) {
            super(looper);
            this.TAG = "InOutDoorEventHandler";
            AmbienceLog.f("InOutDoorEventHandler", "InOutDoorEventHandler init");
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case InOutDoorEvent.MSG_REGISTER_CALLBACK /* 5468 */:
                    if (InOutDoorEvent.this.mEventResult != null) {
                        AmbienceLog.d("InOutDoorEventHandler", "MSG_REGISTER_CALLBACK, event = " + InOutDoorEvent.this.mEventResult);
                        InOutDoorEvent.this.sendEventCallBack(InOutDoorEvent.this.mEventResult);
                    }
                    break;
                case InOutDoorEvent.MSG_IN_OUT_DOOR_CHANGED /* 5469 */:
                    InOutDoorEventResult.INOUT inOut = (InOutDoorEventResult.INOUT) msg.obj;
                    if ((InOutDoorEvent.this.mEventResult != null && InOutDoorEvent.this.mEventResult.getCurrentEventId() != inOut.ordinal()) || InOutDoorEvent.this.mEventResult == null) {
                        InOutDoorEvent.this.mLastEventResult = InOutDoorEvent.this.mEventResult;
                        InOutDoorEvent.this.mEventResult = new InOutDoorEventResult(inOut.name(), inOut.ordinal());
                        AmbienceLog.d("InOutDoorEventHandler", "MSG_IN_OUT_DOOR_CHANGED, event = " + InOutDoorEvent.this.mEventResult);
                        if (!InOutDoorEvent.this.mIsArcDebugMode) {
                            InOutDoorEvent.this.sendEventCallBack(InOutDoorEvent.this.mEventResult);
                        }
                        break;
                    }
                    break;
                case InOutDoorEvent.MSG_DEBUG_CHANGE /* 5470 */:
                    InOutDoorEvent.this.mEventResult = (EventResult) msg.obj;
                    InOutDoorEvent.this.sendEventCallBack(InOutDoorEvent.this.mEventResult);
                    break;
                case InOutDoorEvent.MSG_DEBUG_FINISH /* 5471 */:
                    InOutDoorEvent.this.mIsArcDebugMode = false;
                    InOutDoorEvent.this.requestInOutDoorState();
                    InOutDoorEvent.this.sendRegisterCallback();
                    break;
            }
        }
    }

    @Override // com.android.server.display.feature.ambience.events.utils.EventBase
    public void onEventChange(EventResult result) {
        AmbienceLog.d(TAG, "onEventChange:" + result);
        this.mIsArcDebugMode = true;
        Message msg = this.mInOutDoorEventHandler.obtainMessage(MSG_DEBUG_CHANGE);
        msg.obj = result;
        msg.sendToTarget();
    }

    @Override // com.android.server.display.feature.ambience.events.utils.EventBase
    public void onDebugFinish() {
        AmbienceLog.d(TAG, "onDebugFinish");
        this.mInOutDoorEventHandler.obtainMessage(MSG_DEBUG_FINISH).sendToTarget();
    }
}
