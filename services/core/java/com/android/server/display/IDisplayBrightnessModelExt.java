package com.android.server.display;

import android.util.Pair;
import java.io.PrintWriter;

/* JADX INFO: loaded from: /run/media/sathiya/data/scratch/serv.dex */
public interface IDisplayBrightnessModelExt {
    public static final float INVALID_FLOAT = 0.0f;
    public static final int INVALID_INT = 0;
    public static final long INVALID_LONG = 0;

    default Pair<float[], float[]> getLuxSplineArrays() {
        return null;
    }

    default int getBrightnessModelType() {
        return 0;
    }

    default void setMinBrightness(float val) {
    }

    default float getMinBrightness() {
        return 0.0f;
    }

    default float getMinConfigBrightness() {
        return 0.0f;
    }

    default float getTotalBrightness() {
        return 0.0f;
    }

    default void setMaxBrightness(float val) {
    }

    default float getMaxBrightness() {
        return 0.0f;
    }

    default float getMaxConfigBrightness() {
        return 0.0f;
    }

    default float getDefaultBrightness() {
        return 0.0f;
    }

    default float getMaxPanelBrightness() {
        return 0.0f;
    }

    default float getValueByCurve(String name, float key) {
        return Float.NaN;
    }

    default float getNitFromBrightness(float brightness) {
        return 0.0f;
    }

    default float getBrightnessFromNit(float nit) {
        return 0.0f;
    }

    default boolean isCurveValid(String name) {
        return false;
    }

    default int getBrightnessRampRateFast() {
        return 0;
    }

    default int getBrightnessRampRateFastManual() {
        return 0;
    }

    default int getBrightnessRampRateSlow() {
        return 0;
    }

    default int getBrightnessRampRateGame() {
        return 0;
    }

    default int getBrightnessRampRateWindowExit() {
        return 0;
    }

    default int getBrightnessRampRateWindowEnter() {
        return 0;
    }

    default int getBrightnessRampRateEdrEnter() {
        return 0;
    }

    default int getBrightnessRampRateEdrExit() {
        return 0;
    }

    default int getBrightnessRampRateHdrVisionEnter() {
        return 0;
    }

    default float getBacklightFromBrightness(float brightness) {
        return 0.0f;
    }

    default float getBacklightFromNit(float nit) {
        return 0.0f;
    }

    default float getMaxPanelNit() {
        return 0.0f;
    }

    default float getMinPanelNit() {
        return 0.0f;
    }

    default float getNormalMaxNit() {
        return 0.0f;
    }

    default float getCurrentRegionRate(float targetY, float currY, float nextChange) {
        return 0.0f;
    }

    default void resetSpline() {
    }

    default void drag(float x, float y) {
    }

    default boolean isDragSpline() {
        return false;
    }

    default void changeSplines(boolean pwmEnable) {
    }

    default void changeSinglePulseSplines(boolean change) {
    }

    default float clampNit(float nit) {
        return Float.NaN;
    }

    default float clampBrightness(float brightnessState) {
        return Float.NaN;
    }

    default float getColorFromBrightness(float brightness) {
        return Float.NaN;
    }

    default float getShakeRangeMaxLux() {
        return Float.NaN;
    }

    default Pair<Float, Float> getHBMRangeLux(float lux) {
        return null;
    }

    default boolean printAutoLuxIntervalForMarvels(String path) {
        return false;
    }

    default long getPhysicalDisplayId() {
        return 0L;
    }

    default void dump(PrintWriter pw) {
    }
}
