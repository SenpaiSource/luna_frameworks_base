package com.android.server.display.feature.ambience.scenes.scene;

import com.android.server.display.config.global.OplusDisplayGlobalConfig;
import com.android.server.display.feature.ambience.events.event.DarkModeEvent;
import com.android.server.display.feature.ambience.events.event.InOutDoorEvent;
import com.android.server.display.feature.ambience.events.event.MotionEvent;
import com.android.server.display.feature.ambience.events.event.NetworkEvent;
import com.android.server.display.feature.ambience.events.event.TimeEvent;
import com.android.server.display.feature.ambience.events.utils.PhoneState;
import com.android.server.display.feature.ambience.scenes.OplusBrightnessSceneAdapter;
import com.android.server.display.feature.ambience.scenes.utils.SceneBase;
import com.android.server.display.feature.ambience.utils.AmbienceLog;
import com.android.server.display.feature.ambience.utils.OplusLimitQueue;
import com.android.server.display.feature.ambience.utils.OplusPair;
import java.util.HashMap;
import java.util.LinkedList;

/* JADX INFO: loaded from: /run/media/sathiya/data/scratch/ambience.dex */
public class StableScene extends SceneBase {
    private static final int DEFAULT_INOUT_DOOR_SCORE_WEIGHT = 2;
    private static final int DEFAULT_MOTION_SCORE_WEIGHT = 3;
    private static final int DEFAULT_NETWORK_SCORE_WEIGHT = 4;
    private static final int DEFAULT_SHAKE_RANGE_SCORE_WEIGHT = 3;
    public static final long DEFAULT_STABLE_SCENE_QUEUE_FORCE_UPDATE_RECALL_TIME = 4000;
    public static final float DEFAULT_STABLE_SCENE_UNDER_LIGHT_FORCE_UPDATE_SMALL_RANGE_LIMIT = 5.0f;
    private static final long DEFAULT_TARGET_QUEUE_RECALL_TIME = 20000;
    private static final float DEFAULT_UPDATE_QUEUE_STANDARD_DEVIATION_JUDGMENT_RELAX_COEFFICIENT = 6.6666665f;
    private static final float DEFAULT_UPDATE_QUEUE_STANDARD_DEVIATION_JUDGMENT_STRICT_COEFFICIENT = 10.0f;
    private static final long DEFAULT_UPDATE_QUEUE_STANDARD_DEVIATION_RECALL_TIME = 10000;
    private static final String TAG = "StableScene";
    private OplusBrightnessSceneAdapter mSceneAdapter;

    public StableScene(OplusBrightnessSceneAdapter sceneAdapter) {
        super(TAG);
        this.mSceneAdapter = sceneAdapter;
        AmbienceLog.d(TAG, "StableScene created, " + this);
    }

    public static boolean isSameScene(SceneBase scene) {
        if (scene != null) {
            return TAG.equals(scene.getSceneName());
        }
        return false;
    }

    @Override // com.android.server.display.feature.ambience.scenes.utils.SceneBase
    public void updateSceneConfig() {
        this.mSceneConfig.setLuxQueueUpTrendLuxCount(4);
        this.mSceneConfig.setLuxQueueDownTrendLuxCount(4);
        this.mSceneConfig.setLuxQueueUpTrendLuxTime(800L);
        this.mSceneConfig.setLuxQueueDownTrendLuxTime(800L);
        this.mSceneConfig.setLuxCalculateOneTimeFilterWeight(1.0f);
        this.mSceneConfig.setShakeRangeMainSensorUpWeight(0.85f);
        this.mSceneConfig.setShakeRangeMainSensorDownWeight(0.85f);
        this.mSceneConfig.setShakeRangeAuxSensorUpWeight(0.85f);
        this.mSceneConfig.setShakeRangeAuxSensorDownWeight(0.85f);
        this.mSceneConfig.setDualLightMinLuxRange(1.5f);
        this.mSceneConfig.setReactionAccelerateUpWeight(1.5f);
        this.mSceneConfig.setReactionAccelerateUpOffset(1);
        this.mSceneConfig.setReactionAccelerateDownWeight(1.08f);
        this.mSceneConfig.setReactionAccelerateDownOffset(2);
        this.mSceneConfig.setDayNightModeDuration(10000L);
        this.mSceneConfig.setDayNightModeLuxThreshold(1.5f);
        this.mSceneConfig.setInterimModeDuration(TimeEvent.DEFAULT_TIME_FIVE_SECONDS);
        this.mSceneConfig.setInterimModeLuxThreshold(0.5f);
        this.mSceneConfig.setMotionModeOnFootLuxThreshold(2.0f);
        this.mSceneConfig.setMotionModeInTransportationLuxThreshold(2.5f);
        this.mSceneConfig.setDarknessModeDuration(TimeEvent.DEFAULT_TIME_TWO_MINUTE);
        if (OplusDisplayGlobalConfig.getInstance().isSupportDarkEnvBrightnessSmoothOptimiz()) {
            this.mSceneConfig.setDarknessModeDuration(420000L);
        }
    }

    @Override // com.android.server.display.feature.ambience.scenes.utils.SceneBase
    public int sceneScore(PhoneState state, HashMap<String, OplusLimitQueue> luxMap, SceneBase lastScene) {
        this.mScore = -1;
        boolean isSameScene = isSameScene(lastScene);
        OplusLimitQueue targetQueue = luxMap.get(OplusBrightnessSceneAdapter.DEFAULT_TARGET_LUX_QUEUE_KEY);
        LinkedList<Float> targetCheckList = targetQueue.getLinkedListByPastTime(DEFAULT_TARGET_QUEUE_RECALL_TIME);
        if (targetCheckList.isEmpty()) {
            OplusLimitQueue updateQueue = luxMap.get(OplusBrightnessSceneAdapter.DEFAULT_UPDATE_LUX_QUEUE_KEY);
            LinkedList<Float> updateCheckList = updateQueue.getLinkedListByPastTime(10000L);
            double updateQueueSD = OplusLimitQueue.getStandardDeviation(updateCheckList);
            if (updateQueueSD <= 0.0d) {
                this.mScore = 5467;
            } else {
                OplusPair<Float, Float> largeRange = this.mSceneAdapter.getStabilityProcess().getLargeChangeLimit();
                if (largeRange != null) {
                    double shakeRangeSD = OplusLimitQueue.getStandardDeviationByExtremum(((Float) largeRange.first).floatValue(), ((Float) largeRange.second).floatValue());
                    if (10.0d * updateQueueSD < shakeRangeSD) {
                        this.mScore = 5467;
                    } else if (6.666666507720947d * updateQueueSD < shakeRangeSD) {
                        this.mScore = isSameScene ? 5467 : 3;
                    }
                }
            }
            if (this.mScore < 5467) {
                this.mScore += getPhoneStateJudgmentScore(state);
            }
        }
        return Math.round(this.mScore * (isSameScene ? 1.1f : 1.0f));
    }

    private int getPhoneStateJudgmentScore(PhoneState state) {
        int score = 0;
        NetworkEvent.NetworkEventResult networkEvent = state.getNetworkEvent();
        if (networkEvent != null && networkEvent.isWifiConnected()) {
            score = 0 + 4;
        }
        MotionEvent.MotionEventResult motionEvent = state.getMotionEvent();
        if (motionEvent != null && motionEvent.isMotionless()) {
            score += 3;
        }
        InOutDoorEvent.InOutDoorEventResult inOutDoorEvent = state.getInOutDoorEvent();
        if (inOutDoorEvent != null && inOutDoorEvent.isInDoor()) {
            score += 2;
        }
        DarkModeEvent.DarkModeEventResult darkModeEvent = state.getDarkModeEvent();
        if (darkModeEvent != null && darkModeEvent.isDarkMode()) {
            return score + 1;
        }
        return score;
    }
}
