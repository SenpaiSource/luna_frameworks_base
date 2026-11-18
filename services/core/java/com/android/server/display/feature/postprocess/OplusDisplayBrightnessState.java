package com.android.server.display.feature.postprocess;

import android.hardware.display.DisplayManagerInternal;
import com.android.server.display.util.OplusBrightnessUtils;
import java.util.ArrayList;
import java.util.List;

/* JADX INFO: loaded from: /run/media/sathiya/data/scratch/deps.dex */
public class OplusDisplayBrightnessState {
    private final float mBrightness;
    private final float mBrightnessBarScale;
    private final boolean mEdrSence;
    private final int mEdrType;
    private final float mHdrHbmBaseBrightness;
    private final float mHdrHbmBaseNit;
    private final boolean mIsDragging;
    private final boolean mKeepRateFlag;
    private final boolean mNeedPutDatabase;
    private final float mNit;
    private final float mPowerSaveFactor;
    private final int mRate;
    private final int mRateType;
    private final List<String> mReason;
    private final float mSdrBrightness;
    private final float mSdrNit;
    private final int mSdrRate;

    private OplusDisplayBrightnessState(Builder builder) {
        this.mBrightness = builder.getBrightness();
        this.mSdrBrightness = builder.getSdrBrightness();
        this.mNit = builder.getNit();
        this.mSdrNit = builder.getSdrNit();
        this.mRate = builder.getHdrNoAnimation() ? 0 : builder.getRate();
        this.mSdrRate = builder.getSdrNoAnimation() ? 0 : builder.getSdrRate();
        this.mRateType = builder.getRateType();
        this.mEdrType = builder.getEdrType();
        this.mNeedPutDatabase = builder.getNeedPutDatabase();
        this.mReason = builder.getReason();
        this.mHdrHbmBaseNit = builder.getHdrHbmBaseNit();
        this.mHdrHbmBaseBrightness = builder.getHdrHbmBaseBrightness();
        this.mBrightnessBarScale = builder.getBrightnessBarScale();
        this.mKeepRateFlag = builder.getKeepRateFlag();
        this.mPowerSaveFactor = builder.getPowerSaveFactor();
        this.mEdrSence = builder.isEdrSence();
        this.mIsDragging = builder.isDragging();
    }

    public float getBrightness() {
        return this.mBrightness;
    }

    public float getSdrBrightness() {
        return this.mSdrBrightness;
    }

    public float getHdrHbmBaseBrightness() {
        return this.mHdrHbmBaseBrightness;
    }

    public float getNit() {
        return this.mNit;
    }

    public float getSdrNit() {
        return this.mSdrNit;
    }

    public float getHdrHbmBaseNit() {
        return this.mHdrHbmBaseNit;
    }

    public float getBrightnessBarScale() {
        return this.mBrightnessBarScale;
    }

    public float getPowerSaveFactor() {
        return this.mPowerSaveFactor;
    }

    public int getRate() {
        return this.mRate;
    }

    public int getSdrRate() {
        return this.mSdrRate;
    }

    public int getRateType() {
        return this.mRateType;
    }

    public int getEdrType() {
        return this.mEdrType;
    }

    public boolean isDragging() {
        return this.mIsDragging;
    }

    public boolean getNeedPutDatabase() {
        return this.mNeedPutDatabase;
    }

    public boolean getKeepRateFlag() {
        return this.mKeepRateFlag;
    }

    public boolean isEdrSence() {
        return this.mEdrSence;
    }

    public List<String> getReason() {
        return this.mReason;
    }

    public String toString() {
        return "OplusDisplayBrightnessState:\n\t brightness = " + getBrightness() + "\n\t sdrBrightness = " + getSdrBrightness() + "\n\t hdrHbmBaseBrightness = " + getHdrHbmBaseBrightness() + "\n\t nit = " + getNit() + "\n\t sdrNit = " + getSdrNit() + "\n\t hdrHbmBaseNit = " + getHdrHbmBaseNit() + "\n\t rate = " + getRate() + "\n\t sdrRate = " + getSdrRate() + "\n\t rateType = " + OplusBrightnessUtils.totalRateTypeToString(getRateType()) + "\n\t isDragging = " + isDragging() + "\n\t needPutDatabase = " + getNeedPutDatabase() + "\n\t brightnessBarScale = " + getBrightnessBarScale() + "\n\t powerSaveFactor = " + getPowerSaveFactor() + "\n\t keepRateFlag = " + getKeepRateFlag() + "\n\t isEdrSence = " + isEdrSence() + "\n\t reason = " + getReason();
    }

    public static class Builder {
        private float mBrightness;
        private int mDisplayId;
        private float mDpcNit;
        private int mEdrType;
        private boolean mIsAutoBrightness;
        private boolean mIsDragging;
        private float mLastNit;
        private int mLastRateType;
        private float mNit;
        private int mRateType;
        private float mSdrBrightness;
        private float mSdrNit;
        private boolean mNeedPutDatabase = true;
        private boolean mAmbientLuxValid = false;
        private boolean mHasEdrProcessed = false;
        private boolean mKeepRateFlag = false;
        private boolean mHdrNoAnimation = false;
        private boolean mSdrNoAnimation = false;
        private boolean mEdrSence = false;
        private boolean mIsPrimaryDisplay = false;
        private float mHdrHbmBaseBrightness = Float.NaN;
        private float mHdrHbmBaseNit = Float.NaN;
        private float mBrightnessBarScale = 1.0f;
        private float mPowerSaveFactor = 1.0f;
        private int mRate = -1;
        private int mSdrRate = -1;
        private List<String> mReason = new ArrayList();
        private DisplayManagerInternal.DisplayPowerRequest mPowerReq = null;
        private CharSequence mTag = null;

        public void setWindowManagerBrightnessOverrideTag(CharSequence tag) {
            this.mTag = tag;
        }

        public CharSequence getWindowManagerBrightnessOverrideTag() {
            return this.mTag;
        }

        public float getBrightness() {
            return this.mBrightness;
        }

        public float getSdrBrightness() {
            return this.mSdrBrightness;
        }

        public float getHdrHbmBaseBrightness() {
            return this.mHdrHbmBaseBrightness;
        }

        public float getDpcNit() {
            return this.mDpcNit;
        }

        public float getNit() {
            return this.mNit;
        }

        public float getSdrNit() {
            return this.mSdrNit;
        }

        public float getHdrHbmBaseNit() {
            return this.mHdrHbmBaseNit;
        }

        public int getRate() {
            return this.mRate;
        }

        public int getSdrRate() {
            return this.mSdrRate;
        }

        public int getRateType() {
            return this.mRateType;
        }

        public boolean getNeedPutDatabase() {
            return this.mNeedPutDatabase;
        }

        public boolean getAmbientLuxValid() {
            return this.mAmbientLuxValid;
        }

        public boolean isDragging() {
            return this.mIsDragging;
        }

        public boolean isPrimaryDisplay() {
            return this.mIsPrimaryDisplay;
        }

        public boolean isAutoBrightnessMode() {
            return this.mIsAutoBrightness;
        }

        public boolean getKeepRateFlag() {
            return this.mKeepRateFlag;
        }

        public boolean isEdrSence() {
            return this.mEdrSence;
        }

        public int getDisplayId() {
            return this.mDisplayId;
        }

        public DisplayManagerInternal.DisplayPowerRequest getPowerReq() {
            return this.mPowerReq;
        }

        public int getEdrType() {
            return this.mEdrType;
        }

        public float getBrightnessBarScale() {
            return this.mBrightnessBarScale;
        }

        public float getPowerSaveFactor() {
            return this.mPowerSaveFactor;
        }

        public boolean hasEdrProcessed() {
            return this.mHasEdrProcessed;
        }

        public int getLastRateType() {
            return this.mLastRateType;
        }

        public List<String> getReason() {
            return this.mReason;
        }

        public boolean getSdrNoAnimation() {
            return this.mSdrNoAnimation;
        }

        public boolean getHdrNoAnimation() {
            return this.mHdrNoAnimation;
        }

        public float getLastTargetNit() {
            return this.mLastNit;
        }

        public Builder setLastTargetNit(float nit) {
            this.mLastNit = nit;
            return this;
        }

        public Builder setLastRateType(int rateType) {
            this.mLastRateType = rateType;
            return this;
        }

        public Builder setNoAnimation(boolean sdrNoAnimation, boolean hdrNoAnimation) {
            this.mSdrNoAnimation = sdrNoAnimation;
            this.mHdrNoAnimation = hdrNoAnimation;
            return this;
        }

        public Builder keepRate() {
            this.mKeepRateFlag = true;
            return this;
        }

        public Builder setBrightness(float brightness) {
            this.mBrightness = brightness;
            return this;
        }

        public Builder setSdrBrightness(float sdrBrightness) {
            this.mSdrBrightness = sdrBrightness;
            return this;
        }

        public Builder setHdrHbmBaseBrightness(float brightness) {
            this.mHdrHbmBaseBrightness = brightness;
            return this;
        }

        public Builder setNit(float nit) {
            this.mNit = nit;
            if (!this.mHasEdrProcessed) {
                setSdrNit(nit);
            }
            return this;
        }

        public Builder setDpcNit(float nit) {
            this.mDpcNit = nit;
            return this;
        }

        public Builder setHdrHbmBaseNit(float nit) {
            this.mHdrHbmBaseNit = nit;
            return this;
        }

        public Builder setSdrNit(float sdrNit) {
            this.mSdrNit = sdrNit;
            return this;
        }

        public Builder setRate(int rate) {
            return setRateWithType(rate, this.mRateType, true);
        }

        public Builder setRateWithType(int rate, int rateType) {
            return setRateWithType(rate, rateType, false);
        }

        public Builder setRateWithType(int rate, int rateType, boolean allowLowRate) {
            boolean updateRate = rate == 0 || (this.mRate != 0 && (rate > this.mRate || allowLowRate));
            if (updateRate) {
                this.mRate = rate;
                this.mRateType = rateType;
            }
            if (!this.mHasEdrProcessed) {
                setSdrRate(rate);
            }
            return this;
        }

        public Builder setEdrType(int edrType) {
            this.mEdrType = edrType;
            return this;
        }

        public Builder setEdrSence(boolean isEdr) {
            this.mEdrSence = isEdr;
            return this;
        }

        public Builder setSdrRate(int sdrRate) {
            if (sdrRate == 0 || (this.mSdrRate != 0 && sdrRate > this.mSdrRate)) {
                this.mSdrRate = sdrRate;
            }
            return this;
        }

        public Builder setReason(String reason) {
            this.mReason.add(reason);
            return this;
        }

        public Builder dontPutDatabase() {
            this.mNeedPutDatabase = false;
            return this;
        }

        public Builder setDragging(boolean dragging) {
            this.mIsDragging = dragging;
            this.mNeedPutDatabase = !dragging;
            return this;
        }

        public Builder setHasEdrProcessed() {
            this.mHasEdrProcessed = true;
            return this;
        }

        public Builder setIsAutoBrightness(boolean isAuto) {
            this.mIsAutoBrightness = isAuto;
            return this;
        }

        public Builder setAmbientLuxValid(boolean valid) {
            this.mAmbientLuxValid = valid;
            return this;
        }

        public Builder setDisplayId(int id) {
            this.mDisplayId = id;
            return this;
        }

        public Builder setPowerReq(DisplayManagerInternal.DisplayPowerRequest request) {
            this.mPowerReq = request;
            return this;
        }

        public Builder setBrightnessBarScale(float scale) {
            this.mBrightnessBarScale = scale;
            return this;
        }

        public Builder setPowerSaveFactor(float factor) {
            this.mPowerSaveFactor = factor;
            return this;
        }

        public Builder setPrimaryDisplay(boolean isPrimaryDisplay) {
            this.mIsPrimaryDisplay = isPrimaryDisplay;
            return this;
        }

        public OplusDisplayBrightnessState build() {
            return new OplusDisplayBrightnessState(this);
        }
    }
}
