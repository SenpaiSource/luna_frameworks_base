package com.android.server.display.feature.ambience.processes.process;

import com.android.server.display.feature.ambience.events.delegate.SensorDelegate;
import com.android.server.display.feature.ambience.utils.AmbienceLog;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedList;
import java.util.ListIterator;

/* JADX INFO: loaded from: /run/media/sathiya/data/scratch/ambience.dex */
public class LuxDataProcess {
    private static final int DEFAULT_CALCULATE_RETAINING_DECIMAL = 2;
    private static final float DEFAULT_CALCULATE_WEIGHT = 0.5f;
    private static final int INVALID_VALUE = -1;
    private static String TAG;

    public LuxDataProcess(String tag) {
        TAG = tag + "[LuxDataProcess]";
    }

    public static float findWeightingValue(LinkedList<Float> queue) {
        return findWeightingValue(queue, DEFAULT_CALCULATE_WEIGHT);
    }

    public static float findWeightingValue(LinkedList<Float> queue, float weighting) {
        float result = SensorDelegate.DEFAULT_PROXIMITY_NEAR_CHECK_VALUE;
        if (queue != null && !queue.isEmpty()) {
            float weightingLux = queue.getFirst().floatValue();
            ListIterator<Float> iterator = queue.listIterator(0);
            while (iterator.hasNext()) {
                weightingLux = findWeightingValue(weightingLux, iterator.next().floatValue(), weighting);
            }
            result = weightingLux;
        }
        if (!Float.isFinite(result) && queue != null) {
            AmbienceLog.e(TAG, "findWeightingValue fail! result = " + result + " queue = " + queue);
            queue.remove();
            return findWeightingValue(queue, weighting);
        }
        return BigDecimal.valueOf(result).setScale(2, RoundingMode.HALF_DOWN).floatValue();
    }

    public static float findWeightingValue(float first, float last) {
        return findWeightingValue(first, last, DEFAULT_CALCULATE_WEIGHT);
    }

    public static float findWeightingValue(float first, float last, float weighting) {
        return (last * weighting) + ((1.0f - weighting) * first);
    }

    public static float findRMSValue(LinkedList<Float> queue) {
        float squareSum = SensorDelegate.DEFAULT_PROXIMITY_NEAR_CHECK_VALUE;
        float finalValue = SensorDelegate.DEFAULT_PROXIMITY_NEAR_CHECK_VALUE;
        if (queue != null && !queue.isEmpty()) {
            ListIterator<Float> iterator = queue.listIterator(0);
            while (iterator.hasNext()) {
                float value = iterator.next().floatValue();
                squareSum = (float) (((double) squareSum) + Math.pow(value, 2.0d));
            }
            finalValue = (float) Math.sqrt(squareSum / queue.size());
        }
        return BigDecimal.valueOf(finalValue).setScale(2, RoundingMode.HALF_DOWN).floatValue();
    }

    public static float findNearestValue(LinkedList<Float> queue, float targetValue) {
        float distance = 2.1474836E9f;
        float finalValue = Float.NaN;
        if (queue != null && !queue.isEmpty()) {
            ListIterator<Float> iterator = queue.listIterator(0);
            while (iterator.hasNext()) {
                float value = iterator.next().floatValue();
                float distanceTmp = Math.abs(targetValue - value);
                if (distanceTmp < distance) {
                    finalValue = value;
                    distance = distanceTmp;
                }
            }
        }
        return finalValue;
    }
}
