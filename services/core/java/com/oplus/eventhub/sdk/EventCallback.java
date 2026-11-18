package com.oplus.eventhub.sdk;

import com.oplus.eventhub.sdk.aidl.DeviceEventResult;

public interface EventCallback {
    void onEventStateChanged(DeviceEventResult deviceEventResult);
}
