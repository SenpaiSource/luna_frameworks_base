/*
 * Copyright (C) 2020 The Android Open Source Project
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

import static android.os.Trace.TRACE_TAG_VIBRATOR;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.hardware.vibrator.IVibrator;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IVibratorStateListener;
import android.os.Parcel;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.RichTapVibrationEffect;
import android.os.Trace;
import android.os.VibrationEffect;
import android.os.VibratorInfo;
import android.os.vibrator.PrebakedSegment;
import android.os.vibrator.PrimitiveSegment;
import android.os.vibrator.PwlePoint;
import android.os.vibrator.RampSegment;
import android.util.IndentingPrintWriter;
import android.util.Slog;

import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;

import libcore.util.NativeAllocationRegistry;

/** Controls a single vibrator. */
// TODO(b/409002423): remove this class once remove_hidl_support flag removed
final class VibratorController implements HalVibrator {
    private static final String TAG = "VibratorController";

    private final Object mLock = new Object();

    @GuardedBy("mLock")
    private final NativeWrapper mNativeWrapper;

    private final RemoteCallbackList<IVibratorStateListener> mVibratorStateListeners =
            new RemoteCallbackList<>();

    private volatile VibratorInfo mVibratorInfo;
    private volatile boolean mVibratorInfoLoadSuccessful;
    private volatile State mCurrentState;
    private volatile float mCurrentAmplitude;

    @Nullable
    private RichTapVibratorService mRichTapService;

    // RichTap completion scheduling.
    private Handler mRichTapHandler;
    @GuardedBy("mLock")
    private Object mDispatchToken;
    @GuardedBy("mLock")
    private boolean mRichTapVibrating;
    @GuardedBy("mLock")
    private long mRichTapDuration;
    @GuardedBy("mLock")
    private float mRichTapAmplitude = 1.0f;
    // Timestamp of the last tick-class primitive dispatch, used to preempt a tick when
    // a click-class prebaked effect follows within a short window.
    private volatile long mLastTickDispatchTime = -1;
    private volatile Callbacks mCallbacks;

    VibratorController(int vibratorId) {
        this(vibratorId, new NativeWrapper());
    }

    VibratorController(int vibratorId, NativeWrapper nativeWrapper) {
        mNativeWrapper = nativeWrapper;
        mVibratorInfo = new VibratorInfo.Builder(vibratorId).build();
        mCurrentState = State.IDLE;
    }

    @Override
    public void init(@NonNull Callbacks callbacks) {
        Trace.traceBegin(TRACE_TAG_VIBRATOR, "HalVibrator.init");
        try {
            mCallbacks = callbacks;
            int vibratorId = mVibratorInfo.getId();
            mNativeWrapper.init(vibratorId, callbacks);
            VibratorInfo.Builder vibratorInfoBuilder = new VibratorInfo.Builder(vibratorId);
            mVibratorInfoLoadSuccessful = mNativeWrapper.getInfo(vibratorInfoBuilder);
            if (RichTapVibrationEffect.isSupported()) {
                mRichTapService = new RichTapVibratorService();
                HandlerThread ht = new HandlerThread("RichTapHandler");
                ht.start();
                mRichTapHandler = new Handler(ht.getLooper());

                refreshRichTapInfo(vibratorInfoBuilder);
            }
            mVibratorInfo = vibratorInfoBuilder.build();
            if (!mVibratorInfoLoadSuccessful) {
                Slog.e(TAG, "Init failed to load some HAL info for vibrator " + vibratorId);
            }
            setExternalControl(false);
            off();
        } finally {
            Trace.traceEnd(TRACE_TAG_VIBRATOR);
        }
    }

    @Override
    public void onSystemReady() {
        Trace.traceBegin(TRACE_TAG_VIBRATOR, "HalVibrator.onSystemReady");
        try {
            if (!mVibratorInfoLoadSuccessful) {
                synchronized (mLock) {
                    if (!mVibratorInfoLoadSuccessful) {
                        int vibratorId = mVibratorInfo.getId();
                        VibratorInfo.Builder vibratorInfoBuilder =
                                new VibratorInfo.Builder(vibratorId);
                        mVibratorInfoLoadSuccessful = mNativeWrapper.getInfo(vibratorInfoBuilder);
                        if (RichTapVibrationEffect.isSupported()) {
                            refreshRichTapInfo(vibratorInfoBuilder);
                        }
                        mVibratorInfo = vibratorInfoBuilder.build();
                        if (!mVibratorInfoLoadSuccessful) {
                            Slog.e(TAG, "Failed retry of HAL getInfo for vibrator " + vibratorId);
                        }
                    }
                }
            }

            // Connect to the RichTap HAL on a background thread.
            if (mRichTapService != null && !mRichTapService.isAvailable()) {
                final RichTapVibratorService richTap = mRichTapService;
                new Thread(() -> richTap.init(), "RichTapInit").start();
            }
        } finally {
            Trace.traceEnd(TRACE_TAG_VIBRATOR);
        }
    }

    @Override
    public boolean registerVibratorStateListener(@NonNull IVibratorStateListener listener) {
        final long token = Binder.clearCallingIdentity();
        try {
            // Register the listener and send the first state atomically, to avoid potentially
            // out of order broadcasts in between.
            synchronized (mLock) {
                if (!mVibratorStateListeners.register(listener)) {
                    return false;
                }
                // Notify its callback after new client registered.
                notifyStateListener(listener, isVibrating(mCurrentState));
            }
            return true;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    @Override
    public boolean unregisterVibratorStateListener(@NonNull IVibratorStateListener listener) {
        final long token = Binder.clearCallingIdentity();
        try {
            return mVibratorStateListeners.unregister(listener);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    /** Checks if the {@link VibratorInfo} was loaded from the vibrator hardware successfully. */
    boolean isVibratorInfoLoadSuccessful() {
        return mVibratorInfoLoadSuccessful;
    }

    @NonNull
    @Override
    public VibratorInfo getInfo() {
        return mVibratorInfo;
    }

    @Override
    public boolean isVibrating() {
        return isVibrating(mCurrentState);
    }

    @Override
    public float getCurrentAmplitude() {
        return mCurrentAmplitude;
    }
    
    @Override
    public boolean usesRichTap() {
        return mRichTapService != null;
    }

    @Override
    public boolean setExternalControl(boolean externalControl) {
        Trace.traceBegin(TRACE_TAG_VIBRATOR,
                externalControl ? "HalVibrator.enableExternalControl"
                : "HalVibrator.disableExternalControl");
        try {
            if (!mVibratorInfo.hasCapability(IVibrator.CAP_EXTERNAL_CONTROL)) {
                return false;
            }
            State newState = externalControl ? State.UNDER_EXTERNAL_CONTROL : State.IDLE;
            synchronized (mLock) {
                mNativeWrapper.setExternalControl(externalControl);
                updateStateAndNotifyListenersLocked(newState);
            }
            return true;
        } finally {
            Trace.traceEnd(TRACE_TAG_VIBRATOR);
        }
    }

    @Override
    public boolean setAlwaysOn(int id, @Nullable PrebakedSegment prebaked) {
        Trace.traceBegin(TRACE_TAG_VIBRATOR,
                prebaked != null ? "HalVibrator.enableAlwaysOn" : "HalVibrator.disableAlwaysOn");
        try {
            if (!mVibratorInfo.hasCapability(IVibrator.CAP_ALWAYS_ON_CONTROL)) {
                return false;
            }
            synchronized (mLock) {
                if (prebaked == null) {
                    mNativeWrapper.alwaysOnDisable(id);
                } else {
                    mNativeWrapper.alwaysOnEnable(id, prebaked.getEffectId(),
                            prebaked.getEffectStrength());
                }
            }
            return true;
        } finally {
            Trace.traceEnd(TRACE_TAG_VIBRATOR);
        }
    }

    @Override
    public boolean setAmplitude(float amplitude) {
        Trace.traceBegin(TRACE_TAG_VIBRATOR, "HalVibrator.setAmplitude");
        try {
            boolean success = false;
            synchronized (mLock) {
                if (mRichTapService != null && mRichTapService.isAvailable()) {
                    int strength = (int) (255.0f * amplitude);
                    mRichTapService.richTapVibratorSetAmplitude(strength);
                    mRichTapAmplitude = amplitude;
                    success = true;
                } else if (mVibratorInfo.hasCapability(IVibrator.CAP_AMPLITUDE_CONTROL)) {
                    mNativeWrapper.setAmplitude(amplitude);
                    success = true;
                }
                mCurrentAmplitude = amplitude;
            }
            return success;
        } finally {
            Trace.traceEnd(TRACE_TAG_VIBRATOR);
        }
    }

    @Override
    public long on(long vibrationId, long stepId, long milliseconds) {
        Trace.traceBegin(TRACE_TAG_VIBRATOR, "HalVibrator.onMillis");
        try {
            if (mRichTapService != null) {
                if (mRichTapService.isAvailable()) {
                    Object token = newDispatchToken();
                    int strength = (int) (255 * mRichTapAmplitude);
                    if (strength > 0 && strength < 200) {
                        strength = 200;
                    }
                    mRichTapService.richTapVibratorSetAmplitude(strength);
                    mRichTapService.richTapVibratorOn(milliseconds);
                    synchronized (mLock) {
                        mDispatchToken = token;
                        setRichTapVibratingLocked(true, mRichTapAmplitude, milliseconds);
                    }
                    scheduleCompletion(token, vibrationId, stepId, milliseconds);
                    return milliseconds;
                }
                // HAL died — trigger reconnect for next call.
                final RichTapVibratorService richTap = mRichTapService;
                mRichTapHandler.post(() -> richTap.init());
                return 0;
            }
            synchronized (mLock) {
                long duration = mNativeWrapper.on(milliseconds, vibrationId, stepId);
                if (duration > 0) {
                    updateStateAndNotifyListenersLocked(State.VIBRATING);
                }
                return duration;
            }
        } finally {
            Trace.traceEnd(TRACE_TAG_VIBRATOR);
        }
    }

    @Override
    public long on(long vibrationId, long stepId, VibrationEffect.VendorEffect vendorEffect) {
        Trace.traceBegin(TRACE_TAG_VIBRATOR, "HalVibrator.onVendor");
        synchronized (mLock) {
            Parcel vendorData = Parcel.obtain();
            try {
                vendorEffect.getVendorData().writeToParcel(vendorData, /* flags= */ 0);
                vendorData.setDataPosition(0);
                long duration = mNativeWrapper.performVendorEffect(vendorData,
                        vendorEffect.getEffectStrength(), vendorEffect.getScale(),
                        vendorEffect.getAdaptiveScale(), vibrationId, stepId);
                if (duration > 0) {
                    updateStateAndNotifyListenersLocked(State.VIBRATING);
                }
                return duration;
            } finally {
                vendorData.recycle();
                Trace.traceEnd(TRACE_TAG_VIBRATOR);
            }
        }
    }

    @Override
    public long on(long vibrationId, long stepId, PrebakedSegment prebaked) {
        Trace.traceBegin(TRACE_TAG_VIBRATOR, "HalVibrator.onPrebaked");
        try {
            if (mRichTapService != null) {
                long duration = RichTapVibrationEffect.getInnerEffectDuration(
                        prebaked.getEffectId());
                if (duration > 0 && mRichTapService.isAvailable()) {
                    Object token = newDispatchToken();
                    int strength = RichTapVibrationEffect.getInnerEffectStrength(
                            prebaked.getEffectStrength());
                    if (strength <= 0) {
                        strength = 255;
                    }
                    if (strength < 200) {
                        strength = 200;
                    }
                    int prebakedId = prebaked.getEffectId();
                    // Track when a tick-class effect plays so click preemption can stop it.
                    // This covers the fallback path where HapticFeedbackVibrationProvider
                    // emits EFFECT_TICK instead of PRIMITIVE_TICK (stale VibratorInfo).
                    if (prebakedId == VibrationEffect.EFFECT_TICK) {
                        mLastTickDispatchTime = android.os.SystemClock.elapsedRealtime();
                    }
                    // If a tick just played and this is a click-class effect, stop the tick
                    // before it completes so only the click is felt (prevents double vibration).
                    if ((prebakedId == VibrationEffect.EFFECT_CLICK
                            || prebakedId == VibrationEffect.EFFECT_HEAVY_CLICK)
                            && android.os.SystemClock.elapsedRealtime()
                                    - mLastTickDispatchTime < 80) {
                        mRichTapService.richTapVibratorOff();
                    }
                    mRichTapService.richTapVibratorSetAmplitude(strength);
                    mRichTapService.richTapVibratorOn(duration);
                    synchronized (mLock) {
                        mDispatchToken = token;
                        setRichTapVibratingLocked(true, (float) strength / 255f, duration);
                    }
                    scheduleCompletion(token, vibrationId, stepId, duration);
                    return duration;
                }
                return 0;
            }
            synchronized (mLock) {
                long duration = mNativeWrapper.perform(prebaked.getEffectId(),
                        prebaked.getEffectStrength(), vibrationId, stepId);
                if (duration > 0) {
                    updateStateAndNotifyListenersLocked(State.VIBRATING);
                }
                return duration;
            }
        } finally {
            Trace.traceEnd(TRACE_TAG_VIBRATOR);
        }
    }

    @Override
    public long on(long vibrationId, long stepId, PrimitiveSegment[] primitives) {
        Trace.traceBegin(TRACE_TAG_VIBRATOR, "HalVibrator.onPrimitives");
        try {
            if (mRichTapService != null) {
                if (mRichTapService.isAvailable()
                        && primitives != null && primitives.length > 0) {
                    return dispatchRichTapPrimitives(vibrationId, stepId, primitives);
                }
                return 0;
            }
            if (!mVibratorInfo.hasCapability(IVibrator.CAP_COMPOSE_EFFECTS)) {
                return 0;
            }
            synchronized (mLock) {
                long duration = mNativeWrapper.compose(primitives, vibrationId, stepId);
                if (duration > 0) {
                    updateStateAndNotifyListenersLocked(State.VIBRATING);
                }
                return duration;
            }
        } finally {
            Trace.traceEnd(TRACE_TAG_VIBRATOR);
        }
    }

    private long dispatchRichTapPrimitives(long vibrationId, long stepId,
            PrimitiveSegment[] primitives) {
        Object token = newDispatchToken();
        long totalDuration = 0;
        boolean anyDispatched = false;

        // If all primitives are LOW_TICK (gesture bar touch rumble), drop them completely
        // so touching the gesture bar remains completely silent.
        boolean hasNonLowTick = false;
        for (PrimitiveSegment p : primitives) {
            if (p.getPrimitiveId() != VibrationEffect.Composition.PRIMITIVE_LOW_TICK) {
                hasNonLowTick = true;
                break;
            }
        }
        if (!hasNonLowTick) {
            scheduleCompletion(token, vibrationId, stepId, 1L);
            return 1L;
        }

        if (primitives.length == 1) {
            PrimitiveSegment prim = primitives[0];
            int primId = prim.getPrimitiveId();
            if (primId == VibrationEffect.Composition.PRIMITIVE_LOW_TICK) {
                // Too subtle for RichTap overhead; signal 1ms completion.
                scheduleCompletion(token, vibrationId, stepId, 1L);
                return 1L;
            }
            int mappedId = mapPrimitiveToEffectId(primId);
            float scale = prim.getScale();
            float boostedScale = (float) Math.pow(scale, 0.35);
            int strength = Math.max(200, (int) (255 * boostedScale));
            if (strength >= 10) {
                long effectDuration = getPrimitivePlayDuration(primId);
                long reportedDuration = effectDuration;
                if (mappedId == VibrationEffect.EFFECT_TICK) {
                    mLastTickDispatchTime = android.os.SystemClock.elapsedRealtime();
                    reportedDuration = 12;
                }
                scheduleDispatch(token, null, strength, 0, effectDuration);
                totalDuration = reportedDuration;
                anyDispatched = true;
            }
        } else if (primitives.length == 2
                && primitives[0].getDelay() == 0
                && primitives[0].getPrimitiveId() == VibrationEffect.Composition.PRIMITIVE_QUICK_RISE
                && primitives[1].getPrimitiveId() == VibrationEffect.Composition.PRIMITIVE_TICK) {
            // ASSISTANT_BUTTON: HEAVY_CLICK at full strength for a premium deep thump.
            long effectDuration = RichTapVibrationEffect.getInnerEffectDuration(
                    VibrationEffect.EFFECT_HEAVY_CLICK);
            scheduleDispatch(token, null, 255, 0, effectDuration);
            totalDuration = effectDuration;
            anyDispatched = true;
        } else {
            // To prevent terrible double-buzzes when the framework sends composed effects
            // (like [TICK, CLICK] for buttons), we ONLY dispatch the final non-LOW_TICK primitive
            // in the composition, matching VOS.
            PrimitiveSegment primitive = null;
            for (int i = primitives.length - 1; i >= 0; i--) {
                if (primitives[i].getPrimitiveId() != VibrationEffect.Composition.PRIMITIVE_LOW_TICK) {
                    primitive = primitives[i];
                    break;
                }
            }
            if (primitive == null) {
                scheduleCompletion(token, vibrationId, stepId, 1L);
                return 1L;
            }
            int mappedEffectId = mapPrimitiveToEffectId(primitive.getPrimitiveId());
            long effectDuration = RichTapVibrationEffect.getInnerEffectDuration(mappedEffectId);
            float scale = primitive.getScale();
            float boostedScale = (float) Math.pow(scale, 0.35);
            int strength = Math.max(200, (int) (255 * boostedScale));
            if (strength > 25) {
                long reportedDuration = effectDuration;
                if (mappedEffectId == VibrationEffect.EFFECT_TICK) {
                    reportedDuration = 12;
                }
                scheduleDispatch(token, null, strength, 0, effectDuration);
                totalDuration = reportedDuration;
                anyDispatched = true;
            }
        }

        if (!anyDispatched) {
            scheduleCompletion(token, vibrationId, stepId, 1L);
            return 1L;
        }
        synchronized (mLock) {
            mDispatchToken = token;
            setRichTapVibratingLocked(true, 1f, totalDuration);
        }
        scheduleCompletion(token, vibrationId, stepId, totalDuration);
        return totalDuration;
    }

    private static int mapPrimitiveToEffectId(int primitiveId) {
        switch (primitiveId) {
            case VibrationEffect.Composition.PRIMITIVE_CLICK:
                return VibrationEffect.EFFECT_CLICK;
            case VibrationEffect.Composition.PRIMITIVE_THUD:
                return VibrationEffect.EFFECT_THUD;
            case VibrationEffect.Composition.PRIMITIVE_SPIN:
                return VibrationEffect.EFFECT_POP;
            case VibrationEffect.Composition.PRIMITIVE_QUICK_FALL:
                return VibrationEffect.EFFECT_HEAVY_CLICK;
            case VibrationEffect.Composition.PRIMITIVE_SLOW_RISE:
            case VibrationEffect.Composition.PRIMITIVE_QUICK_RISE:
            case VibrationEffect.Composition.PRIMITIVE_TICK:
            default:
                return VibrationEffect.EFFECT_TICK;
        }
    }

    private static long getPrimitivePlayDuration(int primitiveId) {
        switch (primitiveId) {
            case VibrationEffect.Composition.PRIMITIVE_CLICK:
                return RichTapVibrationEffect.getInnerEffectDuration(VibrationEffect.EFFECT_CLICK);
            case VibrationEffect.Composition.PRIMITIVE_THUD:
                return RichTapVibrationEffect.getInnerEffectDuration(VibrationEffect.EFFECT_THUD);
            case VibrationEffect.Composition.PRIMITIVE_SPIN:
                return RichTapVibrationEffect.getInnerEffectDuration(VibrationEffect.EFFECT_POP);
            case VibrationEffect.Composition.PRIMITIVE_QUICK_FALL:
                return RichTapVibrationEffect.getInnerEffectDuration(
                        VibrationEffect.EFFECT_HEAVY_CLICK);
            case VibrationEffect.Composition.PRIMITIVE_QUICK_RISE:
                return 50L;
            case VibrationEffect.Composition.PRIMITIVE_SLOW_RISE:
                return 150L;
            case VibrationEffect.Composition.PRIMITIVE_TICK:
            default:
                return RichTapVibrationEffect.getInnerEffectDuration(VibrationEffect.EFFECT_TICK);
        }
    }

    private static Object newDispatchToken() {
        return new Object();
    }

    private void scheduleDispatch(Object token, int[] pattern, int strength,
            long delayMillis, long effectDuration) {
        if (delayMillis <= 0) {
            if (mRichTapService.isAvailable()) {
                mRichTapService.richTapVibratorSetAmplitude(strength);
                mRichTapService.richTapVibratorOn(effectDuration);
            }
            return;
        }
        mRichTapHandler.postDelayed(() -> {
            synchronized (mLock) {
                if (mDispatchToken != token) return;
            }
            if (mRichTapService.isAvailable()) {
                mRichTapService.richTapVibratorSetAmplitude(strength);
                mRichTapService.richTapVibratorOn(effectDuration);
            }
        }, token, delayMillis);
    }

    private void scheduleCompletion(Object token, long vibrationId, long stepId, long duration) {
        mRichTapHandler.postDelayed(() -> {
            synchronized (mLock) {
                if (mDispatchToken == token) {
                    mDispatchToken = null;
                    setRichTapVibratingLocked(false, 1.0f, 0);
                }
            }
            Callbacks callbacks = mCallbacks;
            if (callbacks != null) {
                callbacks.onVibrationStepComplete(mVibratorInfo.getId(), vibrationId, stepId);
            }
        }, token, duration);
    }

    private void setRichTapVibratingLocked(boolean vibrating, float amplitude, long duration) {
        mRichTapDuration = duration;
        mRichTapVibrating = vibrating;
        mRichTapAmplitude = vibrating ? amplitude : 1.0f;
    }

    private void refreshRichTapInfo(VibratorInfo.Builder b) {
        long caps = b.build().getCapabilities()
                | IVibrator.CAP_COMPOSE_EFFECTS | IVibrator.CAP_AMPLITUDE_CONTROL;
        b.setCapabilities(caps);
        b.setSupportedEffects(new int[]{
                VibrationEffect.EFFECT_CLICK,
                VibrationEffect.EFFECT_DOUBLE_CLICK,
                VibrationEffect.EFFECT_TICK,
                VibrationEffect.EFFECT_THUD,
                VibrationEffect.EFFECT_POP,
                VibrationEffect.EFFECT_HEAVY_CLICK,
                VibrationEffect.EFFECT_TEXTURE_TICK,
        });
        b.setSupportedPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 10);
        b.setSupportedPrimitive(VibrationEffect.Composition.PRIMITIVE_THUD, 10);
        b.setSupportedPrimitive(VibrationEffect.Composition.PRIMITIVE_SPIN, 10);
        b.setSupportedPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, 50);
        b.setSupportedPrimitive(VibrationEffect.Composition.PRIMITIVE_SLOW_RISE, 150);
        b.setSupportedPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_FALL, 50);
        b.setSupportedPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 10);
        // Exclude PRIMITIVE_LOW_TICK:
        // AAC linear resonant actuators cannot reproduce low-frequency rumble without
        // producing an audible mechanical click. Advertising LOW_TICK causes false double
        // vibrations when touching the navigation gesture bar or invoking Circle to Search.
        b.removeSupportedPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK);
        b.setPrimitiveDelayMax(5000);
        b.setCompositionSizeMax(100);
    }

    @Override
    public long on(long vibrationId, long stepId, RampSegment[] primitives) {
        Trace.traceBegin(TRACE_TAG_VIBRATOR, "HalVibrator.onPwleV1");
        try {
            if (!mVibratorInfo.hasCapability(IVibrator.CAP_COMPOSE_PWLE_EFFECTS)) {
                return 0;
            }
            synchronized (mLock) {
                int braking = mVibratorInfo.getDefaultBraking();
                long duration = mNativeWrapper.composePwle(
                        primitives, braking, vibrationId, stepId);
                if (duration > 0) {
                    updateStateAndNotifyListenersLocked(State.VIBRATING);
                }
                return duration;
            }
        } finally {
            Trace.traceEnd(TRACE_TAG_VIBRATOR);
        }
    }

    @Override
    public long on(long vibrationId, long stepId, PwlePoint[] pwlePoints) {
        Trace.traceBegin(TRACE_TAG_VIBRATOR, "HalVibrator.onPwleV2");
        try {
            if (!mVibratorInfo.hasCapability(IVibrator.CAP_COMPOSE_PWLE_EFFECTS_V2)) {
                return 0;
            }
            synchronized (mLock) {
                long duration = mNativeWrapper.composePwleV2(pwlePoints, vibrationId, stepId);
                if (duration > 0) {
                    updateStateAndNotifyListenersLocked(State.VIBRATING);
                }
                return duration;
            }
        } finally {
            Trace.traceEnd(TRACE_TAG_VIBRATOR);
        }
    }

    @Override
    public boolean off() {
        Trace.traceBegin(TRACE_TAG_VIBRATOR, "HalVibrator.off");
        try {
            Object staleToken;
            synchronized (mLock) {
                staleToken = mDispatchToken;
                mDispatchToken = null;
                // Skip off() for short effects that self-terminate.
                if (mRichTapService != null && mRichTapService.isAvailable()
                        && mRichTapVibrating && mRichTapDuration > 50) {
                    mRichTapService.richTapVibratorOff();
                }
                mRichTapAmplitude = 1.0f;
                setRichTapVibratingLocked(false, 1.0f, 0);
            }
            if (staleToken != null && mRichTapHandler != null) {
                mRichTapHandler.removeCallbacksAndMessages(staleToken);
            }
            // Skip native off() if native HAL was never started.
            if (mRichTapService == null || !mRichTapService.isAvailable()
                    || mCurrentState == State.VIBRATING) {
                synchronized (mLock) {
                    mNativeWrapper.off();
                }
            }
            synchronized (mLock) {
                updateStateAndNotifyListenersLocked(State.IDLE);
            }
            return true;
        } finally {
            Trace.traceEnd(TRACE_TAG_VIBRATOR);
        }
    }

    @Override
    public String toString() {
        boolean richTapVibrating;
        long richTapDuration;
        synchronized (mLock) {
            richTapVibrating = mRichTapVibrating;
            richTapDuration = mRichTapDuration;
        }
        return "VibratorController{"
                + "mVibratorInfo=" + mVibratorInfo
                + ", mVibratorInfoLoadSuccessful=" + mVibratorInfoLoadSuccessful
                + ", mCurrentState=" + mCurrentState.name()
                + ", mCurrentAmplitude=" + mCurrentAmplitude
                + ", mRichTapVibrating=" + richTapVibrating
                + ", mRichTapDuration=" + richTapDuration
                + ", mVibratorStateListeners count="
                + mVibratorStateListeners.getRegisteredCallbackCount()
                + '}';
    }

    @Override
    public void dump(IndentingPrintWriter pw) {
        pw.println("Vibrator (id=" + mVibratorInfo.getId() + "):");
        pw.increaseIndent();
        pw.println("currentState = " + mCurrentState.name());
        pw.println("currentAmplitude = " + mCurrentAmplitude);
        pw.println("vibratorInfoLoadSuccessful = " + mVibratorInfoLoadSuccessful);
        pw.println("vibratorStateListener size = "
                + mVibratorStateListeners.getRegisteredCallbackCount());
        mVibratorInfo.dump(pw);
        pw.decreaseIndent();
    }

    /**
     * Updates current vibrator state and notify listeners if {@link #isVibrating()} result changed.
     */
    @GuardedBy("mLock")
    private void updateStateAndNotifyListenersLocked(State state) {
        if (mCurrentState == State.IDLE && state == State.VIBRATING) {
            // First vibrate command.
            Trace.asyncTraceBegin(TRACE_TAG_VIBRATOR, "HalVibrator.vibration", 0);
        } else if (mCurrentState == State.VIBRATING && state == State.IDLE) {
            // First off after a vibrate command.
            Trace.asyncTraceEnd(TRACE_TAG_VIBRATOR, "HalVibrator.vibration", 0);
        }
        boolean previousIsVibrating = isVibrating(mCurrentState);
        final boolean newIsVibrating = isVibrating(state);
        mCurrentState = state;
        mCurrentAmplitude = newIsVibrating ? -1 : 0;
        if (previousIsVibrating != newIsVibrating) {
            // The broadcast method is safe w.r.t. register/unregister listener methods, but lock
            // is required here to guarantee delivery order.
            mVibratorStateListeners.broadcast(
                    listener -> notifyStateListener(listener, newIsVibrating));
        }
    }

    private void notifyStateListener(IVibratorStateListener listener, boolean isVibrating) {
        try {
            listener.onVibrating(isVibrating);
        } catch (RemoteException | RuntimeException e) {
            Slog.e(TAG, "Vibrator state listener failed to call", e);
        }
    }

    /** Returns true only if given state is not {@link State#IDLE}. */
    private static boolean isVibrating(State state) {
        return state != State.IDLE;
    }

    /** Wrapper around the static-native methods of {@link VibratorController} for tests. */
    @VisibleForTesting
    public static class NativeWrapper {
        /**
         * Initializes the native part of this controller, creating a global reference to given
         * {@link Callbacks} and returns a newly allocated native pointer. This
         * wrapper is responsible for deleting this pointer by calling the method pointed
         * by {@link #getNativeFinalizer()}.
         *
         * <p><b>Note:</b> Make sure the given implementation of {@link Callbacks}
         * do not hold any strong reference to the instance responsible for deleting the returned
         * pointer, to avoid creating a cyclic GC root reference.
         */
        private static native long nativeInit(int vibratorId, Callbacks callbacks);

        /**
         * Returns pointer to native function responsible for cleaning up the native pointer
         * allocated and returned by {@link #nativeInit(int, Callbacks)}.
         */
        private static native long getNativeFinalizer();

        private static native long on(long nativePtr, long milliseconds, long vibrationId,
                long stepId);

        private static native void off(long nativePtr);

        private static native void setAmplitude(long nativePtr, float amplitude);

        private static native long performEffect(long nativePtr, long effect, long strength,
                long vibrationId, long stepId);

        private static native long performVendorEffect(long nativePtr, Parcel vendorData,
                long strength, float scale, float adaptiveScale, long vibrationId, long stepId);

        private static native long performComposedEffect(long nativePtr, PrimitiveSegment[] effect,
                long vibrationId, long stepId);

        private static native long performPwleEffect(long nativePtr, RampSegment[] effect,
                int braking, long vibrationId, long stepId);

        private static native long performPwleV2Effect(long nativePtr, PwlePoint[] effect,
                long vibrationId, long stepId);

        private static native void setExternalControl(long nativePtr, boolean enabled);

        private static native void alwaysOnEnable(long nativePtr, long id, long effect,
                long strength);

        private static native void alwaysOnDisable(long nativePtr, long id);

        private static native boolean getInfo(long nativePtr, VibratorInfo.Builder infoBuilder);

        private long mNativePtr = 0;

        /** Initializes native controller and allocation registry to destroy native instances. */
        public void init(int vibratorId, Callbacks callbacks) {
            mNativePtr = nativeInit(vibratorId, callbacks);
            long finalizerPtr = getNativeFinalizer();

            if (finalizerPtr != 0) {
                NativeAllocationRegistry registry =
                        NativeAllocationRegistry.createMalloced(
                                VibratorController.class.getClassLoader(), finalizerPtr);
                registry.registerNativeAllocation(this, mNativePtr);
            }
        }

        /** Turns vibrator on for given time. */
        public long on(long milliseconds, long vibrationId, long stepId) {
            return on(mNativePtr, milliseconds, vibrationId, stepId);
        }

        /** Turns vibrator off. */
        public void off() {
            off(mNativePtr);
        }

        /** Sets the amplitude for the vibrator to run. */
        public void setAmplitude(float amplitude) {
            setAmplitude(mNativePtr, amplitude);
        }

        /** Turns vibrator on to perform one of the supported effects. */
        public long perform(long effect, long strength, long vibrationId, long stepId) {
            return performEffect(mNativePtr, effect, strength, vibrationId, stepId);
        }

        /** Turns vibrator on to perform a vendor-specific effect. */
        public long performVendorEffect(Parcel vendorData, long strength, float scale,
                float adaptiveScale, long vibrationId, long stepId) {
            return performVendorEffect(mNativePtr, vendorData, strength, scale, adaptiveScale,
                    vibrationId, stepId);
        }

        /** Turns vibrator on to perform effect composed of give primitives effect. */
        public long compose(PrimitiveSegment[] primitives, long vibrationId, long stepId) {
            return performComposedEffect(mNativePtr, primitives, vibrationId, stepId);
        }

        /** Turns vibrator on to perform PWLE effect composed of given primitives. */
        public long composePwle(RampSegment[] primitives, int braking, long vibrationId,
                long stepId) {
            return performPwleEffect(mNativePtr, primitives, braking, vibrationId, stepId);
        }

        /** Turns vibrator on to perform PWLE effect composed of given points. */
        public long composePwleV2(PwlePoint[] pwlePoints, long vibrationId, long stepId) {
            return performPwleV2Effect(mNativePtr, pwlePoints, vibrationId, stepId);
        }

        /** Enabled the device vibrator to be controlled by another service. */
        public void setExternalControl(boolean enabled) {
            setExternalControl(mNativePtr, enabled);
        }

        /** Enable always-on vibration with given id and effect. */
        public void alwaysOnEnable(long id, long effect, long strength) {
            alwaysOnEnable(mNativePtr, id, effect, strength);
        }

        /** Disable always-on vibration for given id. */
        public void alwaysOnDisable(long id) {
            alwaysOnDisable(mNativePtr, id);
        }

        /**
         * Loads device vibrator metadata and returns true if all metadata was loaded successfully.
         */
        public boolean getInfo(VibratorInfo.Builder infoBuilder) {
            return getInfo(mNativePtr, infoBuilder);
        }
    }
}
