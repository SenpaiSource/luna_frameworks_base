package com.android.server.display.feature.viewer;

public class BrightnessRecorder {
    private static final BrightnessRecorder sInstance = new BrightnessRecorder();

    public static BrightnessRecorder getInstance() {
        return sInstance;
    }

    public void updateRecorderLux(float lux) {}
    public void updateRecorderLux(int lux) {}
}
