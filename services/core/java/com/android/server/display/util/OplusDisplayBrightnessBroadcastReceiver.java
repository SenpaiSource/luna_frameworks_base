package com.android.server.display.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.util.Slog;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

/* JADX INFO: loaded from: /run/media/sathiya/data/scratch/deps.dex */
public class OplusDisplayBrightnessBroadcastReceiver extends BroadcastReceiver {
    public static final String TAG = "OplusDisplayBrightnessBroadcastReceiver";
    private Context mContext;
    private Handler mHandler;
    private HashMap<String, ArrayList<Listener>> mListenersMap;
    private final Object mLock;
    private HashMap<String, Boolean> mRegistMap;
    private ArrayList<Listener> mTempListeners;

    public interface Listener {
        void onBroadcastReceive(Context context, Intent intent);
    }

    private OplusDisplayBrightnessBroadcastReceiver() {
        this.mLock = new Object();
        this.mTempListeners = new ArrayList<>();
        this.mContext = null;
        this.mHandler = null;
        this.mListenersMap = new HashMap<>();
        this.mRegistMap = new HashMap<>();
    }

    private static class OplusDisplayBrightnessBroadcastReceiverHolder {
        private static final OplusDisplayBrightnessBroadcastReceiver INSTANCE = new OplusDisplayBrightnessBroadcastReceiver();

        private OplusDisplayBrightnessBroadcastReceiverHolder() {
        }
    }

    public static final OplusDisplayBrightnessBroadcastReceiver getInstance() {
        return OplusDisplayBrightnessBroadcastReceiverHolder.INSTANCE;
    }

    public void init(Context context, Handler handler) {
        this.mContext = context;
        this.mHandler = handler;
    }

    public void addListener(List<String> actions, Listener listener) {
        synchronized (this.mLock) {
            for (String action : actions) {
                this.mListenersMap.putIfAbsent(action, new ArrayList<>());
                if (!this.mListenersMap.get(action).contains(listener)) {
                    this.mListenersMap.get(action).add(listener);
                }
            }
        }
        registActions();
    }

    public void removeListener(final Listener listener) {
        synchronized (this.mLock) {
            this.mListenersMap.forEach(new BiConsumer() { // from class: com.android.server.display.util.OplusDisplayBrightnessBroadcastReceiver$$ExternalSyntheticLambda2
                @Override // java.util.function.BiConsumer
                public final void accept(Object obj, Object obj2) {
                    ((ArrayList) obj2).removeIf(new Predicate() { // from class: com.android.server.display.util.OplusDisplayBrightnessBroadcastReceiver$$ExternalSyntheticLambda1
                        @Override // java.util.function.Predicate
                        public final boolean test(Object obj3) {
                            return OplusDisplayBrightnessBroadcastReceiver.oplusLambda$removeListener$0(listener, (OplusDisplayBrightnessBroadcastReceiver.Listener) obj3);
                        }
                    });
                }
            });
        }
    }

    static /* synthetic */ boolean oplusLambda$removeListener$0(Listener listener, Listener l) {
        return l == listener;
    }

    @Override // android.content.BroadcastReceiver
    public void onReceive(final Context context, final Intent intent) {
        if (this.mHandler != null) {
            this.mHandler.post(() -> oplusLambda$onReceive$2(intent, context));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void oplusLambda$onReceive$2(Intent intent, Context context) {
        synchronized (this.mLock) {
            ArrayList<Listener> listeners = this.mListenersMap.get(intent.getAction());
            if (listeners != null) {
                this.mTempListeners.addAll(listeners);
            }
        }
        try {
            for (Listener listener : this.mTempListeners) {
                listener.onBroadcastReceive(context, intent);
            }
            Slog.d(TAG, "onReceive action = " + intent.getAction() + " , listeners = " + this.mTempListeners);
        } finally {
            this.mTempListeners.clear();
        }
    }

    private void registActions() {
        if (this.mHandler != null) {
            this.mHandler.post(this::oplusLambda$registActions$3);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void oplusLambda$registActions$3() {
        registerForPackageActions();
        registerForGeneralActions();
    }

    public void dump(final PrintWriter pw) {
        pw.println();
        pw.println("OplusDisplayBrightnessBroadcastReceiver:");
        pw.println("action and listeners");
        synchronized (this.mLock) {
            this.mListenersMap.forEach(new BiConsumer() { // from class: com.android.server.display.util.OplusDisplayBrightnessBroadcastReceiver$$ExternalSyntheticLambda6
                @Override // java.util.function.BiConsumer
                public final void accept(Object obj, Object obj2) {
                    pw.println("action = " + ((String) obj) + " , listener = " + ((ArrayList) obj2));
                }
            });
            pw.println();
            this.mRegistMap.forEach(new BiConsumer() { // from class: com.android.server.display.util.OplusDisplayBrightnessBroadcastReceiver$$ExternalSyntheticLambda7
                @Override // java.util.function.BiConsumer
                public final void accept(Object obj, Object obj2) {
                    pw.println("action = " + ((String) obj) + " , isRegist = " + ((Boolean) obj2));
                }
            });
        }
    }

    private void registerForGeneralActions() {
        final IntentFilter intentFilter = new IntentFilter();
        final AtomicBoolean hasNeedRegistAction = new AtomicBoolean(false);
        synchronized (this.mLock) {
            this.mListenersMap.forEach((obj, obj2) -> oplusLambda$registerForGeneralActions$6(intentFilter, hasNeedRegistAction, (String) obj, (ArrayList) obj2));
        }
        if (hasNeedRegistAction.get() && this.mContext != null) {
            try {
                this.mContext.registerReceiver(this, intentFilter);
            } catch (Throwable t) {
                Slog.e(TAG, "Failed to register general actions receiver: " + t);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void oplusLambda$registerForGeneralActions$6(IntentFilter intentFilter, AtomicBoolean hasNeedRegistAction, String action, ArrayList listeners) {
        if (!this.mRegistMap.containsKey(action) || !this.mRegistMap.get(action).booleanValue()) {
            intentFilter.addAction(action);
            if ("android.intent.action.BATTERY_CHANGED".equals(action)) {
                intentFilter.addCategory("oplusBrEx@android.intent.action.BATTERY_CHANGED@BATTERYSTATE=BATTERY_LEVEL_CHANGED|BATTERYSTATE=BATTERY_TEMPERATURE_CHANGED");
            }
            this.mRegistMap.put(action, true);
            hasNeedRegistAction.set(true);
        }
    }

    private void registerForPackageActions() {
        final IntentFilter intentFilterPkg = new IntentFilter();
        final AtomicBoolean hasNeedRegistPkgAction = new AtomicBoolean(false);
        synchronized (this.mLock) {
            this.mListenersMap.forEach((obj, obj2) -> oplusLambda$registerForPackageActions$7(intentFilterPkg, hasNeedRegistPkgAction, (String) obj, (ArrayList) obj2));
        }
        if (hasNeedRegistPkgAction.get() && this.mContext != null) {
            intentFilterPkg.addDataScheme("package");
            try {
                this.mContext.registerReceiver(this, intentFilterPkg);
            } catch (Throwable t) {
                Slog.e(TAG, "Failed to register package actions receiver: " + t);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void oplusLambda$registerForPackageActions$7(IntentFilter intentFilterPkg, AtomicBoolean hasNeedRegistPkgAction, String action, ArrayList listeners) {
        if (PackageAction.isPackageAction(action)) {
            if (!this.mRegistMap.containsKey(action) || !this.mRegistMap.get(action).booleanValue()) {
                intentFilterPkg.addAction(action);
                hasNeedRegistPkgAction.set(true);
                this.mRegistMap.put(action, true);
            }
        }
    }

    private void unRegisterAll() {
        synchronized (this.mLock) {
            this.mListenersMap.clear();
            this.mRegistMap.clear();
            if (this.mContext != null) {
                try {
                    this.mContext.unregisterReceiver(this);
                } catch (Throwable t) {
                    Slog.w(TAG, "Failed to unregister receiver: " + t);
                }
            }
        }
    }

    private enum PackageAction {
        ACTION_PACKAGE_ADDED("android.intent.action.PACKAGE_ADDED"),
        ACTION_PACKAGE_REMOVED("android.intent.action.PACKAGE_REMOVED"),
        ACTION_PACKAGE_REPLACED("android.intent.action.PACKAGE_REPLACED"),
        ACTION_PACKAGE_CHANGED("android.intent.action.PACKAGE_CHANGED"),
        ACTION_PACKAGE_DATA_CLEARED("android.intent.action.PACKAGE_DATA_CLEARED"),
        ACTION_PACKAGE_RESTARTED("android.intent.action.PACKAGE_RESTARTED");

        private String mAction;

        PackageAction(String action) {
            this.mAction = action;
        }

        public String getAction() {
            return this.mAction;
        }

        public static boolean isPackageAction(String action) {
            for (PackageAction packageAction : values()) {
                if (packageAction.getAction().equals(action)) {
                    return true;
                }
            }
            return false;
        }
    }
}
