/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.server.display.brightness.strategy;

import android.content.Context;
import android.service.dreams.DreamService;

import com.android.internal.R;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.display.DisplayBrightnessState;
import com.android.server.display.brightness.BrightnessEvent;
import com.android.server.display.brightness.BrightnessReason;
import com.android.server.display.brightness.BrightnessUtils;
import com.android.server.display.brightness.StrategyExecutionRequest;
import com.android.server.display.brightness.StrategySelectionNotifyRequest;
import com.android.server.display.feature.DisplayManagerFlags;

import java.io.PrintWriter;

/**
 * Manages the brightness of the display when the system is in the doze state. There are different
 * ways to determine the brightness:
 * - an override coming from Dream Manager (see {@link DreamService#setDozeScreenBrightnessFloat})
 * - the current brightness setting multiplied by the doze scale factor (if there is an offload
 *   session) (similar to how auto-brightness in doze is scaled)
 * - the default doze brightness
 */
public class DozeBrightnessStrategy implements DisplayBrightnessStrategy {
    private final DisplayManagerFlags mFlags;
    private final float mDozeScaleFactor;
    private float mDefaultDozeBrightness;
    private final Injector mInjector;

    // The DisplayId of the associated logical display
    private final int mDisplayId;
    private com.android.server.display.IDisplayBrightnessModelExt mBrightnessModel;

    public void setBrightnessModel(com.android.server.display.IDisplayBrightnessModelExt model) {
        mBrightnessModel = model;
    }

    public DozeBrightnessStrategy(DisplayManagerFlags flags, Context context, int displayId,
            float defaultDozeBrightness) {
        this(/* injector= */ null, flags, context, displayId, defaultDozeBrightness);
    }

    @VisibleForTesting
    DozeBrightnessStrategy(Injector injector, DisplayManagerFlags flags, Context context,
            int displayId, float defaultDozeBrightness) {
        mFlags = flags;
        mDisplayId = displayId;
        mDozeScaleFactor = context.getResources().getFraction(
                R.fraction.config_screenAutoBrightnessDozeScaleFactor,
                1, 1);
        mDefaultDozeBrightness = defaultDozeBrightness;
        mInjector = (injector == null) ? new Injector() : injector;
    }

    @Override
    public DisplayBrightnessState updateBrightness(
            StrategyExecutionRequest strategyExecutionRequest) {
        // Validate DreamManager's override
        float dozeScreenBrightness =
                strategyExecutionRequest.getDisplayPowerRequest().dozeScreenBrightness;
        if (BrightnessUtils.isValidBrightnessValue(dozeScreenBrightness)) {
            return BrightnessUtils.constructDisplayBrightnessState(
                    BrightnessReason.REASON_DOZE,
                    dozeScreenBrightness,
                    getName(), /* slowChange= */ false);
        }
        if (mFlags.isDisplayOffloadEnabled()
                && strategyExecutionRequest.getOffloadSession() != null) {
            BrightnessEvent brightnessEvent = mInjector.getBrightnessEvent(mDisplayId);
            brightnessEvent.setFlags(BrightnessEvent.FLAG_DOZE_SCALE);
            return new DisplayBrightnessState.Builder()
                    .setBrightness(getManualDozeBrightness(strategyExecutionRequest
                            .getCurrentScreenBrightness()))
                    .setBrightnessReason(BrightnessReason.REASON_DOZE_MANUAL)
                    .setDisplayBrightnessStrategyName(getName())
                    .setIsSlowChange(false)
                    .setBrightnessEvent(brightnessEvent)
                    .build();
        }

        // Use default brightness when dozing unless overridden, mapped through ColorOS lux_aod.json bands
        float defaultBrightness = mDefaultDozeBrightness;
        try {
            com.android.server.display.feature.ambience.utils.OplusLuxResult luxRes =
                    com.android.server.display.feature.ambience.OplusBrightnessAmbientAdapter.getInstance().getLuxResult();
            if (com.android.server.display.config.OplusAodLuxConfig.getInstance().getDeployMultiAod() == 1
                    && luxRes != null && !Float.isNaN(luxRes.getFinalLux()) && luxRes.getFinalLux() >= 0.0f) {
                float currentLux = luxRes.getFinalLux();
                int aodTier = com.android.server.display.config.OplusAodLuxConfig.getInstance().getAodTier(currentLux);
                float targetNit;
                if (aodTier == 1) {
                    targetNit = 2.5f; // Deep dark room floor (Table 30 minimum floor: 2.5 nits)
                } else if (aodTier == 2) {
                    targetNit = 10.0f; // Calibrated doze floor (stock LONGTAKE_AOD_NIT_10)
                } else {
                    targetNit = (float) android.os.SystemProperties.getInt("ro.oplus.display.aod.brightness", 50); // Daylight AOD (stock LONGTAKE_AOD_NIT_50)
                }

                if (mBrightnessModel != null) {
                    float bl = mBrightnessModel.getValueByCurve(com.android.server.display.util.OplusBrightnessConstants.NIT_SPLINE, targetNit);
                    float maxBl = mBrightnessModel.getMaxBrightness();
                    if (!Float.isNaN(bl) && bl > 0.0f && maxBl > 0.0f) {
                        defaultBrightness = bl / maxBl;
                    }
                } else {
                    if (aodTier == 1) {
                        defaultBrightness = Math.max(0.000488f, mDefaultDozeBrightness * (targetNit / 10.0f));
                    } else if (aodTier == 2) {
                        defaultBrightness = mDefaultDozeBrightness;
                    } else {
                        defaultBrightness = Math.max(mDefaultDozeBrightness, mDefaultDozeBrightness * (targetNit / 10.0f));
                    }
                }
            }
        } catch (Throwable t) {
            defaultBrightness = mDefaultDozeBrightness;
        }
        return BrightnessUtils.constructDisplayBrightnessState(BrightnessReason.REASON_DOZE_DEFAULT,
                defaultBrightness, getName(), /* slowChange= */ false);
    }

    @Override
    public String getName() {
        return "DozeBrightnessStrategy";
    }

    @Override
    public void dump(PrintWriter writer) {
        writer.println("DozeBrightnessStrategy:");
        writer.println("  mDozeScaleFactor:" + mDozeScaleFactor);
        writer.println("  mDefaultDozeBrightness:" + mDefaultDozeBrightness);
    }

    @Override
    public void strategySelectionPostProcessor(
            StrategySelectionNotifyRequest strategySelectionNotifyRequest) {
        // DO NOTHING
    }

    @Override
    public int getReason() {
        return BrightnessReason.REASON_DOZE;
    }

    /**
     * @param currentScreenBrightness The current screen brightness
     * @return The brightness manually selected by the user, scaled for doze.
     */
    public float getManualDozeBrightness(float currentScreenBrightness) {
        return currentScreenBrightness * mDozeScaleFactor;
    }

    public void setDefaultDozeBrightness(float brightness) {
        mDefaultDozeBrightness = brightness;
    }

    static class Injector {
        public BrightnessEvent getBrightnessEvent(int displayId) {
            return new BrightnessEvent(displayId);
        }
    }
}
