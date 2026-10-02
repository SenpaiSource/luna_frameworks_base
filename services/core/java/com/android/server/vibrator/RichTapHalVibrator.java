/*
 * Copyright (C) 2025 The Android Open Source Project
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
import android.hardware.vibrator.IVibrator;
import android.os.Handler;
import android.os.IVibratorStateListener;
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
import android.util.IntArray;
import android.util.Slog;
import android.util.SparseBooleanArray;

import com.android.internal.annotations.GuardedBy;

final class RichTapHalVibrator implements HalVibrator {
    private static final String TAG = "RichTapHalVibrator";

    private final HalVibrator mDelegate;
    private final RichTapVibratorService mRichTap;
    private final Handler mHandler;
    private final Object mLock = new Object();

    private final RemoteCallbackList<IVibratorStateListener> mListeners =
            new RemoteCallbackList<>();

    @GuardedBy("mLock")
    private boolean mRichTapVibrating;
    @GuardedBy("mLock")
    private float mRichTapAmplitude = 1.0f;
    @GuardedBy("mLock")
    private long mRichTapDuration;
    @GuardedBy("mLock")
    private Object mDispatchToken;

    private volatile Callbacks mCallbacks;
    private volatile VibratorInfo mInfo;

    static HalVibrator wrapIfNeeded(HalVibrator delegate, Handler handler) {
        return RichTapVibrationEffect.isSupported()
                ? new RichTapHalVibrator(delegate, handler)
                : delegate;
    }

    private RichTapHalVibrator(HalVibrator delegate, Handler handler) {
        mDelegate = delegate;
        mHandler = handler;
        mRichTap = new RichTapVibratorService();
        mInfo = delegate.getInfo();
    }

    @Override
    public void init(@NonNull Callbacks callbacks) {
        mCallbacks = callbacks;
        mDelegate.init(callbacks);
        mRichTap.init();
        refreshInfo();
    }

    @Override
    public void onSystemReady() {
        mDelegate.onSystemReady();
        mRichTap.init();
        refreshInfo();
    }

    @NonNull
    @Override
    public VibratorInfo getInfo() {
        return mInfo;
    }

    @Override
    public boolean isVibrating() {
        synchronized (mLock) {
            return mRichTapVibrating || mDelegate.isVibrating();
        }
    }

    @Override
    public boolean usesRichTap() {
        return true;
    }

    @Override
    public float getCurrentAmplitude() {
        synchronized (mLock) {
            if (mRichTapVibrating) {
                return mRichTapAmplitude;
            }
        }
        return mDelegate.getCurrentAmplitude();
    }

    @Override
    public boolean registerVibratorStateListener(@NonNull IVibratorStateListener listener) {
        boolean registered;
        boolean currentlyVibrating;
        synchronized (mLock) {
            registered = mListeners.register(listener);
            currentlyVibrating = mRichTapVibrating || mDelegate.isVibrating();
        }
        if (!registered) {
            return false;
        }
        mDelegate.registerVibratorStateListener(listener);
        notifyStateListener(listener, currentlyVibrating);
        return true;
    }

    @Override
    public boolean unregisterVibratorStateListener(@NonNull IVibratorStateListener listener) {
        mDelegate.unregisterVibratorStateListener(listener);
        synchronized (mLock) {
            return mListeners.unregister(listener);
        }
    }

    @Override
    public boolean setExternalControl(boolean externalControl) {
        return mDelegate.setExternalControl(externalControl);
    }

    @Override
    public boolean setAlwaysOn(int id, PrebakedSegment prebaked) {
        return mDelegate.setAlwaysOn(id, prebaked);
    }

    @Override
    public boolean setAmplitude(float amplitude) {
        synchronized (mLock) {
            if (mRichTap.isAvailable()) {
                int strength = (int) (255.0f * amplitude);
                mRichTap.richTapVibratorSetAmplitude(strength);
                mRichTapAmplitude = amplitude;
                return true;
            }
        }
        return mDelegate.setAmplitude(amplitude);
    }

    @Override
    public long on(long vibrationId, long stepId, long milliseconds) {
        if (!mRichTap.isAvailable()) {
            return mDelegate.on(vibrationId, stepId, milliseconds);
        }
        Trace.traceBegin(TRACE_TAG_VIBRATOR, "RichTapHalVibrator.onTimed");
        try {
            Object token = newDispatchToken();
            if (mRichTap.isAvailable()) mRichTap.richTapVibratorSetAmplitude((int) (255 * mRichTapAmplitude));
            mRichTap.richTapVibratorOn(milliseconds);
            synchronized (mLock) {
                mDispatchToken = token;
                setRichTapVibratingLocked(true, 1f, milliseconds);
            }
            scheduleCompletion(token, vibrationId, stepId, milliseconds);
            return milliseconds;
        } finally {
            Trace.traceEnd(TRACE_TAG_VIBRATOR);
        }
    }

    @Override
    public long on(long vibrationId, long stepId, VibrationEffect.VendorEffect vendorEffect) {
        return mDelegate.on(vibrationId, stepId, vendorEffect);
    }

    @Override
    public long on(long vibrationId, long stepId, PrebakedSegment prebaked) {
        Trace.traceBegin(TRACE_TAG_VIBRATOR, "RichTapHalVibrator.onPrebaked");
        try {
            long duration = RichTapVibrationEffect.getInnerEffectDuration(prebaked.getEffectId());
            if (duration <= 0 || !mRichTap.isAvailable()) {
                return mDelegate.on(vibrationId, stepId, prebaked);
            }
            Object token = newDispatchToken();
            int strength = switch (prebaked.getEffectStrength()) {
                case VibrationEffect.EFFECT_STRENGTH_LIGHT -> 80;
                case VibrationEffect.EFFECT_STRENGTH_MEDIUM -> 165;
                case VibrationEffect.EFFECT_STRENGTH_STRONG -> 255;
                default -> 255;
            };
            if (mRichTap.isAvailable()) mRichTap.richTapVibratorSetAmplitude(strength);
            mRichTap.richTapVibratorOn(duration);
            synchronized (mLock) {
                mDispatchToken = token;
                setRichTapVibratingLocked(true, (float) strength / 255f, duration);
            }
            scheduleCompletion(token, vibrationId, stepId, duration);
            return duration;
        } finally {
            Trace.traceEnd(TRACE_TAG_VIBRATOR);
        }
    }

    @Override
    public long on(long vibrationId, long stepId, PrimitiveSegment[] primitives) {
        Trace.traceBegin(TRACE_TAG_VIBRATOR, "RichTapHalVibrator.onPrimitives");
        try {
            if (primitives == null || primitives.length == 0 || !mRichTap.isAvailable()) {
                return mDelegate.on(vibrationId, stepId, primitives);
            }

            Object token = newDispatchToken();
            long totalDuration = 0;
            boolean anyDispatched = false;

            if (primitives.length == 2 && primitives[0].getDelay() == 0
                    && (primitives[1].getPrimitiveId() == VibrationEffect.Composition.PRIMITIVE_CLICK
                            || primitives[1].getPrimitiveId() == VibrationEffect.Composition.PRIMITIVE_THUD)) {
                PrimitiveSegment primitive = primitives[1];
                int mappedEffectId = mapPrimitiveToEffectId(primitive.getPrimitiveId());
                int[] pattern = RichTapVibrationEffect.getInnerEffect(mappedEffectId);
                float scale = primitive.getScale();
                int strength = (int) (255 * scale);
                if (strength > 20) {
                    long effectDuration = RichTapVibrationEffect.getInnerEffectDuration(mappedEffectId);
                    scheduleDispatch(token, pattern, strength, 0, effectDuration);
                    totalDuration = effectDuration;
                    anyDispatched = true;
                }
            } else if (primitives.length == 2
                    && primitives[0].getPrimitiveId() == VibrationEffect.Composition.PRIMITIVE_QUICK_RISE
                    && primitives[1].getPrimitiveId() == VibrationEffect.Composition.PRIMITIVE_TICK) {
                // ASSISTANT_BUTTON: HEAVY_CLICK at full strength for a premium deep thump.
                int[] pattern = RichTapVibrationEffect.getInnerEffect(VibrationEffect.EFFECT_HEAVY_CLICK);
                if (pattern != null) {
                    long effectDuration = RichTapVibrationEffect.getInnerEffectDuration(
                            VibrationEffect.EFFECT_HEAVY_CLICK);
                    scheduleDispatch(token, pattern, 255, 0, effectDuration);
                    totalDuration = effectDuration;
                    anyDispatched = true;
                }
            } else if (primitives.length == 1) {
                PrimitiveSegment primitive = primitives[0];
                int primId = primitive.getPrimitiveId();
                if (primId == VibrationEffect.Composition.PRIMITIVE_LOW_TICK) {
                    return 1L;
                }
                int mappedEffectId = mapPrimitiveToEffectId(primId);
                int[] pattern = RichTapVibrationEffect.getInnerEffect(mappedEffectId);
                float scale = primitive.getScale();
                int strength = (int) (255 * scale);
                if (strength > 20) {
                    long effectDuration = getPrimitivePlayDuration(primId);
                    long reportedDuration = effectDuration;
                    if (mappedEffectId == VibrationEffect.EFFECT_TICK) {
                        reportedDuration = 12;
                    }
                    scheduleDispatch(token, pattern, strength, 0, effectDuration);
                    totalDuration = reportedDuration;
                    anyDispatched = true;
                }
            } else {
                int i = 0;
                long currentOffset = 0;
                while (i < primitives.length) {
                    PrimitiveSegment p = primitives[i];
                    int pId = p.getPrimitiveId();
                    if (pId == VibrationEffect.Composition.PRIMITIVE_LOW_TICK && p.getDelay() == 0) {
                        int runEnd = i;
                        while (runEnd < primitives.length
                                && primitives[runEnd].getPrimitiveId() == VibrationEffect.Composition.PRIMITIVE_LOW_TICK
                                && primitives[runEnd].getDelay() == 0) {
                            runEnd++;
                        }
                        int count = runEnd - i;
                        if (count >= 2) {
                            int strength = Math.max(35, (int) (255 * p.getScale()));
                            scheduleDispatch(token, null, strength, currentOffset, 10L);
                            currentOffset += 10L;
                            anyDispatched = true;
                            i = runEnd;
                            continue;
                        } else {
                            currentOffset += p.getDelay();
                            i++;
                            continue;
                        }
                    } else if (pId == VibrationEffect.Composition.PRIMITIVE_LOW_TICK) {
                        currentOffset += p.getDelay();
                        i++;
                        continue;
                    }

                    long scheduledTime = currentOffset + p.getDelay();
                    float scale = p.getScale();
                    int strength = (int) (255 * scale);
                    if (strength > 20) {
                        long playDuration = getPrimitivePlayDuration(pId);
                        if (pId == VibrationEffect.Composition.PRIMITIVE_QUICK_RISE
                                || pId == VibrationEffect.Composition.PRIMITIVE_SLOW_RISE) {
                            scheduleRampDispatch(token, strength, scheduledTime, playDuration);
                        } else {
                            scheduleDispatch(token, null, strength, scheduledTime, playDuration);
                        }
                        currentOffset = scheduledTime + playDuration;
                        anyDispatched = true;
                    } else {
                        currentOffset = scheduledTime;
                    }
                    i++;
                }
                totalDuration = Math.max(1, currentOffset);
            }

            if (!anyDispatched) {
                return 1L;
            }

            synchronized (mLock) {
                mDispatchToken = token;
                setRichTapVibratingLocked(true, 1f, totalDuration);
            }
            scheduleCompletion(token, vibrationId, stepId, totalDuration);
            return totalDuration;
        } finally {
            Trace.traceEnd(TRACE_TAG_VIBRATOR);
        }
    }

    @Override
    public long on(long vibrationId, long stepId, RampSegment[] primitives) {
        return mDelegate.on(vibrationId, stepId, primitives);
    }

    @Override
    public long on(long vibrationId, long stepId, PwlePoint[] pwlePoints) {
        return mDelegate.on(vibrationId, stepId, pwlePoints);
    }

    @Override
    public boolean off() {
        Object staleToken;
        synchronized (mLock) {
            staleToken = mDispatchToken;
            mDispatchToken = null;
            if (mRichTap.isAvailable() && mRichTapVibrating && mRichTapDuration > 50) {
                mRichTap.richTapVibratorOff();
            }
            setRichTapVibratingLocked(false, 1.0f, 0);
        }
        if (staleToken != null) {
            mHandler.removeCallbacksAndMessages(staleToken);
        }
        if (mDelegate.isVibrating() || !mRichTap.isAvailable()) {
            return mDelegate.off();
        }
        return true;
    }

    @Override
    public void dump(IndentingPrintWriter pw) {
        boolean richTapVibrating;
        float richTapAmplitude;
        synchronized (mLock) {
            richTapVibrating = mRichTapVibrating;
            richTapAmplitude = mRichTapAmplitude;
        }
        pw.println("RichTapHalVibrator:");
        pw.increaseIndent();
        pw.println("mRichTapVibrating = " + richTapVibrating);
        pw.println("mRichTapAmplitude = " + richTapAmplitude);
        pw.println("mRichTapDuration = " + mRichTapDuration);
        pw.decreaseIndent();
        mDelegate.dump(pw);
    }

    @Override
    public String toString() {
        return "RichTapHalVibrator{mInfo=" + mInfo + ", mDelegate=" + mDelegate + '}';
    }

    /** Maps a {@link VibrationEffect.Composition} primitive id to a RichTap inner effect id. */
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
                return RichTapVibrationEffect.getInnerEffectDuration(VibrationEffect.EFFECT_HEAVY_CLICK);
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

    private void scheduleDispatch(Object token, int[] pattern, int strength, long delayMillis, long effectDuration) {
        if (delayMillis <= 0) {
            if (mRichTap.isAvailable()) mRichTap.richTapVibratorSetAmplitude(strength);
            mRichTap.richTapVibratorOn(effectDuration);
            return;
        }
        mHandler.postDelayed(() -> {
            synchronized (mLock) {
                if (mDispatchToken != token) return;
            }
            if (mRichTap.isAvailable()) mRichTap.richTapVibratorSetAmplitude(strength);
            mRichTap.richTapVibratorOn(effectDuration);
        }, token, delayMillis);
    }

    private void scheduleRampDispatch(Object token, int targetStrength, long startDelay,
            long effectDuration) {
        final int RAMP_STEPS = 4;
        long stepInterval = Math.max(8, effectDuration / RAMP_STEPS);

        final int a0 = Math.min(targetStrength, Math.max(15, targetStrength / 5));
        final int a1 = Math.min(targetStrength, Math.max(a0, targetStrength * 9 / 20));
        final int a2 = Math.min(targetStrength, Math.max(a1, targetStrength * 3 / 4));
        final int a3 = targetStrength;
        final int[] rampAmplitudes = { a0, a1, a2, a3 };

        if (startDelay <= 0) {
            if (mRichTap.isAvailable()) {
                mRichTap.richTapVibratorSetAmplitude(rampAmplitudes[0]);
                mRichTap.richTapVibratorOn(effectDuration);
            }
        } else {
            mHandler.postDelayed(() -> {
                synchronized (mLock) {
                    if (mDispatchToken != token) return;
                }
                if (mRichTap.isAvailable()) {
                    mRichTap.richTapVibratorSetAmplitude(rampAmplitudes[0]);
                    mRichTap.richTapVibratorOn(effectDuration);
                }
            }, token, startDelay);
        }

        for (int step = 1; step < RAMP_STEPS; step++) {
            final int amp = rampAmplitudes[step];
            long stepDelay = Math.max(0, startDelay) + (stepInterval * step);
            mHandler.postDelayed(() -> {
                synchronized (mLock) {
                    if (mDispatchToken != token) return;
                }
                if (mRichTap.isAvailable()) mRichTap.richTapVibratorSetAmplitude(amp);
            }, token, stepDelay);
        }
    }

    private void scheduleCompletion(Object token, long vibrationId, long stepId, long duration) {
        mHandler.postDelayed(() -> {
            synchronized (mLock) {
                if (mDispatchToken == token) {
                    mDispatchToken = null;
                    setRichTapVibratingLocked(false, 1.0f, 0);
                }
            }
            Callbacks callbacks = mCallbacks;
            if (callbacks != null) {
                callbacks.onVibrationStepComplete(mInfo.getId(), vibrationId, stepId);
            }
        }, token, duration);
    }

    @GuardedBy("mLock")
    private void setRichTapVibratingLocked(boolean vibrating, float amplitude, long duration) {
        mRichTapDuration = duration;
        boolean previousOverall = mRichTapVibrating || mDelegate.isVibrating();
        mRichTapVibrating = vibrating;
        mRichTapAmplitude = amplitude;
        boolean currentOverall = mRichTapVibrating || mDelegate.isVibrating();
        if (previousOverall != currentOverall) {
            mListeners.broadcast(listener -> notifyStateListener(listener, currentOverall));
        }
    }

    private void notifyStateListener(IVibratorStateListener listener, boolean isVibrating) {
        try {
            listener.onVibrating(isVibrating);
        } catch (RemoteException | RuntimeException e) {
            Slog.e(TAG, "Vibrator state listener failed to call", e);
        }
    }

    private void refreshInfo() {
        VibratorInfo base = mDelegate.getInfo();
        VibratorInfo.Builder builder = new VibratorInfo.Builder(base.getId());

        builder.setCapabilities(base.getCapabilities()
                | IVibrator.CAP_COMPOSE_EFFECTS
                | IVibrator.CAP_AMPLITUDE_CONTROL);

        IntArray supportedEffects = new IntArray();
        supportedEffects.add(VibrationEffect.EFFECT_CLICK);
        supportedEffects.add(VibrationEffect.EFFECT_DOUBLE_CLICK);
        supportedEffects.add(VibrationEffect.EFFECT_TICK);
        supportedEffects.add(VibrationEffect.EFFECT_THUD);
        supportedEffects.add(VibrationEffect.EFFECT_POP);
        supportedEffects.add(VibrationEffect.EFFECT_HEAVY_CLICK);
        supportedEffects.add(VibrationEffect.EFFECT_TEXTURE_TICK);
        SparseBooleanArray baseEffects = base.getSupportedEffects();
        if (baseEffects != null) {
            for (int i = 0; i < baseEffects.size(); i++) {
                if (baseEffects.valueAt(i)) {
                    supportedEffects.add(baseEffects.keyAt(i));
                }
            }
        }
        builder.setSupportedEffects(supportedEffects.toArray());

        for (int primitiveId = VibrationEffect.Composition.PRIMITIVE_NOOP;
                primitiveId <= VibrationEffect.Composition.PRIMITIVE_LOW_TICK; primitiveId++) {
            if (base.isPrimitiveSupported(primitiveId)) {
                builder.setSupportedPrimitive(primitiveId, base.getPrimitiveDuration(primitiveId));
            }
        }
        builder.setSupportedPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 10);
        builder.setSupportedPrimitive(VibrationEffect.Composition.PRIMITIVE_THUD, 10);
        builder.setSupportedPrimitive(VibrationEffect.Composition.PRIMITIVE_SPIN, 10);
        builder.setSupportedPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, 50);
        builder.setSupportedPrimitive(VibrationEffect.Composition.PRIMITIVE_SLOW_RISE, 150);
        builder.setSupportedPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_FALL, 50);
        builder.setSupportedPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 10);
        builder.setSupportedPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 10);

        builder.setPrimitiveDelayMax(Math.max(5000, base.getPrimitiveDelayMax()));
        builder.setCompositionSizeMax(Math.max(100, base.getCompositionSizeMax()));
        builder.setFrequencyProfileLegacy(base.getFrequencyProfileLegacy());
        builder.setFrequencyProfile(base.getFrequencyProfile());
        builder.setQFactor(base.getQFactor());
        builder.setMaxEnvelopeEffectSize(base.getMaxEnvelopeEffectSize());
        builder.setMinEnvelopeEffectControlPointDurationMillis(
                base.getMinEnvelopeEffectControlPointDurationMillis());
        builder.setMaxEnvelopeEffectControlPointDurationMillis(
                base.getMaxEnvelopeEffectControlPointDurationMillis());

        mInfo = builder.build();
    }
}
