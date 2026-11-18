package com.oplus.eventhub.sdk.aidl;

public class DeviceEventResult {
    private int mEventId;
    private int mEventType;
    private int mEventStateType;

    public DeviceEventResult(int eventId, int eventType, int eventStateType) {
        this.mEventId = eventId;
        this.mEventType = eventType;
        this.mEventStateType = eventStateType;
    }

    public int getEventId() {
        return this.mEventId;
    }

    public int getEventType() {
        return this.mEventType;
    }

    public int getEventStateType() {
        return this.mEventStateType;
    }
}
