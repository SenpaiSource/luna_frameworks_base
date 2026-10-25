/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.server.display;

/**
 * Human visual perception converter for display brightness.
 * Implements logarithmic perceptual smoothing matching stock BrightnessFeelingConverter.
 */
public final class BrightnessFeelingConverter {

    private BrightnessFeelingConverter() {
    }

    /**
     * Converts physical nits into perceptual feeling units.
     * S = k * ln(1 + I) (Weber-Fechner logarithmic perception model)
     */
    public static double getFeelingFromNit(float baseNit) {
        if (baseNit <= 0.0f) {
            return 0.0;
        }
        return Math.log(1.0f + baseNit);
    }

    /**
     * Converts perceptual feeling units into physical nits.
     */
    public static double getNitFromFeeling(double feeling) {
        return Math.max(0.0, Math.exp(feeling) - 1.0);
    }

    /**
     * Converts perceptual feeling delta back into target physical nits.
     */
    public static double getNitFromDeltaFeeling(float baseNit, double deltaFeeling) {
        double currentFeeling = getFeelingFromNit(baseNit);
        double targetFeeling = currentFeeling + deltaFeeling;
        return getNitFromFeeling(targetFeeling);
    }

    /**
     * Calculates the delta feeling between two nit levels.
     */
    public static double getDeltaFeelingFromNit(float baseNit, float toNit) {
        return getFeelingFromNit(toNit) - getFeelingFromNit(baseNit);
    }
}
