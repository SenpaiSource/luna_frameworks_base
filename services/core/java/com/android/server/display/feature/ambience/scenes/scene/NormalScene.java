package com.android.server.display.feature.ambience.scenes.scene;

import com.android.server.display.config.global.OplusDisplayGlobalConfig;
import com.android.server.display.feature.ambience.events.utils.PhoneState;
import com.android.server.display.feature.ambience.scenes.OplusBrightnessSceneAdapter;
import com.android.server.display.feature.ambience.scenes.utils.SceneBase;
import com.android.server.display.feature.ambience.utils.AmbienceLog;
import com.android.server.display.feature.ambience.utils.OplusLimitQueue;
import java.util.HashMap;

/* JADX INFO: loaded from: /run/media/sathiya/data/scratch/ambience.dex */
public class NormalScene extends SceneBase {
    private static final int DEFAULT_NORMAL_SCENE_SCORE = 9;
    private static final String TAG = "NormalScene";
    private final OplusBrightnessSceneAdapter mSceneAdapter;

    public NormalScene(OplusBrightnessSceneAdapter sceneAdapter) {
        super(TAG);
        this.mSceneAdapter = sceneAdapter;
        AmbienceLog.d(TAG, "NormalScene created, " + this);
    }

    public static boolean isSameScene(SceneBase scene) {
        if (scene != null) {
            return TAG.equals(scene.getSceneName());
        }
        return false;
    }

    @Override // com.android.server.display.feature.ambience.scenes.utils.SceneBase
    public void updateSceneConfig() {
        if (OplusDisplayGlobalConfig.getInstance().isSupportDarkEnvBrightnessSmoothOptimiz()) {
            this.mSceneConfig.setDarknessModeDuration(420000L);
        }
    }

    @Override // com.android.server.display.feature.ambience.scenes.utils.SceneBase
    public int sceneScore(PhoneState state, HashMap<String, OplusLimitQueue> luxMap, SceneBase lastScene) {
        this.mScore = DEFAULT_NORMAL_SCENE_SCORE;
        return this.mScore;
    }
}
