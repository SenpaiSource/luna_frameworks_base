package com.android.server.display.model;

import android.util.Slog;
import android.util.Spline;

/* JADX INFO: loaded from: /run/media/sathiya/data/scratch/dex_extract/smali_oplus1/com/android/server/display/model/OplusSpline.smali */
public class OplusSpline extends Spline {
    private static final float EXPO_HEIGHT = 3.0f;
    private static final float FLOAT_HALF = 0.5f;
    private static final int INTERPOLATE_COE1 = 1;
    private static final int INTERPOLATE_COE2 = 2;
    private static final int INTERPOLATE_COE3 = 3;
    private static final int MIN_LENGTH = 2;
    public static final String TAG = "OplusDisplaySplineManager";
    private float[] mM;
    private float[] mX;
    private float[] mY;
    private float mReviseXChangePoint = 36.0f;
    private float mReviseX = 54.0f;
    private float mReviseXMultiple = 10.0f;
    private float mPrecision = FLOAT_HALF;

    public OplusSpline(float[] x, float[] y) {
        if (x == null || y == null || x.length != y.length || x.length < 2) {
            throw new IllegalArgumentException("There must be at least two control points and the arrays must be of equal length.");
        }
        int n = x.length;
        float[] d = new float[n - 1];
        float[] m = new float[n];
        for (int i = 0; i < n - 1; i += INTERPOLATE_COE1) {
            float h = x[i + INTERPOLATE_COE1] - x[i];
            if (h <= 0.0f) {
                Slog.w(TAG, "x[" + i + "] = " + x[i] + ", x[" + (i + INTERPOLATE_COE1) + "] = " + x[i + INTERPOLATE_COE1]);
                if (i > 0) {
                    d[i] = d[i - 1];
                } else {
                    d[i] = 0.0f;
                }
            } else {
                d[i] = (y[i + INTERPOLATE_COE1] - y[i]) / h;
            }
        }
        m[0] = d[0];
        for (int i2 = INTERPOLATE_COE1; i2 < n - 1; i2 += INTERPOLATE_COE1) {
            m[i2] = (d[i2 - 1] + d[i2]) * FLOAT_HALF;
        }
        m[n - 1] = d[n - 2];
        for (int i3 = 0; i3 < n - 1; i3 += INTERPOLATE_COE1) {
            if (d[i3] == 0.0f) {
                m[i3] = 0.0f;
                m[i3 + INTERPOLATE_COE1] = 0.0f;
            } else {
                float a = m[i3] / d[i3];
                float b = m[i3 + INTERPOLATE_COE1] / d[i3];
                if (a < 0.0f || b < 0.0f) {
                    throw new IllegalArgumentException("The control points must have monotonic Y values.");
                }
                float h2 = (float) Math.hypot(a, b);
                if (h2 > EXPO_HEIGHT) {
                    float t = EXPO_HEIGHT / h2;
                    m[i3] = m[i3] * t;
                    int i4 = i3 + INTERPOLATE_COE1;
                    m[i4] = m[i4] * t;
                }
            }
        }
        this.mX = x;
        this.mY = y;
        this.mM = m;
    }

    public float interpolate(float x) {
        int n = this.mX.length;
        if (Float.isNaN(x)) {
            return x;
        }
        if (x <= this.mX[0]) {
            return this.mY[0];
        }
        if (x >= this.mX[n - 1]) {
            return this.mY[n - 1];
        }
        int i = 0;
        while (x >= this.mX[i + INTERPOLATE_COE1]) {
            i += INTERPOLATE_COE1;
            if (x == this.mX[i]) {
                return this.mY[i];
            }
        }
        float h = this.mX[i + INTERPOLATE_COE1] - this.mX[i];
        float t = (x - this.mX[i]) / h;
        return (((this.mY[i] * ((t * 2.0f) + 1.0f)) + (this.mM[i] * h * t)) * (1.0f - t) * (1.0f - t)) + (((this.mY[i + INTERPOLATE_COE1] * (EXPO_HEIGHT - (2.0f * t))) + (this.mM[i + INTERPOLATE_COE1] * h * (t - 1.0f))) * t * t);
    }

    public float interpolateInverse(float y) {
        int n = this.mX.length;
        if (y <= this.mY[0]) {
            return this.mX[0];
        }
        if (y >= this.mY[n - 1]) {
            return this.mX[n - 1];
        }
        float x = this.mX[n - 1];
        float left = this.mX[0];
        float right = this.mX[n - 1];
        int index = binarySearchX(y);
        if (index != -1) {
            x = this.mX[index];
            right = this.mX[index];
        }
        float prev = 0.0f;
        while (x >= left && x <= right) {
            float curr = interpolate(x);
            if (y == curr) {
                return x;
            }
            if (y > curr && y < prev) {
                float diff1 = y - curr;
                float diff2 = prev - y;
                if (diff1 > diff2) {
                    return 1.0f + x;
                }
                return x;
            }
            prev = curr;
            x -= this.mPrecision;
        }
        return x;
    }

    public float interpolateHbm(float x) {
        float ret = 0.0f;
        int length = this.mY.length;
        for (int i = 0; i < length; i += INTERPOLATE_COE1) {
            if (x >= this.mX[i]) {
                ret = this.mY[i];
            }
        }
        return ret;
    }

    public String toString() {
        StringBuilder str = new StringBuilder();
        int n = this.mX.length;
        str.append("MonotoneCubicSpline{[");
        for (int i = 0; i < n; i += INTERPOLATE_COE1) {
            if (i != 0) {
                str.append(", ");
            }
            str.append("(").append(this.mX[i]);
            str.append(", ").append(this.mY[i]);
            str.append(": ").append(this.mM[i]).append(")");
        }
        str.append("]}");
        return str.toString();
    }

    public float[] getX() {
        return this.mX;
    }

    public float[] getY() {
        return this.mY;
    }

    public float transX(float lux) {
        if (lux < this.mReviseXChangePoint) {
            float x = (float) Math.sqrt(lux);
            return x;
        }
        float x2 = ((float) (Math.sqrt(lux) + ((double) this.mReviseX))) / this.mReviseXMultiple;
        return x2;
    }

    public float restoreTransX(float x) {
        if (x >= Math.sqrt(this.mReviseXChangePoint)) {
            float lux = this.mReviseXMultiple;
            float tmp = (lux * x) - this.mReviseX;
            float lux2 = tmp * tmp;
            return lux2;
        }
        if (x >= 0.0f) {
            float lux3 = x * x;
            return lux3;
        }
        float lux4 = -(x * x);
        return lux4;
    }

    private int binarySearchX(float value) {
        int low = 0;
        int high = this.mX.length - 1;
        if (value <= this.mY[0] || value >= this.mY[high]) {
            return -1;
        }
        while (low < high) {
            int middle = (low + high) / 2;
            if (middle == low || middle == high) {
                break;
            }
            if (value <= this.mY[middle] && middle - 1 >= 0 && value > this.mY[middle - 1]) {
                return middle;
            }
            if (value > this.mY[middle]) {
                low = middle;
            } else {
                high = middle;
            }
        }
        return -1;
    }

    private float directSearchX(float value) {
        float x = 0.0f;
        while (x < this.mX[this.mX.length - 1]) {
            float find = interpolate(x);
            if (value == find) {
                return x;
            }
            if (value > interpolate(x) && value < interpolate(x + 1.0f)) {
                return 1.0f + x;
            }
            x += this.mPrecision;
        }
        return x;
    }
}
