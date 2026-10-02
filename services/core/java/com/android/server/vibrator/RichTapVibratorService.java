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
package com.android.server.vibrator;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.hardware.vibrator.IVibrator;
import android.os.Binder;
import android.os.IBinder;
import android.os.ServiceManager;
import android.util.Slog;

import vendor.aac.hardware.richtap.vibrator.IRichtapCallback;
import vendor.aac.hardware.richtap.vibrator.IRichtapVibrator;

public class RichTapVibratorService {
    private static final String TAG = RichTapVibratorService.class.getSimpleName();
    private static final String VIBRATOR_DESCRIPTOR = IVibrator.DESCRIPTOR + "/default";
    private static final boolean DEBUG = false;
    private static final int RICHTAP_LOOPER_ONCE = 1;

    @NonNull
    private final IRichtapCallback mCallback;

    // Volatile so isAvailable() is a cheap field read with no IPC on the hot path.
    private volatile IRichtapVibrator sRichtapVibratorService = null;

    public RichTapVibratorService() {
        this(null);
    }

    public RichTapVibratorService(@Nullable IRichtapCallback callback) {
        mCallback = callback != null ? callback : new IRichtapCallback.Stub() {
            @Override
            public void onCallback(int status) {
                if (DEBUG) Slog.d(TAG, "onCallback status=" + status);
            }

            @Override
            public int getInterfaceVersion() {
                return IRichtapCallback.VERSION;
            }

            @Override
            public String getInterfaceHash() {
                return IRichtapCallback.HASH;
            }
        };
    }

    /**
     * Attempt a non-blocking connection to the RichTap HAL.
     * Safe to call from any thread. Does NOT block — uses getService() only.
     */
    public void init() {
        getRichtapService();
    }

    /**
     * Returns true if the RichTap HAL is currently connected.
     * This is a volatile field read — zero IPC, safe on any hot path.
     */
    public boolean isAvailable() {
        return sRichtapVibratorService != null;
    }

    /**
     * Non-blocking HAL connection attempt. Returns null if HAL is not yet available.
     * No pingBinder() — the death recipient handles stale proxy cleanup.
     */
    @Nullable
    private synchronized IRichtapVibrator getRichtapService() {
        if (sRichtapVibratorService != null) {
            return sRichtapVibratorService;
        }

        if (DEBUG) Slog.d(TAG, "Connecting to: " + VIBRATOR_DESCRIPTOR);

        // Non-blocking: getService() returns null immediately if HAL is not up.
        // We never call waitForDeclaredService() here to avoid blocking any thread.
        IBinder halBinder = ServiceManager.getService(VIBRATOR_DESCRIPTOR);
        if (halBinder == null) {
            if (DEBUG) Slog.d(TAG, "HAL not yet available, will retry on next call");
            return null;
        }
        halBinder = Binder.allowBlocking(halBinder);

        try {
            IBinder extBinder = halBinder.getExtension();
            if (extBinder == null) {
                Slog.e(TAG, "Extension binder is null");
                return null;
            }
            extBinder = Binder.allowBlocking(extBinder);
            IRichtapVibrator richtapService = IRichtapVibrator.Stub.asInterface(extBinder);
            VibHalDeathRecipient deathRecipient = new VibHalDeathRecipient(this);
            halBinder.linkToDeath(deathRecipient, 0);
            extBinder.linkToDeath(deathRecipient, 0);
            richtapService.init(mCallback);
            sRichtapVibratorService = richtapService;
            Slog.i(TAG, "RichTap HAL connected");
        } catch (Exception e) {
            Slog.e(TAG, "Failed to connect to RichTap HAL", e);
            sRichtapVibratorService = null;
        }
        return sRichtapVibratorService;
    }

    public void richTapVibratorOn(long millis) {
        try {
            IRichtapVibrator service = getRichtapService();
            if (service != null) {
                if (DEBUG) Slog.d(TAG, "vibratorOn " + millis + "ms");
                service.on((int) millis, mCallback);
            }
        } catch (Exception e) {
            Slog.e(TAG, "Failed to execute vibratorOn", e);
            resetHalServiceProxy();
        }
    }

    public void richTapVibratorOff() {
        try {
            IRichtapVibrator service = sRichtapVibratorService;
            if (service != null) {
                if (DEBUG) Slog.d(TAG, "vibratorOff");
                service.stop(mCallback);
                service.off(mCallback);
            }
        } catch (Exception e) {
            Slog.e(TAG, "Failed to execute vibratorOff", e);
            resetHalServiceProxy();
        }
    }

    public void richTapVibratorSetAmplitude(int amplitude) {
        try {
            IRichtapVibrator service = getRichtapService();
            if (service != null) {
                if (DEBUG) Slog.d(TAG, "setAmplitude " + amplitude);
                service.setAmplitude(amplitude, mCallback);
            }
        } catch (Exception e) {
            Slog.e(TAG, "Failed to set amplitude", e);
            resetHalServiceProxy();
        }
    }

    public void richTapVibratorOnRawPattern(@NonNull int[] pattern, int amplitude, int freq) {
        try {
            IRichtapVibrator service = getRichtapService();
            if (service != null) {
                if (DEBUG) Slog.d(TAG, "performHe amplitude=" + amplitude + " freq=" + freq);
                service.performHe(RICHTAP_LOOPER_ONCE, 0, amplitude, freq, pattern, mCallback);
            }
        } catch (Exception e) {
            Slog.e(TAG, "Failed to execute raw pattern", e);
            resetHalServiceProxy();
        }
    }

    void resetHalServiceProxy() {
        sRichtapVibratorService = null;
    }

    private static final class VibHalDeathRecipient implements IBinder.DeathRecipient {
        private final RichTapVibratorService mRichTapService;

        VibHalDeathRecipient(@NonNull RichTapVibratorService richtapService) {
            mRichTapService = richtapService;
        }

        @Override
        public void binderDied() {
            Slog.w(TAG, "Vibrator HAL died, resetting proxy");
            mRichTapService.resetHalServiceProxy();
        }
    }
}
