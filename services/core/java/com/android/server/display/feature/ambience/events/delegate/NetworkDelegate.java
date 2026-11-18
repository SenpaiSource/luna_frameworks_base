package com.android.server.display.feature.ambience.events.delegate;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import com.android.server.display.feature.ambience.events.event.NetworkEvent;
import com.android.server.display.feature.ambience.utils.AmbienceLog;

/* JADX INFO: loaded from: /run/media/sathiya/data/scratch/ambience.dex */
public class NetworkDelegate {
    private static final int DEFAULT_NUMBER_ZERO = 0;
    private static final long DEFAULT_REGISTER_NETWORK_MANAGER_DELAY_TIME = 500;
    private static final int MSG_FIRST_INDEX = 5467;
    private static final int MSG_REGISTER_NETWORK_MANAGER_CALLBACK = 5468;
    private static final String TAG = "NetworkDelegate";
    private ConnectivityManager mConnectivityManager;
    private Context mContext;
    private boolean mIsBootComplete;
    private boolean mIsRegistered;
    private ConnectivityManager.NetworkCallback mNetWorkEventCallback;
    private NetworkDelegateHandler mNetworkDelegateHandler;

    private static class NetworkDelegateHolder {
        private static final NetworkDelegate INSTANCE = new NetworkDelegate();

        private NetworkDelegateHolder() {
        }
    }

    public static final NetworkDelegate getInstance() {
        return NetworkDelegateHolder.INSTANCE;
    }

    private NetworkDelegate() {
        this.mIsRegistered = false;
        this.mIsBootComplete = false;
        this.mContext = null;
        this.mNetworkDelegateHandler = null;
        this.mNetWorkEventCallback = null;
    }

    public void init(Context context, Handler handler) {
        AmbienceLog.f(TAG, "NetworkDelegate init");
        this.mContext = context;
        this.mNetworkDelegateHandler = new NetworkDelegateHandler(handler.getLooper());
    }

    public void onBootCompleted() {
        AmbienceLog.f(TAG, "onBootCompleted");
        this.mIsBootComplete = true;
        sendRegisterMessage(true, this.mNetWorkEventCallback);
    }

    public boolean registerEventCallback(ConnectivityManager.NetworkCallback listener, Object caller) {
        if (!(caller instanceof NetworkEvent)) {
            AmbienceLog.e(TAG, "don't support other package use, please access class NetworkEvent");
            return false;
        }
        if (listener == null) {
            AmbienceLog.e(TAG, "listener is null, please check code");
            return false;
        }
        return sendRegisterMessage(true, listener);
    }

    public boolean unregisterEventCallback(Object caller) {
        if (!(caller instanceof NetworkEvent)) {
            AmbienceLog.e(TAG, "don't support other package use, please access class NetworkEvent");
            return false;
        }
        return sendRegisterMessage(false, this.mNetWorkEventCallback);
    }

    private boolean sendRegisterMessage(boolean z, ConnectivityManager.NetworkCallback networkCallback) {
        this.mNetworkDelegateHandler.removeMessages(MSG_REGISTER_NETWORK_MANAGER_CALLBACK);
        Message messageObtainMessage = this.mNetworkDelegateHandler.obtainMessage(MSG_REGISTER_NETWORK_MANAGER_CALLBACK);
        messageObtainMessage.arg1 = z ? 1 : 0;
        messageObtainMessage.obj = networkCallback;
        this.mNetworkDelegateHandler.sendMessageDelayed(messageObtainMessage, 500L);
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void prepareRegisterNetworkCallback(boolean isRegister, ConnectivityManager.NetworkCallback listener) {
        if (this.mIsRegistered && isRegister) {
            AmbienceLog.e(TAG, "NetworkCallback has been registered");
            return;
        }
        if (!this.mIsRegistered && !isRegister) {
            AmbienceLog.e(TAG, "NetworkCallback has been unregistered");
            return;
        }
        this.mNetWorkEventCallback = listener;
        if (this.mIsBootComplete) {
            registerNetworkCallback(isRegister);
        } else {
            AmbienceLog.e(TAG, "prepareRegisterNetworkCallback fail, the component is not ready");
        }
    }

    private boolean registerNetworkCallback(boolean isRegister) {
        if (this.mContext == null) {
            AmbienceLog.e(TAG, "registerNetworkCallback fail, init not completed");
            return false;
        }
        if (this.mNetWorkEventCallback == null) {
            AmbienceLog.e(TAG, "NetworkCallback is null");
            return false;
        }
        if (this.mConnectivityManager == null) {
            this.mConnectivityManager = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        }
        if (this.mConnectivityManager != null) {
            try {
                if (isRegister) {
                    NetworkRequest request = new NetworkRequest.Builder().addTransportType(1).addTransportType(0).build();
                    this.mConnectivityManager.registerNetworkCallback(request, this.mNetWorkEventCallback);
                    this.mIsRegistered = true;
                    AmbienceLog.f(TAG, "registerNetworkCallback success.");
                } else {
                    this.mConnectivityManager.unregisterNetworkCallback(this.mNetWorkEventCallback);
                    this.mNetWorkEventCallback = null;
                    this.mIsRegistered = false;
                    AmbienceLog.f(TAG, "unregisterNetworkCallback success.");
                }
            } catch (Throwable t) {
                AmbienceLog.e(TAG, "register/unregisterNetworkCallback failed: " + t);
            }
        }
        return true;
    }

    public int getNetworkType() {
        if (this.mContext == null) {
            AmbienceLog.e(TAG, "getNetworkType fail, init not completed");
            return -1;
        }
        if (this.mConnectivityManager == null) {
            this.mConnectivityManager = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        }
        if (this.mConnectivityManager != null) {
            NetworkInfo networkInfo = this.mConnectivityManager.getNetworkInfo(1);
            if (networkInfo != null && networkInfo.isConnected()) {
                return 1;
            }
            NetworkInfo networkInfo2 = this.mConnectivityManager.getNetworkInfo(0);
            if (networkInfo2 != null && networkInfo2.isConnected()) {
                return 0;
            }
        }
        return -1;
    }

    public int getNetworkType(Network network) {
        NetworkInfo networkInfo;
        if (this.mContext == null) {
            AmbienceLog.e(TAG, "getNetworkType fail, init not completed");
            return -1;
        }
        if (this.mConnectivityManager == null) {
            this.mConnectivityManager = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        }
        if (this.mConnectivityManager != null && (networkInfo = this.mConnectivityManager.getNetworkInfo(network)) != null) {
            return networkInfo.getType();
        }
        return -1;
    }

    private class NetworkDelegateHandler extends Handler {
        public NetworkDelegateHandler(Looper looper) {
            super(looper);
            AmbienceLog.f(NetworkDelegate.TAG, "NetworkDelegateHandler init");
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case NetworkDelegate.MSG_REGISTER_NETWORK_MANAGER_CALLBACK /* 5468 */:
                    NetworkDelegate.this.prepareRegisterNetworkCallback(msg.arg1 == 1, (ConnectivityManager.NetworkCallback) msg.obj);
                    break;
            }
        }
    }
}
