package com.android.server.display.feature.ambience.utils;

import android.os.SystemProperties;
import android.util.Slog;

/* JADX INFO: loaded from: /run/media/sathiya/data/scratch/ambience.dex */
public class AmbienceLog {
    public static boolean isLogOn = false;
    public static boolean isUserVersion = "user".equals(SystemProperties.get("ro.build.type", "unknown"));
    public static boolean isBootCompleted = false;

    public interface ILogPrintLogic {
        boolean isLogPrint();
    }

    private AmbienceLog() {
    }

    public static void setAmbienceLogOn(boolean isOn) {
        Slog.d("AmbienceLog", "setAmbienceLogOn = " + isOn);
        isLogOn = isOn;
    }

    public static void onBootCompleted() {
        isBootCompleted = true;
    }

    public static void d(String tag, String msg) {
        if (isLogOn || !isBootCompleted) {
            Slog.d(tag, msg);
        }
    }

    public static void i(String tag, String msg) {
        if (isLogOn || !isBootCompleted) {
            Slog.i(tag, msg);
        }
    }

    public static void w(String tag, String msg) {
        Slog.w(tag, msg);
    }

    public static void e(String tag, String msg) {
        Slog.e(tag, msg);
    }

    public static void f(String tag, String msg) {
        Slog.d(tag, msg);
    }

    public static void logic(String tag, ILogPrintLogic logic, String msg) {
        if (logic.isLogPrint()) {
            Slog.d(tag, msg);
        }
    }

    public static void logic(String tag, ILogPrintLogic logic, String trueMsg, String falseMsg) {
        if (logic.isLogPrint()) {
            Slog.d(tag, trueMsg);
        } else {
            Slog.d(tag, falseMsg);
        }
    }
}
