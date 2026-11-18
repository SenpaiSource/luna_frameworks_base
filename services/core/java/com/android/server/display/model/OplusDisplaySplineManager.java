package com.android.server.display.model;

import android.util.Slog;
import com.android.server.display.util.OplusBrightnessPoint;
import com.android.server.display.util.OplusBrightnessUtils;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/* JADX INFO: loaded from: /run/media/sathiya/data/scratch/dex_extract/smali_oplus1/com/android/server/display/model/OplusDisplaySplineManager.smali */
public class OplusDisplaySplineManager {
    public static final String BACKLIGHT_SPLINE = "backlights";
    public static final float BRIGHTNESS_OFF_FLOAT = -1.0f;
    public static final int BUFFER_SIZE = 1024;
    public static final String DEFAULT_SPLINE = "default";
    public static final String DEFAULT_SPLINE_INVERSE = "default_inverse";
    private static final float DRAG_POINT_RALATED_TO_DEFAULT_CHECK_OFFSET = 1.0f;
    public static final String DRAG_SPLINE = "drags";
    public static final float FLOAT_CHECK_ACCURACY = 0.01f;
    public static final String HBM_SPLINE = "hbm";
    public static final float[] LIGHT_DISTRIBUATION_REGION = {30.0f, 600.0f};
    public static final float LIMIT_FLOAT = 999999.0f;
    public static final String NIT_SPLINE = "nits";
    public static final int THREAD_SLEEP_TIME = 10;
    public String TAG;
    private OplusSpline backlightToNitsSpline;
    private OplusSpline backlightToNitsSplinePWM;
    private OplusSpline backlightToNitsSplineSPD;
    private float[] mDragLuxs;
    private float[] mDragNits;
    private OplusBrightnessPoint mDragPoint;
    private OplusSpline mDragSpline;
    private OplusSpline mLuxToNitsSpline;
    private OplusDisplayBrightnessModel mModel;
    private long mPhysicalDisplayId;
    private Map<String, OplusSpline> mSplines;
    private OplusSpline nitsToBacklightSpline;
    private OplusSpline nitsToBacklightSplinePWM;
    private OplusSpline nitsToBacklightSplineSPD;

    public OplusDisplaySplineManager() {
        this.TAG = "OplusDisplaySplineManager";
        this.mSplines = new ConcurrentHashMap();
        this.mModel = null;
        this.mDragPoint = new OplusBrightnessPoint();
        this.mDragSpline = null;
        this.mPhysicalDisplayId = -1L;
        this.nitsToBacklightSpline = null;
        this.backlightToNitsSpline = null;
        this.nitsToBacklightSplinePWM = null;
        this.backlightToNitsSplinePWM = null;
        this.nitsToBacklightSplineSPD = null;
        this.backlightToNitsSplineSPD = null;
    }

    public OplusDisplaySplineManager(long physicalId) {
        this.TAG = "OplusDisplaySplineManager";
        this.mSplines = new ConcurrentHashMap();
        this.mModel = null;
        this.mDragPoint = new OplusBrightnessPoint();
        this.mDragSpline = null;
        this.mPhysicalDisplayId = -1L;
        this.nitsToBacklightSpline = null;
        this.backlightToNitsSpline = null;
        this.nitsToBacklightSplinePWM = null;
        this.backlightToNitsSplinePWM = null;
        this.nitsToBacklightSplineSPD = null;
        this.backlightToNitsSplineSPD = null;
        this.mPhysicalDisplayId = physicalId;
        this.TAG += "[" + this.mPhysicalDisplayId + "]";
    }

    public Map<String, OplusSpline> createSplines(OplusDisplayBrightnessModel model) {
        if (model == null) {
            Slog.e(this.TAG, "createSplines init failed as for model == null!!!");
            return null;
        }
        this.mModel = model;
        this.mSplines.clear();
        if (OplusBrightnessUtils.isValidMap(model.mLuxs, model.mNits)) {
            Slog.d(this.TAG, "createSpline default spline!!!");
            this.mLuxToNitsSpline = new OplusSpline(model.mLuxs, model.mNits);
            this.mSplines.put(DEFAULT_SPLINE, this.mLuxToNitsSpline);
            OplusSpline nitToLuxSpline = new OplusSpline(model.mNits, model.mLuxs);
            this.mSplines.put(DEFAULT_SPLINE_INVERSE, nitToLuxSpline);
        }
        if (model.mPanelNits != null && model.mPanelBrightness != null && model.mPanelBrightness.length == model.mPanelNits.length) {
            int len = model.mPanelNits.length;
            if (len > 1) {
                Slog.d(this.TAG, "createSpline panel spline len=" + len + " max(" + model.mPanelBrightness[len - 1] + "," + model.mPanelNits[len - 1] + ")");
            }
            this.nitsToBacklightSpline = new OplusSpline(model.mPanelNits, model.mPanelBrightness);
            this.backlightToNitsSpline = new OplusSpline(model.mPanelBrightness, model.mPanelNits);
            this.mSplines.put(NIT_SPLINE, this.nitsToBacklightSpline);
            this.mSplines.put(BACKLIGHT_SPLINE, this.backlightToNitsSpline);
        }
        if (model.mPanelNitsPWM != null && model.mPanelBrightnessPWM != null && model.mPanelBrightnessPWM.length == model.mPanelNitsPWM.length && model.mPanelBrightnessPWM.length > 1) {
            this.nitsToBacklightSplinePWM = new OplusSpline(model.mPanelNitsPWM, model.mPanelBrightnessPWM);
            this.backlightToNitsSplinePWM = new OplusSpline(model.mPanelBrightnessPWM, model.mPanelNitsPWM);
        }
        if (model.mPanelNitsSPD != null && model.mPanelBrightnessSPD != null && model.mPanelBrightnessSPD.length == model.mPanelNitsSPD.length && model.mPanelBrightnessSPD.length > 1) {
            this.nitsToBacklightSplineSPD = new OplusSpline(model.mPanelNitsSPD, model.mPanelBrightnessSPD);
            this.backlightToNitsSplineSPD = new OplusSpline(model.mPanelBrightnessSPD, model.mPanelNitsSPD);
        }
        return this.mSplines;
    }

    public boolean updateLuxNitSplineSuccess(float[] rusLuxs, float[] rusNits) {
        if (OplusBrightnessUtils.isValidMap(rusLuxs, rusNits)) {
            this.mLuxToNitsSpline = new OplusSpline(rusLuxs, rusNits);
            this.mSplines.put(DEFAULT_SPLINE, this.mLuxToNitsSpline);
            OplusSpline nitToLuxSpline = new OplusSpline(rusNits, rusLuxs);
            this.mSplines.put(DEFAULT_SPLINE_INVERSE, nitToLuxSpline);
            return true;
        }
        return false;
    }

    public void changeSplines(boolean pwmEnable) {
        if (pwmEnable) {
            if (this.nitsToBacklightSplinePWM != null && this.backlightToNitsSplinePWM != null) {
                Slog.d(this.TAG, "change splines to PWM mode");
                this.mSplines.put(NIT_SPLINE, this.nitsToBacklightSplinePWM);
                this.mSplines.put(BACKLIGHT_SPLINE, this.backlightToNitsSplinePWM);
                return;
            }
            return;
        }
        if (this.nitsToBacklightSpline != null && this.backlightToNitsSpline != null) {
            Slog.d(this.TAG, "recover splines to normal mode");
            this.mSplines.put(NIT_SPLINE, this.nitsToBacklightSpline);
            this.mSplines.put(BACKLIGHT_SPLINE, this.backlightToNitsSpline);
        }
    }

    public void changeSinglePulseSplines(boolean enable) {
        if (enable) {
            if (this.nitsToBacklightSplineSPD != null && this.backlightToNitsSplineSPD != null) {
                Slog.d(this.TAG, "change to Single Pulse Splines");
                this.mSplines.put(NIT_SPLINE, this.nitsToBacklightSplineSPD);
                this.mSplines.put(BACKLIGHT_SPLINE, this.backlightToNitsSplineSPD);
                return;
            }
            return;
        }
        if (this.nitsToBacklightSpline != null && this.backlightToNitsSpline != null) {
            Slog.d(this.TAG, "recover to normal Splines");
            this.mSplines.put(NIT_SPLINE, this.nitsToBacklightSpline);
            this.mSplines.put(BACKLIGHT_SPLINE, this.backlightToNitsSpline);
        }
    }

    public OplusSpline createSpline(String desc, float[] x, float[] y) {
        OplusSpline tmp = new OplusSpline(x, y);
        this.mSplines.put(desc, tmp);
        return tmp;
    }

    public OplusDisplayBrightnessModel getBrightnessModel() {
        return this.mModel;
    }

    public OplusBrightnessPoint drag(float x, float y, boolean useNit) {
        if (useNit) {
            float backlight = this.mSplines.get(NIT_SPLINE).interpolate(y);
            return drag(x, backlight);
        }
        return drag(x, y);
    }

    public OplusBrightnessPoint drag(float x, float y) {
        int i;
        float originalNit = this.mSplines.get(DEFAULT_SPLINE).interpolate(x);
        this.mDragPoint.backlight = y;
        this.mDragPoint.lux = x;
        this.mDragPoint.nit = this.mSplines.get(BACKLIGHT_SPLINE).interpolate(y);
        float dragDiff = this.mDragPoint.nit - originalNit;
        boolean isUp = dragDiff > 0.0f;
        ArrayList<Float> newLuxs = new ArrayList<>();
        ArrayList<Float> newNits = new ArrayList<>();
        OplusBrightnessPoint limitPoint = new OplusBrightnessPoint(999999.0f, 999999.0f);
        boolean hasAddedLast = false;
        int i2 = 0;
        while (i2 < this.mModel.mLuxs.length) {
            if (!isUp) {
                i = i2;
                OplusBrightnessPoint limitPoint2 = limitPoint;
                if (x < LIGHT_DISTRIBUATION_REGION[0]) {
                    float firstPointDiff = this.mModel.mNits[0] < this.mDragPoint.nit ? 0.0f : this.mDragPoint.nit - this.mModel.mNits[0];
                    float firstPointDiff2 = this.mModel.mLuxs[0];
                    float dragDiff2 = dragDiff;
                    calculateCurveChange(firstPointDiff2, this.mDragPoint.lux, firstPointDiff, dragDiff2, limitPoint2, this.mModel.mLuxs[i], this.mModel.mNits[i], newLuxs, newNits);
                    calculateCurveChange(this.mDragPoint.lux, LIGHT_DISTRIBUATION_REGION[0], dragDiff2, dragDiff2, this.mDragPoint, this.mModel.mLuxs[i], this.mModel.mNits[i], newLuxs, newNits);
                    calculateCurveChange(LIGHT_DISTRIBUATION_REGION[0], LIGHT_DISTRIBUATION_REGION[1], dragDiff2, 0.0f, limitPoint2, this.mModel.mLuxs[i], this.mModel.mNits[i], newLuxs, newNits);
                    calculateCurveChange(LIGHT_DISTRIBUATION_REGION[1], this.mModel.mLuxs[this.mModel.mLuxs.length - 1], 0.0f, 0.0f, limitPoint2, this.mModel.mLuxs[i], this.mModel.mNits[i], newLuxs, newNits);
                    limitPoint = limitPoint2;
                    dragDiff = dragDiff2;
                } else {
                    float dragDiff3 = dragDiff;
                    if (x < LIGHT_DISTRIBUATION_REGION[1]) {
                        calculateCurveChange(this.mModel.mLuxs[0], LIGHT_DISTRIBUATION_REGION[0], 0.0f, dragDiff3, limitPoint2, this.mModel.mLuxs[i], this.mModel.mNits[i], newLuxs, newNits);
                        calculateCurveChange(LIGHT_DISTRIBUATION_REGION[0], this.mDragPoint.lux, dragDiff3, dragDiff3, limitPoint2, this.mModel.mLuxs[i], this.mModel.mNits[i], newLuxs, newNits);
                        float basicsPoint = Math.max(this.mDragPoint.lux * 2.0f, LIGHT_DISTRIBUATION_REGION[1]);
                        calculateCurveChange(this.mDragPoint.lux, basicsPoint, dragDiff3, 0.0f, this.mDragPoint, this.mModel.mLuxs[i], this.mModel.mNits[i], newLuxs, newNits);
                        calculateCurveChange(basicsPoint, this.mModel.mLuxs[this.mModel.mLuxs.length - 1], 0.0f, 0.0f, limitPoint2, this.mModel.mLuxs[i], this.mModel.mNits[i], newLuxs, newNits);
                        dragDiff = dragDiff3;
                        limitPoint = limitPoint2;
                    } else if (this.mDragPoint.nit < this.mSplines.get(DEFAULT_SPLINE).interpolate(LIGHT_DISTRIBUATION_REGION[0])) {
                        calculateCurveChange(this.mModel.mLuxs[0], this.mDragPoint.lux, 0.0f, dragDiff3, limitPoint2, this.mModel.mLuxs[i], this.mModel.mNits[i], newLuxs, newNits);
                        limitPoint = limitPoint2;
                        calculateCurveChange(this.mDragPoint.lux, this.mModel.mLuxs[this.mModel.mLuxs.length - 1], dragDiff3, dragDiff3, this.mDragPoint, this.mModel.mLuxs[i], this.mModel.mNits[i], newLuxs, newNits);
                        if (i != this.mModel.mLuxs.length - 1) {
                            dragDiff = dragDiff3;
                        } else if (Math.abs(this.mDragPoint.lux - this.mModel.mLuxs[this.mModel.mLuxs.length - 1]) < 0.01f) {
                            checkDragPointPosition(this.mDragPoint.lux + DRAG_POINT_RALATED_TO_DEFAULT_CHECK_OFFSET, this.mDragPoint, newLuxs, newNits);
                            hasAddedLast = true;
                            dragDiff = dragDiff3;
                        } else {
                            dragDiff = dragDiff3;
                        }
                    } else {
                        limitPoint = limitPoint2;
                        calculateCurveChange(this.mModel.mLuxs[0], LIGHT_DISTRIBUATION_REGION[0], 0.0f, 0.0f, limitPoint2, this.mModel.mLuxs[i], this.mModel.mNits[i], newLuxs, newNits);
                        calculateCurveChange(LIGHT_DISTRIBUATION_REGION[0], this.mDragPoint.lux, 0.0f, dragDiff3, limitPoint2, this.mModel.mLuxs[i], this.mModel.mNits[i], newLuxs, newNits);
                        dragDiff = dragDiff3;
                        calculateCurveChange(this.mDragPoint.lux, this.mModel.mLuxs[this.mModel.mLuxs.length - 1], dragDiff, dragDiff, this.mDragPoint, this.mModel.mLuxs[i], this.mModel.mNits[i], newLuxs, newNits);
                        if (i == this.mModel.mLuxs.length - 1 && Math.abs(this.mDragPoint.lux - this.mModel.mLuxs[this.mModel.mLuxs.length - 1]) < 0.01f) {
                            checkDragPointPosition(this.mDragPoint.lux + DRAG_POINT_RALATED_TO_DEFAULT_CHECK_OFFSET, this.mDragPoint, newLuxs, newNits);
                            hasAddedLast = true;
                        }
                    }
                }
            } else if (x >= LIGHT_DISTRIBUATION_REGION[0]) {
                i = i2;
                OplusBrightnessPoint limitPoint3 = limitPoint;
                if (x >= LIGHT_DISTRIBUATION_REGION[1]) {
                    float dragDiff4 = dragDiff;
                    calculateCurveChange(this.mModel.mLuxs[0], LIGHT_DISTRIBUATION_REGION[0], 0.0f, 0.0f, limitPoint3, this.mModel.mLuxs[i], this.mModel.mNits[i], newLuxs, newNits);
                    calculateCurveChange(LIGHT_DISTRIBUATION_REGION[0], this.mDragPoint.lux, 0.0f, dragDiff4, limitPoint3, this.mModel.mLuxs[i], this.mModel.mNits[i], newLuxs, newNits);
                    dragDiff = dragDiff4;
                    calculateCurveChange(this.mDragPoint.lux, this.mModel.mLuxs[this.mModel.mLuxs.length - 1], dragDiff, 0.0f, this.mDragPoint, this.mModel.mLuxs[i], this.mModel.mNits[i], newLuxs, newNits);
                    limitPoint = limitPoint3;
                } else if (this.mDragPoint.nit < this.mSplines.get(DEFAULT_SPLINE).interpolate(LIGHT_DISTRIBUATION_REGION[1])) {
                    float dragDiff5 = dragDiff;
                    calculateCurveChange(this.mModel.mLuxs[0], this.mDragPoint.lux, 0.0f, dragDiff5, limitPoint3, this.mModel.mLuxs[i], this.mModel.mNits[i], newLuxs, newNits);
                    calculateCurveChange(this.mDragPoint.lux, LIGHT_DISTRIBUATION_REGION[1], dragDiff5, 0.0f, this.mDragPoint, this.mModel.mLuxs[i], this.mModel.mNits[i], newLuxs, newNits);
                    calculateCurveChange(LIGHT_DISTRIBUATION_REGION[1], this.mModel.mLuxs[this.mModel.mLuxs.length - 1], 0.0f, 0.0f, limitPoint3, this.mModel.mLuxs[i], this.mModel.mNits[i], newLuxs, newNits);
                    dragDiff = dragDiff5;
                    limitPoint = limitPoint3;
                } else {
                    float dragDiff6 = dragDiff;
                    calculateCurveChange(this.mModel.mLuxs[0], this.mDragPoint.lux, 0.0f, dragDiff6, limitPoint3, this.mModel.mLuxs[i], this.mModel.mNits[i], newLuxs, newNits);
                    dragDiff = dragDiff6;
                    calculateCurveChange(this.mDragPoint.lux, this.mModel.mLuxs[this.mModel.mLuxs.length - 1], dragDiff, 0.0f, this.mDragPoint, this.mModel.mLuxs[i], this.mModel.mNits[i], newLuxs, newNits);
                    limitPoint = limitPoint3;
                }
            } else if (this.mDragPoint.nit < this.mSplines.get(DEFAULT_SPLINE).interpolate(LIGHT_DISTRIBUATION_REGION[1])) {
                i = i2;
                calculateCurveChange(this.mModel.mLuxs[0], LIGHT_DISTRIBUATION_REGION[0], dragDiff, dragDiff, this.mDragPoint, this.mModel.mLuxs[i2], this.mModel.mNits[i2], newLuxs, newNits);
                OplusBrightnessPoint limitPoint4 = limitPoint;
                calculateCurveChange(LIGHT_DISTRIBUATION_REGION[0], LIGHT_DISTRIBUATION_REGION[1], dragDiff, 0.0f, limitPoint4, this.mModel.mLuxs[i], this.mModel.mNits[i], newLuxs, newNits);
                calculateCurveChange(LIGHT_DISTRIBUATION_REGION[1], this.mModel.mLuxs[this.mModel.mLuxs.length - 1], 0.0f, 0.0f, limitPoint4, this.mModel.mLuxs[i], this.mModel.mNits[i], newLuxs, newNits);
                dragDiff = dragDiff;
                limitPoint = limitPoint4;
            } else {
                i = i2;
                OplusBrightnessPoint limitPoint5 = limitPoint;
                float dragDiff7 = dragDiff;
                dragDiff = dragDiff7;
                calculateCurveChange(this.mModel.mLuxs[0], LIGHT_DISTRIBUATION_REGION[0], dragDiff, dragDiff7, this.mDragPoint, this.mModel.mLuxs[i], this.mModel.mNits[i], newLuxs, newNits);
                calculateCurveChange(LIGHT_DISTRIBUATION_REGION[0], this.mModel.mLuxs[this.mModel.mLuxs.length - 1], dragDiff, 0.0f, limitPoint5, this.mModel.mLuxs[i], this.mModel.mNits[i], newLuxs, newNits);
                limitPoint = limitPoint5;
            }
            i2 = i + 1;
        }
        if (!hasAddedLast) {
            newLuxs.add(Float.valueOf(this.mModel.mLuxs[this.mModel.mLuxs.length - 1]));
            newNits.add(Float.valueOf(this.mModel.mNits[this.mModel.mNits.length - 1]));
        }
        removeErrorsPoints(newLuxs, newNits);
        this.mDragLuxs = new float[newLuxs.size()];
        this.mDragNits = new float[newLuxs.size()];
        for (int i3 = 0; i3 < newLuxs.size(); i3++) {
            this.mDragLuxs[i3] = newLuxs.get(i3).floatValue();
            this.mDragNits[i3] = newNits.get(i3).floatValue();
        }
        if (OplusBrightnessUtils.isValidMap(this.mDragLuxs, this.mDragNits)) {
            this.mDragSpline = createSpline(DRAG_SPLINE, this.mDragLuxs, this.mDragNits);
        }
        return this.mDragPoint;
    }

    private void removeErrorsPoints(ArrayList<Float> srcX, ArrayList<Float> srcY) {
        if (srcX.size() != srcY.size()) {
            return;
        }
        Iterator<Float> iterX = srcX.iterator();
        Iterator<Float> iterY = srcY.iterator();
        Float prevX = null;
        Float prevY = null;
        while (iterX.hasNext() && iterY.hasNext()) {
            Float currX = iterX.next();
            Float currY = iterY.next();
            if (prevX != null && prevY != null && (prevX.floatValue() >= currX.floatValue() || prevY.floatValue() > currY.floatValue())) {
                iterX.remove();
                iterY.remove();
            } else {
                prevX = currX;
                prevY = currY;
            }
        }
    }

    private void calculateCurveChange(float startX, float endX, float startDiff, float endDiff, OplusBrightnessPoint point, float populateLux, float populateNit, ArrayList<Float> newLuxs, ArrayList<Float> newNits) {
        if (populateLux < startX || populateLux >= endX) {
            return;
        }
        float startNit = this.mSplines.get(DEFAULT_SPLINE).interpolate(startX);
        float endNit = this.mSplines.get(DEFAULT_SPLINE).interpolate(endX);
        float currNit = this.mSplines.get(DEFAULT_SPLINE).interpolate(populateLux);
        float deltaX = endNit - startNit;
        float deltaDiff = endDiff - startDiff;
        if (deltaX <= 0.0f) {
            return;
        }
        float slope = deltaDiff / deltaX;
        float nit = ((currNit - startNit) * slope) + startDiff + populateNit;
        if (point.lux <= populateLux && (newLuxs.isEmpty() || newLuxs.get(newLuxs.size() - 1).floatValue() < point.lux)) {
            newLuxs.add(Float.valueOf(point.lux));
            newNits.add(Float.valueOf(point.nit));
            for (int index = newNits.size(); index > 0; index--) {
                if (newNits.get(index - 1).floatValue() > point.nit) {
                    newNits.set(index - 1, Float.valueOf(point.nit));
                }
            }
        }
        if (Math.abs(point.lux - populateLux) < 0.01f) {
            return;
        }
        if (!newNits.isEmpty()) {
            float lastNit = newNits.get(newNits.size() - 1).floatValue();
            nit = Math.min(Math.max(nit, lastNit), Math.max(this.mModel.mNits[this.mModel.mNits.length - 1], lastNit));
        }
        newLuxs.add(Float.valueOf(populateLux));
        newNits.add(Float.valueOf(nit));
    }

    private void checkDragPointPosition(float srcX, OplusBrightnessPoint point, ArrayList<Float> newLuxs, ArrayList<Float> newNits) {
        if (point.lux <= srcX) {
            if (newLuxs.isEmpty() || newLuxs.get(newLuxs.size() - 1).floatValue() < point.lux) {
                newLuxs.add(Float.valueOf(point.lux));
                newNits.add(Float.valueOf(point.nit));
                for (int index = newNits.size(); index > 0; index--) {
                    if (newNits.get(index - 1).floatValue() > point.nit) {
                        newNits.set(index - 1, Float.valueOf(point.nit));
                    }
                }
            }
        }
    }

    public void reset() {
        if (this.mSplines != null) {
            this.mSplines.remove(DRAG_SPLINE);
        }
        this.mDragSpline = null;
    }

    public boolean isDragSpline() {
        return this.mDragSpline != null;
    }

    public OplusSpline getCurve() {
        if (this.mSplines.get(DRAG_SPLINE) != null) {
            return this.mSplines.get(DRAG_SPLINE);
        }
        return this.mSplines.get(DEFAULT_SPLINE);
    }

    public OplusSpline getCurve(String desc) {
        return this.mSplines.get(desc);
    }

    public String getDragSplineAsString() {
        if (this.mSplines.get(DRAG_SPLINE) != null) {
            return this.mSplines.get(DRAG_SPLINE).toString();
        }
        return null;
    }

    public boolean printAutoLuxIntervalForMarvels(String path) {
        OplusSpline lux2Nit = this.mSplines.get(DEFAULT_SPLINE);
        OplusSpline nit2Lux = this.mSplines.get(DEFAULT_SPLINE_INVERSE);
        OplusSpline nit2Brightness = this.mSplines.get(NIT_SPLINE);
        List<ArrayList<Float>> csvCache = new ArrayList<>();
        if (lux2Nit == null || nit2Lux == null || nit2Brightness == null) {
            Slog.w(this.TAG, "mDefaultnit2Lux is not inited.");
            return false;
        }
        int startNit = (int) Math.floor(lux2Nit.interpolate(0.0f));
        int maxNit = (int) Math.ceil(lux2Nit.interpolate(999999.0f));
        int maxBrightness = (int) getBrightnessModel().getMaxBrightness();
        for (int i = startNit; i < maxNit; i++) {
            ArrayList<Float> rowData = new ArrayList<>();
            rowData.add(Float.valueOf(nit2Lux.interpolate(i)));
            rowData.add(Float.valueOf(Math.min(maxBrightness, nit2Brightness.interpolate(i))));
            csvCache.add(rowData);
        }
        File file = new File(path);
        File parentpath = file.getParentFile();
        if (parentpath != null && !parentpath.exists()) {
            parentpath.mkdirs();
        }
        try (BufferedWriter csvWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8), BUFFER_SIZE)) {
            ArrayList<String> headTag = new ArrayList<>();
            headTag.add("oX");
            headTag.add("oY");
            writeRow(headTag, csvWriter);
            for (ArrayList<Float> rowData2 : csvCache) {
                csvWriter.newLine();
                writeRow(rowData2, csvWriter);
            }
            return true;
        } catch (Exception e) {
            Slog.e(this.TAG, "[IOException]saveToCsvFile fail " + e.toString());
            return false;
        }
    }

    private void saveToCsvFile(List<ArrayList<Float>> csvCache) {
        ArrayList<String> header = new ArrayList<>();
        header.add("oX");
        header.add("oY");
        header.add("LXLeft");
        header.add("LYLeft");
        header.add("LXRight");
        header.add("LYRight");
        header.add("SXLeft");
        header.add("SYLeft");
        header.add("SXRight");
        header.add("SYRight");
        header.add("STXLeft");
        header.add("STYLeft");
        header.add("STXRight");
        header.add("STYRight");
        File file = new File("/data/system/aibrightness.csv");
        File path = file.getParentFile();
        if (path != null && !path.exists()) {
            path.mkdirs();
        }
        try (BufferedWriter csvWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8), BUFFER_SIZE)) {
            writeRow(header, csvWriter);
            for (ArrayList<Float> rowData : csvCache) {
                csvWriter.newLine();
                writeRow(rowData, csvWriter);
            }
        } catch (Exception e) {
            Slog.e(this.TAG, "[IOException]saveToCsvFile fail " + e.toString());
        }
    }

    private void writeRow(ArrayList<? extends Object> rowDatas, BufferedWriter bufferedWriter) throws IOException {
        for (int i = 0; i < rowDatas.size(); i++) {
            if (i != 0) {
                bufferedWriter.write(",");
            }
            bufferedWriter.write("\"" + rowDatas.get(i) + "\"");
        }
    }
}
