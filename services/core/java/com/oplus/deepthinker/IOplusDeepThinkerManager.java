package com.oplus.deepthinker;

import android.os.Bundle;
import com.oplus.eventhub.sdk.EventCallback;
import com.oplus.eventhub.sdk.aidl.Event;
import com.oplus.eventhub.sdk.aidl.EventConfig;
import java.util.ArrayList;
import java.util.List;

public interface IOplusDeepThinkerManager {
    IOplusDeepThinkerManager DEFAULT = new IOplusDeepThinkerManager() {};

    default List<Event> getAvailableEvent() {
        return new ArrayList<>();
    }

    default int registerEventCallback(EventCallback callback, EventConfig config) {
        return -1;
    }

    default int unregisterEventCallback(EventCallback callback) {
        return -1;
    }

    default int getInOutDoorState() {
        return -1;
    }

    default int getInOutDoorState(Bundle bundle) {
        return -1;
    }
}
