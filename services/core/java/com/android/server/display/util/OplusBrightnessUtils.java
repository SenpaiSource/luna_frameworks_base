package com.android.server.display.util;

import android.content.Context;
import android.database.Cursor;
import android.hardware.display.DisplayManagerInternal;
import android.hardware.display.OplusDisplayManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Process;
import android.provider.Settings;
import android.text.TextUtils;
import com.android.internal.display.BrightnessSynchronizer;
import com.android.server.display.IDisplayBrightnessModelExt;
import com.android.server.display.config.global.OplusDisplayGlobalConfig;
import com.android.server.display.feature.ambience.utils.AmbienceLog;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/* JADX INFO: loaded from: /run/media/sathiya/data/scratch/deps.dex */
public class OplusBrightnessUtils {
    private static final float EPSILON = 0.01f;
    private static final String FEATURE_REDUCE_BRIGHTNESS_RM = "oplus.software.display.reduce_brightness_rm";
    private static final String FEATURE_REDUCE_TEMPERATURE_RM = "oplus.software.display.temperature_rm";
    private static final String TAG = "OplusBrightnessUtils";
    private static final Map<Integer, String> EDRTYPEMAP = new HashMap();
    private static final Map<Integer, String> BRIGHTNESSRATETYPEMAP = new HashMap();
    private static final Uri OPLUS_ROM_UPDATE_URI = Uri.parse("content://com.oplus.romupdate.provider.db/update_list");
    private static AtomicBoolean sBootCompled = new AtomicBoolean(false);

    static {
        EDRTYPEMAP.put(0, "NORMAL");
        EDRTYPEMAP.put(1, "LOCAL_HDR");
        EDRTYPEMAP.put(2, "DOLBY_VISION");
        EDRTYPEMAP.put(4, "PW");
        EDRTYPEMAP.put(8, "AURORA_WALLPAPER");
        EDRTYPEMAP.put(16, "HDR_VISION");
        EDRTYPEMAP.put(32, "GAME");
        EDRTYPEMAP.put(64, "UHDR");
        BRIGHTNESSRATETYPEMAP.put(0, "automatic");
        BRIGHTNESSRATETYPEMAP.put(1, OplusBrightnessConstants.AppSplineXml.TAG_APP);
        BRIGHTNESSRATETYPEMAP.put(2, "manual");
        BRIGHTNESSRATETYPEMAP.put(3, "window");
        BRIGHTNESSRATETYPEMAP.put(4, "on");
        BRIGHTNESSRATETYPEMAP.put(5, "off");
        BRIGHTNESSRATETYPEMAP.put(6, "dim");
        BRIGHTNESSRATETYPEMAP.put(7, "hdrhbm");
        BRIGHTNESSRATETYPEMAP.put(9, "edr enter");
        BRIGHTNESSRATETYPEMAP.put(10, "edr exit");
        BRIGHTNESSRATETYPEMAP.put(14, "pwedr enter");
        BRIGHTNESSRATETYPEMAP.put(15, "pwedr exit");
        BRIGHTNESSRATETYPEMAP.put(28, "min auto brightness");
        BRIGHTNESSRATETYPEMAP.put(11, "uncovermode");
        BRIGHTNESSRATETYPEMAP.put(12, "gallery in");
        BRIGHTNESSRATETYPEMAP.put(13, "gallery out");
        BRIGHTNESSRATETYPEMAP.put(16, "long take");
        BRIGHTNESSRATETYPEMAP.put(30, "panoramic aod E/X");
        BRIGHTNESSRATETYPEMAP.put(32, "panoramic aod...");
        BRIGHTNESSRATETYPEMAP.put(17, "camera out");
        BRIGHTNESSRATETYPEMAP.put(18, "temperature");
        BRIGHTNESSRATETYPEMAP.put(19, "sunlight in");
        BRIGHTNESSRATETYPEMAP.put(20, "sunlight out");
        BRIGHTNESSRATETYPEMAP.put(21, "window enter");
        BRIGHTNESSRATETYPEMAP.put(22, "window exit");
        BRIGHTNESSRATETYPEMAP.put(23, "game");
        BRIGHTNESSRATETYPEMAP.put(24, "brightness bar changed");
        BRIGHTNESSRATETYPEMAP.put(25, "low voltage");
        BRIGHTNESSRATETYPEMAP.put(26, "temperature limit");
        BRIGHTNESSRATETYPEMAP.put(27, "boost");
        BRIGHTNESSRATETYPEMAP.put(29, "camera in");
    }

    public static String totalRateTypeToString(int type) {
        return BRIGHTNESSRATETYPEMAP.getOrDefault(Integer.valueOf(type), Integer.toString(type));
    }

    public static final String edrTypeToString(int type) {
        return EDRTYPEMAP.getOrDefault(Integer.valueOf(type), Integer.toString(type));
    }

    public static void traversalMaps(HashMap<Integer, Float> map, float[] array) {
        for (int i = 0; i < map.size(); i++) {
            array[i] = map.get(Integer.valueOf(i)).floatValue();
        }
    }

    public static boolean isValidMap(float[] x, float[] y) {
        if (x == null || y == null || x.length == 0 || y.length == 0 || x.length < 2) {
            AmbienceLog.w(TAG, "invalid x or y !!!");
            return false;
        }
        if (x.length != y.length) {
            AmbienceLog.w(TAG, "x's lenght should be equal with y's !!!");
            return false;
        }
        int N = x.length;
        float prevX = x[0];
        float prevY = y[0];
        if (prevX < 0.0f || prevY < 0.0f || Float.isNaN(prevX) || Float.isNaN(prevY)) {
            AmbienceLog.w(TAG, "prevX and prevY should be anomral float value !!!");
            return false;
        }
        for (int i = 1; i < N; i++) {
            if (prevX >= x[i] || prevY > y[i]) {
                AmbienceLog.w(TAG, "prevX = " + prevX + ", x = " + x[i] + ", prevY = " + prevY + ", y = " + y[i] + "@" + i);
                return false;
            }
            if (Float.isNaN(x[i]) || Float.isNaN(y[i])) {
                AmbienceLog.w(TAG, "value is nan!!!");
                return false;
            }
            prevX = x[i];
            prevY = y[i];
        }
        return true;
    }

    public static boolean isValidMap(HashMap<Integer, Float> luxTable, HashMap<Integer, Float> nitTable) {
        float[] luxs = new float[luxTable.size()];
        float[] nits = new float[nitTable.size()];
        traversalMaps(luxTable, luxs);
        traversalMaps(nitTable, nits);
        return isValidMap(luxs, nits);
    }

    public static boolean isValidMap(HashMap<Integer, Float[]> hbmArray) {
        if (hbmArray.isEmpty()) {
            AmbienceLog.w(TAG, "hbmArray is empty!!!");
            return false;
        }
        float x0 = 0.0f;
        float x1 = 0.0f;
        for (int i = 0; i < hbmArray.size(); i++) {
            Float[] data = hbmArray.get(Integer.valueOf(i));
            if (data == null) {
                AmbienceLog.w(TAG, "data is null!!!");
                return false;
            }
            if (data.length == 4 || data.length == 2) {
                if (x0 < data[0].floatValue() && x1 < data[1].floatValue()) {
                    x0 = data[0].floatValue();
                    x1 = data[1].floatValue();
                } else {
                    AmbienceLog.w(TAG, "not increate");
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean isSpecialAdj(float value, float maxBrightness) {
        float adjustmentFactor = maxBrightness / 255.0f;
        float adjCameraOur = 500.0f * adjustmentFactor;
        float adjGalleryIn = 300.0f * adjustmentFactor;
        float adjCameraIn = 301.0f * adjustmentFactor;
        float[] specialValues = {adjCameraOur, adjGalleryIn, adjCameraIn, 16384.0f, 32768.0f, 16385.0f, 32769.0f, 32771.0f, 16386.0f, 32770.0f};
        for (float specialValue : specialValues) {
            if (floatEquals(value, specialValue)) {
                return true;
            }
        }
        return false;
    }

    public static boolean floatEquals(float a, float b) {
        if (a == b) {
            return true;
        }
        return (Float.isNaN(a) && Float.isNaN(b)) || Math.abs(a - b) < EPSILON;
    }

    public static boolean less(float a, float b) {
        return !floatEquals(a, b) && a < b;
    }

    public static boolean greater(float a, float b) {
        return !floatEquals(a, b) && a > b;
    }

    public static boolean isFolding(boolean isScreenOn, DisplayManagerInternal.DisplayPowerRequest request) {
        boolean proxi = request.useProximitySensor;
        boolean bright = request.isBrightOrDim();
        if (isScreenOn || !bright || proxi) {
            return false;
        }
        return true;
    }

    public static boolean isBootCompled() {
        return sBootCompled.get();
    }

    public static void setBootCompled(boolean bootCompled) {
        sBootCompled.set(bootCompled);
    }

    public static String queryUpdateXmlFromProvider(String filterName, Context ctx) {
        if (ctx == null) {
            AmbienceLog.e(TAG, "Query update provider failed ctx is null !!!");
            return null;
        }
        Cursor cursor = null;
        String[] projection = {OplusBrightnessConstants.Config.COLUMN_NAME_VERSION, OplusBrightnessConstants.Config.COLUMN_NAME_XML};
        String xml = null;
        try {
            cursor = ctx.getContentResolver().query(OPLUS_ROM_UPDATE_URI, projection, "filtername=\"" + filterName + "\"", null, null);
            if (cursor != null && cursor.getCount() > 0) {
                int versionColumnIndex = cursor.getColumnIndex(OplusBrightnessConstants.Config.COLUMN_NAME_VERSION);
                int xmlColumnIndex = cursor.getColumnIndex(OplusBrightnessConstants.Config.COLUMN_NAME_XML);
                cursor.moveToNext();
                long version = cursor.getLong(versionColumnIndex);
                xml = cursor.getString(xmlColumnIndex);
                AmbienceLog.d(TAG, "rom update version = " + version);
            }
        } catch (Exception e) {
            AmbienceLog.e(TAG, "Query update provider exception: " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return xml;
    }

    public static boolean updateLocalDynamicInfo(String content, String dir, String name) {
        if (TextUtils.isEmpty(content)) {
            AmbienceLog.e(TAG, "Save dynamic info file failed! Content is empty.");
            return false;
        }
        try {
            File dynamicInfoFile = new File(dir, name);
            if (saveContentToFile(content, dynamicInfoFile)) {
                AmbienceLog.d(TAG, "Save dynamic info file success!" + dir + "/" + name);
                return true;
            }
        } catch (Exception e) {
            AmbienceLog.e(TAG, e.getMessage());
        }
        return false;
    }

    public static boolean saveContentToFile(String content, File file) {
        if (content == null || file == null) {
            return false;
        }
        if (file.exists() && !file.delete()) {
            AmbienceLog.w(TAG, "delete file failed!");
            return false;
        }
        FileOutputStream fileOutputStream = null;
        try {
            try {
                fileOutputStream = new FileOutputStream(file);
                fileOutputStream.write(content.getBytes());
                try {
                    fileOutputStream.close();
                    return true;
                } catch (IOException e) {
                    AmbienceLog.e(TAG, "IOException: " + e.getMessage());
                    return true;
                }
            } catch (Exception e2) {
                AmbienceLog.e(TAG, "save content exception: " + e2.getMessage());
                if (fileOutputStream != null) {
                    try {
                        fileOutputStream.close();
                    } catch (IOException e3) {
                        AmbienceLog.e(TAG, "IOException: " + e3.getMessage());
                    }
                }
                return false;
            }
        } catch (Throwable th) {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e4) {
                    AmbienceLog.e(TAG, "IOException: " + e4.getMessage());
                }
            }
            throw th;
        }
    }

    public static int getScreenBrightness(Context context) {
        String value = Settings.System.getStringForUser(context.getContentResolver(), "screen_brightness", -2);
        try {
            int screenBrightness = Integer.parseInt(value);
            return screenBrightness;
        } catch (NumberFormatException nfe) {
            AmbienceLog.e(TAG, "getScreenBrightness fail !!" + nfe.toString());
            return -1;
        }
    }

    public static boolean getLongTakeSupport(Context context) {
        if (context != null) {
            boolean support = Settings.Secure.getIntForUser(context.getContentResolver(), OplusBrightnessConstants.SUB_DISPLAY_AOD_ON_TO_DOZE_SWITCH, 0, -2) == 1;
            return support && OplusDisplayGlobalConfig.getInstance().hasRemapDisable();
        }
        AmbienceLog.e(TAG, "getLongTakeSupport false for context is null");
        return false;
    }

    public static boolean getFullScreenAodSupport(Context context) {
        if (context != null) {
            return Settings.Secure.getIntForUser(context.getContentResolver(), OplusBrightnessConstants.FULL_SCREEN_AOD, 0, -2) == 1;
        }
        AmbienceLog.e(TAG, "isFullScreenAodSupport false for context is null");
        return false;
    }

    public static boolean isAutoBrightness(Context context) {
        if (context != null) {
            int brightnessMode = Settings.System.getIntForUser(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, 0, -2);
            return brightnessMode == 1;
        }
        AmbienceLog.e(TAG, "isAutoBrightness false for context is null");
        return false;
    }

    public static boolean getPanoramicAodSupport(Context context) {
        if (context != null) {
            return Settings.Secure.getIntForUser(context.getContentResolver(), OplusBrightnessConstants.PANORAMIC_SCREEN_AOD, 0, -2) == 1;
        }
        AmbienceLog.e(TAG, "isPanoramicAodSupport false for context is null");
        return false;
    }

    public static float getBrightnessForRemapDisable(int displayId, float brightness, IDisplayBrightnessModelExt model) {
        if (!OplusDisplayGlobalConfig.getInstance().hasRemapDisable() || displayId != 1 || Float.isNaN(brightness) || model == null) {
            return brightness;
        }
        float nit = OplusDisplayManager.getInstance().getNitFromBrightness(0, brightness);
        float resultBrightness = model.clampBrightness(model.getBrightnessFromNit(nit));
        return resultBrightness;
    }

    public static String getDeviceManufactureByIO(String path) {
        File file = new File(path);
        String manufacture = "unknown";
        if (!file.exists()) {
            AmbienceLog.e(TAG, "File " + path + " is not exist...");
            return "unknown";
        }
        if (!file.canRead()) {
            AmbienceLog.e(TAG, "No permission to read " + path);
            return "unknown";
        }
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (line.contains(OplusBrightnessConstants.KEY_DEVICE_VERSION)) {
                    String[] lineSplit = line.split(",");
                    if (lineSplit.length > 1) {
                        manufacture = lineSplit[1].trim();
                        AmbienceLog.i(TAG, "version : " + manufacture);
                    }
                }
                if (line.contains(OplusBrightnessConstants.KEY_DEVICE_MANUFACTURE)) {
                    String[] lineSplit2 = line.split(",");
                    if (lineSplit2.length > 1) {
                        manufacture = lineSplit2[1].trim();
                        AmbienceLog.i(TAG, "manufacture : " + manufacture);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            AmbienceLog.i(TAG, "getDeviceManufactureByIO error: " + e.getMessage());
        }
        return manufacture;
    }

    public static boolean setUxThreadValueByFile(int value) {
        return true;
    }

    public static boolean isRM() {
        return false;
    }

    public static boolean setUAD(boolean isRequest, int event) {
        return true;
    }

    public static boolean isValidBrightnessValue(float brightness) {
        return !Float.isNaN(brightness) && brightness >= 0.0f && brightness <= 1.0f;
    }

    public static boolean isValidBrightnessValue(float brightness, float screenBrightnessRangeMinimum, float screenBrightnessRangeMaximum) {
        return !Float.isNaN(brightness) && (brightness >= screenBrightnessRangeMinimum || BrightnessSynchronizer.floatEquals(brightness, screenBrightnessRangeMinimum)) && (brightness <= screenBrightnessRangeMaximum || BrightnessSynchronizer.floatEquals(brightness, screenBrightnessRangeMaximum));
    }

    public static boolean isAutoStepper(float rate, boolean isDragging, boolean isAutoBrightness, int rateType, IDisplayBrightnessModelExt model) {
        if (model != null) {
            return !isDragging && isAutoBrightness && rate != ((float) model.getBrightnessRampRateFast()) && rateType == 0;
        }
        AmbienceLog.e(TAG, "DisplayBrightnessModel is null");
        return false;
    }

    public static float parseFloatSafe(String value, float defaultValue) {
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
