package com.android.server.display.feature.ambience.events.event;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import com.android.server.display.feature.ambience.events.delegate.DeepThinkerDelegate;
import com.android.server.display.feature.ambience.events.utils.EventBase;
import com.android.server.display.feature.ambience.events.utils.EventResult;
import com.android.server.display.feature.ambience.utils.AmbienceLog;
import com.oplus.eventhub.sdk.EventCallback;
import com.oplus.eventhub.sdk.aidl.DeviceEventResult;

/* JADX INFO: loaded from: /run/media/sathiya/data/scratch/ambience.dex */
public class MotionEvent extends EventBase {
    private static final long DEFAULT_ACTIVITY_CHANGED_MESSAGE_DELAY_TIME = 500;
    private static final int MSG_ACTIVITY_CHANGED = 5469;
    private static final int MSG_DEBUG_CHANGE = 5470;
    private static final int MSG_DEBUG_FINISH = 5471;
    private static final int MSG_FIRST_INDEX = 5467;
    private static final int MSG_REGISTER_CALLBACK = 5468;
    private static final String TAG = "MotionEvent";
    private Context mContext;
    private EventCallback mEventCallback;
    private boolean mIsBootComplete;
    private boolean mIsRegistered;
    private MotionEventHandler mMotionEventHandler;

    private static class MotionEventHolder {
        private static final MotionEvent INSTANCE = new MotionEvent();

        private MotionEventHolder() {
        }
    }

    public static final MotionEvent getInstance() {
        return MotionEventHolder.INSTANCE;
    }

    private MotionEvent() {
        super(TAG);
        this.mIsRegistered = false;
        this.mIsBootComplete = false;
        this.mContext = null;
        this.mMotionEventHandler = null;
        this.mEventCallback = new EventCallback() { // from class: com.android.server.display.feature.ambience.events.event.MotionEvent.1
            private final int ACTIVITY_MODE_MOTIONLESS = MotionEvent.MSG_FIRST_INDEX;
            private int preMotionType = MotionEvent.MSG_FIRST_INDEX;

            public void onEventStateChanged(DeviceEventResult deviceEventResult) {
                if (deviceEventResult == null) {
                    AmbienceLog.e(MotionEvent.TAG, "event result is null, nothing to do");
                    return;
                }
                int motionType = deviceEventResult.getEventType();
                if (deviceEventResult.getEventStateType() == 0) {
                    if (this.preMotionType == motionType) {
                        return;
                    } else {
                        this.preMotionType = motionType;
                    }
                } else if (this.preMotionType != motionType) {
                    return;
                } else {
                    this.preMotionType = MotionEvent.MSG_FIRST_INDEX;
                }
                Message msg = MotionEvent.this.mMotionEventHandler.obtainMessage(MotionEvent.MSG_ACTIVITY_CHANGED);
                long activity_change_delay_time = TimeEvent.DEFAULT_TIME_FIVE_SECONDS;
                switch (this.preMotionType) {
                    case 302:
                        msg.obj = MotionEventResult.ACTIVITY.FOOT;
                        activity_change_delay_time = TimeEvent.DEFAULT_TIME_FIVE_SECONDS;
                        break;
                    case 306:
                        msg.obj = MotionEventResult.ACTIVITY.WALKING;
                        activity_change_delay_time = TimeEvent.DEFAULT_TIME_EIGHT_SECONDS;
                        break;
                    case 307:
                        msg.obj = MotionEventResult.ACTIVITY.RUNNING;
                        activity_change_delay_time = TimeEvent.DEFAULT_TIME_EIGHT_SECONDS;
                        break;
                    case 313:
                        msg.obj = MotionEventResult.ACTIVITY.TRANSPORTATION;
                        activity_change_delay_time = TimeEvent.DEFAULT_TIME_TEN_SECONDS;
                        break;
                    default:
                        msg.obj = MotionEventResult.ACTIVITY.MOTIONLESS;
                        break;
                }
                AmbienceLog.f(MotionEvent.TAG, "onEventStateChanged, enter motion type:" + msg.obj.toString());
                MotionEvent.this.mMotionEventHandler.removeMessages(MotionEvent.MSG_ACTIVITY_CHANGED);
                MotionEvent.this.mMotionEventHandler.sendMessageDelayed(msg, activity_change_delay_time);
            }
        };
    }

    public void init(Context context, Handler handler) {
        AmbienceLog.f(TAG, "MotionEvent init");
        this.mContext = context;
        this.mMotionEventHandler = new MotionEventHandler(handler.getLooper());
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
            this.mIsRegistered = DeepThinkerDelegate.getInstance().registerEventCallback(this.mEventCallback, this);
            AmbienceLog.f(TAG, "register DeepThinker activity event result:" + this.mIsRegistered);
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
            isUnregisterSuccess = DeepThinkerDelegate.getInstance().unregisterEventCallback(this);
            this.mMotionEventHandler.obtainMessage(MSG_DEBUG_FINISH).sendToTarget();
            AmbienceLog.f(TAG, "unregister DeepThinker activity event result:" + isUnregisterSuccess);
        } catch (Exception e) {
            AmbienceLog.e(TAG, "Oops! Exception on register: " + e.getMessage());
        }
        this.mIsRegistered = !isUnregisterSuccess;
        resetResultData();
        return isUnregisterSuccess ? 1 : 16;
    }

    @Override // com.android.server.display.feature.ambience.events.utils.EventBase
    public void sendRegisterCallback() {
        if ((this.mEventResult == null || !this.mIsRegistered) && !this.mIsArcDebugMode) {
            this.mEventResult = new MotionEventResult(MotionEventResult.ACTIVITY.MOTIONLESS.name(), MotionEventResult.ACTIVITY.MOTIONLESS.ordinal());
        }
        this.mMotionEventHandler.obtainMessage(MSG_REGISTER_CALLBACK).sendToTarget();
    }

    public static class MotionEventResult extends EventResult {

        public enum ACTIVITY {
            MOTIONLESS,
            WALKING,
            RUNNING,
            FOOT,
            TRANSPORTATION
        }

        public MotionEventResult(String eventType, int eventId) {
            super(eventType, eventId);
        }

        private MotionEventResult(MotionEventResult result) {
            this(result.getCurrentEventType(), result.getCurrentEventId());
            this.obj = result.obj;
            this.isDebugResult = result.isDebugResult;
        }

        public static MotionEventResult copyFrom(MotionEventResult result) {
            if (result == null) {
                return null;
            }
            return new MotionEventResult(result);
        }

        public boolean isMotionless() {
            return getCurrentEventId() == ACTIVITY.MOTIONLESS.ordinal();
        }

        public boolean isTransportation() {
            return getCurrentEventId() == ACTIVITY.TRANSPORTATION.ordinal();
        }

        public boolean isFoot() {
            return (isMotionless() || isTransportation()) ? false : true;
        }

        @Override // com.android.server.display.feature.ambience.events.utils.EventResult
        public String toString() {
            StringBuffer sb = new StringBuffer(MotionEvent.TAG);
            sb.append("{");
            sb.append("event:").append(getCurrentEventType());
            sb.append(" ");
            sb.append("id:").append(getCurrentEventId());
            sb.append("}");
            return sb.toString();
        }
    }

    private class MotionEventHandler extends Handler {
        private final String TAG;

        public MotionEventHandler(Looper looper) {
            super(looper);
            this.TAG = "MotionEventHandler";
            AmbienceLog.f("MotionEventHandler", "MotionEventHandler init");
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MotionEvent.MSG_REGISTER_CALLBACK /* 5468 */:
                    if (MotionEvent.this.mEventResult != null) {
                        AmbienceLog.d("MotionEventHandler", "MSG_REGISTER_CALLBACK, event = " + MotionEvent.this.mEventResult);
                        MotionEvent.this.sendEventCallBack(MotionEvent.this.mEventResult);
                    }
                    break;
                case MotionEvent.MSG_ACTIVITY_CHANGED /* 5469 */:
                    MotionEventResult.ACTIVITY activity = (MotionEventResult.ACTIVITY) msg.obj;
                    if ((MotionEvent.this.mEventResult != null && MotionEvent.this.mEventResult.getCurrentEventId() != activity.ordinal()) || MotionEvent.this.mEventResult == null) {
                        MotionEvent.this.mLastEventResult = MotionEvent.this.mEventResult;
                        MotionEvent.this.mEventResult = new MotionEventResult(activity.name(), activity.ordinal());
                        if (!MotionEvent.this.mIsArcDebugMode) {
                            AmbienceLog.d("MotionEventHandler", "MSG_ACTIVITY_CHANGED, event = " + MotionEvent.this.mEventResult);
                            MotionEvent.this.sendEventCallBack(MotionEvent.this.mEventResult);
                        }
                        break;
                    }
                    break;
                case MotionEvent.MSG_DEBUG_CHANGE /* 5470 */:
                    MotionEvent.this.mEventResult = (EventResult) msg.obj;
                    MotionEvent.this.sendEventCallBack(MotionEvent.this.mEventResult);
                    break;
                case MotionEvent.MSG_DEBUG_FINISH /* 5471 */:
                    MotionEvent.this.mIsArcDebugMode = false;
                    MotionEvent.this.mEventResult = null;
                    MotionEvent.this.sendRegisterCallback();
                    break;
            }
        }
    }

    @Override // com.android.server.display.feature.ambience.events.utils.EventBase
    public void onEventChange(EventResult result) {
        AmbienceLog.d(TAG, "onEventChange:" + result);
        this.mIsArcDebugMode = true;
        Message msg = this.mMotionEventHandler.obtainMessage(MSG_DEBUG_CHANGE);
        msg.obj = result;
        msg.sendToTarget();
    }

    @Override // com.android.server.display.feature.ambience.events.utils.EventBase
    public void onDebugFinish() {
        AmbienceLog.d(TAG, "onDebugFinish");
        this.mMotionEventHandler.obtainMessage(MSG_DEBUG_FINISH).sendToTarget();
    }
}
