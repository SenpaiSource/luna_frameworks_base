package com.android.server.display.feature.ambience.events.event;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import com.android.server.display.feature.ambience.events.utils.EventBase;
import com.android.server.display.feature.ambience.events.utils.EventResult;
import com.android.server.display.feature.ambience.utils.AmbienceLog;
import com.android.server.display.util.OplusDisplayBrightnessBroadcastReceiver;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Calendar;

/* JADX INFO: loaded from: /run/media/sathiya/data/scratch/ambience.dex */
public class TimeEvent extends EventBase {
    private static final String DEFAULT_DATE_FORMAT = "HH:mm:ss";
    public static final int DEFAULT_HOUR_TIME_NORMALIZATION_VALUE = 10000;
    public static final int DEFAULT_MINUTE_TIME_NORMALIZATION_VALUE = 100;
    private static final long DEFAULT_TIME_CHANGED_MESSAGE_DELAY_TIME = 500;
    public static final long DEFAULT_TIME_EIGHT_SECONDS = 8000;
    public static final long DEFAULT_TIME_FIVE_SECONDS = 5000;
    public static final long DEFAULT_TIME_HALF_DAY = 43200000;
    public static final long DEFAULT_TIME_HALF_HOUR = 1800000;
    public static final long DEFAULT_TIME_HALF_MINUTE = 30000;
    public static final long DEFAULT_TIME_HALF_SECONDS = 500;
    public static final long DEFAULT_TIME_ONE_DAY = 86400000;
    public static final long DEFAULT_TIME_ONE_HOUR = 3600000;
    public static final long DEFAULT_TIME_ONE_MINUTE = 60000;
    public static final long DEFAULT_TIME_ONE_SECONDS = 1000;
    public static final long DEFAULT_TIME_TEN_MINUTE = 600000;
    public static final long DEFAULT_TIME_TEN_SECONDS = 10000;
    public static final long DEFAULT_TIME_TWO_HOUR = 7200000;
    public static final long DEFAULT_TIME_TWO_MINUTE = 120000;
    public static final long DEFAULT_TIME_TWO_SECONDS = 2000;
    public static final long DEVICE_BOOT_UP_TIME = System.currentTimeMillis();
    public static final int INVALID_VALUE = -1;
    private static final int MSG_DEBUG_CHANGE = 5470;
    private static final int MSG_DEBUG_FINISH = 5471;
    private static final int MSG_FIRST_INDEX = 5467;
    private static final int MSG_MINUTE_CHANGED = 5469;
    private static final int MSG_REGISTER_CALLBACK = 5468;
    private static final String TAG = "TimeEvent";
    private Context mContext;
    private boolean mIsBootComplete;
    private boolean mIsRegistered;
    private OplusDisplayBrightnessBroadcastReceiver mOplusReceiver;
    private TimeChangeReceiver mTimeChangeReceiver;
    private TimeEventHandler mTimeEventHandler;

    private static class TimeEventHolder {
        private static final TimeEvent INSTANCE = new TimeEvent();

        private TimeEventHolder() {
        }
    }

    public static final TimeEvent getInstance() {
        return TimeEventHolder.INSTANCE;
    }

    private TimeEvent() {
        super(TAG);
        this.mIsRegistered = false;
        this.mIsBootComplete = false;
        this.mContext = null;
        this.mTimeEventHandler = null;
        this.mOplusReceiver = null;
        this.mTimeChangeReceiver = null;
    }

    public void init(Context context, Handler handler) {
        AmbienceLog.f(TAG, "TimeEvent init");
        this.mContext = context;
        this.mTimeEventHandler = new TimeEventHandler(handler.getLooper());
        this.mOplusReceiver = OplusDisplayBrightnessBroadcastReceiver.getInstance();
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
            if (this.mOplusReceiver != null) {
                this.mTimeChangeReceiver = new TimeChangeReceiver();
                this.mOplusReceiver.addListener(Arrays.asList("android.intent.action.TIME_TICK", "android.intent.action.TIME_SET"), this.mTimeChangeReceiver);
                this.mIsRegistered = true;
                AmbienceLog.f(TAG, "register TimeChangeReceiver event result:" + this.mIsRegistered);
            }
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
            if (this.mOplusReceiver != null) {
                this.mOplusReceiver.removeListener(this.mTimeChangeReceiver);
                isUnregisterSuccess = true;
                this.mTimeEventHandler.obtainMessage(MSG_DEBUG_FINISH).sendToTarget();
                AmbienceLog.f(TAG, "unregister TimeChangeReceiver event result:true");
            }
        } catch (Exception e) {
            AmbienceLog.e(TAG, "Oops! Exception on register: " + e.getMessage());
        }
        this.mIsRegistered = !isUnregisterSuccess;
        resetResultData();
        return isUnregisterSuccess ? 1 : 16;
    }

    @Override // com.android.server.display.feature.ambience.events.utils.EventBase
    public void sendRegisterCallback() {
        if ((this.mEventResult == null || this.mIsRegistered) && !this.mIsArcDebugMode) {
            this.mEventResult = new TimeEventResult(TimeEventResult.TIME.CALENDER_TIME.name(), TimeEventResult.TIME.CALENDER_TIME.ordinal());
        }
        this.mTimeEventHandler.obtainMessage(MSG_REGISTER_CALLBACK).sendToTarget();
    }

    private class TimeChangeReceiver implements OplusDisplayBrightnessBroadcastReceiver.Listener {
        private TimeChangeReceiver() {
        }

        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        /* JADX WARN: Code duplicated, block: B:11:0x0020  */
        public void onBroadcastReceive(Context context, Intent intent) {
            byte b;
            String action = intent.getAction();
            switch (action.hashCode()) {
                case -1513032534:
                    if (!action.equals("android.intent.action.TIME_TICK")) {
                        b = -1;
                    } else {
                        b = 1;
                    }
                    break;
                case 505380757:
                    if (!action.equals("android.intent.action.TIME_SET")) {
                        b = -1;
                    } else {
                        b = 0;
                    }
                    break;
                default:
                    b = -1;
                    break;
            }
            switch (b) {
                case 0:
                case 1:
                    TimeEvent.this.handleTimeChange();
                    break;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleTimeChange() {
        Message msg = this.mTimeEventHandler.obtainMessage(MSG_MINUTE_CHANGED);
        msg.obj = TimeEventResult.TIME.CALENDER_TIME;
        this.mTimeEventHandler.removeMessages(MSG_MINUTE_CHANGED);
        this.mTimeEventHandler.sendMessageDelayed(msg, 500L);
    }

    public TimeEventResult createSimulateTimeEventResult(long time) {
        return createSimulateTimeEventResult(time, 0L);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public TimeEventResult createSimulateTimeEventResult(long time, long offset) {
        return new TimeEventResult(time + offset, this.mIsArcDebugMode);
    }

    public static long getSystemMillisTime() {
        return System.currentTimeMillis();
    }

    public static long getDeviceBootUpPastTime() {
        return getSystemMillisTime() - DEVICE_BOOT_UP_TIME;
    }

    public static class TimeEventResult extends EventResult {
        private int mNormalizationTime;

        public enum TIME {
            MILLIS_TIME,
            CALENDER_TIME,
            FORMAT_TIME,
            NORMALIZATION_TIME
        }

        public TimeEventResult(String eventType, int eventId) {
            super(eventType, eventId);
            this.mNormalizationTime = -1;
            switch (TIME.values()[eventId]) {
                case MILLIS_TIME:
                    this.obj = Long.valueOf(System.currentTimeMillis());
                    break;
                case CALENDER_TIME:
                    this.obj = Calendar.getInstance();
                    break;
                case FORMAT_TIME:
                    this.obj = DateTimeFormatter.ofPattern(TimeEvent.DEFAULT_DATE_FORMAT).format(LocalTime.now());
                    break;
                case NORMALIZATION_TIME:
                    this.obj = Integer.valueOf(getNormalizationTimeFromMillis(System.currentTimeMillis()));
                    break;
                default:
                    this.obj = Long.valueOf(System.currentTimeMillis());
                    break;
            }
        }

        public TimeEventResult(long time, boolean isDebug) {
            super(TIME.MILLIS_TIME.name(), TIME.MILLIS_TIME.ordinal());
            this.mNormalizationTime = -1;
            this.obj = Long.valueOf(time);
            this.isDebugResult = isDebug;
        }

        private TimeEventResult(TimeEventResult result) {
            this(result.getCurrentEventType(), result.getCurrentEventId());
            this.obj = result.obj;
            this.isDebugResult = result.isDebugResult;
        }

        public static TimeEventResult copyFrom(TimeEventResult result) {
            if (result == null) {
                return null;
            }
            return new TimeEventResult(result);
        }

        public int getHour() {
            if (getCurrentEventId() == TIME.CALENDER_TIME.ordinal()) {
                return ((Calendar) this.obj).get(11);
            }
            if (getCurrentEventId() == TIME.MILLIS_TIME.ordinal()) {
                long millis = ((Long) this.obj).longValue();
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(millis);
                return calendar.get(11);
            }
            if (getCurrentEventId() == TIME.FORMAT_TIME.ordinal()) {
                try {
                    LocalTime localTime = LocalTime.parse((String) this.obj, DateTimeFormatter.ofPattern(TimeEvent.DEFAULT_DATE_FORMAT));
                    if (localTime != null) {
                        return localTime.getHour();
                    }
                } catch (DateTimeParseException e) {
                    AmbienceLog.e(TimeEvent.TAG, "[DateTimeParseException]getHour:" + e.getMessage());
                }
                return ((int) (System.currentTimeMillis() / TimeEvent.DEFAULT_TIME_ONE_HOUR)) % 24;
            }
            return ((Integer) this.obj).intValue() / TimeEvent.DEFAULT_HOUR_TIME_NORMALIZATION_VALUE;
        }

        public int getMinute() {
            if (getCurrentEventId() == TIME.CALENDER_TIME.ordinal()) {
                Calendar calendar = (Calendar) this.obj;
                return calendar.get(12);
            }
            if (getCurrentEventId() == TIME.MILLIS_TIME.ordinal()) {
                long millis = ((Long) this.obj).longValue();
                return ((int) (millis / TimeEvent.DEFAULT_TIME_ONE_MINUTE)) % 60;
            }
            if (getCurrentEventId() == TIME.FORMAT_TIME.ordinal()) {
                try {
                    LocalTime localTime = LocalTime.parse((String) this.obj, DateTimeFormatter.ofPattern(TimeEvent.DEFAULT_DATE_FORMAT));
                    if (localTime != null) {
                        return localTime.getMinute();
                    }
                } catch (DateTimeParseException e) {
                    AmbienceLog.e(TimeEvent.TAG, "[DateTimeParseException]getMinute:" + e.getMessage());
                }
                return ((int) (System.currentTimeMillis() / TimeEvent.DEFAULT_TIME_ONE_MINUTE)) % 60;
            }
            return (((Integer) this.obj).intValue() % TimeEvent.DEFAULT_HOUR_TIME_NORMALIZATION_VALUE) / 100;
        }

        public int getSecond() {
            if (getCurrentEventId() == TIME.CALENDER_TIME.ordinal()) {
                Calendar calendar = (Calendar) this.obj;
                return calendar.get(13);
            }
            if (getCurrentEventId() == TIME.MILLIS_TIME.ordinal()) {
                long millis = ((Long) this.obj).longValue();
                return ((int) (millis / 1000)) % 60;
            }
            if (getCurrentEventId() == TIME.FORMAT_TIME.ordinal()) {
                try {
                    LocalTime localTime = LocalTime.parse((String) this.obj, DateTimeFormatter.ofPattern(TimeEvent.DEFAULT_DATE_FORMAT));
                    if (localTime != null) {
                        return localTime.getSecond();
                    }
                } catch (DateTimeParseException e) {
                    AmbienceLog.e(TimeEvent.TAG, "[DateTimeParseException]getSecond:" + e.getMessage());
                }
                return ((int) (System.currentTimeMillis() / 1000)) % 60;
            }
            return ((Integer) this.obj).intValue() % 100;
        }

        public int getNormalizationTime() {
            if (this.mNormalizationTime == -1) {
                if (getCurrentEventId() == TIME.NORMALIZATION_TIME.ordinal()) {
                    this.mNormalizationTime = ((Integer) this.obj).intValue();
                } else {
                    this.mNormalizationTime = getNormalizationTime(getHour(), getMinute(), getSecond());
                }
            }
            return this.mNormalizationTime;
        }

        public String getNormalizationTimeFormat() {
            if (getCurrentEventId() == TIME.FORMAT_TIME.ordinal()) {
                return (String) this.obj;
            }
            return DateTimeFormatter.ofPattern(TimeEvent.DEFAULT_DATE_FORMAT).format(LocalTime.of(getHour(), getMinute(), getSecond()));
        }

        public static int getNormalizationTime(int hour, int minute, int second) {
            return (hour * TimeEvent.DEFAULT_HOUR_TIME_NORMALIZATION_VALUE) + (minute * 100) + second;
        }

        public static int getNormalizationTimeFromCalendar(Calendar calendar) {
            return getNormalizationTime(calendar.get(11), calendar.get(12), calendar.get(13));
        }

        public static int getNormalizationTimeFromMillis(long millis) {
            int totalSeconds = (int) (millis / 1000);
            int totalMinutes = (int) (millis / TimeEvent.DEFAULT_TIME_ONE_MINUTE);
            int totalHours = (int) (millis / TimeEvent.DEFAULT_TIME_ONE_HOUR);
            return getNormalizationTime(totalHours % 24, totalMinutes % 60, totalSeconds % 60);
        }

        @Override // com.android.server.display.feature.ambience.events.utils.EventResult
        public String toString() {
            StringBuffer sb = new StringBuffer(TimeEvent.TAG);
            sb.append("{");
            sb.append("event:").append(getCurrentEventType());
            sb.append(" ");
            sb.append("id:").append(getCurrentEventId());
            sb.append("}");
            return sb.toString();
        }
    }

    private class TimeEventHandler extends Handler {
        private final String TAG;

        public TimeEventHandler(Looper looper) {
            super(looper);
            this.TAG = "TimeEventHandler";
            AmbienceLog.f("TimeEventHandler", "TimeEventHandler init");
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case TimeEvent.MSG_REGISTER_CALLBACK /* 5468 */:
                    if (TimeEvent.this.mEventResult != null) {
                        AmbienceLog.d("TimeEventHandler", "MSG_REGISTER_CALLBACK, event = " + TimeEvent.this.mEventResult);
                        TimeEvent.this.sendEventCallBack(TimeEvent.this.mEventResult);
                    }
                    break;
                case TimeEvent.MSG_MINUTE_CHANGED /* 5469 */:
                    TimeEventResult.TIME time = (TimeEventResult.TIME) msg.obj;
                    TimeEvent.this.mLastEventResult = TimeEvent.this.mEventResult;
                    TimeEvent.this.mEventResult = new TimeEventResult(time.name(), time.ordinal());
                    if (!TimeEvent.this.mIsArcDebugMode) {
                        AmbienceLog.d("TimeEventHandler", "MSG_MINUTE_CHANGED, event = " + TimeEvent.this.mEventResult);
                        TimeEvent.this.sendEventCallBack(TimeEvent.this.mEventResult);
                    }
                    break;
                case TimeEvent.MSG_DEBUG_CHANGE /* 5470 */:
                    TimeEvent.this.mEventResult = (EventResult) msg.obj;
                    TimeEvent.this.sendEventCallBack(TimeEvent.this.mEventResult);
                    Message nextMsg = TimeEvent.this.mTimeEventHandler.obtainMessage(TimeEvent.MSG_DEBUG_CHANGE);
                    nextMsg.obj = TimeEvent.this.createSimulateTimeEventResult(((Long) TimeEvent.this.mEventResult.obj).longValue(), TimeEvent.DEFAULT_TIME_ONE_MINUTE);
                    sendMessageDelayed(nextMsg, TimeEvent.DEFAULT_TIME_ONE_MINUTE);
                    break;
                case TimeEvent.MSG_DEBUG_FINISH /* 5471 */:
                    TimeEvent.this.mTimeEventHandler.removeMessages(TimeEvent.MSG_DEBUG_CHANGE);
                    TimeEvent.this.mIsArcDebugMode = false;
                    TimeEvent.this.mEventResult = null;
                    TimeEvent.this.sendRegisterCallback();
                    break;
            }
        }
    }

    @Override // com.android.server.display.feature.ambience.events.utils.EventBase
    public void onEventChange(EventResult result) {
        AmbienceLog.d(TAG, "onEventChange:" + result);
        this.mIsArcDebugMode = true;
        Message msg = this.mTimeEventHandler.obtainMessage(MSG_DEBUG_CHANGE);
        msg.obj = result;
        msg.sendToTarget();
    }

    @Override // com.android.server.display.feature.ambience.events.utils.EventBase
    public void onDebugFinish() {
        AmbienceLog.d(TAG, "onDebugFinish");
        this.mTimeEventHandler.obtainMessage(MSG_DEBUG_FINISH).sendToTarget();
    }
}
