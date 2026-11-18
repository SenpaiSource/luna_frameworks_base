package com.android.server.display.feature.viewer;

public class BrightnessViewer {
    private static final BrightnessViewer sInstance = new BrightnessViewer();

    public static BrightnessViewer getInstance() {
        return sInstance;
    }

    public void setLux(float lux) {}
    public void setLux(int lux) {}
}
