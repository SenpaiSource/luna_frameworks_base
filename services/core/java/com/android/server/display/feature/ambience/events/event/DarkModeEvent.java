package com.android.server.display.feature.ambience.events.event;

import android.app.UiModeManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import com.android.server.display.feature.ambience.events.utils.EventBase;
import com.android.server.display.feature.ambience.events.utils.EventResult;
import com.android.server.display.feature.ambience.utils.AmbienceLog;

/* JADX INFO: loaded from: /run/media/sathiya/data/scratch/ambience.dex */
public class DarkModeEvent extends EventBase {
    private static final long DEFAULT_DARK_MODE_CHANGED_MESSAGE_DELAY_TIME = 500;
    private static final int MSG_DARK_MODE_CHANGED = 5469;
    private static final int MSG_DEBUG_CHANGE = 5470;
    private static final int MSG_DEBUG_FINISH = 5471;
    private static final int MSG_FIRST_INDEX = 5467;
    private static final int MSG_REGISTER_CALLBACK = 5468;
    private static final String TAG = "DarkModeEvent";
    private Context mContext;
    private DarkModeEventHandler mDarkModeEventHandler;
    private boolean mIsBootComplete;
    private boolean mIsRegistered;

    private static class DarkModeEventHolder {
        private static final DarkModeEvent INSTANCE = new DarkModeEvent();

        private DarkModeEventHolder() {
        }
    }

    public static final DarkModeEvent getInstance() {
        return DarkModeEventHolder.INSTANCE;
    }

    private DarkModeEvent() {
        super(TAG);
        this.mIsRegistered = false;
        this.mIsBootComplete = false;
        this.mContext = null;
        this.mDarkModeEventHandler = null;
    }

    public void init(Context context, Handler handler) {
        AmbienceLog.f(TAG, "DarkModeEvent init");
        this.mContext = context;
        this.mDarkModeEventHandler = new DarkModeEventHandler(handler.getLooper());
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
        this.mIsRegistered = true;
        AmbienceLog.f(TAG, "register Dark Mode event result:" + this.mIsRegistered);
        return 1;
    }

    @Override // com.android.server.display.feature.ambience.events.utils.EventBase
    public int unregister(EventBase.IEventListener listener) {
        return removeListener(listener) | unregister();
    }

    private int unregister() {
        if (isListenerEmpty()) {
            this.mDarkModeEventHandler.obtainMessage(MSG_DEBUG_FINISH).sendToTarget();
            this.mIsRegistered = false;
            resetResultData();
            return 1;
        }
        return 1;
    }

    @Override // com.android.server.display.feature.ambience.events.utils.EventBase
    public void sendRegisterCallback() {
        if (this.mContext == null) {
            AmbienceLog.e(TAG, "sendRegisterCallback fail, init not completed");
            return;
        }
        if (this.mEventResult == null || !this.mIsRegistered) {
            UiModeManager uiModeManager = (UiModeManager) this.mContext.getSystemService("uimode");
            if (uiModeManager != null && uiModeManager.getNightMode() == 2) {
                this.mEventResult = new DarkModeEventResult(DarkModeEventResult.MODE.DARK_MODE.name(), DarkModeEventResult.MODE.DARK_MODE.ordinal());
            } else {
                this.mEventResult = new DarkModeEventResult(DarkModeEventResult.MODE.BRIGHT_MODE.name(), DarkModeEventResult.MODE.BRIGHT_MODE.ordinal());
            }
        }
        this.mDarkModeEventHandler.obtainMessage(MSG_REGISTER_CALLBACK).sendToTarget();
    }

    public void darkModeChanged(int uiMode, int lastUiMode) {
        if (this.mIsRegistered && uiMode != lastUiMode) {
            Message msg = this.mDarkModeEventHandler.obtainMessage(MSG_DARK_MODE_CHANGED);
            msg.obj = ((uiMode & 32) >> 5) == 1 ? DarkModeEventResult.MODE.DARK_MODE : DarkModeEventResult.MODE.BRIGHT_MODE;
            this.mDarkModeEventHandler.removeMessages(MSG_DARK_MODE_CHANGED);
            this.mDarkModeEventHandler.sendMessageDelayed(msg, 500L);
        }
    }

    public static class DarkModeEventResult extends EventResult {

        public enum MODE {
            DARK_MODE,
            BRIGHT_MODE
        }

        public DarkModeEventResult(String eventType, int eventId) {
            super(eventType, eventId);
        }

        private DarkModeEventResult(DarkModeEventResult result) {
            this(result.getCurrentEventType(), result.getCurrentEventId());
            this.obj = result.obj;
            this.isDebugResult = result.isDebugResult;
        }

        public static DarkModeEventResult copyFrom(DarkModeEventResult result) {
            if (result == null) {
                return null;
            }
            return new DarkModeEventResult(result);
        }

        public boolean isDarkMode() {
            return getCurrentEventId() == MODE.DARK_MODE.ordinal();
        }

        @Override // com.android.server.display.feature.ambience.events.utils.EventResult
        public String toString() {
            StringBuffer sb = new StringBuffer(DarkModeEvent.TAG);
            sb.append("{");
            sb.append("event:").append(getCurrentEventType());
            sb.append(" ");
            sb.append("id:").append(getCurrentEventId());
            sb.append("}");
            return sb.toString();
        }
    }

    private class DarkModeEventHandler extends Handler {
        private final String TAG;

        public DarkModeEventHandler(Looper looper) {
            super(looper);
            this.TAG = "DarkModeEventHandler";
            AmbienceLog.f("DarkModeEventHandler", "DarkModeEventHandler init");
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case DarkModeEvent.MSG_REGISTER_CALLBACK /* 5468 */:
                    if (DarkModeEvent.this.mEventResult != null) {
                        AmbienceLog.d("DarkModeEventHandler", "MSG_REGISTER_CALLBACK, event = " + DarkModeEvent.this.mEventResult);
                        DarkModeEvent.this.sendEventCallBack(DarkModeEvent.this.mEventResult);
                    }
                    break;
                case DarkModeEvent.MSG_DARK_MODE_CHANGED /* 5469 */:
                    DarkModeEventResult.MODE mode = (DarkModeEventResult.MODE) msg.obj;
                    if ((DarkModeEvent.this.mEventResult != null && DarkModeEvent.this.mEventResult.getCurrentEventId() != mode.ordinal()) || DarkModeEvent.this.mEventResult == null) {
                        DarkModeEvent.this.mLastEventResult = DarkModeEvent.this.mEventResult;
                        DarkModeEvent.this.mEventResult = new DarkModeEventResult(mode.name(), mode.ordinal());
                        if (!DarkModeEvent.this.mIsArcDebugMode) {
                            AmbienceLog.d("DarkModeEventHandler", "MSG_DARK_MODE_CHANGED, event = " + DarkModeEvent.this.mEventResult);
                            DarkModeEvent.this.sendEventCallBack(DarkModeEvent.this.mEventResult);
                        }
                        break;
                    }
                    break;
                case DarkModeEvent.MSG_DEBUG_CHANGE /* 5470 */:
                    DarkModeEvent.this.mEventResult = (EventResult) msg.obj;
                    DarkModeEvent.this.sendEventCallBack(DarkModeEvent.this.mEventResult);
                    break;
                case DarkModeEvent.MSG_DEBUG_FINISH /* 5471 */:
                    DarkModeEvent.this.mIsArcDebugMode = false;
                    DarkModeEvent.this.mEventResult = null;
                    DarkModeEvent.this.sendRegisterCallback();
                    break;
            }
        }
    }

    @Override // com.android.server.display.feature.ambience.events.utils.EventBase
    public void onEventChange(EventResult result) {
        AmbienceLog.d(TAG, "onEventChange:" + result);
        this.mIsArcDebugMode = true;
        Message msg = this.mDarkModeEventHandler.obtainMessage(MSG_DEBUG_CHANGE);
        msg.obj = result;
        msg.sendToTarget();
    }

    @Override // com.android.server.display.feature.ambience.events.utils.EventBase
    public void onDebugFinish() {
        AmbienceLog.d(TAG, "onDebugFinish");
        this.mDarkModeEventHandler.obtainMessage(MSG_DEBUG_FINISH).sendToTarget();
    }
}
