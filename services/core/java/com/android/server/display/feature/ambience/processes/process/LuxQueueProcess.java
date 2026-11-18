package com.android.server.display.feature.ambience.processes.process;

import com.android.server.display.feature.ambience.OplusBrightnessAmbientAdapter;
import com.android.server.display.feature.ambience.events.delegate.SensorDelegate;
import com.android.server.display.feature.ambience.scenes.OplusBrightnessSceneAdapter;
import com.android.server.display.feature.ambience.scenes.utils.SceneConfig;
import com.android.server.display.feature.ambience.utils.AmbienceLog;
import com.android.server.display.feature.ambience.utils.OplusLimitQueue;
import com.android.server.display.feature.ambience.utils.OplusLuxResult;
import java.util.Iterator;
import java.util.LinkedList;

/* JADX INFO: loaded from: /run/media/sathiya/data/scratch/ambience.dex */
public class LuxQueueProcess {
    private final String TAG;
    private OplusBrightnessSceneAdapter mSceneAdapter;
    private final float DEFAULT_STABLE_AVERAGE_MULTIPLES = 2.0f;
    private final float DEFAULT_STABLE_OFFSET_DIFFERENCE_MULTIPLE = 2.0f;
    private int mDirection = 0;

    public LuxQueueProcess(String tag, OplusBrightnessSceneAdapter adapter) {
        this.mSceneAdapter = null;
        this.TAG = tag + "[LuxQueueProcess]";
        this.mSceneAdapter = adapter;
    }

    /* JADX WARN: Code duplicated, block: B:19:0x0051  */
    /* JADX WARN: Code duplicated, block: B:21:0x0059  */
    /* JADX WARN: Code duplicated, block: B:24:0x0062  */
    /* JADX WARN: Code duplicated, block: B:29:0x0071  */
    /* JADX WARN: Code duplicated, block: B:38:0x005e A[SYNTHETIC] */
    /* JADX WARN: Code duplicated, block: B:40:0x007a A[SYNTHETIC] */
    /* JADX WARN: Code duplicated, block: B:41:0x007c A[SYNTHETIC] */
    /* JADX WARN: Code duplicated, block: B:42:0x0078 A[SYNTHETIC] */
    /* JADX WARN: Code duplicated, block: B:44:0x0018 A[SYNTHETIC] */
    private LinkedList<Float> getLastMonotonousList(LinkedList<Float> luxQueue, StabilityProcess stabilityProcess) {
        float tempAvg;
        if (luxQueue.size() == 0) {
            return luxQueue;
        }
        this.mDirection = 0;
        LinkedList<Float> lastMonotonousList = new LinkedList<>();
        float lastValue = Float.NaN;
        float luxQueueAvg = Float.NaN;
        boolean isVigilant = false;
        Iterator<Float> iterator = luxQueue.descendingIterator();
        while (iterator.hasNext()) {
            float lux = iterator.next().floatValue();
            if (!Float.isNaN(lastValue)) {
                int result = Float.compare(lastValue, lux);
                if (result == 0) {
                    result = this.mDirection;
                }
                if (this.mDirection * result < 0) {
                    break;
                }
                this.mDirection = result;
                lastMonotonousList.addFirst(Float.valueOf(lux));
                lastValue = lux;
                tempAvg = (float) OplusLimitQueue.getAverage(lastMonotonousList);
                if (Float.isNaN(luxQueueAvg)) {
                    if (!isUp()) {
                        if (2.0f * tempAvg < luxQueueAvg) {
                            lastMonotonousList.remove();
                            break;
                        }
                        luxQueueAvg = tempAvg;
                        if (stabilityProcess.isLargeChange(lux)) {
                            isVigilant = true;
                        } else if (isVigilant) {
                            lastMonotonousList.remove();
                            break;
                        }
                    } else if (!isDown()) {
                        luxQueueAvg = tempAvg;
                        if (stabilityProcess.isLargeChange(lux)) {
                            isVigilant = true;
                        } else if (isVigilant) {
                            lastMonotonousList.remove();
                            break;
                        }
                    } else {
                        luxQueueAvg = tempAvg;
                        if (stabilityProcess.isLargeChange(lux)) {
                            isVigilant = true;
                        } else if (isVigilant) {
                            lastMonotonousList.remove();
                            break;
                        }
                    }
                } else {
                    luxQueueAvg = tempAvg;
                    if (stabilityProcess.isLargeChange(lux)) {
                        isVigilant = true;
                    } else if (isVigilant) {
                        lastMonotonousList.remove();
                        break;
                    }
                }
            } else {
                lastMonotonousList.addFirst(Float.valueOf(lux));
                lastValue = lux;
                tempAvg = (float) OplusLimitQueue.getAverage(lastMonotonousList);
                if (Float.isNaN(luxQueueAvg)) {
                    if (!isUp()) {
                        if (!isDown() && 2.0f * luxQueueAvg < tempAvg) {
                            lastMonotonousList.remove();
                            break;
                        }
                        luxQueueAvg = tempAvg;
                        if (stabilityProcess.isLargeChange(lux)) {
                            isVigilant = true;
                        } else if (isVigilant) {
                            lastMonotonousList.remove();
                            break;
                        }
                    } else {
                        if (2.0f * tempAvg < luxQueueAvg) {
                            lastMonotonousList.remove();
                            break;
                        }
                        luxQueueAvg = tempAvg;
                        if (stabilityProcess.isLargeChange(lux)) {
                            isVigilant = true;
                        } else if (isVigilant) {
                            lastMonotonousList.remove();
                            break;
                        }
                    }
                } else {
                    luxQueueAvg = tempAvg;
                    if (stabilityProcess.isLargeChange(lux)) {
                        isVigilant = true;
                    } else if (isVigilant) {
                        lastMonotonousList.remove();
                        break;
                    }
                }
            }
        }
        return lastMonotonousList;
    }

    private LinkedList<Float> getTrendList(LinkedList<Float> luxQueue, OplusLuxResult luxResult) {
        if (luxQueue.size() == 0) {
            return luxQueue;
        }
        this.mDirection = 0;
        LinkedList<Float> trendList = new LinkedList<>();
        float deviceLux = Math.max(luxResult.getLastFinalLux(), SensorDelegate.DEFAULT_PROXIMITY_NEAR_CHECK_VALUE);
        float luxQueueAvg = Float.NaN;
        boolean isLuxPostProcessTakeEffect = luxResult.isLuxPostProcessMonitoring();
        boolean isVigilant = false;
        boolean isConditionSuccess = true;
        int noiseCount = 0;
        Iterator<Float> iterator = luxQueue.descendingIterator();
        while (iterator.hasNext()) {
            float lux = iterator.next().floatValue();
            int result = Float.compare(lux, deviceLux);
            isVigilant = Boolean.logicalOr(isVigilant, luxResult.isSceneChange() ? luxResult.getStabilityProcess().isSmallChange(lux) : luxResult.getStabilityProcess().isLargeChange(lux));
            if (Float.isNaN(lux)) {
                isConditionSuccess = false;
                break;
            }
            if (this.mDirection == 0 && !isLuxPostProcessTakeEffect && result == 0) {
                isConditionSuccess = false;
                break;
            }
            if (this.mDirection != 0 && this.mDirection != result) {
                int noiseCount2 = noiseCount + 1;
                if (noiseCount <= 0) {
                    noiseCount = noiseCount2;
                } else {
                    isConditionSuccess = false;
                    break;
                }
            } else {
                trendList.addFirst(Float.valueOf(lux));
                this.mDirection = result;
                if (!Float.isNaN(luxQueueAvg) && isVigilant) {
                    if (!isUp()) {
                        if (isDown() && luxQueueAvg * 2.0f < lux && Math.abs(lux - deviceLux) * 2.0f > Math.abs(luxQueueAvg - deviceLux)) {
                            isConditionSuccess = false;
                            break;
                        }
                    } else if (lux * 2.0f < luxQueueAvg && Math.abs(lux - deviceLux) * 2.0f < Math.abs(luxQueueAvg - deviceLux)) {
                        isConditionSuccess = false;
                        break;
                    }
                }
                luxQueueAvg = (float) OplusLimitQueue.getAverage(trendList);
            }
        }
        if (!isConditionSuccess) {
            trendList.clear();
        }
        return trendList;
    }

    public LinkedList<Float> getUpdateBackwardList(OplusLuxResult luxResult) {
        long checkTime;
        SceneConfig config = luxResult.getScene().getSceneConfig();
        if (isUp()) {
            checkTime = (long) (config.getLuxQueueUpTrendLuxTime() / luxResult.getAccelerateScale());
        } else {
            checkTime = (long) (config.getLuxQueueDownTrendLuxTime() / luxResult.getAccelerateScale());
        }
        if (checkTime > Math.abs(OplusBrightnessAmbientAdapter.getScreenOnPastTime())) {
            AmbienceLog.w(this.TAG, "screen on past time is too short, not to get trend list! checkTime = " + checkTime);
            return new LinkedList<>();
        }
        return getTrendList(this.mSceneAdapter.getRealtimeLuxLimitQueue().getLinkedListByPastTime(checkTime), luxResult);
    }

    public int getDirection() {
        return this.mDirection;
    }

    public boolean isUp() {
        return this.mDirection == 1;
    }

    public boolean isDown() {
        return this.mDirection == -1;
    }

    public boolean isStable() {
        return this.mDirection == 0;
    }
}
