/*
 * Copyright (C) 2012 The Android Open Source Project
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

import android.animation.ValueAnimator;
import android.os.SystemClock;
import android.util.FloatProperty;
import android.util.MathUtils;
import android.view.Choreographer;

import com.android.internal.display.BrightnessUtils;

/**
 * A custom animator that progressively updates a property value at
 * The ramping at the given rate is done in the perceptual space using
 * the HLG transfer functions.
 */
class RampAnimator<T> {
    private final T mObject;
    private final FloatProperty<T> mProperty;

    private final Clock mClock;

    private float mCurrentValue;

    // target in HLG space
    private float mTargetHlgValue;

    // target in linear space
    private float mTargetLinearValue;
    private float mRate;
    private float mAnimationIncreaseMaxTimeSecs;
    private float mAnimationDecreaseMaxTimeSecs;

    private boolean mAnimating;
    private float mAnimatedValue; // higher precision copy of mCurrentValue
    private long mLastFrameTimeNanos;

    private boolean mFirstTime = true;

    // Stock AnimationStepper state (nit-space geometric/exponential transitions)
    public interface NitConverter {
        float convertToNits(float brightness);
        float getBrightnessFromNits(float nits);
    }

    private static final float[] SEQ_ANIMATION_REGION = {15.0f, 50.0f, 100.0f};
    private static final float[] SEQ_ANIMATION_WEIGHT = {0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f};
    private static final float[] BRIGHTNESS_REGION_SLOPE = {
            0.55f, 0.75f, 0.85f, 1.0f, 1.2f, 2.0f, 2.5f, 3.0f, 4.0f};
    private static final long SHORT_TERM_INTERRUPT_MILLIS = 40000L;
    private static final float UPDATE_AUTO_ANI_GAP_NITS = 0.3f;

    private NitConverter mNitConverter;
    private boolean mUseAutoStepper;
    private float mAccelerateScale = 1.0f;
    private boolean mIsUpForAnimation;
    private float mStartForAnimation;
    private float mStopForAnimation;
    private float mStepperAccelerateScale = 1.0f;
    private long mLastAutoAnimUpdateMs;
    private int mParameterSeqForAnimation = -1;
    RampAnimator(T object, FloatProperty<T> property) {
        this(object, property, System::nanoTime);
    }

    RampAnimator(T object, FloatProperty<T> property, Clock clock) {
        mObject = object;
        mProperty = property;
        mClock = clock;
    }

    /**
     * Sets the maximum time that a brightness animation can take.
     */
    void setAnimationTimeLimits(long animationRampIncreaseMaxTimeMillis,
            long animationRampDecreaseMaxTimeMillis) {
        mAnimationIncreaseMaxTimeSecs = (animationRampIncreaseMaxTimeMillis > 0)
                ? (animationRampIncreaseMaxTimeMillis / 1000.0f) : 0.0f;
        mAnimationDecreaseMaxTimeSecs = (animationRampDecreaseMaxTimeMillis > 0)
                ? (animationRampDecreaseMaxTimeMillis / 1000.0f) : 0.0f;
    }

    /**
     * Sets the animation target and the rate of this ramp animator.
     * Animation rate will be set ignoring maxTime animation limits
     * If this is the first time the property is being set or if the rate is 0,
     * the value jumps directly to the target.
     *
     * @param targetLinear The target value.
     * @param rate The convergence rate in units per second, or 0 to set the value immediately.
     * @param ignoreAnimationLimits if mAnimationIncreaseMaxTimeSecs and
     *                              mAnimationDecreaseMaxTimeSecs should be respected when adjusting
     *                              animation speed
     * @return True if the target differs from the previous target.
     */
    void setNitConverter(NitConverter converter) {
        mNitConverter = converter;
    }

    void setAutoStepperParams(boolean useAutoStepper, float accelerateScale) {
        mUseAutoStepper = useAutoStepper;
        mAccelerateScale = Math.max(1.0f, accelerateScale);
    }

    boolean setAnimationTarget(float targetLinear, float rate, boolean ignoreAnimationLimits) {
        float maxIncreaseTimeSecs = ignoreAnimationLimits ? 0 : mAnimationIncreaseMaxTimeSecs;
        float maxDecreaseTimeSecs = ignoreAnimationLimits ? 0 : mAnimationDecreaseMaxTimeSecs;
        return setAnimationTarget(targetLinear, rate, maxIncreaseTimeSecs, maxDecreaseTimeSecs);
    }

    private boolean setAnimationTarget(float targetLinear, float rate,
            float maxIncreaseTimeSecs, float maxDecreaseTimeSecs) {
        final float target = BrightnessUtils.convertLinearToGamma(targetLinear);

        // Immediately jump to the target the first time.
        if (mFirstTime || rate <= 0 || mCurrentValue == 0) {
            if (mFirstTime || target != mCurrentValue) {
                mFirstTime = false;
                mRate = 0;
                mTargetHlgValue = target;
                mTargetLinearValue = targetLinear;
                mCurrentValue = target;
                setPropertyValue(target);
                mAnimating = false;
                return true;
            }
            return false;
        }

        // Adjust the rate so that we do not exceed our maximum animation time.
        if (target > mCurrentValue && maxIncreaseTimeSecs > 0.0f
                && ((target - mCurrentValue) / rate) > maxIncreaseTimeSecs) {
            rate = (target - mCurrentValue) / maxIncreaseTimeSecs;
        } else if (target < mCurrentValue && maxDecreaseTimeSecs > 0.0f
                && ((mCurrentValue - target) / rate) > maxDecreaseTimeSecs) {
            rate = (mCurrentValue - target) / maxDecreaseTimeSecs;
        }

        // Adjust the rate based on the closest target.
        // If a faster rate is specified, then use the new rate so that we converge
        // more rapidly based on the new request.
        // If a slower rate is specified, then use the new rate only if the current
        // value is somewhere in between the new and the old target meaning that
        // we will be ramping in a different direction to get there.
        // Otherwise, continue at the previous rate.
        if (!mAnimating
                || rate > mRate
                || (target <= mCurrentValue && mCurrentValue <= mTargetHlgValue)
                || (mTargetHlgValue <= mCurrentValue && mCurrentValue <= target)) {
            mRate = rate;
        }

        final boolean changed = (mTargetHlgValue != target);
        mTargetHlgValue = target;
        mTargetLinearValue = targetLinear;

        if (changed && mUseAutoStepper && mNitConverter != null) {
            float currLinear = BrightnessUtils.convertGammaToLinear(mCurrentValue);
            float currNit = mNitConverter.convertToNits(currLinear);
            float targetNit = mNitConverter.convertToNits(targetLinear);
            if (currNit > 0.0f && targetNit > 0.0f) {
                updateAutoAnimationParameter(targetNit, currNit);
            }
        }

        // Start animating.
        if (!mAnimating && target != mCurrentValue) {
            mAnimating = true;
            mAnimatedValue = mCurrentValue;
            mLastFrameTimeNanos = mClock.nanoTime();
        }

        return changed;
    }

    /**
     * Returns true if the animation is running.
     */
    boolean isAnimating() {
        return mAnimating;
    }

    /**
     * Sets the brightness property by converting the given value from HLG space
     * into linear space.
     */
    private void setPropertyValue(float val) {
        // To avoid linearVal inconsistency when converting to HLG and back to linear space
        // used original target linear value for final animation step
        float linearVal =
                val == mTargetHlgValue ? mTargetLinearValue : BrightnessUtils.convertGammaToLinear(
                        val);
        mProperty.setValue(mObject, linearVal);
    }

    private static int getCalculateParameterSeqForAnimation(boolean isUp, float start, float stop) {
        if (isUp) {
            if (start < SEQ_ANIMATION_REGION[0]) {
                if (stop < SEQ_ANIMATION_REGION[0]) {
                    return 0;
                } else if (stop < SEQ_ANIMATION_REGION[1]) {
                    return 1;
                } else {
                    return 2;
                }
            } else if (stop < SEQ_ANIMATION_REGION[1]) {
                return 3;
            } else {
                return 4;
            }
        } else {
            if (start > SEQ_ANIMATION_REGION[2]) {
                return (stop > SEQ_ANIMATION_REGION[0]) ? 5 : 6;
            } else if (start <= SEQ_ANIMATION_REGION[0]) {
                return 9;
            } else {
                return (stop > SEQ_ANIMATION_REGION[0]) ? 7 : 8;
            }
        }
    }

    private void updateAutoAnimationParameter(float targetNit, float currentNit) {
        if (Math.abs(targetNit - mStopForAnimation) > UPDATE_AUTO_ANI_GAP_NITS) {
            long now = SystemClock.uptimeMillis();
            boolean directionResult = targetNit > currentNit;
            float currentResult = currentNit;
            float accelerateScaleResult = mAccelerateScale;
            if (Math.abs(currentNit - mStopForAnimation) > 1e-6f && mStopForAnimation > 0.0f) {
                boolean isSameDirection = (directionResult == mIsUpForAnimation);
                boolean isShortTermChange = (now - mLastAutoAnimUpdateMs) < SHORT_TERM_INTERRUPT_MILLIS;
                if (isSameDirection && isShortTermChange) {
                    currentResult = mStartForAnimation;
                    accelerateScaleResult = mStepperAccelerateScale;
                }
            }
            mIsUpForAnimation = directionResult;
            mStartForAnimation = currentResult;
            mStopForAnimation = targetNit;
            mStepperAccelerateScale = accelerateScaleResult;
            mLastAutoAnimUpdateMs = now;
            mParameterSeqForAnimation = getCalculateParameterSeqForAnimation(
                    mIsUpForAnimation, mStartForAnimation, mStopForAnimation);
        }
    }

    private static float calculateChangeGapNit(boolean isUp, float currY, float start, float stop,
            float slope, float adjfreq, int type, float accelerateScale) {
        if (isUp) {
            if (type == 1) {
                float safeStart = Math.max(start, 0.1f);
                float safeCurr = Math.max(currY, 0.1f);
                float denom = Math.max(1.0f, (adjfreq * slope) / accelerateScale);
                float target = (float) (safeCurr * Math.pow(stop / safeStart, 1.0f / denom));
                return Math.abs(target - currY);
            } else if (type == 2) {
                float offset = 0.8f * start;
                float safeStart = Math.max(start - offset, 0.1f);
                float denom = Math.max(1.0f, (adjfreq * slope) / accelerateScale);
                double percent = Math.pow(Math.max(stop - offset, 0.1f) / safeStart, 1.0f / denom);
                float target = (float) (((currY - offset) * percent) + offset);
                return Math.abs(target - currY);
            }
        } else {
            if (type == 1) {
                float target = currY * slope;
                return Math.abs(target - currY);
            } else if (type == 2) {
                float offset = 0.92f * stop;
                float target = ((currY - offset) * slope) + offset;
                return Math.abs(target - currY);
            }
        }
        return -1.0f;
    }

    private float calcStockAutoStepperNextLinear(float currLinear, float targetLinear,
            float timeDelta) {
        if (mNitConverter == null || timeDelta <= 0.0f) {
            return Float.NaN;
        }
        float currY = mNitConverter.convertToNits(currLinear);
        float targetY = mNitConverter.convertToNits(targetLinear);
        if (currY <= 0.0f || targetY <= 0.0f || Float.isNaN(currY) || Float.isNaN(targetY)) {
            return Float.NaN;
        }
        if (mParameterSeqForAnimation < 0) {
            updateAutoAnimationParameter(targetY, currY);
        }
        float adjfreq = MathUtils.constrain(1.0f / timeDelta, 10.0f, 240.0f);
        float accel = Math.max(1.0f, mStepperAccelerateScale);
        float startY = mStartForAnimation > 0.0f ? mStartForAnimation : currY;
        float stopY = mStopForAnimation > 0.0f ? mStopForAnimation : targetY;
        int seq = mParameterSeqForAnimation;
        float deltaNit = -1.0f;

        if (targetY < currY && currY < 3.0f && targetY < 3.0f) {
            float slope = (float) Math.pow(0.98f, 1.0f / (adjfreq / accel));
            deltaNit = calculateChangeGapNit(false, currY, startY, stopY, slope, adjfreq, 1, accel);
        } else if (targetY < currY && currY < 10.0f && targetY < 3.0f) {
            float slope = (float) Math.pow(0.95f, 1.0f / (adjfreq / accel));
            deltaNit = calculateChangeGapNit(false, currY, startY, stopY, slope, adjfreq, 1, accel);
        } else {
            switch (seq) {
                case 0:
                    deltaNit = calculateChangeGapNit(true, currY, startY, stopY,
                            BRIGHTNESS_REGION_SLOPE[7], adjfreq, 1, accel);
                    break;
                case 1:
                    if (currY < SEQ_ANIMATION_REGION[0]) {
                        deltaNit = calculateChangeGapNit(true, currY, startY,
                                SEQ_ANIMATION_REGION[0], BRIGHTNESS_REGION_SLOPE[6], adjfreq, 1, accel);
                    } else {
                        deltaNit = calculateChangeGapNit(true, currY, SEQ_ANIMATION_REGION[0],
                                stopY, BRIGHTNESS_REGION_SLOPE[3], adjfreq, 2, accel);
                    }
                    break;
                case 2: {
                    float tempY = (SEQ_ANIMATION_WEIGHT[3] * stopY)
                            + (SEQ_ANIMATION_WEIGHT[3] * startY);
                    if (currY < SEQ_ANIMATION_REGION[0]) {
                        deltaNit = calculateChangeGapNit(true, currY, startY,
                                SEQ_ANIMATION_REGION[0], BRIGHTNESS_REGION_SLOPE[4], adjfreq, 1, accel);
                    } else if (currY >= tempY) {
                        deltaNit = calculateChangeGapNit(true, currY, tempY, stopY,
                                BRIGHTNESS_REGION_SLOPE[6], adjfreq, 2, accel);
                    } else {
                        deltaNit = calculateChangeGapNit(true, currY, SEQ_ANIMATION_REGION[0],
                                tempY, BRIGHTNESS_REGION_SLOPE[3], adjfreq, 1, accel);
                    }
                    break;
                }
                case 3: {
                    float tempY = (SEQ_ANIMATION_WEIGHT[5] * stopY)
                            + (SEQ_ANIMATION_WEIGHT[1] * startY);
                    if (currY < tempY) {
                        deltaNit = calculateChangeGapNit(true, currY, startY, tempY,
                                BRIGHTNESS_REGION_SLOPE[8], adjfreq, 2, accel);
                    } else {
                        deltaNit = calculateChangeGapNit(true, currY, tempY, stopY,
                                BRIGHTNESS_REGION_SLOPE[3], adjfreq, 2, accel);
                    }
                    break;
                }
                case 4: {
                    float tempY = (SEQ_ANIMATION_WEIGHT[5] * stopY)
                            + (SEQ_ANIMATION_WEIGHT[1] * startY);
                    if (currY < tempY) {
                        deltaNit = calculateChangeGapNit(true, currY, startY, tempY,
                                BRIGHTNESS_REGION_SLOPE[6], adjfreq, 1, accel);
                    } else {
                        deltaNit = calculateChangeGapNit(true, currY, tempY, stopY,
                                BRIGHTNESS_REGION_SLOPE[5], adjfreq, 2, accel);
                    }
                    break;
                }
                case 5: {
                    float tempY = (SEQ_ANIMATION_WEIGHT[6] * startY)
                            + (SEQ_ANIMATION_WEIGHT[0] * stopY);
                    float slope = (float) Math.pow(BRIGHTNESS_REGION_SLOPE[0],
                            1.0f / (adjfreq / accel));
                    if (currY > tempY) {
                        deltaNit = calculateChangeGapNit(false, currY, startY, tempY,
                                slope, adjfreq, 1, accel);
                    } else {
                        deltaNit = calculateChangeGapNit(false, currY, tempY, stopY,
                                slope, adjfreq, 2, accel);
                    }
                    break;
                }
                case 6: {
                    float tempY = (SEQ_ANIMATION_WEIGHT[6] * startY)
                            + (SEQ_ANIMATION_WEIGHT[0] * stopY);
                    if (currY > tempY) {
                        float slope = (float) Math.pow(BRIGHTNESS_REGION_SLOPE[0],
                                1.0f / (adjfreq / accel));
                        deltaNit = calculateChangeGapNit(false, currY, startY, tempY,
                                slope, adjfreq, 1, accel);
                    } else if (currY <= SEQ_ANIMATION_REGION[0]) {
                        float slope = (float) Math.pow(BRIGHTNESS_REGION_SLOPE[2],
                                1.0f / adjfreq);
                        deltaNit = calculateChangeGapNit(false, currY, SEQ_ANIMATION_REGION[0],
                                stopY, slope, adjfreq, 2, accel);
                    } else {
                        float slope = (float) Math.pow(BRIGHTNESS_REGION_SLOPE[0],
                                1.0f / (adjfreq / accel));
                        deltaNit = calculateChangeGapNit(false, currY, tempY,
                                SEQ_ANIMATION_REGION[0], slope, adjfreq, 2, accel);
                    }
                    break;
                }
                case 7: {
                    float slope = (float) Math.pow(BRIGHTNESS_REGION_SLOPE[0],
                            1.0f / (adjfreq / accel));
                    deltaNit = calculateChangeGapNit(false, currY, startY, stopY,
                            slope, adjfreq, 2, accel);
                    break;
                }
                case 8: {
                    if (currY > SEQ_ANIMATION_REGION[0]) {
                        float slope = (float) Math.pow(BRIGHTNESS_REGION_SLOPE[0],
                                1.0f / (adjfreq / accel));
                        deltaNit = calculateChangeGapNit(false, currY, startY,
                                SEQ_ANIMATION_REGION[0], slope, adjfreq, 2, accel);
                    } else {
                        float slope = (float) Math.pow(BRIGHTNESS_REGION_SLOPE[2],
                                1.0f / adjfreq);
                        deltaNit = calculateChangeGapNit(false, currY, SEQ_ANIMATION_REGION[0],
                                stopY, slope, adjfreq, 2, accel);
                    }
                    break;
                }
                case 9: {
                    float slope = (float) Math.pow(BRIGHTNESS_REGION_SLOPE[2],
                            1.0f / (adjfreq / accel));
                    deltaNit = calculateChangeGapNit(false, currY, startY, stopY,
                            slope, adjfreq, 1, accel);
                    break;
                }
                default:
                    break;
            }
        }
        if (deltaNit <= 0.0f || Float.isNaN(deltaNit)) {
            return Float.NaN;
        }
        // Ensure convergence near target without stalling on sub-step rounding.
        float minStepNit = Math.max(0.01f, Math.abs(targetY - currY) * timeDelta * 0.5f);
        deltaNit = Math.max(deltaNit, minStepNit);
        float nextNit = (targetY > currY)
                ? Math.min(currY + deltaNit, targetY)
                : Math.max(currY - deltaNit, targetY);
        float nextLinear = mNitConverter.getBrightnessFromNits(nextNit);
        if (nextLinear < 0.0f || Float.isNaN(nextLinear)) {
            return Float.NaN;
        }
        return nextLinear;
    }

    void performNextAnimationStep(long frameTimeNanos) {
        final float timeDelta = (frameTimeNanos - mLastFrameTimeNanos) * 0.000000001f;
        mLastFrameTimeNanos = frameTimeNanos;

        // Advance the animated value towards the target at the specified rate
        // and clamp to the target. This gives us the new current value but
        // we keep the animated value around to allow for fractional increments
        // towards the target.
        final float scale = ValueAnimator.getDurationScale();
        if (scale == 0) {
            // Animation off.
            mAnimatedValue = mTargetHlgValue;
        } else {
            float nextHlg = Float.NaN;
            if (mUseAutoStepper && mNitConverter != null && mRate > 0.0f) {
                float currLinear = BrightnessUtils.convertGammaToLinear(mAnimatedValue);
                float nextLinear = calcStockAutoStepperNextLinear(
                        currLinear, mTargetLinearValue, timeDelta / scale);
                if (!Float.isNaN(nextLinear)) {
                    nextHlg = BrightnessUtils.convertLinearToGamma(nextLinear);
                }
            }
            if (!Float.isNaN(nextHlg)) {
                if (mTargetHlgValue > mCurrentValue) {
                    mAnimatedValue = Math.min(Math.max(nextHlg, mAnimatedValue + 1e-5f),
                            mTargetHlgValue);
                } else {
                    mAnimatedValue = Math.max(Math.min(nextHlg, mAnimatedValue - 1e-5f),
                            mTargetHlgValue);
                }
            } else {
                final float amount = timeDelta * mRate / scale;
                if (mTargetHlgValue > mCurrentValue) {
                    mAnimatedValue = Math.min(mAnimatedValue + amount, mTargetHlgValue);
                } else {
                    mAnimatedValue = Math.max(mAnimatedValue - amount, mTargetHlgValue);
                }
            }
        }
        final float oldCurrentValue = mCurrentValue;
        mCurrentValue = mAnimatedValue;
        if (oldCurrentValue != mCurrentValue) {
            setPropertyValue(mCurrentValue);
        }
        if (mTargetHlgValue == mCurrentValue) {
            mAnimating = false;
        }
    }

    public interface Listener {
        void onAnimationEnd();
    }

    interface Clock {
        /**
         * Returns current system time in nanoseconds.
         */
        long nanoTime();
    }

    static class DualRampAnimator<T> {
        private final Choreographer mChoreographer;
        private final RampAnimator<T> mFirst;
        private final RampAnimator<T> mSecond;

        private Listener mListener;
        private boolean mAwaitingCallback;

        DualRampAnimator(T object, FloatProperty<T> firstProperty,
                FloatProperty<T> secondProperty) {
            mChoreographer = Choreographer.getInstance();
            mFirst = new RampAnimator<>(object, firstProperty);
            mSecond = new RampAnimator<>(object, secondProperty);
        }

        /**
         * Sets the maximum time that a brightness animation can take.
         */
        public void setAnimationTimeLimits(long animationRampIncreaseMaxTimeMillis,
                long animationRampDecreaseMaxTimeMillis) {
            mFirst.setAnimationTimeLimits(animationRampIncreaseMaxTimeMillis,
                    animationRampDecreaseMaxTimeMillis);
            mSecond.setAnimationTimeLimits(animationRampIncreaseMaxTimeMillis,
                    animationRampDecreaseMaxTimeMillis);
        }

        public void setNitConverter(NitConverter converter) {
            mFirst.setNitConverter(converter);
            mSecond.setNitConverter(converter);
        }

        public void setAutoStepperParams(boolean useAutoStepper, float accelerateScale) {
            mFirst.setAutoStepperParams(useAutoStepper, accelerateScale);
            mSecond.setAutoStepperParams(useAutoStepper, accelerateScale);
        }
        /**
         * Starts animating towards the specified values.
         *
         * If this is the first time the property is being set or if the rate is 0,
         * the value jumps directly to the target.
         *
         * @param linearFirstTarget The first target value in linear space.
         * @param linearSecondTarget The second target value in linear space.
         * @param rate The convergence rate in units per second, or 0 to set the value immediately.
         * @param ignoreAnimationLimits if mAnimationIncreaseMaxTimeSecs and
         *                              mAnimationDecreaseMaxTimeSecs should be respected
         *                              when adjusting animation speed
         * @return True if either target differs from the previous target.
         */
        public boolean animateTo(float linearFirstTarget, float linearSecondTarget, float rate,
                boolean ignoreAnimationLimits) {
            boolean animationTargetChanged = mFirst.setAnimationTarget(linearFirstTarget, rate,
                    ignoreAnimationLimits);
            animationTargetChanged |= mSecond.setAnimationTarget(linearSecondTarget, rate,
                    ignoreAnimationLimits);
            boolean shouldBeAnimating = isAnimating();

            if (shouldBeAnimating != mAwaitingCallback) {
                if (shouldBeAnimating) {
                    mAwaitingCallback = true;
                    postAnimationCallback();
                } else if (mAwaitingCallback) {
                    mChoreographer.removeCallbacks(Choreographer.CALLBACK_ANIMATION,
                            mAnimationCallback, null);
                    mAwaitingCallback = false;
                }
            }
            return animationTargetChanged;
        }

        /**
        * Sets a listener to watch for animation events.
        */
        public void setListener(Listener listener) {
            mListener = listener;
        }

        /**
        * Returns true if the animation is running.
        */
        public boolean isAnimating() {
            return mFirst.isAnimating() || mSecond.isAnimating();
        }

        private void postAnimationCallback() {
            mChoreographer.postCallback(Choreographer.CALLBACK_ANIMATION, mAnimationCallback, null);
        }

        private final Runnable mAnimationCallback = new Runnable() {
            @Override // Choreographer callback
            public void run() {
                long frameTimeNanos = mChoreographer.getFrameTimeNanos();
                mFirst.performNextAnimationStep(frameTimeNanos);
                mSecond.performNextAnimationStep(frameTimeNanos);
                if (isAnimating()) {
                    postAnimationCallback();
                } else {
                    if (mListener != null) {
                        mListener.onAnimationEnd();
                    }
                    mAwaitingCallback = false;
                }
            }
        };
    }
}
