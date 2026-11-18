package com.android.server.display.feature.postprocess;

import android.util.MathUtils;
import com.android.server.display.DisplayDeviceConfig;
import com.android.server.display.IDisplayBrightnessModelExt;
import com.android.server.display.feature.ambience.utils.AmbienceLog;
import com.android.server.display.util.OplusBrightnessConstants;

/**
 * Manages downstream post-processing for screen brightness, including EDR/HDR
 * enhancement and animation rate overrides matching stock ColorOS behavior.
 * Grounded in decompiled ColorOS 14/15 dumps for OnePlus SM7675/SM8650 (avalon).
 */
public class OplusBrightnessPostProcessingManager {
    private static final String TAG = "BrightnessPostProcessing";
    private int mDisplayId = 0;
    private IDisplayBrightnessModelExt mBrightnessModel = null;
    private OplusDisplayBrightnessState mLastOplusDisplayBrightnessState = null;
    private OplusDisplayBrightnessState mOplusDisplayBrightnessState = null;

    // EDR / HDR state tracking driven by display power controller / compositor
    private boolean mHasHdrLayers = false;
    private int mEdrType = OplusBrightnessConstants.EDR_TYPE_NORMAL;
    private int mGameFactor = 25;
    private boolean mIsEnhancedStatus = false;
    private boolean mIsExiting = false;
    private int mRateType = 0;

    public OplusBrightnessPostProcessingManager(int displayId, IDisplayBrightnessModelExt model) {
        this.mDisplayId = displayId;
        this.mBrightnessModel = model;
    }

    public void setHdrInfo(boolean hasHdrLayers, int edrType, int gameFactor) {
        this.mHasHdrLayers = hasHdrLayers;
        this.mEdrType = edrType;
        this.mGameFactor = (gameFactor > 0) ? gameFactor : 25;
    }

    public void setHdrInfo(boolean hasHdrLayers, float maxDesiredHdrRatio, int edrType, int gameFactor) {
        setHdrInfo(hasHdrLayers, edrType, gameFactor);
    }

    public boolean isHdrActive() {
        return this.mHasHdrLayers;
    }

    public int getEdrType() {
        return this.mEdrType;
    }

    public boolean isEnhancedStatus() {
        return this.mIsEnhancedStatus;
    }

    public boolean isExiting() {
        return this.mIsExiting;
    }

    public OplusDisplayBrightnessState postProcessing(OplusDisplayBrightnessState.Builder builder, int initialRate) {
        return postProcessing(builder, initialRate, -1.0f, -1.0f);
    }

    public OplusDisplayBrightnessState postProcessing(OplusDisplayBrightnessState.Builder builder, int initialRate,
            float currentBrightness, float currentSdrBrightness) {
        if (this.mBrightnessModel != null) {
            float sdrNit = builder.getSdrNit();
            if (Float.isNaN(sdrNit) || sdrNit <= 0.0f) {
                sdrNit = builder.getNit();
                builder.setSdrNit(sdrNit);
            }

            // EDR / HDR enhancement pipeline matching stock avalon OplusFeatureEdrEnhanceBrightness
            boolean isEdrScene = this.mHasHdrLayers || builder.isEdrSence();
            if (isEdrScene && this.mEdrType != OplusBrightnessConstants.EDR_TYPE_NORMAL) {
                float enhancedNit;
                if (this.mEdrType == OplusBrightnessConstants.EDR_TYPE_GAME) {
                    enhancedNit = this.mBrightnessModel.calculateEnhanceNit(sdrNit, this.mGameFactor);
                } else {
                    // Avalon stock: <hdr_vision> enhance curve (mode 0)
                    enhancedNit = this.mBrightnessModel.calculateEnhanceNit(sdrNit, 0);
                }

                float maxPanelNit = this.mBrightnessModel.getMaxPanelNit();
                if (maxPanelNit > 0.0f) {
                    enhancedNit = Math.min(enhancedNit, maxPanelNit);
                }

                int edrRate;
                int sdrRate;
                if (!this.mIsEnhancedStatus) {
                    // Enter transition: RateType 9 (EDR_ENTER)
                    this.mRateType = OplusBrightnessConstants.BRIGHTNESS_RATE_TYPE_EDR_ENTER;
                    this.mIsEnhancedStatus = true;
                    this.mIsExiting = false;
                    edrRate = (this.mEdrType == OplusBrightnessConstants.EDR_TYPE_HDR_VISION)
                            ? this.mBrightnessModel.getBrightnessRampRateHdrVisionEnter()
                            : this.mBrightnessModel.getBrightnessRampRateEdrEnter();
                    if (edrRate <= 0) edrRate = (this.mEdrType == OplusBrightnessConstants.EDR_TYPE_HDR_VISION) ? 94 : 255;
                    sdrRate = this.mBrightnessModel.getBrightnessRampRateSlow();
                    if (sdrRate <= 0) sdrRate = 40;
                } else {
                    // Steady EDR / HDR: RateType 7 (HDRHBM)
                    this.mRateType = OplusBrightnessConstants.BRIGHTNESS_RATE_TYPE_HDRHBM;
                    edrRate = this.mBrightnessModel.getBrightnessRampRateSlow();
                    if (edrRate <= 0) edrRate = 40;
                    sdrRate = edrRate;
                }

                builder.setHasEdrProcessed()
                       .setEdrSence(true)
                       .setEdrType(this.mEdrType)
                       .setHdrHbmBaseNit(sdrNit)
                       .setNit(enhancedNit)
                       .setRateWithType(edrRate, this.mRateType, true)
                       .setSdrRate(sdrRate);
            } else if (this.mIsEnhancedStatus || this.mIsExiting) {
                if (currentBrightness >= 0.0f && isEdrExited(sdrNit, currentBrightness, currentSdrBrightness)) {
                    this.mIsExiting = false;
                    this.mIsEnhancedStatus = false;
                    this.mRateType = -1;
                } else {
                    // Exit transition when HDR is no longer active: RateType 10 (EDR_EXIT)
                    this.mRateType = OplusBrightnessConstants.BRIGHTNESS_RATE_TYPE_EDR_EXIT;
                    this.mIsEnhancedStatus = false;
                    this.mIsExiting = true;
                    int edrExitRate = this.mBrightnessModel.getBrightnessRampRateEdrExit();
                    if (edrExitRate <= 0) edrExitRate = 47;
                    int sdrRate = this.mBrightnessModel.getBrightnessRampRateSlow();
                    if (sdrRate <= 0) sdrRate = 40;
                    builder.setRateWithType(edrExitRate, this.mRateType, true)
                           .setSdrRate(sdrRate)
                           .keepRate();
                }
            }

            builder.setNit(this.mBrightnessModel.clampNit(builder.getNit()));
            convertNitToBrightness(builder);
        }

        int temRate = initialRate;
        if (builder.isDragging() && this.mBrightnessModel != null) {
            temRate = this.mBrightnessModel.getBrightnessRampRateFastManual();
            if (temRate <= 0) temRate = this.mBrightnessModel.getBrightnessRampRateFast();
        }
        if (builder.isEdrSence() && builder.getSdrRate() == -1) {
            builder.setSdrRate(temRate);
        }
        if (builder.getRate() == -1 || builder.isDragging()) {
            builder.setRateWithType(temRate, builder.isAutoBrightnessMode()
                    ? OplusBrightnessConstants.BRIGHTNESS_RATE_TYPE_AUTO
                    : OplusBrightnessConstants.BRIGHTNESS_RATE_TYPE_MANUAL);
        }
        if (this.mLastOplusDisplayBrightnessState != null
                && this.mLastOplusDisplayBrightnessState.getKeepRateFlag()
                && !builder.getKeepRateFlag()) {
            builder.setRateWithType(this.mLastOplusDisplayBrightnessState.getRate(),
                    this.mLastOplusDisplayBrightnessState.getRateType());
            builder.setSdrRate(this.mLastOplusDisplayBrightnessState.getSdrRate());
        }

        this.mLastOplusDisplayBrightnessState = this.mOplusDisplayBrightnessState;
        this.mOplusDisplayBrightnessState = builder.build();
        AmbienceLog.d(TAG, this.mOplusDisplayBrightnessState.toString());
        return this.mOplusDisplayBrightnessState;
    }

    private boolean isEdrExited(float sdrNit, float currentBrightness, float currentSdrBrightness) {
        if (this.mRateType != OplusBrightnessConstants.BRIGHTNESS_RATE_TYPE_EDR_EXIT
                || this.mBrightnessModel == null) {
            return false;
        }
        float targetBrightness = getBrightnessByNit(sdrNit);
        float maxBrightness = this.mBrightnessModel.getMaxBrightness();
        if (maxBrightness > 0.0f) {
            targetBrightness /= maxBrightness;
        } else {
            targetBrightness /= 4095.0f;
        }
        return Math.abs(targetBrightness - currentBrightness) < 0.002f
                && Math.abs(targetBrightness - currentSdrBrightness) < 0.002f;
    }

    public void setBrightnessModel(IDisplayBrightnessModelExt model) {
        this.mBrightnessModel = model;
    }

    private void convertNitToBrightness(OplusDisplayBrightnessState.Builder builder) {
        builder.setBrightness(clampAbsoluteBrightness(getBrightnessByNit(builder.getNit())));
        builder.setSdrBrightness(clampAbsoluteBrightness(getBrightnessByNit(builder.getSdrNit())));
        if (!Float.isNaN(builder.getHdrHbmBaseNit())) {
            builder.setHdrHbmBaseBrightness(clampAbsoluteBrightness(getBrightnessByNit(builder.getHdrHbmBaseNit())));
        }
    }

    private float clampAbsoluteBrightness(float value) {
        if (this.mBrightnessModel == null) return value;
        float minLimit = this.mBrightnessModel.getMinBrightness();
        float maxLimit = this.mBrightnessModel.getTotalBrightness();
        if (maxLimit <= 0.0f) maxLimit = this.mBrightnessModel.getMaxBrightness();
        if (maxLimit <= 0.0f) maxLimit = 1.0f;
        return MathUtils.constrain(value, minLimit, maxLimit);
    }

    public float getBrightnessByNit(float nit) {
        if (this.mBrightnessModel == null) return nit;
        float val = this.mBrightnessModel.getValueByCurve(OplusBrightnessConstants.NIT_SPLINE, nit);
        if (Float.isNaN(val) || val <= 0.0f) {
            float normalMax = this.mBrightnessModel.getNormalMaxNit();
            if (normalMax > 0.0f) {
                return (nit / normalMax) * this.mBrightnessModel.getMaxBrightness();
            }
            return nit;
        }
        return val;
    }

    public float getNitByBrightness(float brightness) {
        if (this.mBrightnessModel == null) return brightness;
        float val = this.mBrightnessModel.getValueByCurve(OplusBrightnessConstants.BACKLIGHT_SPLINE, brightness);
        if (Float.isNaN(val) || val <= 0.0f) {
            float maxBright = this.mBrightnessModel.getMaxBrightness();
            if (maxBright > 0.0f) {
                return (brightness / maxBright) * this.mBrightnessModel.getNormalMaxNit();
            }
            return brightness;
        }
        return val;
    }

    public float getScreenNormalMaxBrightness() {
        if (this.mBrightnessModel == null) return 1.0f;
        return this.mBrightnessModel.getMaxBrightness();
    }
}
