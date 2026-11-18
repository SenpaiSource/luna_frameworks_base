package com.oplus.eventhub.sdk.aidl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class EventConfig {
    private List<Object> mEvents;

    public EventConfig(Collection<?> events) {
        this.mEvents = events != null ? new ArrayList<>(events) : new ArrayList<>();
    }

    public List<Object> getEvents() {
        return this.mEvents;
    }
}
