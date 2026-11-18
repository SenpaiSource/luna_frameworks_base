package com.android.server.display.test.impl;

import com.android.server.display.feature.ambience.events.utils.EventResult;
import com.android.server.display.feature.ambience.scenes.utils.SceneBase;

public class OplusBrightnessDebugCenter {
    private static final OplusBrightnessDebugCenter sInstance = new OplusBrightnessDebugCenter();

    public enum EVENT {
        DARKMODE,
        INOUTDOOR,
        LUX,
        MOTION,
        NETWORK,
        PROXIMITY,
        TIME,
        SCENE
    }

    public interface IOplusDebugListener {
        void onDebugFinish();
        void onEventChange(EventResult eventResult);
        void onSceneChange(SceneBase sceneBase);
    }

    public static OplusBrightnessDebugCenter getInstance() {
        return sInstance;
    }

    public void registerDebugListener(EVENT event, IOplusDebugListener listener, int verifyNumber) {}
    public void unregisterDebugListener(EVENT event, IOplusDebugListener listener) {}
}
