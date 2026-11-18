package com.android.server.display.util;

import android.os.Parcel;
import android.os.Parcelable;

/* JADX INFO: loaded from: /run/media/sathiya/data/scratch/deps.dex */
public class OplusBrightnessPoint implements Parcelable {
    public static final Parcelable.Creator<OplusBrightnessPoint> CREATOR = new Parcelable.Creator<OplusBrightnessPoint>() { // from class: com.android.server.display.util.OplusBrightnessPoint.1
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public OplusBrightnessPoint createFromParcel(Parcel in) {
            return new OplusBrightnessPoint(in);
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public OplusBrightnessPoint[] newArray(int size) {
            return new OplusBrightnessPoint[size];
        }
    };
    public float backlight;
    public float lux;
    public float nit;

    public OplusBrightnessPoint() {
    }

    public OplusBrightnessPoint(float lux, float nit) {
        this.lux = lux;
        this.nit = nit;
        this.backlight = nit;
    }

    public OplusBrightnessPoint(float lux, float nit, float backlight) {
        this.lux = lux;
        this.nit = nit;
        this.backlight = backlight;
    }

    protected OplusBrightnessPoint(Parcel in) {
        this.lux = in.readFloat();
        this.nit = in.readFloat();
        this.backlight = in.readFloat();
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeFloat(this.lux);
        parcel.writeFloat(this.nit);
        parcel.writeFloat(this.backlight);
    }

    public String toString() {
        return "(" + this.lux + ", " + this.nit + ", " + this.backlight + ")";
    }

    public static OplusBrightnessPoint createPoint(OplusBrightnessPoint point) {
        OplusBrightnessPoint newPoint = new OplusBrightnessPoint();
        newPoint.lux = point.lux;
        newPoint.nit = point.nit;
        newPoint.backlight = point.backlight;
        return newPoint;
    }
}
