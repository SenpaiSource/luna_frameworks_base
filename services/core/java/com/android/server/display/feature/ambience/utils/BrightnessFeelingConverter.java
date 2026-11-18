package com.android.server.display.feature.ambience.utils;

/* JADX INFO: loaded from: /run/media/sathiya/data/scratch/ambience.dex */
public class BrightnessFeelingConverter {
    private static final float CONSTANT_C = 0.0f;
    private static final float CONSTANT_K = 1.0f;
    private static final float LOGARITHM_OFFSET = 1.0f;
    private static final String TAG = "BrightnessFeelingConverter";

    public static double getFeelingFromNit(float baseNit) {
        return (Math.log(1.0f + baseNit) * 1.0d) + 0.0d;
    }

    public static double getDeltaFeelingFromNit(float baseNit, float toNit) {
        return getFeelingFromNit(toNit) - getFeelingFromNit(baseNit);
    }

    public static double getNitFromFeeling(double feeling) {
        return Math.exp((feeling - 0.0d) / 1.0d) - 1.0d;
    }

    public static double getNitFromDeltaFeeling(float baseNit, double deltaFeeling) {
        return getNitFromFeeling(getFeelingFromNit(baseNit) + deltaFeeling);
    }

    public static double getDeltaNitFromDeltaFeeling(float baseNit, double deltaFeeling) {
        return getNitFromDeltaFeeling(baseNit, deltaFeeling) - ((double) baseNit);
    }
}
