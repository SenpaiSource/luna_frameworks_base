package com.android.server.display.feature.ambience.scenes.scene;

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
public class ErraticScene extends SceneBase {
    public static final int DEFAULT_CALCULATE_HISTORY_UPDATE_SIZE = 3;
    public static final long DEFAULT_CALCULATE_HISTORY_UPDATE_TIME = 3000;
    private static final float DEFAULT_ERRATIC_SCENE_ENTER_THRESHOLD = 600000.0f;
    private static final int DEFAULT_INOUT_DOOR_SCORE_WEIGHT = 4;
    private static final int DEFAULT_MOTION_SCORE_WEIGHT = 6;
    private static final int DEFAULT_NETWORK_SCORE_WEIGHT = 2;
    private static final int DEFAULT_SHAKE_RANGE_SCORE_WEIGHT = 5;
    private static final int DEFAULT_TARGET_QUEUE_HIT_RELAX_SIZE_BY_MONOTONOUS = 1;
    private static final int DEFAULT_TARGET_QUEUE_HIT_SIZE_BY_TIME = 4;
    private static final int DEFAULT_TARGET_QUEUE_HIT_STRICT_SIZE_BY_MONOTONOUS = 2;
    private static final long DEFAULT_TARGET_QUEUE_RECALL_TIME = 30000;
    private static final long DEFAULT_UPDATE_QUEUE_RECALL_TIME = 5000;
    private static final float DEFAULT_UPDATE_QUEUE_STANDARD_DEVIATION_JUDGMENT_COEFFICIENT = 2.0f;
    private static final String TAG = "ErraticScene";
    private final OplusBrightnessSceneAdapter mSceneAdapter;

    public ErraticScene(OplusBrightnessSceneAdapter sceneAdapter) {
        super(TAG);
        this.mSceneAdapter = sceneAdapter;
        AmbienceLog.d(TAG, "ErraticScene created, " + this);
    }

    public static boolean isSameScene(SceneBase scene) {
        if (scene != null) {
            return TAG.equals(scene.getSceneName());
        }
        return false;
    }

    @Override // com.android.server.display.feature.ambience.scenes.utils.SceneBase
    public void updateSceneConfig() {
        this.mSceneConfig.setLuxQueueUpTrendLuxCount(9);
        this.mSceneConfig.setLuxQueueDownTrendLuxCount(10);
        this.mSceneConfig.setLuxQueueUpTrendLuxTime(1500L);
        this.mSceneConfig.setLuxQueueDownTrendLuxTime(TimeEvent.DEFAULT_TIME_TWO_SECONDS);
        this.mSceneConfig.setLuxCalculateOneTimeFilterWeight(0.3f);
        this.mSceneConfig.setShakeRangeMainSensorUpWeight(1.1f);
        this.mSceneConfig.setShakeRangeMainSensorDownWeight(1.1f);
        this.mSceneConfig.setShakeRangeAuxSensorUpWeight(1.1f);
        this.mSceneConfig.setShakeRangeAuxSensorDownWeight(1.1f);
        this.mSceneConfig.setDualLightMinLuxRange(3.0f);
        this.mSceneConfig.setReactionAccelerateSwitch(true);
        this.mSceneConfig.setReactionAccelerateUpWeight(1.2f);
        this.mSceneConfig.setReactionAccelerateUpOffset(2);
        this.mSceneConfig.setReactionAccelerateDownWeight(1.08f);
        this.mSceneConfig.setReactionAccelerateDownOffset(4);
        this.mSceneConfig.setAnimationAccelerateUpWeight(0.8f);
        this.mSceneConfig.setAnimationAccelerateDownWeight(0.8f);
        this.mSceneConfig.setDayNightModeDuration(TimeEvent.DEFAULT_TIME_ONE_MINUTE);
        this.mSceneConfig.setDayNightModeLuxThreshold(DEFAULT_UPDATE_QUEUE_STANDARD_DEVIATION_JUDGMENT_COEFFICIENT);
        this.mSceneConfig.setInterimModeDuration(45000L);
        this.mSceneConfig.setInterimModeLuxThreshold(1.5f);
        this.mSceneConfig.setMotionModeOnFootLuxThreshold(4.5f);
        this.mSceneConfig.setMotionModeInTransportationLuxThreshold(4.5f);
        this.mSceneConfig.setDarknessModeDuration(9999000L);
    }

    @Override // com.android.server.display.feature.ambience.scenes.utils.SceneBase
    public int sceneScore(PhoneState state, HashMap<String, OplusLimitQueue> luxMap, SceneBase lastScene) {
        OplusPair<Float, Float> largeRange;
        this.mScore = -1;
        boolean isSameScene = isSameScene(lastScene);
        OplusLimitQueue targetQueue = luxMap.get(OplusBrightnessSceneAdapter.DEFAULT_TARGET_LUX_QUEUE_KEY);
        LinkedList<Float> targetQueueCheckList = targetQueue.getLinkedListByPastTime(30000L);
        int changeCount = OplusLimitQueue.getNotMonotonousCount(targetQueueCheckList);
        if (isSameScene && state.isOnlyLuxChanged()) {
            this.mScore = 5467;
        }
        if (changeCount > 0 && OplusLimitQueue.getAverage(targetQueueCheckList) < 600000.0d) {
            if (targetQueueCheckList.size() >= 4) {
                if (changeCount >= 2) {
                    this.mScore = 5467;
                }
            } else if (isSameScene || changeCount >= 1) {
                LinkedList<Float> updateCheckList = luxMap.get(OplusBrightnessSceneAdapter.DEFAULT_UPDATE_LUX_QUEUE_KEY).getLinkedListByPastTime(5000L);
                double updateQueueSD = OplusLimitQueue.getStandardDeviation(updateCheckList);
                if (updateQueueSD >= 0.0d && (largeRange = this.mSceneAdapter.getStabilityProcess().getLargeChangeLimit()) != null) {
                    double shakeRangeSD = OplusLimitQueue.getStandardDeviationByExtremum(((Float) largeRange.first).floatValue(), ((Float) largeRange.second).floatValue());
                    if (2.0d * updateQueueSD > shakeRangeSD) {
                        this.mScore = isSameScene ? 5467 : 5;
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
        MotionEvent.MotionEventResult motionEvent = state.getMotionEvent();
        if (motionEvent != null && !motionEvent.isMotionless()) {
            score = 0 + 6;
        }
        InOutDoorEvent.InOutDoorEventResult inOutDoorEvent = state.getInOutDoorEvent();
        if (inOutDoorEvent != null && inOutDoorEvent.isOutDoor()) {
            score += 4;
        }
        NetworkEvent.NetworkEventResult networkEvent = state.getNetworkEvent();
        if (networkEvent != null && !networkEvent.isWifiConnected()) {
            score += 2;
        }
        TimeEvent.TimeEventResult timeEvent = state.getTimeEvent();
        if (timeEvent != null && !this.mSceneAdapter.getLuxPostProcessStrategy().isDayMode(timeEvent.getNormalizationTime(), this.mSceneConfig)) {
            score++;
        }
        DarkModeEvent.DarkModeEventResult darkModeEvent = state.getDarkModeEvent();
        if (darkModeEvent != null && darkModeEvent.isDarkMode()) {
            return score + 1;
        }
        return score;
    }
}
