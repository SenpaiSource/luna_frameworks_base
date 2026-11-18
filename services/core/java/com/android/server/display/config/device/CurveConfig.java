package com.android.server.display.config.device;

import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.IndentingPrintWriter;
import com.android.internal.util.XmlUtils;
import com.android.server.display.feature.ambience.events.delegate.DeepThinkerDelegate;
import com.android.server.display.feature.ambience.utils.AmbienceLog;
import com.android.server.display.util.AutoReleaseReadWriteLock;
import com.android.server.display.util.OplusBrightnessConstants;
import com.android.server.display.util.OplusBrightnessUtils;
import java.util.HashMap;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/* JADX INFO: loaded from: /run/media/sathiya/data/scratch/deps.dex */
public class CurveConfig {
    private static final String DISPLAY_BRIGHTNESS_APOLLO_PROP = "persist.brightness.apollo";
    private static final String DISPLAY_BRIGHTNESS_PROP = "ro.display.brightness";
    private static final String DISPLAY_BRIGHTNESS_SILKY_PROP = "persist.vendor.sys.pq.disp.ccorr.silky.brightness";
    private static final int INVALID_PARAM = -1;
    private static final String OPLUS_DISPLAY_BRIGHTNESS_PROP = "ro.oplus.display.brightness";
    private static final String TAG = "CurveConfig";
    private static final String TAG_BRIGHTNESS_TABLE = "brightness_table";
    private static final String TAG_BRIGHTNESS_TABLE_PWM = "brightness_table_pwm";
    private static final String TAG_BRIGHTNESS_TABLE_SINGLE_PULSE = "brightness_table_single_pulse";
    private static final String TAG_ENTER_LUX = "enter";
    private static final String TAG_EXIT_LUX = "exit";
    private static final String TAG_FILTER_LEVELS = "Levels";
    private static final String TAG_HBM_LUX_TABLE = "hbm_lux_table";
    private static final String TAG_HBM_LUX_TABLE_MODE = "hbm_lux_table_mode";
    private static final String TAG_ID = "id";
    private static final String TAG_LEVEL = "level";
    private static final String TAG_LUX = "lux";
    private static final String TAG_LUX_TABLE = "lux_table";
    private static final String TAG_LUX_TABLE_MODE = "lux_table_mode";
    private static final String TAG_MANUFACTURE = "manufacture";
    private static final String TAG_MAX = "max";
    private static final String TAG_MIN = "min";
    private static final String TAG_NAME = "name";
    private static final String TAG_NIT = "nit";
    private static final String TAG_RANGE = "range";
    private static final String TAG_SALES_MODE_SCREEN_BRIGHTNESS = "sales_mode_screen_brightness";
    private static final String mSettingsMinBrightnessProp = SystemProperties.get("ro.oplus.display.brightness.min_settings.rm", DeepThinkerDelegate.InOutDoorImpl.Constants.EMPTY);
    private static final int FORCE_LUXTABLEMODE_ID = SystemProperties.getInt("ro.oplus.display.force_LuxTableMode_ID", 0);
    private final AutoReleaseReadWriteLock mLock = new AutoReleaseReadWriteLock();
    private String deviceManufacture4Lux = "unknown";
    private String deviceManufacture4HbmLux = "unknown";
    private String mBrightnessCurveName = SystemProperties.get("ro.display.brightness.curve.name", OplusBrightnessConstants.UNKNOW_CURVE);
    private HashMap<Integer, Float> mLuxTable = new HashMap<>();
    private HashMap<Integer, Float> mNitTable = new HashMap<>();
    private HashMap<Integer, Float[]> mHbmDateTable = new HashMap<>();
    private HashMap<Integer, float[]> mPanelBrightnessTable = new HashMap<>();
    private HashMap<Integer, float[]> mPanelBrightnessTablePWM = new HashMap<>();
    private HashMap<Integer, float[]> mPanelBrightnessTableSPD = new HashMap<>();
    private long mMaxVersion = -1;
    private int mMaxLevel = OplusBrightnessConstants.DEFAULT_NORMAL_MAX_BRIGHTNESS;
    private int mCurveMapRange = OplusBrightnessConstants.DEFAULT_MAX_BRIGHTNESS;
    private int mMinLevel = 4;
    private int mLuxTableMode = -1;
    private int mHbmLuxTableMode = 0;
    private float mSalesModeScreenBrightness = Float.NaN;
    private boolean mIsRusUpdate = false;
    private boolean mIsRusDataDir = false;
    private int luxTableId = 0;
    private int hbmLuxTableId = 0;
    private int hbmLuxEnter = -1;
    private int hbmLuxExit = -1;
    private int mapManufactureCnt = 0;
    private String manufacture = null;
    private String name = OplusBrightnessConstants.UNKNOW_CURVE;
    private boolean mNeedParseDataDirCurve = false;
    private boolean mIsForceMode = false;
    private boolean mSettingsMinBrightnessSupport = false;

    public void init(boolean vndCurveFileExit) {
        if (vndCurveFileExit && !OplusBrightnessConstants.UNKNOW_CURVE.equals(this.mBrightnessCurveName) && this.mLuxTableMode != -1) {
            this.mLuxTableMode = -1;
            this.mHbmLuxTableMode = -1;
        } else if (!vndCurveFileExit) {
            this.mBrightnessCurveName = OplusBrightnessConstants.UNKNOW_CURVE;
        }
        this.mSettingsMinBrightnessSupport = !TextUtils.isEmpty(mSettingsMinBrightnessProp);
    }

    public void init(long currentVersion, boolean isRusUpdate, boolean isRusDataDir) {
        this.mNeedParseDataDirCurve = !isRusUpdate && isRusDataDir && currentVersion > this.mMaxVersion;
        this.mIsRusUpdate = isRusUpdate;
        this.mIsRusDataDir = isRusDataDir;
    }

    
    public int getLuxTableModeId() {
        return ((Integer) this.mLock.executeReadOperation(() -> this.mLuxTableMode)).intValue();
    }

    
    public int getHbmLuxTableModeId() {
        return ((Integer) this.mLock.executeReadOperation(() -> this.mHbmLuxTableMode)).intValue();
    }

    
    public int getMaxLevel() {
        return ((Integer) this.mLock.executeReadOperation(() -> Integer.valueOf(this.mMaxLevel > 0 ? this.mMaxLevel : 4095))).intValue();
    }

    
    public int getMinLevel() {
        return ((Integer) this.mLock.executeReadOperation(() -> Integer.valueOf(this.mMinLevel > 0 ? this.mMinLevel : 2))).intValue();
    }

    
    public long getMaxVersion() {
        return ((Long) this.mLock.executeReadOperation(() -> this.mMaxVersion)).longValue();
    }

    public void setMaxVersion(final long maxVersion) {
        this.mLock.executeWriteOperation(() -> this.mMaxVersion = maxVersion);
    }

    
    
    public String getBrightnessCurveName() {
        return (String) this.mLock.executeReadOperation(() -> this.mBrightnessCurveName);
    }

    
    public HashMap<Integer, Float> getDisplayBrightnessLuxTable() {
        return (HashMap) this.mLock.executeReadOperation(() -> this.mLuxTable);
    }

    
    public HashMap<Integer, Float> getDisplayBrightnessNitTable() {
        return (HashMap) this.mLock.executeReadOperation(() -> this.mNitTable);
    }

    
    public HashMap<Integer, Float[]> getDisplayBrightnessHbmDateTable() {
        return (HashMap) this.mLock.executeReadOperation(() -> this.mHbmDateTable);
    }

    
    public HashMap<Integer, float[]> getDisplayPanelBrightnessTable() {
        return (HashMap) this.mLock.executeReadOperation(() -> this.mPanelBrightnessTable);
    }

    
    public HashMap<Integer, float[]> getDisplayPanelBrightnessTablePWM() {
        return (HashMap) this.mLock.executeReadOperation(() -> this.mPanelBrightnessTablePWM);
    }

    
    public HashMap<Integer, float[]> getDisplayPanelBrightnessTableSPD() {
        return (HashMap) this.mLock.executeReadOperation(() -> this.mPanelBrightnessTableSPD);
    }

    public void traversalPanelMaps(final float[] array, final int index) {
        this.mLock.executeReadOperation(() -> {
            for (int i = 0; i < this.mPanelBrightnessTable.size(); i++) {
                array[i] = this.mPanelBrightnessTable.get(Integer.valueOf(i))[index];
            }
            return null;
        });
    }

    
    public void traversalPanelMapsPWM(final float[] array, final int index) {
        this.mLock.executeReadOperation(() -> {
            for (int i = 0; i < this.mPanelBrightnessTablePWM.size(); i++) {
                array[i] = this.mPanelBrightnessTablePWM.get(Integer.valueOf(i))[index];
            }
            return null;
        });
    }

    
    public void traversalPanelMapsSPD(final float[] array, final int index) {
        this.mLock.executeReadOperation(() -> {
            for (int i = 0; i < this.mPanelBrightnessTableSPD.size(); i++) {
                array[i] = this.mPanelBrightnessTableSPD.get(Integer.valueOf(i))[index];
            }
            return null;
        });
    }

    
    public void cloneConfig(final CurveConfig config) {
        this.mLock.executeWriteOperation(() -> {
            this.mLuxTable = config.getDisplayBrightnessLuxTable();
            this.mNitTable = config.getDisplayBrightnessNitTable();
            this.mBrightnessCurveName = config.getBrightnessCurveName();
            this.mMaxVersion = config.getMaxVersion();
            if (!config.getDisplayBrightnessHbmDateTable().isEmpty()) {
                this.mHbmDateTable = config.getDisplayBrightnessHbmDateTable();
            }
        });
    }

    public void clearPanelBrightnessMap() {
        this.mLock.executeWriteOperation(() -> {
            if (this.mPanelBrightnessTable != null) {
                this.mPanelBrightnessTable.clear();
                this.mPanelBrightnessTable = null;
            }
            if (this.mPanelBrightnessTablePWM != null) {
                this.mPanelBrightnessTablePWM.clear();
                this.mPanelBrightnessTablePWM = null;
            }
            if (this.mPanelBrightnessTableSPD != null) {
                this.mPanelBrightnessTableSPD.clear();
                this.mPanelBrightnessTableSPD = null;
            }
        });
    }

    public void parseContent(OplusDisplayDeviceConfig.TagParameter tag, String mManufacture) throws XmlPullParserException, IllegalArgumentException {
        Float[] parameters;
        XmlPullParser parser = tag.parser;
        if (FORCE_LUXTABLEMODE_ID > 0) {
            this.mIsForceMode = true;
        }
        switch (parser.getEventType()) {
            case 2:
                if (TAG_LUX_TABLE.equals(tag.startTag)) {
                    if (!this.mNeedParseDataDirCurve && !this.mLuxTable.isEmpty()) {
                        tag.startTag = null;
                        return;
                    }
                    if (this.mNeedParseDataDirCurve) {
                        AmbienceLog.d(TAG, " mNeedParseDataDirCurve for TAG_LUX_TABLE");
                        this.mLuxTable.clear();
                        this.mNitTable.clear();
                    }
                    if (!this.mIsRusUpdate && !this.mIsRusDataDir) {
                        this.luxTableId = XmlUtils.convertValueToInt(parser.getAttributeValue(null, TAG_ID), 0);
                        this.name = parser.getAttributeValue(null, "name");
                    } else {
                        String tmpManufactor = parser.getAttributeValue(null, TAG_MANUFACTURE);
                        if (tmpManufactor != null) {
                            this.deviceManufacture4Lux = tmpManufactor;
                        }
                    }
                    tag.tableCnt = 0;
                    return;
                }
                if (TAG_HBM_LUX_TABLE.equals(tag.startTag)) {
                    if (!this.mNeedParseDataDirCurve && !this.mHbmDateTable.isEmpty()) {
                        tag.startTag = null;
                        return;
                    }
                    if (this.mNeedParseDataDirCurve) {
                        AmbienceLog.d(TAG, " mNeedParseDataDirCurve for TAG_HBM_LUX_TABLE");
                        this.mHbmDateTable.clear();
                    }
                    if (!this.mIsRusUpdate && !this.mIsRusDataDir) {
                        this.hbmLuxTableId = XmlUtils.convertValueToInt(parser.getAttributeValue(null, TAG_ID), 0);
                    } else {
                        String tmpManufactor2 = parser.getAttributeValue(null, TAG_MANUFACTURE);
                        if (tmpManufactor2 != null) {
                            this.deviceManufacture4HbmLux = tmpManufactor2;
                        }
                    }
                    tag.tableCnt = 0;
                    return;
                }
                if (TAG_BRIGHTNESS_TABLE.equals(tag.startTag)) {
                    this.mMaxLevel = XmlUtils.convertValueToInt(parser.getAttributeValue(null, TAG_MAX), this.mMaxLevel);
                    this.mMinLevel = XmlUtils.convertValueToInt(parser.getAttributeValue(null, TAG_MIN), this.mMinLevel);
                    tag.tableCnt = 0;
                    return;
                }
                if (TAG_BRIGHTNESS_TABLE_PWM.equals(tag.startTag)) {
                    this.mMaxLevel = XmlUtils.convertValueToInt(parser.getAttributeValue(null, TAG_MAX), this.mMaxLevel);
                    this.mMinLevel = XmlUtils.convertValueToInt(parser.getAttributeValue(null, TAG_MIN), this.mMinLevel);
                    tag.tableCnt = 0;
                    return;
                } else {
                    if (TAG_FILTER_LEVELS.equals(tag.startTag)) {
                        tag.tableCnt = 0;
                        this.manufacture = parser.getAttributeValue(null, TAG_MANUFACTURE);
                        this.mCurveMapRange = XmlUtils.convertValueToInt(parser.getAttributeValue(null, TAG_RANGE), this.mCurveMapRange);
                        this.mapManufactureCnt++;
                        AmbienceLog.i(TAG, "display_curve_map: mManufacture=" + mManufacture + ", manufacture=" + this.manufacture + ", range=" + this.mCurveMapRange + ", cnt=" + this.mapManufactureCnt);
                        return;
                    }
                    if (TAG_LUX.equals(tag.startTag) && TAG_HBM_LUX_TABLE.equals(tag.topTag)) {
                        this.hbmLuxEnter = XmlUtils.convertValueToInt(parser.getAttributeValue(null, TAG_ENTER_LUX), -1);
                        this.hbmLuxExit = XmlUtils.convertValueToInt(parser.getAttributeValue(null, TAG_EXIT_LUX), -1);
                        return;
                    }
                    return;
                }
            case 3:
                if (this.mIsRusUpdate && TAG_LUX_TABLE.equals(tag.endTag)) {
                    if (!OplusBrightnessUtils.isValidMap(this.mLuxTable, this.mNitTable)) {
                        this.mLuxTable.clear();
                        this.mNitTable.clear();
                        AmbienceLog.d(TAG, "config of TAG_LUX_TABLE is invalid");
                        throw new IllegalArgumentException("config of TAG_LUX_TABLE is invalid");
                    }
                    return;
                }
                if (this.mIsRusUpdate && TAG_HBM_LUX_TABLE.equals(tag.endTag) && !OplusBrightnessUtils.isValidMap(this.mHbmDateTable)) {
                    this.mHbmDateTable.clear();
                    AmbienceLog.d(TAG, "config of TAG_HBM_LUX_TABLE is invalid");
                    throw new IllegalArgumentException("config of TAG_LUX_TABLE is invalid");
                }
                return;
            case 4:
                String text = parser.getText();
                char c = 2;
                if (TAG_LUX_TABLE.equals(tag.topTag) && TAG_LUX.equals(tag.startTag) && (this.luxTableId == this.mLuxTableMode || this.mBrightnessCurveName.equals(this.name) || ((this.mIsRusUpdate || this.mIsRusDataDir) && ("unknown".equals(this.deviceManufacture4Lux) || mManufacture.equals(this.deviceManufacture4Lux))))) {
                    String[] texts = text.split(",");
                    float lux = OplusBrightnessUtils.parseFloatSafe(texts[0].trim(), 0.0f);
                    float nit = texts.length > 1 ? OplusBrightnessUtils.parseFloatSafe(texts[1].trim(), 0.0f) : 0.0f;
                    if (this.mSettingsMinBrightnessSupport && tag.tableCnt == 0 && this.mPanelBrightnessTable != null) {
                        float[] minEntry = this.mPanelBrightnessTable.get(Integer.valueOf(this.mMinLevel));
                        if (minEntry != null && minEntry.length > 2) {
                            nit = minEntry[2];
                        }
                    }
                    this.mLuxTable.put(Integer.valueOf(tag.tableCnt), Float.valueOf(lux));
                    this.mNitTable.put(Integer.valueOf(tag.tableCnt), Float.valueOf(nit));
                    tag.tableCnt++;
                    return;
                }
                int i = 4;
                if (TAG_BRIGHTNESS_TABLE.equals(tag.topTag) && TAG_LEVEL.equals(tag.startTag)) {
                    String[] ts = text.split(";");
                    if (ts.length > 2) {
                        int i2 = 0;
                        while (i2 < ts.length - 1) {
                            String[] texts2 = ts[i2].split(",");
                            float brightness = Float.parseFloat(texts2[0].trim());
                            float backlight = Float.parseFloat(texts2[1].trim());
                            float nit2 = Float.parseFloat(texts2[c].trim());
                            float pcc = Float.parseFloat(texts2[3].trim());
                            char c2 = c;
                            float[] brightnessparameters = new float[i];
                            brightnessparameters[0] = brightness;
                            brightnessparameters[1] = backlight;
                            brightnessparameters[c2] = nit2;
                            brightnessparameters[3] = pcc;
                            this.mPanelBrightnessTable.put(Integer.valueOf(tag.tableCnt), brightnessparameters);
                            tag.tableCnt++;
                            i2++;
                            c = c2;
                            i = 4;
                        }
                        return;
                    }
                    String[] texts3 = text.split(",");
                    float brightness2 = Float.parseFloat(texts3[0].trim());
                    float backlight2 = Float.parseFloat(texts3[1].trim());
                    float nit3 = Float.parseFloat(texts3[2].trim());
                    float pcc2 = Float.parseFloat(texts3[3].trim());
                    float[] brightnessparameters2 = {brightness2, backlight2, nit3, pcc2};
                    this.mPanelBrightnessTable.put(Integer.valueOf(tag.tableCnt), brightnessparameters2);
                    tag.tableCnt++;
                    return;
                }
                if (TAG_BRIGHTNESS_TABLE_PWM.equals(tag.topTag) && TAG_LEVEL.equals(tag.startTag)) {
                    String[] texts4 = text.split(",");
                    float brightness3 = Float.parseFloat(texts4[0].trim());
                    float backlight3 = Float.parseFloat(texts4[1].trim());
                    float nit4 = Float.parseFloat(texts4[c].trim());
                    float pcc3 = Float.parseFloat(texts4[3].trim());
                    float[] brightnessparameters3 = {brightness3, backlight3, nit4, pcc3};
                    this.mPanelBrightnessTablePWM.put(Integer.valueOf(tag.tableCnt), brightnessparameters3);
                    tag.tableCnt++;
                    return;
                }
                if (TAG_BRIGHTNESS_TABLE_SINGLE_PULSE.equals(tag.topTag) && TAG_LEVEL.equals(tag.startTag)) {
                    String[] texts5 = text.split(",");
                    float brightness4 = Float.parseFloat(texts5[0].trim());
                    float backlight4 = Float.parseFloat(texts5[1].trim());
                    float nit5 = Float.parseFloat(texts5[c].trim());
                    float pcc4 = Float.parseFloat(texts5[3].trim());
                    float[] brightnessparameters4 = {brightness4, backlight4, nit5, pcc4};
                    this.mPanelBrightnessTableSPD.put(Integer.valueOf(tag.tableCnt), brightnessparameters4);
                    tag.tableCnt++;
                    return;
                }
                if (TAG_LUX_TABLE_MODE.equals(tag.startTag)) {
                    if (!this.mIsForceMode) {
                        this.mLuxTableMode = Integer.parseInt(text.trim());
                        return;
                    } else {
                        this.mLuxTableMode = FORCE_LUXTABLEMODE_ID;
                        AmbienceLog.d(TAG, "mLuxTableMode set to " + this.mLuxTableMode);
                        return;
                    }
                }
                if (TAG_SALES_MODE_SCREEN_BRIGHTNESS.equals(tag.startTag)) {
                    this.mSalesModeScreenBrightness = OplusBrightnessUtils.parseFloatSafe(text, Float.NaN);
                    AmbienceLog.d(TAG, "mSalesModeScreenBrightness set to " + this.mSalesModeScreenBrightness);
                    return;
                }
                if (TAG_HBM_LUX_TABLE.equals(tag.topTag) && TAG_LUX.equals(tag.startTag) && (this.hbmLuxTableId == this.mHbmLuxTableMode || ((this.mIsRusUpdate || this.mIsRusDataDir) && ("unknown".equals(this.deviceManufacture4HbmLux) || mManufacture.equals(this.deviceManufacture4HbmLux))))) {
                    String[] texts6 = text.split(",");
                    float parameterFirst = OplusBrightnessUtils.parseFloatSafe(texts6[0].trim(), 0.0f);
                    float parameterSecond = texts6.length > 1 ? OplusBrightnessUtils.parseFloatSafe(texts6[1].trim(), 0.0f) : 0.0f;
                    if (this.hbmLuxEnter != -1 && this.hbmLuxExit != -1) {
                        parameters = new Float[]{Float.valueOf(this.hbmLuxEnter), Float.valueOf(this.hbmLuxExit), Float.valueOf(parameterFirst), Float.valueOf(parameterSecond)};
                    } else {
                        parameters = new Float[]{Float.valueOf(parameterFirst), Float.valueOf(parameterSecond)};
                    }
                    this.mHbmDateTable.put(Integer.valueOf(tag.tableCnt), parameters);
                    this.hbmLuxEnter = -1;
                    this.hbmLuxExit = -1;
                    tag.tableCnt++;
                    return;
                }
                if (TAG_HBM_LUX_TABLE_MODE.equals(tag.startTag)) {
                    if (!this.mIsForceMode) {
                        this.mHbmLuxTableMode = Integer.parseInt(text.trim());
                        return;
                    } else {
                        this.mHbmLuxTableMode = FORCE_LUXTABLEMODE_ID;
                        AmbienceLog.d(TAG, "mHbmLuxTableMode set to " + this.mHbmLuxTableMode);
                        return;
                    }
                }
                if (TAG_FILTER_LEVELS.equals(tag.topTag) && TAG_NIT.equals(tag.startTag)) {
                    if (this.mapManufactureCnt == 1 || (this.manufacture != null && this.mapManufactureCnt > 1 && this.manufacture.equals(mManufacture))) {
                        String[] texts7 = text.split(",");
                        float brightness5 = Float.parseFloat(texts7[0].trim());
                        float nit6 = Float.parseFloat(texts7[1].trim());
                        float[] brightnessparameters5 = {brightness5, brightness5, nit6, 1.0f};
                        if (tag.tableCnt == 0) {
                            brightnessparameters5 = new float[]{0.0f, 0.0f, 0.0f, 1.0f};
                            this.mPanelBrightnessTable.clear();
                            this.mPanelBrightnessTable.put(0, brightnessparameters5);
                            tag.tableCnt++;
                        }
                        this.mPanelBrightnessTable.put(Integer.valueOf(tag.tableCnt), brightnessparameters5);
                        tag.tableCnt++;
                        return;
                    }
                    return;
                }
                return;
            default:
                return;
        }
    }

    public void dump(IndentingPrintWriter ipw) {
        ipw.println("luxId=" + this.mLuxTableMode);
        ipw.println("hbmLuxId=" + this.mHbmLuxTableMode);
    }

    public boolean mustUseDefault() {
        if (parseValueFromProp(DISPLAY_BRIGHTNESS_APOLLO_PROP) > 0 || parseValueFromProp(DISPLAY_BRIGHTNESS_SILKY_PROP) > 0) {
            return true;
        }
        return false;
    }

    public float getSalesModeScreenBrightness() {
        return ((Float) this.mLock.executeReadOperation(() -> {
            if (!Float.isNaN(this.mSalesModeScreenBrightness)) {
                return Float.valueOf(this.mSalesModeScreenBrightness);
            }
            return Float.valueOf(this.mMaxLevel);
        })).floatValue();
    }

    
    public boolean initFromProp(String manufacture) {
        int panelNitSize = this.mPanelBrightnessTable.size();
        if (!parseArrayFromProp(OPLUS_DISPLAY_BRIGHTNESS_PROP.concat(".xs_").concat(manufacture), this.mLuxTable) && !parseArrayFromProp(OPLUS_DISPLAY_BRIGHTNESS_PROP.concat(".xs"), this.mLuxTable) && !parseArrayFromProp(DISPLAY_BRIGHTNESS_PROP.concat(".xs"), this.mLuxTable)) {
            return false;
        }
        if (!parseArrayFromProp(OPLUS_DISPLAY_BRIGHTNESS_PROP.concat(".ys_").concat(manufacture), this.mNitTable) && !parseArrayFromProp(OPLUS_DISPLAY_BRIGHTNESS_PROP.concat(".ys"), this.mNitTable) && !parseArrayFromProp(DISPLAY_BRIGHTNESS_PROP.concat(".ys"), this.mNitTable)) {
            return false;
        }
        if (panelNitSize > 0) {
            for (int i = 0; i < this.mNitTable.size(); i++) {
                if (this.mNitTable.get(Integer.valueOf(i)).intValue() < panelNitSize) {
                    this.mNitTable.replace(Integer.valueOf(i), Float.valueOf(this.mPanelBrightnessTable.get(Integer.valueOf(this.mNitTable.get(Integer.valueOf(i)).intValue()))[2]));
                } else {
                    this.mNitTable.replace(Integer.valueOf(i), Float.valueOf(this.mPanelBrightnessTable.get(Integer.valueOf(panelNitSize - 1))[2]));
                }
            }
        }
        if (!parseHbmDataFromProp(".hbm_xs", ".hbm_ys", this.mHbmDateTable, manufacture)) {
            return false;
        }
        int value = parseValueFromProp(OPLUS_DISPLAY_BRIGHTNESS_PROP.concat(".normal_min_brightness_").concat(manufacture));
        if (value != -1) {
            this.mMinLevel = value;
        } else {
            int value2 = parseValueFromProp(OPLUS_DISPLAY_BRIGHTNESS_PROP.concat(".normal_min_brightness"));
            if (value2 != -1) {
                this.mMinLevel = value2;
            } else {
                int value3 = parseValueFromProp(DISPLAY_BRIGHTNESS_PROP.concat(".normal_min_brightness"));
                if (value3 != -1) {
                    this.mMinLevel = value3;
                }
            }
        }
        int value4 = parseValueFromProp(OPLUS_DISPLAY_BRIGHTNESS_PROP.concat(".normal_max_brightness_").concat(manufacture));
        if (value4 != -1) {
            this.mMaxLevel = value4;
            return true;
        }
        int value5 = parseValueFromProp(OPLUS_DISPLAY_BRIGHTNESS_PROP.concat(".normal_max_brightness"));
        if (value5 != -1) {
            this.mMaxLevel = value5;
            return true;
        }
        int value6 = parseValueFromProp(DISPLAY_BRIGHTNESS_PROP.concat(".normal_max_brightness"));
        if (value6 != -1) {
            this.mMaxLevel = value6;
            return true;
        }
        return true;
    }

    private boolean parseArrayFromProp(String property, HashMap<Integer, Float> map) {
        map.clear();
        String result = SystemProperties.get(property, "1");
        if (!result.equals("1")) {
            String[] array = result.trim().split(",");
            for (int i = 0; i < array.length; i++) {
                float value = Float.parseFloat(array[i]);
                map.put(Integer.valueOf(i), Float.valueOf(value));
            }
            return true;
        }
        return false;
    }

    private int parseValueFromProp(String property) {
        String result = SystemProperties.get(property, "1");
        if (!result.equals("1")) {
            return Integer.parseInt(result.trim());
        }
        return -1;
    }

    private boolean parseHbmDataFromProp(String keyProp, String valueProp, HashMap<Integer, Float[]> map, String manufacture) {
        map.clear();
        int panelNitSize = this.mPanelBrightnessTable.size();
        String keyStr = SystemProperties.get(DISPLAY_BRIGHTNESS_PROP.concat(keyProp), DeepThinkerDelegate.InOutDoorImpl.Constants.EMPTY);
        if (TextUtils.isEmpty(keyStr)) {
            keyStr = SystemProperties.get(DISPLAY_BRIGHTNESS_PROP.concat(keyProp).concat("_").concat(manufacture), DeepThinkerDelegate.InOutDoorImpl.Constants.EMPTY);
            if (TextUtils.isEmpty(keyStr)) {
                keyStr = SystemProperties.get(OPLUS_DISPLAY_BRIGHTNESS_PROP.concat(keyProp), DeepThinkerDelegate.InOutDoorImpl.Constants.EMPTY);
            }
        }
        String valueStr = SystemProperties.get(OPLUS_DISPLAY_BRIGHTNESS_PROP.concat(valueProp), DeepThinkerDelegate.InOutDoorImpl.Constants.EMPTY);
        if (TextUtils.isEmpty(valueStr)) {
            valueStr = SystemProperties.get(OPLUS_DISPLAY_BRIGHTNESS_PROP.concat(valueProp).concat("_").concat(manufacture), DeepThinkerDelegate.InOutDoorImpl.Constants.EMPTY);
            if (TextUtils.isEmpty(valueStr)) {
                valueStr = SystemProperties.get(DISPLAY_BRIGHTNESS_PROP.concat(valueProp), DeepThinkerDelegate.InOutDoorImpl.Constants.EMPTY);
            }
        }
        if (TextUtils.isEmpty(keyStr) || TextUtils.isEmpty(valueStr)) {
            return false;
        }
        String[] keyArray = keyStr.trim().split(",");
        String[] valueArray = valueStr.trim().split(",");
        if (keyArray.length != valueArray.length) {
            return false;
        }
        for (int i = 0; i < keyArray.length; i++) {
            float key = Float.parseFloat(keyArray[i]);
            float value = Float.parseFloat(valueArray[i]);
            if (panelNitSize > 0) {
                if (((int) value) < panelNitSize) {
                    value = this.mPanelBrightnessTable.get(Integer.valueOf((int) value))[2];
                } else {
                    value = this.mPanelBrightnessTable.get(Integer.valueOf(panelNitSize - 1))[2];
                }
            }
            map.put(Integer.valueOf(i), new Float[]{Float.valueOf(key), Float.valueOf(value)});
        }
        return true;
    }

    public void ensureDefaultCalibration() {
        if (!this.mLuxTable.isEmpty() && !this.mPanelBrightnessTable.isEmpty()) {
            return;
        }
        AmbienceLog.i(TAG, "Loading canonical ColorOS table 30 calibration fallback");
        if (this.mLuxTable.isEmpty()) {
            float[] luxPoints = new float[]{
                0f, 2f, 4f, 6f, 8f, 10f, 15f, 20f, 25f, 30f, 40f, 60f, 95f, 100f, 200f, 300f, 400f, 500f,
                600f, 700f, 800f, 900f, 1000f, 1100f, 1200f, 1300f, 1400f, 1500f, 1600f, 1700f, 1800f,
                1900f, 2000f, 2500f, 3000f, 3500f, 4000f, 5000f, 6000f, 10000f
            };
            float[] nitPoints = new float[]{
                2.5f, 7.5f, 13f, 18f, 25f, 30f, 38f, 45f, 50f, 55f, 60f, 70f, 79f, 82f, 90f, 98f, 106f, 113f,
                121f, 129f, 137f, 144f, 152f, 160f, 168f, 177f, 185f, 194f, 203f, 212f, 222f, 232f, 243f,
                280f, 317f, 353f, 391f, 408f, 435f, 500f
            };
            for (int i = 0; i < luxPoints.length; i++) {
                this.mLuxTable.put(i, luxPoints[i]);
                this.mNitTable.put(i, nitPoints[i]);
            }
            this.mLuxTableMode = 30;
        }
        if (this.mHbmDateTable.isEmpty()) {
            this.mHbmDateTable.put(0, new Float[]{10000f, 5000f, 2000f, 10f});
            this.mHbmDateTable.put(1, new Float[]{30000f, 10000f, 2000f, 10f});
            this.mHbmDateTable.put(2, new Float[]{50000f, 30000f, 2000f, 10f});
            this.mHbmDateTable.put(3, new Float[]{80000f, 50000f, 2000f, 10f});
            this.mHbmDateTable.put(4, new Float[]{100000f, 80000f, 2000f, 10f});
            this.mHbmDateTable.put(5, new Float[]{130000f, 90000f, 2000f, 10f});
            this.mHbmLuxTableMode = 30;
        }
        if (this.mPanelBrightnessTable.isEmpty()) {
            this.mMinLevel = 2;
            this.mMaxLevel = 4095;
            for (int level = 0; level <= 4095; level++) {
                float brightness = level;
                float backlight = level;
                float nit = (level <= 2) ? 2.03f : (2.03f + (500.0f - 2.03f) * (float) Math.pow((level - 2.0f) / 4093.0f, 2.2f));
                this.mPanelBrightnessTable.put(level, new float[]{brightness, backlight, nit, 1.0f});
            }
        }
    }
}
