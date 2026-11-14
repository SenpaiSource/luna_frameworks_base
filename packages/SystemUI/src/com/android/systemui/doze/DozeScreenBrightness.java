/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.systemui.doze;

import static android.os.PowerManager.GO_TO_SLEEP_REASON_TIMEOUT;

import static com.android.systemui.keyguard.WakefulnessLifecycle.WAKEFULNESS_GOING_TO_SLEEP;

import static java.lang.Integer.max;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.IndentingPrintWriter;
import android.view.Display;

import com.android.app.tracing.TraceUtils;
import com.android.internal.R;
import com.android.internal.display.BrightnessSynchronizer;
import com.android.systemui.dagger.qualifiers.Application;
import com.android.systemui.dagger.qualifiers.Main;
import com.android.systemui.doze.dagger.BrightnessSensor;
import com.android.systemui.doze.dagger.DozeScope;
import android.util.MathUtils;
import com.android.systemui.doze.dagger.WrappedService;
import com.android.systemui.keyguard.WakefulnessLifecycle;
import com.android.systemui.scene.shared.flag.SceneContainerFlag;
import com.android.systemui.statusbar.phone.DozeParameters;
import com.android.systemui.statusbar.policy.DevicePostureController;
import com.android.systemui.util.kotlin.JavaAdapterKt;
import com.android.systemui.util.sensors.AsyncSensorManager;
import com.android.systemui.util.settings.SystemSettings;
import com.android.systemui.wallpapers.domain.interactor.WallpaperInteractor;

import kotlin.Unit;

import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.Job;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CancellationException;

import javax.inject.Inject;

/**
 * Controls the screen brightness when dozing.
 */
@DozeScope
public class DozeScreenBrightness extends BroadcastReceiver implements DozeMachine.Part,
        SensorEventListener {
    private static final boolean DEBUG_AOD_BRIGHTNESS = SystemProperties
            .getBoolean("debug.aod_brightness", false);
    protected static final String ACTION_AOD_BRIGHTNESS =
            "com.android.systemui.doze.AOD_BRIGHTNESS";
    protected static final String BRIGHTNESS_BUCKET = "brightness_bucket";

    /**
     * Just before the screen times out from user inactivity, DisplayPowerController dims the screen
     * brightness to the lower of {@link #mScreenBrightnessDim}, or the current brightness minus
     * this amount.
     */
    private final float mScreenBrightnessMinimumDimAmount;
    private final Context mContext;
    private final DozeMachine.Service mDozeService;
    private final DozeHost mDozeHost;
    private final Handler mHandler;
    private final SensorManager mSensorManager;
    private final DisplayManager mDisplayManager;
    private final Optional<Sensor>[] mLightSensorOptional; // light sensors to use per posture
    private final WakefulnessLifecycle mWakefulnessLifecycle;
    private final DozeParameters mDozeParameters;
    private final DevicePostureController mDevicePostureController;
    private final DozeLog mDozeLog;
    private final SystemSettings mSystemSettings;
    private final WallpaperInteractor mWallpaperInteractor;
    private final CoroutineScope mScope;
    private Job mWallpaperSupportsAmbientModeJob = null;
    private boolean mWallpaperSupportsAmbientMode;
    private final float[] mSensorToBrightness;
    private final int[] mSensorToWallpaperScrimOpacity;
    private final int[] mSensorToScrimOpacity;
    private final float mScreenBrightnessDim;
    private final int[] mAodLuxLevels;
    private final float[] mAodLuxToBrightness;
    private final Sensor mAmbientLightSensor;
    private final Sensor mProximitySensor;
    private boolean mAmbientLightRegistered;
    private boolean mProximityRegistered;
    private boolean mLastProxNear;
    private boolean mWaitDelayFinish;
    private boolean mIsFirstPanoramicAodBrightness = true;
    private int mLightSensorSampleCount;
    private float mLatestAmbientLux = -1.0f;
    private final Runnable mDelayedAodBrightnessRunnable = () -> {
        mWaitDelayFinish = false;
        if (mLatestAmbientLux >= 0.0f) {
            applyPanoramicAodLux(mLatestAmbientLux);
        }
    };
    private final SensorEventListener mAmbientLightListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (!mRegistered || event.values == null || event.values.length == 0) {
                return;
            }
            mLatestAmbientLux = event.values[0];
            if (mIsFirstPanoramicAodBrightness) {
                mLightSensorSampleCount = 4;
                mIsFirstPanoramicAodBrightness = false;
            } else {
                mLightSensorSampleCount++;
            }
            if (mLightSensorSampleCount % 4 == 0) {
                mLightSensorSampleCount = 0;
                if (!mWaitDelayFinish && !mLastProxNear) {
                    applyPanoramicAodLux(mLatestAmbientLux);
                }
                setAmbientLightSensorEnabled(false);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };
    private final SensorEventListener mProximityListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (!mRegistered || mProximitySensor == null || event.values == null
                    || event.values.length == 0) {
                return;
            }
            boolean proxNear = event.values[0] < mProximitySensor.getMaximumRange();
            if (mLastProxNear && !proxNear) {
                mWaitDelayFinish = true;
                mHandler.removeCallbacks(mDelayedAodBrightnessRunnable);
                mHandler.postDelayed(mDelayedAodBrightnessRunnable, 1000L);
            } else if (proxNear) {
                mWaitDelayFinish = false;
                mHandler.removeCallbacks(mDelayedAodBrightnessRunnable);
            }
            mLastProxNear = proxNear;
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    @DevicePostureController.DevicePostureInt
    private int mDevicePosture;
    private boolean mRegistered;
    private final float mDefaultDozeBrightness;
    private boolean mPaused = false;
    private boolean mScreenOff = false;
    private int mLastSensorValue = -1;
    private DozeMachine.State mState = DozeMachine.State.UNINITIALIZED;
    private final DozeHost.Callback mAuroraCallback = new DozeHost.Callback() {
        @Override
        public void onAuroraPulseChanged() {
            resetBrightnessToDefault();
            updateBrightnessAndReady(true);
        }
    };

    /**
     * Debug value used for emulating various display brightness buckets:
     *
     * {@code am broadcast -p com.android.systemui -a com.android.systemui.doze.AOD_BRIGHTNESS
     * --ei brightness_bucket 1}
     */
    private int mDebugBrightnessBucket = -1;

    @Inject
    @SuppressLint("AndroidFrameworkRequiresPermission")
    public DozeScreenBrightness(
            Context context,
            @WrappedService DozeMachine.Service service,
            AsyncSensorManager sensorManager,
            @BrightnessSensor Optional<Sensor>[] lightSensorOptional,
            DozeHost host, @Main Handler handler,
            AlwaysOnDisplayPolicy alwaysOnDisplayPolicy,
            WakefulnessLifecycle wakefulnessLifecycle,
            DozeParameters dozeParameters,
            DevicePostureController devicePostureController,
            DozeLog dozeLog,
            SystemSettings systemSettings,
            DisplayManager displayManager,
            WallpaperInteractor wallpaperInteractor,
            @Application CoroutineScope scope
    ) {
        mContext = context;
        mDozeService = service;
        mSensorManager = sensorManager;
        mDisplayManager = displayManager;
        mLightSensorOptional = lightSensorOptional;
        mDevicePostureController = devicePostureController;
        mDevicePosture = mDevicePostureController.getDevicePosture();
        mWakefulnessLifecycle = wakefulnessLifecycle;
        mDozeParameters = dozeParameters;
        mDozeHost = host;
        mHandler = handler;
        mDozeLog = dozeLog;
        mSystemSettings = systemSettings;
        mWallpaperInteractor = wallpaperInteractor;
        mScope = scope;

        mScreenBrightnessMinimumDimAmount = context.getResources().getFloat(
                R.dimen.config_screenBrightnessMinimumDimAmountFloat);

        mDefaultDozeBrightness = mDisplayManager.getDefaultDozeBrightness(mContext.getDisplayId());
        mScreenBrightnessDim = alwaysOnDisplayPolicy.dimBrightness;
        float[] sensorToBrightness =
                mDisplayManager.getDozeBrightnessSensorValueToBrightness(mContext.getDisplayId());
        if (sensorToBrightness == null) {
            int[] screenBrightnessArray = alwaysOnDisplayPolicy.screenBrightnessArray;
            sensorToBrightness = new float[screenBrightnessArray.length];
            for (int i = 0; i < screenBrightnessArray.length; i++) {
                sensorToBrightness[i] = BrightnessSynchronizer.brightnessIntToFloat(
                        screenBrightnessArray[i]);
            }
        }
        mSensorToBrightness = sensorToBrightness;
        mSensorToScrimOpacity = alwaysOnDisplayPolicy.dimmingScrimArray;
        mSensorToWallpaperScrimOpacity = alwaysOnDisplayPolicy.wallpaperDimmingScrimArray;

        mAodLuxLevels = context.getResources().getIntArray(
                com.android.systemui.res.R.array.config_doze_lux_levels);
        android.content.res.TypedArray aodBrightnessArray = context.getResources()
                .obtainTypedArray(com.android.systemui.res.R.array.config_doze_lux_to_brightness_float);
        mAodLuxToBrightness = new float[aodBrightnessArray.length()];
        for (int i = 0; i < aodBrightnessArray.length(); i++) {
            mAodLuxToBrightness[i] = aodBrightnessArray.getFloat(i, -1.0f);
        }
        aodBrightnessArray.recycle();
        mAmbientLightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        mProximitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

        mDevicePostureController.addCallback(mDevicePostureCallback);
    }

    @Override
    public void transitionTo(DozeMachine.State oldState, DozeMachine.State newState) {
        mState = newState;
        switch (newState) {
            case INITIALIZED:
                mDozeHost.addCallback(mAuroraCallback);
                startListeningForWallpaperSupportsAmbientMode();
                resetBrightnessToDefault();
                break;
            case DOZE_AOD:
            case DOZE_REQUEST_PULSE:
            case DOZE_AOD_DOCKED:
            case DOZE_AOD_MINMODE:
                setLightSensorEnabled(true);
                break;
            case DOZE:
            case DOZE_SUSPEND_TRIGGERS:
                setLightSensorEnabled(false);
                resetBrightnessToDefault();
                break;
            case DOZE_AOD_PAUSED:
                setLightSensorEnabled(false);
                break;
            case FINISH:
                destroy();
                break;
        }
        if (newState != DozeMachine.State.FINISH) {
            setScreenOff(newState == DozeMachine.State.DOZE);
            setPaused(newState == DozeMachine.State.DOZE_AOD_PAUSED);
        }
    }

    @Override
    public void destroy() {
        mDozeHost.removeCallback(mAuroraCallback);
        stopListeningForWallpaperSupportsAmbientMode();
        setLightSensorEnabled(false);
        mDevicePostureController.removeCallback(mDevicePostureCallback);
        if (SceneContainerFlag.isEnabled()) {
            mDozeHost.setAodDimmingScrim(0f);
            mDozeHost.setAodWallpaperDimmingScrim(0f);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        TraceUtils.trace(() -> "DozeScreenBrightness.onSensorChanged" + event.values[0], () -> {
            if (mRegistered) {
                mLastSensorValue = (int) event.values[0];
                if (hasPanoramicAodCurve()) {
                    setAmbientLightSensorEnabled(true);
                } else {
                    updateBrightnessAndReady(false /* force */);
                }
            }
            return Unit.INSTANCE;
        });
    }

    public void updateBrightnessAndReady(boolean force) {
        if (applyAuroraBrightness()) return;
        if (force || mRegistered || mDebugBrightnessBucket != -1) {
            int sensorValue = mDebugBrightnessBucket == -1
                    ? mLastSensorValue : mDebugBrightnessBucket;
            boolean brightnessReady;
            float brightness = computeBrightness(sensorValue);
            brightnessReady = brightness >= 0;
            if (brightnessReady) {
                mDozeService.setDozeScreenBrightness(
                        clampToDimBrightnessForScreenOff(clampToUserSetting(brightness)));
            }

            int scrimOpacity = -1;
            int wallpaperScrimOpacity = -1;
            if (!isLightSensorPresent()) {
                // No light sensor, scrims are always transparent.
                scrimOpacity = 0;
                wallpaperScrimOpacity = 0;
            } else if (brightnessReady) {
                // Only unblank scrim once brightness is ready.
                scrimOpacity = computeScrimOpacity(sensorValue);
                wallpaperScrimOpacity = computeWallpaperScrimOpacity(sensorValue);
            }
            if (scrimOpacity >= 0) {
                mDozeHost.setAodDimmingScrim(scrimOpacity / 255f);
            }
            if (wallpaperScrimOpacity >= 0) {
                mDozeHost.setAodWallpaperDimmingScrim(wallpaperScrimOpacity / 255f);
            }
        }
    }

    private boolean lightSensorSupportsCurrentPosture() {
        return mLightSensorOptional != null
                && mDevicePosture < mLightSensorOptional.length;
    }

    private boolean isLightSensorPresent() {
        if (!lightSensorSupportsCurrentPosture()) {
            return mLightSensorOptional != null && mLightSensorOptional[0].isPresent();
        }

        return mLightSensorOptional[mDevicePosture].isPresent();
    }

    private Sensor getLightSensor() {
        if (!lightSensorSupportsCurrentPosture()) {
            return null;
        }

        return mLightSensorOptional[mDevicePosture].get();
    }

    private int computeScrimOpacity(int sensorValue) {
        if (sensorValue < 0 || sensorValue >= mSensorToScrimOpacity.length) {
            return -1;
        }
        int wallpaperScrimOpacity = -1;
        if (!SceneContainerFlag.isEnabled()) {
            if (mWallpaperSupportsAmbientMode && sensorValue
                    < mSensorToWallpaperScrimOpacity.length) {
                wallpaperScrimOpacity = mSensorToWallpaperScrimOpacity[sensorValue];
            }
        }
        return max(wallpaperScrimOpacity, mSensorToScrimOpacity[sensorValue]);
    }

    private int computeWallpaperScrimOpacity(int sensorValue) {
        if (!SceneContainerFlag.isEnabled()
                || !mWallpaperSupportsAmbientMode
                || sensorValue < 0
                || sensorValue >= mSensorToWallpaperScrimOpacity.length) {
            return -1;
        }
        return mSensorToWallpaperScrimOpacity[sensorValue];
    }

    private float computeBrightness(int sensorValue) {
        if (sensorValue < 0 || sensorValue >= mSensorToBrightness.length) {
            return -1;
        }
        return mSensorToBrightness[sensorValue];
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private void resetBrightnessToDefault() {
        if (applyAuroraBrightness()) return;
        mDozeService.setDozeScreenBrightness(clampToDimBrightnessForScreenOff(
                clampToUserSettingOrAutoBrightness(mDefaultDozeBrightness)));
        mDozeHost.setAodDimmingScrim(0f);
        mDozeHost.setAodWallpaperDimmingScrim(0f);
    }

    private boolean applyAuroraBrightness() {
        final float brightness = mDozeHost.getAuroraPulseBrightness();
        if (brightness < 0f) return false;
        mDozeService.setDozeScreenBrightness(brightness);
        mDozeHost.setAodDimmingScrim(0f);
        mDozeHost.setAodWallpaperDimmingScrim(0f);
        return true;
    }

    private float clampToUserSetting(float brightness) {
        int screenBrightnessModeSetting = mSystemSettings.getIntForUser(
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL, UserHandle.USER_CURRENT);
        if (screenBrightnessModeSetting == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
            return brightness;
        }

        return Math.min(brightness, getScreenBrightness());
    }

    private float clampToUserSettingOrAutoBrightness(float brightness) {
        return Math.min(brightness, getScreenBrightness());
    }

    /**
     * Gets the current screen brightness that may have been set by manually by the user
     * or by autobrightness.
     */
    @SuppressLint("AndroidFrameworkRequiresPermission")
    private float getScreenBrightness() {
        return mDisplayManager.getBrightness(Display.DEFAULT_DISPLAY);
    }

    /**
     * Clamp the brightness to the dim brightness value used by PowerManagerService just before the
     * device times out and goes to sleep, if we are sleeping from a timeout. This ensures that we
     * don't raise the brightness back to the user setting before or during the screen off
     * animation.
     */
    private float clampToDimBrightnessForScreenOff(float brightness) {
        final boolean screenTurningOff =
                (mDozeParameters.shouldClampToDimBrightness()
                        || mWakefulnessLifecycle.getWakefulness() == WAKEFULNESS_GOING_TO_SLEEP)
                && mState == DozeMachine.State.INITIALIZED;
        if (screenTurningOff
                && mWakefulnessLifecycle.getLastSleepReason() == GO_TO_SLEEP_REASON_TIMEOUT) {
            return Math.max(
                    PowerManager.BRIGHTNESS_MIN,
                    // Use the lower of either the dim brightness, or the current brightness reduced
                    // by the minimum dim amount. This is the same logic used in
                    // DisplayPowerController#updatePowerState to apply a minimum dim amount.
                    Math.min(brightness - mScreenBrightnessMinimumDimAmount, mScreenBrightnessDim));
        } else {
            return brightness;
        }
    }

    private boolean hasPanoramicAodCurve() {
        return mAodLuxLevels != null && mAodLuxToBrightness != null
                && mAodLuxLevels.length > 0 && mAodLuxToBrightness.length > 0
                && mAmbientLightSensor != null;
    }

    private float computePanoramicAodBrightness(float lux) {
        if (mAodLuxLevels == null || mAodLuxToBrightness == null
                || mAodLuxLevels.length == 0 || mAodLuxToBrightness.length == 0) {
            return -1.0f;
        }
        int rank = 0;
        int maxIdx = mAodLuxLevels.length - 1;
        if (lux >= mAodLuxLevels[maxIdx]) {
            rank = maxIdx;
        } else if (lux >= mAodLuxLevels[0]) {
            for (int i = 0; i < maxIdx; i++) {
                if (lux >= mAodLuxLevels[i] && lux < mAodLuxLevels[i + 1]) {
                    rank = i;
                    break;
                }
            }
        }
        int clampedRank = Math.min(rank, mAodLuxToBrightness.length - 1);
        return mAodLuxToBrightness[clampedRank];
    }

    private void applyPanoramicAodLux(float lux) {
        if (applyAuroraBrightness()) {
            return;
        }
        float brightness = computePanoramicAodBrightness(lux);
        if (brightness > 0.0f) {
            brightness = MathUtils.constrain(clampToUserSetting(brightness),
                    PowerManager.BRIGHTNESS_MIN, PowerManager.BRIGHTNESS_MAX);
            mDozeService.setDozeScreenBrightness(clampToDimBrightnessForScreenOff(brightness));
        }
        mDozeHost.setAodDimmingScrim(0f);
        mDozeHost.setAodWallpaperDimmingScrim(0f);
    }

    private void setAmbientLightSensorEnabled(boolean enabled) {
        if (enabled && !mAmbientLightRegistered && mAmbientLightSensor != null) {
            mLightSensorSampleCount = 0;
            mAmbientLightRegistered = mSensorManager.registerListener(
                    mAmbientLightListener, mAmbientLightSensor,
                    SensorManager.SENSOR_DELAY_UI, mHandler);
        } else if (!enabled && mAmbientLightRegistered) {
            mSensorManager.unregisterListener(mAmbientLightListener);
            mAmbientLightRegistered = false;
        }
    }

    private void setProximitySensorEnabled(boolean enabled) {
        if (enabled && !mProximityRegistered && mProximitySensor != null) {
            mLastProxNear = false;
            mWaitDelayFinish = false;
            mProximityRegistered = mSensorManager.registerListener(
                    mProximityListener, mProximitySensor,
                    SensorManager.SENSOR_DELAY_NORMAL, mHandler);
        } else if (!enabled && mProximityRegistered) {
            mSensorManager.unregisterListener(mProximityListener);
            mProximityRegistered = false;
            mWaitDelayFinish = false;
            mHandler.removeCallbacks(mDelayedAodBrightnessRunnable);
        }
    }

    private void setLightSensorEnabled(boolean enabled) {
        if (enabled && !mRegistered && (isLightSensorPresent() || hasPanoramicAodCurve())) {
            mIsFirstPanoramicAodBrightness = true;
            if (isLightSensorPresent()) {
                // Wait until we get an event from the sensor until indicating ready.
                mRegistered = mSensorManager.registerListener(this, getLightSensor(),
                        SensorManager.SENSOR_DELAY_NORMAL, mHandler);
            } else {
                mRegistered = true;
            }
            if (hasPanoramicAodCurve()) {
                setProximitySensorEnabled(true);
                setAmbientLightSensorEnabled(true);
            }
            mLastSensorValue = -1;
        } else if (!enabled && mRegistered) {
            if (isLightSensorPresent()) {
                mSensorManager.unregisterListener(this);
            }
            setAmbientLightSensorEnabled(false);
            setProximitySensorEnabled(false);
            mRegistered = false;
            mLastSensorValue = -1;
            // Sensor is not enabled, hence we use the default brightness and are always ready.
        }
    }

    private void setPaused(boolean paused) {
        if (mPaused != paused) {
            mPaused = paused;
            updateBrightnessAndReady(false /* force */);
        }
    }

    private void setScreenOff(boolean screenOff) {
        if (mScreenOff != screenOff) {
            mScreenOff = screenOff;
            updateBrightnessAndReady(true /* force */);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        mDebugBrightnessBucket = intent.getIntExtra(BRIGHTNESS_BUCKET, -1);
        updateBrightnessAndReady(false /* force */);
    }

    private void startListeningForWallpaperSupportsAmbientMode() {
        if (mWallpaperSupportsAmbientModeJob != null) return;
        mWallpaperSupportsAmbientModeJob = JavaAdapterKt.collectFlow(
                mScope,
                mScope.getCoroutineContext(),
                mWallpaperInteractor.getWallpaperSupportsAmbientMode(),
                supportsAmbientMode -> mWallpaperSupportsAmbientMode = supportsAmbientMode
        );
    }

    private void stopListeningForWallpaperSupportsAmbientMode() {
        if (mWallpaperSupportsAmbientModeJob == null) return;
        mWallpaperSupportsAmbientModeJob.cancel(new CancellationException("Stop monitoring"));
        mWallpaperSupportsAmbientModeJob = null;
    }

    /** Dump current state */
    public void dump(PrintWriter pw) {
        pw.println("DozeScreenBrightness:");
        IndentingPrintWriter idpw = new IndentingPrintWriter(pw);
        idpw.increaseIndent();
        idpw.println("registered=" + mRegistered);
        idpw.println("posture=" + DevicePostureController.devicePostureToString(mDevicePosture));
        idpw.println("sensorToBrightness=" + Arrays.toString(mSensorToBrightness));
        idpw.println("sensorToScrimOpacity=" + Arrays.toString(mSensorToScrimOpacity));
        idpw.println("sensorToWallpaperScrimOpacity="
                + Arrays.toString(mSensorToWallpaperScrimOpacity));
        idpw.println("screenBrightnessDim=" + mScreenBrightnessDim);
        idpw.println("mDefaultDozeBrightness=" + mDefaultDozeBrightness);
        idpw.println("mLastSensorValue=" + mLastSensorValue);
    }

    private final DevicePostureController.Callback mDevicePostureCallback =
            new DevicePostureController.Callback() {
        @Override
        public void onPostureChanged(int posture) {
            if (mDevicePosture == posture
                    || mLightSensorOptional.length < 2
                    || posture >= mLightSensorOptional.length) {
                return;
            }
            Sensor oldSensor = mLightSensorOptional[mDevicePosture].orElse(null);
            Sensor newSensor = mLightSensorOptional[posture].orElse(null);
            if (Objects.equals(oldSensor, newSensor)) {
                mDevicePosture = posture;
                // uses the same sensor for the new posture
                return;
            }

            // cancel the previous sensor:
            if (mRegistered) {
                setLightSensorEnabled(false);
                mDevicePosture = posture;
                setLightSensorEnabled(true);
            } else {
                mDevicePosture = posture;
            }
            mDozeLog.tracePostureChanged(mDevicePosture, "DozeScreenBrightness swap "
                    + "{" + oldSensor + "} => {" + newSensor + "}, mRegistered=" + mRegistered);
        }
    };
}
