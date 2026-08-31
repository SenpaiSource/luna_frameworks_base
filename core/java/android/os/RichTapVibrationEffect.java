/*
 * Copyright (C) 2024-2025 Paranoid Android
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
package android.os;

import android.annotation.Nullable;
import android.content.res.Resources;
import android.util.Slog;

import com.android.internal.R;

/**
 * RichTap vibration effect implementation.
 * @hide
 */
public final class RichTapVibrationEffect {
    private static final String TAG = RichTapVibrationEffect.class.getSimpleName();

    // Prevent instantiation
    private RichTapVibrationEffect() {}

    /**
     * Checks if RichTap vibration is supported on this device.
     */
    public static boolean isSupported() {
        return Resources.getSystem().getBoolean(R.bool.config_usesRichtapVibration);
    }

    /**
     * Gets the inner effect pattern for a given vibration effect ID.
     * @param id The vibration effect ID
     * @return Array containing the effect pattern, or null if invalid
     */
    @Nullable
    public static int[] getInnerEffect(int id) {
        switch (id) {
            case VibrationEffect.EFFECT_CLICK:
                return new int[]{1, 4097, 0, 100, 65, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            case VibrationEffect.EFFECT_DOUBLE_CLICK:
                return new int[]{1, 4097, 0, 100, 80, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4097, 70, 100, 80, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            case VibrationEffect.EFFECT_TICK:
                return new int[]{1, 4097, 0, 100, 20, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            case VibrationEffect.EFFECT_THUD:
                return new int[]{1, 4097, 0, 100, 50, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            case VibrationEffect.EFFECT_POP:
                return new int[]{1, 4097, 0, 100, 65, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            case VibrationEffect.EFFECT_HEAVY_CLICK:
                return new int[]{1, 4097, 0, 100, 57, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            case VibrationEffect.EFFECT_TEXTURE_TICK:
                return new int[]{1, 4097, 0, 50, 33, 29, 0, 0, 0, 12, 59, 0, 22, 75, -21, 29, 0, 0, 4097, 30, 100, 30, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            default:
                Slog.w(TAG, "Invalid effect id: " + id);
                return null;
        }
    }

    /**
     * Gets an approximate playback duration (in milliseconds) for the given effect id's inner
     * pattern.
     *
     * <p>RichTap's {@code IRichtapVibrator} HAL is fire-and-forget: it does not report back a
     * duration, and {@link #getInnerEffect} does not encode an explicit total duration either
     * (each "transient" event only carries a relative start time, not a length). These values
     * are therefore conservative estimates, cross-checked against this device's own
     * {@code /vendor/etc/richtapresources} {@code .he} definitions where possible (e.g.
     * {@code double_click.he} plays its second transient 70ms after the first).
     *
     * <p>This must never return 0 or a negative value for an id that {@link #getInnerEffect}
     * can resolve: callers use this to report the vibration's "on" duration to the platform,
     * and a non-positive duration is interpreted as "unsupported/failed", which cancels the
     * whole vibration instead of just this segment.
     *
     * @param id The vibration effect ID
     * @return Approximate duration in milliseconds, always positive.
     */
    public static long getInnerEffectDuration(int id) {
        switch (id) {
            case VibrationEffect.EFFECT_CLICK:
            case VibrationEffect.EFFECT_POP:
                return 40L;
            case VibrationEffect.EFFECT_DOUBLE_CLICK:
                // Second transient starts 70ms after the first (see double_click.he) plus one
                // transient's own ring-down tail.
                return 110L;
            case VibrationEffect.EFFECT_TICK:
                return 25L;
            case VibrationEffect.EFFECT_THUD:
                return 60L;
            case VibrationEffect.EFFECT_HEAVY_CLICK:
                return 50L;
            case VibrationEffect.EFFECT_TEXTURE_TICK:
                return 45L;
            default:
                // Unknown id: still return a small positive placeholder so callers that already
                // confirmed getInnerEffect(id) != null do not accidentally cancel the vibration.
                return 30L;
        }
    }

    /**
     * Gets the inner effect strength value for a given strength level.
     * @param strength The desired effect strength
     * @return Strength value, or 0 if invalid
     */
    public static int getInnerEffectStrength(int strength) {
        switch (strength) {
            case VibrationEffect.EFFECT_STRENGTH_LIGHT:
                return 150;
            case VibrationEffect.EFFECT_STRENGTH_MEDIUM:
                return 200;
            case VibrationEffect.EFFECT_STRENGTH_STRONG:
                return 250;
            default:
                Slog.e(TAG, "Invalid effect strength: " + strength);
                return 0;
        }
    }
}
