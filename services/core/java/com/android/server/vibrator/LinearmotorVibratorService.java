/*
 * Copyright (C) 2022 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.server.vibrator;

import android.content.Context;
import android.os.Binder;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Slog;

import com.android.server.SystemService;

import com.oplus.os.ILinearmotorVibratorService;
import com.oplus.os.WaveformEffect;


public class LinearmotorVibratorService extends SystemService {

    private static final String TAG = "LinearmotorVibratorService";

    private static final VibrationEffect EFFECT_CLICK =
            VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK);

    private final Vibrator mVibrator;

    private final Object mLock = new Object();

    private final ILinearmotorVibratorService.Stub mService = new ILinearmotorVibratorService.Stub() {
        @Override
        public void vibrate(int uid, String opPkg, WaveformEffect effect, IBinder token) {
            synchronized (mLock) {
                final long ident = Binder.clearCallingIdentity();
                try {
                    Slog.d(TAG, "WaveformEffect: " + effect);
                    // Currently maps all incoming effects to a standard EFFECT_CLICK
                    mVibrator.vibrate(EFFECT_CLICK);
                } finally {
                    Binder.restoreCallingIdentity(ident);
                }
            }
        }

        @Override
        public void cancelVibrate(WaveformEffect effect, IBinder token) {
            synchronized (mLock) {
                mVibrator.cancel();
            }
        }

        @Override
        public int getVibratorStatus() {
            return 0; // always ready
        }

        @Override
        public void setVibratorStrength(int strength) {
            // no-op
        }

        @Override
        public int getSettingsTouchEffectStrength() {
            return 2400;
        }

        @Override
        public int getSettingsRingtoneEffectStrength() {
            return 2400;
        }

        @Override
        public int getSettingsNotificationEffectStrength() {
            return 2400;
        }

        @Override
        public int getVibratorTouchStyle() {
            return 0;
        }

        @Override
        public void setVibratorTouchStyle(int style) {
            // no-op
        }

        @Override
        public void updateVibrationAmplitude(int uid, String opPkg, float amplitudeRatio) {
            // no-op
        }

        @Override
        public boolean checkRichtapBlackList(String packageName) {
            return false;
        }
    };

    public LinearmotorVibratorService(Context context) {
        super(context);
        mVibrator = context.getSystemService(Vibrator.class);
    }

    @Override
    public void onStart() {
        publishBinderService(Context.LINEARMOTOR_VIBRATOR_SERVICE, mService);
    }
}
