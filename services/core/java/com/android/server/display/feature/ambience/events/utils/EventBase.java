package com.android.server.display.feature.ambience.events.utils;

import com.android.server.display.feature.ambience.events.delegate.DeepThinkerDelegate;
import com.android.server.display.feature.ambience.scenes.utils.SceneBase;
import com.android.server.display.feature.ambience.utils.AmbienceLog;
import com.android.server.display.test.impl.OplusBrightnessDebugCenter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/* JADX INFO: loaded from: /run/media/sathiya/data/scratch/ambience.dex */
public abstract class EventBase implements OplusBrightnessDebugCenter.IOplusDebugListener {
    private static final int OBMS_VERIFY_NUMBER = 80261364;
    public static final int OPERATE_ADD_ARRAY_LIST_FAIL_CODE = 2;
    public static final int OPERATE_REGISTER_FAIL_CODE = 4;
    public static final int OPERATE_REMOVE_ARRAY_LIST_FAIL_CODE = 8;
    public static final int OPERATE_SUCCESS_CODE = 1;
    public static final int OPERATE_UNREGISTER_FAIL_CODE = 16;
    public final String TAG;
    public List<IEventListener> mEventListeners = new CopyOnWriteArrayList();
    public EventResult mLastEventResult = null;
    public EventResult mEventResult = null;
    public boolean mIsArcDebugMode = false;

    public interface IEventListener {
        void onEventChange(EventResult eventResult);
    }

    public abstract void onBootCompleted();

    public abstract void onDebugFinish();

    public abstract void onEventChange(EventResult eventResult);

    public abstract int register(IEventListener iEventListener);

    public abstract void sendRegisterCallback();

    public abstract int unregister(IEventListener iEventListener);

    public EventBase(String tag) {
        this.TAG = tag;
        this.mEventListeners.clear();
        registerDebugCallback();
    }

    public void onScreenEvent(int displayId, boolean isScreenOn) {
    }

    public int addListener(IEventListener listener) {
        if (listener == null || this.mEventListeners.contains(listener)) {
            return 2;
        }
        int result = this.mEventListeners.add(listener) ? 1 : 2;
        AmbienceLog.d(this.TAG, "addListener, listener:" + listener);
        return result;
    }

    public int removeListener(IEventListener listener) {
        if (listener == null || !this.mEventListeners.contains(listener)) {
            return 8;
        }
        int result = this.mEventListeners.remove(listener) ? 1 : 8;
        AmbienceLog.d(this.TAG, "removeListener, listener:" + listener);
        return result;
    }

    public void resetResultData() {
        this.mLastEventResult = null;
        this.mEventResult = null;
    }

    public boolean isListenerEmpty() {
        return this.mEventListeners.isEmpty();
    }

    public boolean sendEventCallBack(EventResult result) {
        for (IEventListener listener : this.mEventListeners) {
            if (listener != null) {
                listener.onEventChange(result);
            }
        }
        return true;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code duplicated, block: B:26:0x0051  */
    private void registerDebugCallback() {
        byte b;
        OplusBrightnessDebugCenter.EVENT type = null;
        String str = this.TAG;
        switch (str.hashCode()) {
            case -1504627333:
                if (!str.equals("ProximityEvent")) {
                    b = -1;
                } else {
                    b = 5;
                }
                break;
            case -1423230015:
                if (!str.equals("DarkModeEvent")) {
                    b = -1;
                } else {
                    b = 0;
                }
                break;
            case -723815445:
                if (!str.equals("LuxEvent")) {
                    b = -1;
                } else {
                    b = 2;
                }
                break;
            case 120891692:
                if (!str.equals("NetworkEvent")) {
                    b = -1;
                } else {
                    b = 4;
                }
                break;
            case 626647491:
                if (!str.equals("InOutDoorEvent")) {
                    b = -1;
                } else {
                    b = 1;
                }
                break;
            case 780913764:
                if (!str.equals("MotionEvent")) {
                    b = -1;
                } else {
                    b = 3;
                }
                break;
            case 2016675757:
                if (!str.equals("TimeEvent")) {
                    b = -1;
                } else {
                    b = 6;
                }
                break;
            default:
                b = -1;
                break;
        }
        switch (b) {
            case 0:
                type = OplusBrightnessDebugCenter.EVENT.DARKMODE;
                break;
            case 1:
                type = OplusBrightnessDebugCenter.EVENT.INOUTDOOR;
                break;
            case 2:
                type = OplusBrightnessDebugCenter.EVENT.LUX;
                break;
            case 3:
                type = OplusBrightnessDebugCenter.EVENT.MOTION;
                break;
            case 4:
                type = OplusBrightnessDebugCenter.EVENT.NETWORK;
                break;
            case DeepThinkerDelegate.InOutDoorImpl.Constants.NRE_AVG_GPGSV /* 5 */:
                type = OplusBrightnessDebugCenter.EVENT.PROXIMITY;
                break;
            case DeepThinkerDelegate.InOutDoorImpl.Constants.FEATURE_SIZE /* 6 */:
                type = OplusBrightnessDebugCenter.EVENT.TIME;
                break;
        }
        OplusBrightnessDebugCenter.getInstance().registerDebugListener(type, this, OBMS_VERIFY_NUMBER);
    }

    public void onSceneChange(SceneBase base) {
    }
}
