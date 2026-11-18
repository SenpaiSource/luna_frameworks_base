package com.android.server.display.feature.stat;

public class TrackManager {
    private static final TrackManager sInstance = new TrackManager();

    public static TrackManager getInstance() {
        return sInstance;
    }

    public void setLux(float lux) {}
    public void setLux(int lux) {}
}
