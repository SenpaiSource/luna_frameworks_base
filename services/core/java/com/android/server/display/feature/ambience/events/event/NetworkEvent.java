package com.android.server.display.feature.ambience.events.event;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import com.android.server.display.feature.ambience.events.delegate.NetworkDelegate;
import com.android.server.display.feature.ambience.events.utils.EventBase;
import com.android.server.display.feature.ambience.events.utils.EventResult;
import com.android.server.display.feature.ambience.utils.AmbienceLog;
import com.android.server.display.util.OplusDisplayState;

/* JADX INFO: loaded from: /run/media/sathiya/data/scratch/ambience.dex */
public class NetworkEvent extends EventBase {
    private static final long DEFAULT_NETWORK_CHANGED_MESSAGE_DELAY_TIME = 500;
    private static final int MSG_DEBUG_CHANGE = 5470;
    private static final int MSG_DEBUG_FINISH = 5471;
    private static final int MSG_FIRST_INDEX = 5467;
    private static final int MSG_FORCE_UPDATE_EVENT = 5472;
    private static final int MSG_NETWORK_CHANGED = 5469;
    private static final int MSG_REGISTER_CALLBACK = 5468;
    private static final String TAG = "NetworkEvent";
    private Context mContext;
    private boolean mIsBootComplete;
    private boolean mIsRegistered;
    private ConnectivityManager.NetworkCallback mNetworkCallback;
    private NetworkEventHandler mNetworkEventHandler;

    private static class NetworkEventHolder {
        private static final NetworkEvent INSTANCE = new NetworkEvent();

        private NetworkEventHolder() {
        }
    }

    public static final NetworkEvent getInstance() {
        return NetworkEventHolder.INSTANCE;
    }

    private NetworkEvent() {
        super(TAG);
        this.mIsRegistered = false;
        this.mIsBootComplete = false;
        this.mContext = null;
        this.mNetworkEventHandler = null;
        this.mNetworkCallback = new AnonymousClass1();
    }

    public void init(Context context, Handler handler) {
        AmbienceLog.f(TAG, "NetworkEvent init");
        this.mContext = context;
        this.mNetworkEventHandler = new NetworkEventHandler(handler.getLooper());
    }

    @Override // com.android.server.display.feature.ambience.events.utils.EventBase
    public void onBootCompleted() {
        AmbienceLog.f(TAG, "onBootCompleted");
        this.mIsBootComplete = true;
    }

    @Override // com.android.server.display.feature.ambience.events.utils.EventBase
    public void onScreenEvent(int displayId, boolean isScreenOn) {
        if (isScreenOn) {
            this.mNetworkEventHandler.sendEmptyMessage(MSG_FORCE_UPDATE_EVENT);
        }
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
            this.mIsRegistered = NetworkDelegate.getInstance().registerEventCallback(this.mNetworkCallback, this);
            AmbienceLog.f(TAG, "register Network event result:" + this.mIsRegistered);
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
            isUnregisterSuccess = NetworkDelegate.getInstance().unregisterEventCallback(this);
            this.mNetworkEventHandler.obtainMessage(MSG_DEBUG_FINISH).sendToTarget();
            AmbienceLog.f(TAG, "register network event result:" + isUnregisterSuccess);
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
            NetworkEventResult.NETWORK network = getCurrentNetworkState();
            this.mEventResult = new NetworkEventResult(network.name(), network.ordinal());
        }
        this.mNetworkEventHandler.obtainMessage(MSG_REGISTER_CALLBACK).sendToTarget();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public NetworkEventResult.NETWORK getCurrentNetworkState() {
        NetworkEventResult.NETWORK network = NetworkEventResult.NETWORK.UNKNOWN;
        switch (NetworkDelegate.getInstance().getNetworkType()) {
            case 0:
                NetworkEventResult.NETWORK result = NetworkEventResult.NETWORK.MOBILE;
                return result;
            case 1:
                NetworkEventResult.NETWORK result2 = NetworkEventResult.NETWORK.WIFI;
                return result2;
            default:
                NetworkEventResult.NETWORK result3 = NetworkEventResult.NETWORK.UNKNOWN;
                return result3;
        }
    }

    private NetworkEventResult.NETWORK getCurrentNetworkState(Network network) {
        NetworkEventResult.NETWORK network2 = NetworkEventResult.NETWORK.UNKNOWN;
        switch (NetworkDelegate.getInstance().getNetworkType(network)) {
            case 0:
                NetworkEventResult.NETWORK result = NetworkEventResult.NETWORK.MOBILE;
                return result;
            case 1:
                NetworkEventResult.NETWORK result2 = NetworkEventResult.NETWORK.WIFI;
                return result2;
            default:
                NetworkEventResult.NETWORK result3 = NetworkEventResult.NETWORK.UNKNOWN;
                return result3;
        }
    }

    public boolean isWifiConnected() {
        if (this.mEventResult != null) {
            return ((NetworkEventResult) this.mEventResult).isWifiConnected();
        }
        return getCurrentNetworkState().ordinal() == NetworkEventResult.NETWORK.WIFI.ordinal();
    }

    /* JADX INFO: renamed from: com.android.server.display.feature.ambience.events.event.NetworkEvent$1, reason: invalid class name */
    class AnonymousClass1 extends ConnectivityManager.NetworkCallback {
        private Network wifi = null;

        AnonymousClass1() {
        }

        @Override // android.net.ConnectivityManager.NetworkCallback
        public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
            super.onCapabilitiesChanged(network, networkCapabilities);
            if (network == null || networkCapabilities == null) {
                AmbienceLog.e(NetworkEvent.TAG, "network result is null, nothing to do");
                return;
            }
            if (this.wifi != null) {
                return;
            }
            Message msg = NetworkEvent.this.mNetworkEventHandler.obtainMessage(NetworkEvent.MSG_NETWORK_CHANGED);
            if (networkCapabilities.hasTransport(1)) {
                msg.obj = NetworkEventResult.NETWORK.WIFI;
                this.wifi = network;
            } else if (networkCapabilities.hasTransport(0)) {
                msg.obj = NetworkEventResult.NETWORK.MOBILE;
            } else {
                msg.obj = NetworkEventResult.NETWORK.UNKNOWN;
            }
            NetworkEvent.this.mNetworkEventHandler.removeMessages(NetworkEvent.MSG_NETWORK_CHANGED);
            NetworkEvent.this.mNetworkEventHandler.sendMessageDelayed(msg, 500L);
        }

        @Override // android.net.ConnectivityManager.NetworkCallback
        public void onLost(Network network) {
            super.onLost(network);
            if (this.wifi != null && this.wifi.equals(network)) {
                AmbienceLog.f(NetworkEvent.TAG, "wifi network is lost");
                this.wifi = null;
            }
            NetworkEvent.this.mNetworkEventHandler.postDelayed(this::oplusLambda$onLost$0, 500L);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void oplusLambda$onLost$0() {
            Message msg = NetworkEvent.this.mNetworkEventHandler.obtainMessage(NetworkEvent.MSG_NETWORK_CHANGED);
            msg.obj = NetworkEvent.this.getCurrentNetworkState();
            NetworkEvent.this.mNetworkEventHandler.sendMessage(msg);
        }
    }

    public static class NetworkEventResult extends EventResult {

        public enum NETWORK {
            WIFI,
            MOBILE,
            UNKNOWN
        }

        public NetworkEventResult(String eventType, int eventId) {
            super(eventType, eventId);
        }

        private NetworkEventResult(NetworkEventResult result) {
            this(result.getCurrentEventType(), result.getCurrentEventId());
            this.obj = result.obj;
            this.isDebugResult = result.isDebugResult;
        }

        public static NetworkEventResult copyFrom(NetworkEventResult result) {
            if (result == null) {
                return null;
            }
            return new NetworkEventResult(result);
        }

        public boolean isWifiConnected() {
            return getCurrentEventId() == NETWORK.WIFI.ordinal();
        }

        @Override // com.android.server.display.feature.ambience.events.utils.EventResult
        public String toString() {
            StringBuffer sb = new StringBuffer(NetworkEvent.TAG);
            sb.append("{");
            sb.append("event:").append(getCurrentEventType());
            sb.append(" ");
            sb.append("id:").append(getCurrentEventId());
            sb.append("}");
            return sb.toString();
        }
    }

    private class NetworkEventHandler extends Handler {
        private final String TAG;

        public NetworkEventHandler(Looper looper) {
            super(looper);
            this.TAG = "NetworkEventHandler";
            AmbienceLog.f("NetworkEventHandler", "NetworkEventHandler init");
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case NetworkEvent.MSG_REGISTER_CALLBACK /* 5468 */:
                    if (NetworkEvent.this.mEventResult != null) {
                        AmbienceLog.d("NetworkEventHandler", "MSG_REGISTER_CALLBACK, event = " + NetworkEvent.this.mEventResult);
                        NetworkEvent.this.sendEventCallBack(NetworkEvent.this.mEventResult);
                    }
                    break;
                case NetworkEvent.MSG_NETWORK_CHANGED /* 5469 */:
                    NetworkEventResult.NETWORK network = (NetworkEventResult.NETWORK) msg.obj;
                    if ((NetworkEvent.this.mEventResult != null && NetworkEvent.this.mEventResult.getCurrentEventId() != network.ordinal()) || NetworkEvent.this.mEventResult == null) {
                        NetworkEvent.this.mLastEventResult = NetworkEvent.this.mEventResult;
                        NetworkEvent.this.mEventResult = new NetworkEventResult(network.name(), network.ordinal());
                        if (!NetworkEvent.this.mIsArcDebugMode) {
                            AmbienceLog.d("NetworkEventHandler", "MSG_NETWORK_CHANGED, event = " + NetworkEvent.this.mEventResult);
                            NetworkEvent.this.sendEventCallBack(NetworkEvent.this.mEventResult);
                        }
                    }
                    removeMessages(NetworkEvent.MSG_FORCE_UPDATE_EVENT);
                    sendEmptyMessageDelayed(NetworkEvent.MSG_FORCE_UPDATE_EVENT, TimeEvent.DEFAULT_TIME_TEN_MINUTE);
                    break;
                case NetworkEvent.MSG_DEBUG_CHANGE /* 5470 */:
                    NetworkEvent.this.mEventResult = (EventResult) msg.obj;
                    NetworkEvent.this.sendEventCallBack(NetworkEvent.this.mEventResult);
                    break;
                case NetworkEvent.MSG_DEBUG_FINISH /* 5471 */:
                    NetworkEvent.this.mIsArcDebugMode = false;
                    NetworkEvent.this.mEventResult = null;
                    NetworkEvent.this.sendRegisterCallback();
                    break;
                case NetworkEvent.MSG_FORCE_UPDATE_EVENT /* 5472 */:
                    if (OplusDisplayState.getInstance().isScreenOn()) {
                        Message forceUpdateMessage = obtainMessage(NetworkEvent.MSG_NETWORK_CHANGED);
                        forceUpdateMessage.obj = NetworkEvent.this.getCurrentNetworkState();
                        sendMessage(forceUpdateMessage);
                    }
                    break;
            }
        }
    }

    @Override // com.android.server.display.feature.ambience.events.utils.EventBase
    public void onEventChange(EventResult result) {
        AmbienceLog.d(TAG, "onEventChange:" + result);
        this.mIsArcDebugMode = true;
        Message msg = this.mNetworkEventHandler.obtainMessage(MSG_DEBUG_CHANGE);
        msg.obj = result;
        msg.sendToTarget();
    }

    @Override // com.android.server.display.feature.ambience.events.utils.EventBase
    public void onDebugFinish() {
        AmbienceLog.d(TAG, "onDebugFinish");
        this.mNetworkEventHandler.obtainMessage(MSG_DEBUG_FINISH).sendToTarget();
    }
}
