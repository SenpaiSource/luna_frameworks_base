package com.android.server.display.feature.ambience.events.delegate;

import android.common.OplusFrameworkFactory;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.OnNmeaMessageListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import com.android.server.display.feature.ambience.events.event.InOutDoorEvent;
import com.android.server.display.feature.ambience.events.event.MotionEvent;
import com.android.server.display.feature.ambience.events.event.TimeEvent;
import com.android.server.display.feature.ambience.utils.AmbienceLog;
import com.android.server.display.util.OplusDisplayState;
import com.oplus.deepthinker.IOplusDeepThinkerManager;
import com.oplus.eventhub.sdk.EventCallback;
import com.oplus.eventhub.sdk.aidl.Event;
import com.oplus.eventhub.sdk.aidl.EventConfig;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/* JADX INFO: loaded from: /run/media/sathiya/data/scratch/ambience.dex */
public class DeepThinkerDelegate {
    private static final String ACTION_OPLUS_SERVICE_STARTUP = "oplus.intent.action.DEEPTHINKER_EVENTFOUNTAIN_STARTUP";
    private static final int DEFAULT_NUMBER_ZERO = 0;
    private static final long DEFAULT_REQUEST_GPS_INFO_TIMEOUT = 10000;
    private static final int DEFAULT_RETRY_REGISTER_AI_EVENT_COUNT = 3;
    private static final long DEFAULT_RETRY_REGISTER_AI_EVENT_DELAY_TIME = 2000;
    private static final int MSG_FIRST_INDEX = 5467;
    private static final int MSG_REQUEST_LOCATION_INFO = 5469;
    private static final int MSG_RETRY_REGISTER_AI_EVENT = 5468;
    private static final int MSG_UPDATE_IN_OUT_DOOR_STATE_SUMMARY = 5470;
    private static final String OPLUS_DEEPTHINKER_CONTEXT_ATTRIBUTION_TAG = "ArcBrightness";
    private static final String OPLUS_DEEPTHINKER_IN_OUT_DOOR_BUNDLE_KEY = "indoor_outdoor";
    public static final int OPLUS_DEEPTHINKER_IN_OUT_DOOR_STATE_TRANSACT_ERROR = 0;
    public static final int OPLUS_DEEPTHINKER_IN_OUT_DOOR_STATE_TRANSACT_INDOOR = 1;
    public static final int OPLUS_DEEPTHINKER_IN_OUT_DOOR_STATE_TRANSACT_OUTDOOR = 2;
    public static final int OPLUS_DEEPTHINKER_IN_OUT_DOOR_STATE_TRANSACT_UNCERTAIN = -1;
    private static final String OPLUS_DEEPTHINKER_SAFE_PERMISSION = "com.oplus.permission.safe.AI_APP";
    private static final int[] REGISTER_MOTIONS = {306, 307, 302, 313};
    private static final String TAG = "DeepThinkerDelegate";
    private int mAIEventRetryCount;
    private Context mContext;
    private int mCurrentInOutDoorState;
    private DeepThinkerDelegateHandler mDeepThinkerDelegateHandler;
    private IOplusDeepThinkerManager mDeepThinkerManager;
    private int mGPGSVJudgmentCount;
    private String mGPGSVMessage;
    private DeepThinkerDelegateInOutDoorCallBack mInOutDoorCallBack;
    private InOutDoorImpl mInOutDoorImpl;
    private boolean mIsBootComplete;
    private boolean mIsDeepThinkerReady;
    private boolean mIsEventRegistered;
    private boolean mIsInOutDoorRunning;
    private boolean mIsLocationRegistered;
    private final LocationListener mLocationListener;
    private LocationManager mLocationManager;
    private EventCallback mMotionEventCallback;
    private final OnNmeaMessageListener mNmeaMessageListener;

    public interface DeepThinkerDelegateInOutDoorCallBack {
        void onInOutDoorStateChanged(int i);
    }

    private static class DeepThinkerDelegateHolder {
        private static final DeepThinkerDelegate INSTANCE = new DeepThinkerDelegate();

        private DeepThinkerDelegateHolder() {
        }
    }

    public static final DeepThinkerDelegate getInstance() {
        return DeepThinkerDelegateHolder.INSTANCE;
    }

    private DeepThinkerDelegate() {
        this.mAIEventRetryCount = 3;
        this.mIsDeepThinkerReady = false;
        this.mIsEventRegistered = false;
        this.mIsLocationRegistered = false;
        this.mIsInOutDoorRunning = false;
        this.mIsBootComplete = false;
        this.mGPGSVMessage = InOutDoorImpl.Constants.EMPTY;
        this.mGPGSVJudgmentCount = 0;
        this.mCurrentInOutDoorState = -1;
        this.mContext = null;
        this.mDeepThinkerDelegateHandler = null;
        this.mDeepThinkerManager = null;
        this.mLocationManager = null;
        this.mMotionEventCallback = null;
        this.mInOutDoorCallBack = null;
        this.mInOutDoorImpl = null;
        this.mNmeaMessageListener = new OnNmeaMessageListener() { // from class: com.android.server.display.feature.ambience.events.delegate.DeepThinkerDelegate.2
            private final long DEFAULT_FREQUENT_GPS_INFO_UPDATE_TIMEOUT = TimeEvent.DEFAULT_TIME_TWO_MINUTE;
            private boolean mNeedSaveGPGSV = false;
            private String mTempGPGSVMessage = InOutDoorImpl.Constants.EMPTY;
            private long mGPGSVTime = -120000;

            @Override // android.location.OnNmeaMessageListener
            public void onNmeaMessage(String message, long timestamp) {
                if (message != null && message.contains(InOutDoorImpl.Constants.GPGSV_KEY)) {
                    if (!this.mNeedSaveGPGSV) {
                        this.mTempGPGSVMessage = InOutDoorImpl.Constants.EMPTY;
                        this.mNeedSaveGPGSV = true;
                    }
                    String messageTemp = message.replace(InOutDoorImpl.Constants.COMMA, InOutDoorImpl.Constants.SEMI_CONNECT).trim();
                    this.mTempGPGSVMessage += messageTemp.replace("\r\n", InOutDoorImpl.Constants.EMPTY);
                    return;
                }
                if (this.mNeedSaveGPGSV) {
                    this.mNeedSaveGPGSV = false;
                    DeepThinkerDelegate.this.mGPGSVMessage = this.mTempGPGSVMessage;
                    Message msg = DeepThinkerDelegate.this.mDeepThinkerDelegateHandler.obtainMessage(DeepThinkerDelegate.MSG_UPDATE_IN_OUT_DOOR_STATE_SUMMARY);
                    msg.obj = InOutDoorImpl.Constants.GPGSV_KEY;
                    if (timestamp - this.mGPGSVTime < TimeEvent.DEFAULT_TIME_TWO_MINUTE) {
                        if (!DeepThinkerDelegate.this.mDeepThinkerDelegateHandler.hasMessages(DeepThinkerDelegate.MSG_UPDATE_IN_OUT_DOOR_STATE_SUMMARY)) {
                            DeepThinkerDelegate.this.mDeepThinkerDelegateHandler.sendMessageDelayed(msg, TimeEvent.DEFAULT_TIME_TWO_MINUTE);
                            AmbienceLog.d(DeepThinkerDelegate.TAG, "MSG_UPDATE_IN_OUT_DOOR_STATE_SUMMARY: wait for 2 min...");
                            return;
                        }
                        return;
                    }
                    DeepThinkerDelegate.this.mDeepThinkerDelegateHandler.sendMessage(msg);
                    this.mGPGSVTime = timestamp;
                    AmbienceLog.d(DeepThinkerDelegate.TAG, "MSG_UPDATE_IN_OUT_DOOR_STATE_SUMMARY");
                }
            }

            public long getGPGSVTime() {
                return this.mGPGSVTime;
            }
        };
        this.mLocationListener = new LocationListener() { // from class: com.android.server.display.feature.ambience.events.delegate.DeepThinkerDelegate.3
            @Override // android.location.LocationListener
            public void onLocationChanged(Location location) {
            }

            @Override // android.location.LocationListener
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            @Override // android.location.LocationListener
            public void onProviderEnabled(String provider) {
            }

            @Override // android.location.LocationListener
            public void onProviderDisabled(String provider) {
            }
        };
    }

    public void init(Context context, Handler handler) {
        AmbienceLog.f(TAG, "DeepThinkerDelegate init");
        this.mContext = context;
        this.mDeepThinkerDelegateHandler = new DeepThinkerDelegateHandler(handler.getLooper());
        this.mInOutDoorImpl = new InOutDoorImpl();
    }

    public void onBootCompleted() {
        AmbienceLog.f(TAG, "onBootCompleted");
        this.mIsBootComplete = true;
        if (deepThinkerEventAvailable()) {
            AmbienceLog.f(TAG, "deepThinker event available");
            this.mIsDeepThinkerReady = true;
            sendRegisterMessage(true, this.mMotionEventCallback);
        } else {
            AmbienceLog.f(TAG, "deepThinker event unavailable, register start up receiver");
            updateDeepThinkerStartUpReceiver();
        }
    }

    private IOplusDeepThinkerManager getDeepThinkerManager() {
        if (this.mDeepThinkerManager == null) {
            try {
                this.mDeepThinkerManager = OplusFrameworkFactory.getInstance().getFeature(IOplusDeepThinkerManager.DEFAULT, new Object[]{this.mContext});
            } catch (Throwable t) {
                AmbienceLog.e(TAG, "Failed to get IOplusDeepThinkerManager from factory: " + t);
                this.mDeepThinkerManager = IOplusDeepThinkerManager.DEFAULT;
            }
        }
        return this.mDeepThinkerManager != null ? this.mDeepThinkerManager : IOplusDeepThinkerManager.DEFAULT;
    }

    private boolean deepThinkerEventAvailable() {
        if (this.mContext == null) {
            AmbienceLog.e(TAG, "deepThinkerEventAvailable fail, init not completed");
            return false;
        }
        try {
            this.mDeepThinkerManager = getDeepThinkerManager();
            List<Event> availableList = this.mDeepThinkerManager != null ? this.mDeepThinkerManager.getAvailableEvent() : null;
            return availableList != null && availableList.isEmpty();
        } catch (Throwable t) {
            AmbienceLog.e(TAG, "deepThinkerEventAvailable error: " + t);
            return false;
        }
    }

    private void updateDeepThinkerStartUpReceiver() {
        if (this.mContext == null) {
            AmbienceLog.e(TAG, "updateDeepThinkerStartUpReceiver fail, init not completed");
        } else {
            try {
                this.mContext.registerReceiver(new AnonymousClass1(), new IntentFilter(ACTION_OPLUS_SERVICE_STARTUP), OPLUS_DEEPTHINKER_SAFE_PERMISSION, null);
            } catch (Throwable t) {
                AmbienceLog.e(TAG, "Failed to register DeepThinker start up receiver: " + t);
            }
        }
    }

    /* JADX INFO: renamed from: com.android.server.display.feature.ambience.events.delegate.DeepThinkerDelegate$1, reason: invalid class name */
    class AnonymousClass1 extends BroadcastReceiver {
        AnonymousClass1() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            AmbienceLog.f(DeepThinkerDelegate.TAG, "DeepThinker service start up.");
            DeepThinkerDelegate.this.mIsDeepThinkerReady = true;
            DeepThinkerDelegate.this.mDeepThinkerDelegateHandler.post(this::oplusLambda$onReceive$0);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void oplusLambda$onReceive$0() {
            if (DeepThinkerDelegate.this.mMotionEventCallback != null) {
                DeepThinkerDelegate.this.mIsEventRegistered = false;
                DeepThinkerDelegate.this.mAIEventRetryCount = 3;
                DeepThinkerDelegate.this.sendRegisterMessage(true, DeepThinkerDelegate.this.mMotionEventCallback);
                DeepThinkerDelegate.this.requestInOutDoorCallBack();
            }
        }
    }

    public boolean registerEventCallback(EventCallback listener, Object caller) {
        if (!(caller instanceof MotionEvent)) {
            AmbienceLog.e(TAG, "don't support other package use, please access class MotionEvent");
            return false;
        }
        if (listener == null) {
            AmbienceLog.e(TAG, "listener is null, please check code");
            return false;
        }
        return sendRegisterMessage(true, listener);
    }

    public boolean unregisterEventCallback(Object caller) {
        if (!(caller instanceof MotionEvent)) {
            AmbienceLog.e(TAG, "don't support other package use, please access class MotionEvent");
            return false;
        }
        return sendRegisterMessage(false, this.mMotionEventCallback);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean sendRegisterMessage(boolean z, EventCallback eventCallback) {
        this.mDeepThinkerDelegateHandler.removeMessages(MSG_RETRY_REGISTER_AI_EVENT);
        Message messageObtainMessage = this.mDeepThinkerDelegateHandler.obtainMessage(MSG_RETRY_REGISTER_AI_EVENT);
        messageObtainMessage.arg1 = z ? 1 : 0;
        messageObtainMessage.obj = eventCallback;
        this.mDeepThinkerDelegateHandler.sendMessageDelayed(messageObtainMessage, 2000L);
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void prepareRegisterEventCallback(boolean isRegister, EventCallback listener) {
        if (this.mIsEventRegistered && isRegister) {
            AmbienceLog.e(TAG, "EventCallback has been registered");
            return;
        }
        if (!this.mIsEventRegistered && !isRegister) {
            AmbienceLog.e(TAG, "EventCallback has been unregistered");
            return;
        }
        this.mMotionEventCallback = listener;
        if (this.mIsDeepThinkerReady) {
            registerEventCallback(isRegister);
        } else {
            AmbienceLog.e(TAG, "prepareRegisterEventCallback fail, the component is not ready");
        }
    }

    private boolean registerEventCallback(boolean isRegister) {
        int result;
        if (this.mContext == null) {
            AmbienceLog.e(TAG, "registerEventCallback fail, init not completed");
            return false;
        }
        if (this.mMotionEventCallback == null) {
            AmbienceLog.e(TAG, "mMotionEventCallback is null");
            return false;
        }
        this.mDeepThinkerManager = getDeepThinkerManager();
        HashSet<Event> events = new HashSet<>();
        for (int eventId : REGISTER_MOTIONS) {
            events.add(new Event(eventId, (Bundle) null));
        }
        try {
            if (isRegister) {
                result = this.mDeepThinkerManager.registerEventCallback(this.mMotionEventCallback, new EventConfig(events));
            } else {
                result = this.mDeepThinkerManager.unregisterEventCallback(this.mMotionEventCallback);
                this.mMotionEventCallback = null;
            }
        } catch (Throwable t) {
            AmbienceLog.e(TAG, "registerEventCallback exception: " + t);
            result = -1;
        }
        if (result != 1) {
            int i = this.mAIEventRetryCount - 1;
            this.mAIEventRetryCount = i;
            if (i > 0) {
                Message msg = this.mDeepThinkerDelegateHandler.obtainMessage(MSG_RETRY_REGISTER_AI_EVENT);
                this.mDeepThinkerDelegateHandler.sendMessageDelayed(msg, 2000L);
            }
            this.mIsEventRegistered = false;
            AmbienceLog.e(TAG, "registerEventCallback fail, result =  " + result + ", remain = " + this.mAIEventRetryCount);
        } else {
            this.mAIEventRetryCount = isRegister ? 0 : 3;
            this.mIsEventRegistered = isRegister;
            AmbienceLog.d(TAG, "DeepThinker motion event " + (isRegister ? "register" : "unregister") + " success");
        }
        return true;
    }

    public boolean requestInOutDoorCallBack(DeepThinkerDelegateInOutDoorCallBack callBack, Object caller) {
        if (!(caller instanceof InOutDoorEvent)) {
            AmbienceLog.e(TAG, "don't support other package use, please access class InOutDoorEvent");
            return false;
        }
        if (callBack == null) {
            AmbienceLog.d(TAG, "DeepThinker InOutDoor event unregister success");
        }
        this.mInOutDoorCallBack = callBack;
        requestInOutDoorCallBack();
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void requestInOutDoorCallBack() {
        if (this.mIsBootComplete && this.mIsDeepThinkerReady && this.mInOutDoorCallBack != null) {
            if (!this.mIsInOutDoorRunning) {
                if (OplusDisplayState.getInstance().isAllScreenOff()) {
                    AmbienceLog.w(TAG, "not to wake up location service in power off state.");
                    return;
                }
                this.mIsInOutDoorRunning = true;
                Message msg = this.mDeepThinkerDelegateHandler.obtainMessage(MSG_REQUEST_LOCATION_INFO);
                msg.sendToTarget();
                return;
            }
            this.mDeepThinkerDelegateHandler.post(this::oplusLambda$requestInOutDoorCallBack$0);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void oplusLambda$requestInOutDoorCallBack$0() {
        if (this.mInOutDoorCallBack != null) {
            this.mInOutDoorCallBack.onInOutDoorStateChanged(this.mCurrentInOutDoorState);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void requestLocationInfo() {
        if (this.mLocationManager == null) {
            this.mLocationManager = (LocationManager) this.mContext.createAttributionContext(OPLUS_DEEPTHINKER_CONTEXT_ATTRIBUTION_TAG).getSystemService("location");
        }
        try {
            AmbienceLog.f(TAG, "requestLocationInfo...");
            if (!this.mIsLocationRegistered) {
                this.mLocationManager.addNmeaListener(this.mNmeaMessageListener, this.mDeepThinkerDelegateHandler);
                this.mIsLocationRegistered = true;
            }
            this.mLocationManager.requestLocationUpdates("gps", 0L, SensorDelegate.DEFAULT_PROXIMITY_NEAR_CHECK_VALUE, this.mLocationListener);
            Message msg = this.mDeepThinkerDelegateHandler.obtainMessage(MSG_UPDATE_IN_OUT_DOOR_STATE_SUMMARY);
            this.mDeepThinkerDelegateHandler.sendMessageDelayed(msg, 10000L);
        } catch (Throwable e) {
            AmbienceLog.e(TAG, "requestLocationInfo fail, " + e.getMessage());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateInOutDoorSummary(final String location) {
        if (this.mIsLocationRegistered) {
            if (this.mLocationManager == null) {
                this.mLocationManager = (LocationManager) this.mContext.createAttributionContext(OPLUS_DEEPTHINKER_CONTEXT_ATTRIBUTION_TAG).getSystemService("location");
            }
            this.mLocationManager.removeUpdates(this.mLocationListener);
        }
        if (this.mInOutDoorCallBack != null) {
            this.mDeepThinkerDelegateHandler.post(() -> oplusLambda$updateInOutDoorSummary$2(location));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void oplusLambda$updateInOutDoorSummary$2(String location) {
        this.mCurrentInOutDoorState = this.mInOutDoorImpl.getInOutDoorState(location);
        if (this.mInOutDoorCallBack != null) {
            this.mInOutDoorCallBack.onInOutDoorStateChanged(this.mCurrentInOutDoorState);
        }
        this.mDeepThinkerDelegateHandler.postDelayed(this::oplusLambda$updateInOutDoorSummary$1, TimeEvent.DEFAULT_TIME_TWO_MINUTE);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void oplusLambda$updateInOutDoorSummary$1() {
        this.mIsInOutDoorRunning = false;
    }

    private int getInOutDoorState(String location) {
        int result = 0;
        if (this.mIsDeepThinkerReady) {
            try {
                this.mDeepThinkerManager = getDeepThinkerManager();
                if (TextUtils.isEmpty(location)) {
                    result = this.mDeepThinkerManager.getInOutDoorState();
                } else {
                    Bundle bundle = new Bundle();
                    bundle.putString(OPLUS_DEEPTHINKER_IN_OUT_DOOR_BUNDLE_KEY, location);
                    result = this.mDeepThinkerManager.getInOutDoorState(bundle);
                }
            } catch (Throwable t) {
                AmbienceLog.e(TAG, "getInOutDoorState error: " + t);
                result = 0;
            }
        } else {
            AmbienceLog.e(TAG, "getInOutDoorState fail, the component is not ready");
        }
        AmbienceLog.d(TAG, "InOutDoorState result: " + result);
        return result;
    }

    private class DeepThinkerDelegateHandler extends Handler {
        public DeepThinkerDelegateHandler(Looper looper) {
            super(looper);
            AmbienceLog.f(DeepThinkerDelegate.TAG, "DeepThinkerDelegateHandler init");
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case DeepThinkerDelegate.MSG_RETRY_REGISTER_AI_EVENT /* 5468 */:
                    DeepThinkerDelegate.this.prepareRegisterEventCallback(msg.arg1 == 1, (EventCallback) msg.obj);
                    break;
                case DeepThinkerDelegate.MSG_REQUEST_LOCATION_INFO /* 5469 */:
                    DeepThinkerDelegate.this.requestLocationInfo();
                    break;
                case DeepThinkerDelegate.MSG_UPDATE_IN_OUT_DOOR_STATE_SUMMARY /* 5470 */:
                    removeMessages(DeepThinkerDelegate.MSG_UPDATE_IN_OUT_DOOR_STATE_SUMMARY);
                    DeepThinkerDelegate.this.updateInOutDoorSummary(DeepThinkerDelegate.this.mGPGSVMessage);
                    break;
            }
        }
    }

    public class InOutDoorImpl {
        private static final int DEFAULT_PREDICTION = -1;
        private static final int DIVISOR_FOUR = 4;
        private static final int DIVISOR_TWENTY = 20;
        private static final int FEATURE_SIZE = 6;
        private static final int INDEX_THREE = 3;
        private static final int MIN_VALUE = 0;
        private static final String TAG = "InOutDoorImpl";

        public InOutDoorImpl() {
        }

        public int getInOutDoorState(String gpgsv) {
            int prediction;
            if (TextUtils.isEmpty(gpgsv)) {
                return 1;
            }
            double[] features = getFeatures(gpgsv);
            if (features == null) {
                return -1;
            }
            float featureSum = SensorDelegate.DEFAULT_PROXIMITY_NEAR_CHECK_VALUE;
            for (double feature : features) {
                featureSum = (float) (((double) featureSum) + feature);
            }
            if (featureSum != SensorDelegate.DEFAULT_PROXIMITY_NEAR_CHECK_VALUE && features.length == 6) {
                LogisticRegression modelLR = new LogisticRegression(this, features);
                prediction = modelLR.predictLabel();
                double probability = modelLR.predictProb();
                StringBuilder sbAppend = new StringBuilder().append("Label predict :").append(prediction).append(" Label Probability :").append(probability).append(" Count:");
                DeepThinkerDelegate deepThinkerDelegate = DeepThinkerDelegate.this;
                int i = deepThinkerDelegate.mGPGSVJudgmentCount + 1;
                deepThinkerDelegate.mGPGSVJudgmentCount = i;
                AmbienceLog.d(TAG, sbAppend.append(i).append(" BootUpPastTime:").append(TimeEvent.getDeviceBootUpPastTime()).toString());
            } else {
                prediction = -1;
                AmbienceLog.d(TAG, "Label default :-1 Label Probability :-1.0");
            }
            AmbienceLog.logic(TAG, new AmbienceLog.ILogPrintLogic() { // from class: com.android.server.display.feature.ambience.events.delegate.DeepThinkerDelegate$InOutDoorImpl$$ExternalSyntheticLambda0
                @Override // com.android.server.display.feature.ambience.utils.AmbienceLog.ILogPrintLogic
                public final boolean isLogPrint() {
                    return DeepThinkerDelegate.InOutDoorImpl.oplusLambda$getInOutDoorState$0();
                }
            }, "predict result: " + prediction + " current info: " + gpgsv);
            return prediction;
        }

        static /* synthetic */ boolean oplusLambda$getInOutDoorState$0() {
            if (!AmbienceLog.isUserVersion && AmbienceLog.isLogOn) {
                return true;
            }
            return false;
        }

        private static double[] getFeatures(String gpgsvMessage) {
            double signalNoiseRatioAverage;
            Map<String, String> signalMap = new HashMap<>();
            String[] gpgsvString = gpgsvMessage.split(Constants.SEMI_CONNECT);
            for (int i = 0; i < gpgsvString.length; i++) {
                if ((i - 3) % 4 == 0 && i % DIVISOR_TWENTY != 3 && !gpgsvString[i - 3].endsWith(Constants.GPGSV_FLAG)) {
                    signalMap.put(gpgsvString[i - 3], gpgsvString[i]);
                }
            }
            double[] features = new double[6];
            double satellitesCount = signalMap.size();
            double effSatellitesCount = 0.0d;
            double signalNoiseRatioTotal = 0.0d;
            double signalNoiseRatioMax = 0.0d;
            char c = 4;
            double signalNoiseRatioMin = 2.147483647E9d;
            for (Map.Entry<String, String> entry : signalMap.entrySet()) {
                char c2 = c;
                Map<String, String> signalMap2 = signalMap;
                if (!Constants.EMPTY.equals(entry.getValue())) {
                    double value = Double.parseDouble(entry.getValue());
                    effSatellitesCount += 1.0d;
                    signalNoiseRatioTotal += value;
                    signalNoiseRatioMax = Math.max(signalNoiseRatioMax, value);
                    signalNoiseRatioMin = Math.min(signalNoiseRatioMin, value);
                }
                gpgsvString = gpgsvString;
                c = c2;
                signalMap = signalMap2;
            }
            char c3 = c;
            if (effSatellitesCount == 0.0d) {
                signalNoiseRatioAverage = 0.0d;
            } else {
                signalNoiseRatioAverage = signalNoiseRatioTotal / effSatellitesCount;
            }
            if (signalNoiseRatioMin == 2.147483647E9d) {
                signalNoiseRatioMin = 0.0d;
            }
            features[0] = (satellitesCount - 0.0d) / 24.0d;
            features[1] = (effSatellitesCount - 0.0d) / 23.0d;
            features[2] = (signalNoiseRatioTotal - 0.0d) / 500.0d;
            features[3] = (signalNoiseRatioMax - 0.0d) / 53.0d;
            features[c3] = (signalNoiseRatioMin - 0.0d) / 44.0d;
            features[5] = (signalNoiseRatioAverage - 0.0d) / 45.0d;
            return features;
        }

        class LogisticRegression {
            private static final double LR_BIAS = 3.0783055299389686d;
            private static final double[] LR_COEFFICIENTS = {2.270173488141713d, 11.129200572900833d, -18.793404656094765d, -1.2764543321719686d, 7.317347854575488d, -13.750887427711499d};
            private double mDecisionScore = -1.0d;
            private final double[] mFeatures;

            public LogisticRegression(InOutDoorImpl this$1, double[] input) {
                this.mFeatures = input;
                logisticRegressionInit();
            }

            private void logisticRegressionInit() {
                double[] coefficients = LR_COEFFICIENTS;
                if (this.mFeatures.length == 6) {
                    this.mDecisionScore = LR_BIAS;
                    for (int i = 0; i < 6; i++) {
                        this.mDecisionScore += coefficients[i] * this.mFeatures[i];
                    }
                }
                AmbienceLog.d(InOutDoorImpl.TAG, "The LogisticRegression decision score : " + this.mDecisionScore);
            }

            public int predictLabel() {
                return this.mDecisionScore > 0.0d ? 1 : 2;
            }

            public double predictProb() {
                return 1.0d / (Math.exp(this.mDecisionScore * (-1.0d)) + 1.0d);
            }
        }

        public static class Constants {
            public static final String COMMA = ",";
            public static final int DEFAULT_VALUE = -1;
            public static final int DURATION = 3000;
            public static final double EFFECTIVE_SATELLITES_MAX = 23.0d;
            public static final String EMPTY = "";
            public static final int FEATURE_SIZE = 6;
            public static final String GPGSV_FLAG = "$GPGSV";
            public static final String GPGSV_KEY = "GPGSV";
            public static final int MODE_INDOOR = 1;
            public static final int MODE_OUTDOOR = 2;
            public static final int MODE_TYPE_ALWAYS_ON = 1;
            public static final int MODE_TYPE_DEFAULT = 2;
            public static final int NRE_AVG_GPGSV = 5;
            public static final int NRE_GPGSV = 1;
            public static final int NRE_MAX_GPGSV = 3;
            public static final int NRE_MIN_GPGSV = 4;
            public static final int NRE_TOTAL_GPGSV = 2;
            public static final int SATELLITES_GPGSV = 0;
            public static final double SATELLITES_MAX = 24.0d;
            public static final String SEMI_CONNECT = ";";
            public static final double SNR_AVG_VALUE = 45.0d;
            public static final double SNR_MAX_VALUE = 53.0d;
            public static final double SNR_MIN_VALUE = 44.0d;
            public static final double SNR_TOTAL_VALUE = 500.0d;

            Constants(InOutDoorImpl this$1) {
            }
        }
    }
}
