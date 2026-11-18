package com.oplus.eventhub.sdk.aidl;

import android.os.Bundle;

public class Event {
    private int mEventId;
    private Bundle mBundle;

    public Event(int eventId) {
        this.mEventId = eventId;
    }

    public Event(int eventId, Bundle bundle) {
        this.mEventId = eventId;
        this.mBundle = bundle;
    }

    public int getEventId() {
        return this.mEventId;
    }

    public Bundle getBundle() {
        return this.mBundle;
    }
}
