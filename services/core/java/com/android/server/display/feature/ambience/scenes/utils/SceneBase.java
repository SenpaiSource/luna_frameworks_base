package com.android.server.display.feature.ambience.scenes.utils;

import com.android.server.display.config.global.ComfortableBrightnessConfig;
import com.android.server.display.config.global.OplusDisplayGlobalConfig;
import com.android.server.display.feature.ambience.events.utils.PhoneState;
import com.android.server.display.feature.ambience.utils.OplusLimitQueue;
import java.util.HashMap;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/* JADX INFO: loaded from: /run/media/sathiya/data/scratch/ambience.dex */
public abstract class SceneBase {
    private final String TAG;
    public final int INVALID_VALUE = -1;
    public final int CONFIDENCE_VALUE = 5467;
    public final float DEFAULT_SCENE_SCORE_KEEP_COEFFICIENT = 1.1f;
    public int mScore = -1;
    public final SceneConfig mSceneConfig = new SceneConfig();

    public abstract int sceneScore(PhoneState phoneState, HashMap<String, OplusLimitQueue> map, SceneBase sceneBase);

    public abstract void updateSceneConfig();

    public SceneBase(String tag) {
        this.TAG = tag;
    }

    public void init() {
        updateSceneConfig();
        Optional.ofNullable(OplusDisplayGlobalConfig.getInstance())
                .map(OplusDisplayGlobalConfig::getComfortableBrightnessConfig)
                .ifPresent(this::oplusLambda$init$1);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void oplusLambda$init$1(ComfortableBrightnessConfig comfortableBrightnessConfig) {
        comfortableBrightnessConfig.mountSceneConfig(this.TAG, this.mSceneConfig);
    }

    public String getSceneName() {
        return this.TAG;
    }

    public SceneConfig getSceneConfig() {
        return this.mSceneConfig;
    }

    public int getSceneScore() {
        return this.mScore;
    }

    public boolean isConfidentScene() {
        return this.mScore >= 5467;
    }

    public String toString() {
        return getSceneName();
    }
}
