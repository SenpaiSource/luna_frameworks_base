package com.android.server.display.feature.ambience.events.utils;

/* JADX INFO: loaded from: /run/media/sathiya/data/scratch/ambience.dex */
public abstract class EventResult {
    private final int eventId;
    private final String eventType;
    public boolean isDebugResult = false;
    public Object obj = null;

    public abstract String toString();

    public EventResult(String eventType, int eventId) {
        this.eventType = eventType;
        this.eventId = eventId;
    }

    public String getCurrentEventType() {
        return this.eventType;
    }

    public int getCurrentEventId() {
        return this.eventId;
    }

    public boolean objectIsNull() {
        return this.obj == null;
    }

    public boolean isDebugResult() {
        return this.isDebugResult;
    }
}
