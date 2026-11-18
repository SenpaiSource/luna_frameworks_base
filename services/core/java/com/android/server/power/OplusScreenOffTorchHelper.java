package com.android.server.power;

import android.content.Context;
import android.hardware.camera2.CameraManager;
import android.util.Slog;

public class OplusScreenOffTorchHelper {
    private static final String TAG = "OplusScreenOffTorchHelper";
    private static volatile OplusScreenOffTorchHelper sInstance;
    private volatile boolean mFlashLightEnable = false;

    public static OplusScreenOffTorchHelper getInstance(Context context) {
        if (sInstance == null) {
            synchronized (OplusScreenOffTorchHelper.class) {
                if (sInstance == null) {
                    sInstance = new OplusScreenOffTorchHelper(context);
                }
            }
        }
        return sInstance;
    }

    private OplusScreenOffTorchHelper(Context context) {
        if (context != null) {
            try {
                CameraManager cm = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
                if (cm != null) {
                    cm.registerTorchCallback(new CameraManager.TorchCallback() {
                        @Override
                        public void onTorchModeChanged(String cameraId, boolean enabled) {
                            mFlashLightEnable = enabled;
                        }
                    }, null);
                }
            } catch (Exception e) {
                Slog.e(TAG, "failed to register torch callback", e);
            }
        }
    }

    public boolean isFlashLightOn() {
        return this.mFlashLightEnable;
    }
}
