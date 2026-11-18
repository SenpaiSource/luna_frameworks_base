package com.android.server.display.config.device;

import android.os.SystemProperties;
import android.util.IndentingPrintWriter;
import android.util.Slog;
import android.util.Xml;
import com.android.internal.util.XmlUtils;
import com.android.server.display.feature.ambience.events.delegate.DeepThinkerDelegate;
import com.android.server.display.util.AutoReleaseReadWriteLock;
import com.android.server.display.util.OplusBrightnessConstants;
import com.android.server.display.util.OplusDisplayXmlParserHelper;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/* JADX INFO: loaded from: /run/media/sathiya/data/scratch/deps.dex */
public class OplusDisplayDeviceConfig {
    private static final String COMMON_DISPALY_BRIGHTNESS = "common.xml";
    private static boolean DEBUG = SystemProperties.getBoolean("persist.sys.display.config.debug", false);
    private static final String DEFAULT_DISPALY_BRIGHTNESS = "default.xml";
    private static final int DEFAULT_WINDOW_MAX_LIMIT_NIT = 1000000;
    private static final String DISPALY_BRIGHTNESS_CONFIG_PREFIX = "display_brightness_config_";
    private static final String DISPALY_BRIGHTNESS_CONFIG_PREFIX_PWM = "display_brightness_config_pwm_";
    private static final String DISPALY_BRIGHTNESS_CONFIG_PREFIX_SINGLE_PULSE = "display_brightness_config_single_pulse_";
    private static final String DISPLAY_CURVE_MAP = "display_curve_map";
    private static final int INVALID_PARAM = -1;
    private static final String SYS_DISPALY_BRIGHTNESS_CONFIG_DIR = "/data/system/";
    private static final String SYS_EXT_DISPALY_BRIGHTNESS_CONFIG_DIR = "/system_ext/etc/";
    private static final String TAG = "OplusDisplayDeviceConfig";
    private static final String TAG_DISPLAY_ID = "display_id";
    private static final String TAG_ENHANCE_NIT = "enhance_nit";
    private static final String TAG_GAME_EDR = "game_edr";
    private static final String TAG_HBM_LUX_TABLE = "hbm_lux_table";
    private static final String TAG_HDR_ENHANCE_TABLE = "hdr_enhance_method";
    private static final String TAG_HDR_FACTOR = "factor";
    private static final String TAG_LUX_TABLE = "lux_table";
    private static final String TAG_MANUAL_BRIGHTNESS_LIMIT = "manual_brightness_limit";
    private static final String TAG_ORIGIN_NIT = "origin_nit";
    private static final String TAG_RATIO = "ratio";
    private static final String TAG_WINDOW_MAX_BRIGHTNESS_LIMIT_NIT = "window_max_brightness_limit_nit";
    private static final String VND_DISPALY_BRIGHTNESS_CONFIG_DIR = "/my_product/vendor/etc/";
    private static final String[] CANDIDATE_ETC_DIRS = new String[] {
        "/my_product/vendor/etc/",
        "/vendor/etc/",
        "/odm/etc/",
        "/system_ext/etc/",
        "/system/etc/"
    };

    private static String resolveCandidateFile(String fileName) {
        for (String dir : CANDIDATE_ETC_DIRS) {
            File f = new File(dir, fileName);
            if (f.exists()) {
                return f.getAbsolutePath();
            }
        }
        return CANDIDATE_ETC_DIRS[0] + fileName;
    }
    private final AutoReleaseReadWriteLock mLock = new AutoReleaseReadWriteLock();
    private int mDisplayId = -1;
    private int mManualMaxBrightnessLimit = DEFAULT_WINDOW_MAX_LIMIT_NIT;
    private int mWindowMaxBrightnessLimitNit = DEFAULT_WINDOW_MAX_LIMIT_NIT;
    private String mVndDisplayBrightnessConfigFileName = DeepThinkerDelegate.InOutDoorImpl.Constants.EMPTY;
    private String mSysDisplayBrightnessConfigFileName = DeepThinkerDelegate.InOutDoorImpl.Constants.EMPTY;
    private String mDefaultDisplayBrightnessConfigFileName = DeepThinkerDelegate.InOutDoorImpl.Constants.EMPTY;
    private String mCommonDisplayBrightnessConfigFileName = DeepThinkerDelegate.InOutDoorImpl.Constants.EMPTY;
    private String mManufacture = "unknown";
    private String mFactoryEngineeringVersion = SystemProperties.get("ro.oplus.image.my_engineering.type", OplusBrightnessConstants.UNKNOW_CURVE);
    private String mBrightnessCurveName = SystemProperties.get("ro.display.brightness.curve.name", OplusBrightnessConstants.UNKNOW_CURVE);
    private String mBrightnessCurveProp = SystemProperties.get("ro.oplus.display.brightness.xs", OplusBrightnessConstants.UNKNOW_CURVE);
    private boolean mIsVndConfigLoaded = false;
    private long mVoltageMaxVersion = -1;
    
    public static class LowVoltageConfig {
        public void setManufacture(String m) {}
        public void loadConfig(String m) {}
        public void dump(IndentingPrintWriter ipw) {}
        public long getMaxVersion() { return 0; }
        public void cloneConfig(LowVoltageConfig other) {}
        public boolean parseContentConfig(String c, int f, long v) { return false; }
    }
    public static class DMRConfig {
        public void loadConfig(boolean b) {}
        public long getFileMaxVersion() { return 0; }
        public boolean updateRusConfig(String c, long v) { return false; }
    }
    
    private LowVoltageConfig mLowVoltageConfig = new LowVoltageConfig();
    private CurveConfig mCurveConfig = new CurveConfig();
    private DMRConfig mDMRConfig = new DMRConfig();

    public static class TagParameter {
        String rootTag = null;
        String topTag = null;
        String orgTopTag = null;
        String startTag = null;
        String endTag = null;
        String text = null;
        int tableCnt = 0;
        XmlPullParser parser = null;
    }

    public void init(String manufacture, boolean isFirstDisplay) {
        OplusDisplayXmlParserHelper xmlParserHelper;
        OplusDisplayDeviceConfigParser parser = new OplusDisplayDeviceConfigParser();
        String defaultName = DISPALY_BRIGHTNESS_CONFIG_PREFIX.concat(DEFAULT_DISPALY_BRIGHTNESS);
        String commonName = DISPALY_BRIGHTNESS_CONFIG_PREFIX.concat(COMMON_DISPALY_BRIGHTNESS);
        String defaultNamePWM = DISPALY_BRIGHTNESS_CONFIG_PREFIX_PWM.concat(DEFAULT_DISPALY_BRIGHTNESS);
        String fileNameSinglePulse = DISPALY_BRIGHTNESS_CONFIG_PREFIX_SINGLE_PULSE.concat(DEFAULT_DISPALY_BRIGHTNESS);
        this.mDefaultDisplayBrightnessConfigFileName = resolveCandidateFile(defaultName);
        this.mCommonDisplayBrightnessConfigFileName = resolveCandidateFile(commonName);
        if (manufacture == null || "unknown".equals(manufacture)) {
            String propName = SystemProperties.get("ro.display.brightness.curve.name", "");
            if (!propName.isEmpty() && !OplusBrightnessConstants.UNKNOW_CURVE.equals(propName)) {
                manufacture = propName;
            } else {
                String panelName = SystemProperties.get("ro.boot.panel_name", "");
                if (!panelName.isEmpty()) {
                    manufacture = panelName;
                }
            }
        }
        if (manufacture == null || "unknown".equals(manufacture)) {
            xmlParserHelper = new OplusDisplayXmlParserHelper(this.mDefaultDisplayBrightnessConfigFileName, null);
        } else {
            this.mManufacture = manufacture;
            String roundcorner = getDeviceRoundcorner();
            if (roundcorner != null) {
                SystemProperties.set("sys.oplus.roundcorners", roundcorner);
            }
            String deviceManufacture = manufacture.replace(' ', '_');
            String fileName = DISPALY_BRIGHTNESS_CONFIG_PREFIX.concat(deviceManufacture);
            fileNameSinglePulse = DISPALY_BRIGHTNESS_CONFIG_PREFIX_SINGLE_PULSE.concat(deviceManufacture).concat(".xml");
            defaultNamePWM = DISPALY_BRIGHTNESS_CONFIG_PREFIX_PWM.concat(deviceManufacture).concat(".xml");
            this.mVndDisplayBrightnessConfigFileName = resolveCandidateFile(fileName.concat(".xml"));
            this.mSysDisplayBrightnessConfigFileName = "/data/system/".concat(fileName).concat(".xml");
            File vndFile = new File(this.mVndDisplayBrightnessConfigFileName);
            File sysFile = new File(this.mSysDisplayBrightnessConfigFileName);
            if (!vndFile.exists() && !sysFile.exists()) {
                boolean mustUseDefault = this.mCurveConfig.mustUseDefault();
                String fileName2 = DISPLAY_CURVE_MAP.concat(".xml");
                this.mVndDisplayBrightnessConfigFileName = resolveCandidateFile(fileName2);
                this.mSysDisplayBrightnessConfigFileName = "/data/system/".concat(fileName2);
                File vndFile2 = new File(this.mVndDisplayBrightnessConfigFileName);
                File sysFile2 = new File(this.mSysDisplayBrightnessConfigFileName);
                if (mustUseDefault || (!vndFile2.exists() && !sysFile2.exists())) {
                    xmlParserHelper = new OplusDisplayXmlParserHelper(this.mDefaultDisplayBrightnessConfigFileName, null);
                } else {
                    xmlParserHelper = new OplusDisplayXmlParserHelper(this.mSysDisplayBrightnessConfigFileName, this.mVndDisplayBrightnessConfigFileName);
                }
            } else {
                xmlParserHelper = new OplusDisplayXmlParserHelper(this.mSysDisplayBrightnessConfigFileName, this.mVndDisplayBrightnessConfigFileName);
            }
        }
        xmlParserHelper.init(parser);
        String vndCurvePath = resolveCandidateFile("multimedia_display_brightness_config.xml");
        File vndCurveFile = new File(vndCurvePath);
        this.mCurveConfig.init(vndCurveFile.exists());
        OplusDisplayXmlParserHelper xmlParserHelper2 = new OplusDisplayXmlParserHelper(vndCurvePath, null);
        xmlParserHelper2.init(parser);
        long curveMaxVersion = xmlParserHelper2.getMaxVersion();
        this.mCurveConfig.setMaxVersion(curveMaxVersion);
        this.mIsVndConfigLoaded = true;
        OplusDisplayXmlParserHelper xmlParserHelper3 = new OplusDisplayXmlParserHelper(this.mCommonDisplayBrightnessConfigFileName, null);
        xmlParserHelper3.init(parser);
        File sysCurveFile = new File(OplusBrightnessConstants.Config.CURVE_LUXNIT_DATA_FILE_DIR);
        if (sysCurveFile.exists()) {
            OplusDisplayXmlParserHelper xmlParserHelper4 = new OplusDisplayXmlParserHelper(OplusBrightnessConstants.Config.CURVE_LUXNIT_DATA_FILE_DIR, null);
            xmlParserHelper4.init(parser);
            this.mCurveConfig.setMaxVersion(curveMaxVersion > xmlParserHelper4.getMaxVersion() ? curveMaxVersion : xmlParserHelper4.getMaxVersion());
        }
        OplusDisplayXmlParserHelper xmlParserHelper5 = new OplusDisplayXmlParserHelper("/data/system/".concat(defaultNamePWM), resolveCandidateFile(defaultNamePWM));
        xmlParserHelper5.init(parser);
        OplusDisplayXmlParserHelper xmlParserHelper6 = new OplusDisplayXmlParserHelper("/data/system/".concat(fileNameSinglePulse), resolveCandidateFile(fileNameSinglePulse));
        xmlParserHelper6.init(parser);
        String appListPath = resolveCandidateFile("display_brightness_app_list.xml");
        File appListFile = new File(appListPath);
        if (appListFile.exists()) {
            OplusDisplayXmlParserHelper xmlParserHelperAppList = new OplusDisplayXmlParserHelper("/data/system/display_brightness_app_list.xml", appListPath);
            xmlParserHelperAppList.init(parser);
        }
        this.mLowVoltageConfig.loadConfig(this.mManufacture);
        this.mDMRConfig.loadConfig(isFirstDisplay);
        this.mCurveConfig.ensureDefaultCalibration();
    }

    public void printerDMRConfig() {
        Slog.d(TAG, "DMRConfig Switch: " + this.mDMRConfig.toString());
    }

    
    public boolean parseContentConfig(String content, int fileType, long currentFileVersion) {
        OplusDisplayDeviceConfigParser parser = new OplusDisplayDeviceConfigParser();
        boolean ret = false;
        try {
            long rusFileVersion = parser.parseVersion(content);
            switch (fileType) {
                case 2:
                    if (rusFileVersion >= currentFileVersion && (ret = parser.parseContent(content, true))) {
                        this.mCurveConfig.setMaxVersion(rusFileVersion);
                    }
                    break;
                case 3:
                    ret = this.mLowVoltageConfig.parseContentConfig(content, fileType, currentFileVersion);
                    break;
                case 5:
                case 8:
                    ret = this.mDMRConfig.updateRusConfig(content, currentFileVersion);
                    break;
            }
        } catch (Exception e) {
            Slog.e(TAG, "parseContentConfig error", e);
            return false;
        }
        return ret;
    }

    private class OplusDisplayDeviceConfigParser extends OplusDisplayXmlParserHelper.DisplayParser {
        private boolean mIsRusDataDir;
        private boolean mIsRusUpdate;

        private OplusDisplayDeviceConfigParser() {
            this.mIsRusDataDir = false;
            this.mIsRusUpdate = false;
        }

        /* JADX WARN: Code duplicated, block: B:69:0x0196 A[PHI: r9 r16
          0x0196: PHI (r9v8 'strReader' java.io.StringReader) = 
          (r9v5 'strReader' java.io.StringReader)
          (r9v6 'strReader' java.io.StringReader)
          (r9v7 'strReader' java.io.StringReader)
          (r9v9 'strReader' java.io.StringReader)
         binds: [B:83:0x01b5, B:68:0x0194, B:74:0x01a3, B:79:0x01ad] A[DONT_GENERATE, DONT_INLINE]
          0x0196: PHI (r16v8 boolean) = (r16v4 boolean), (r16v5 boolean), (r16v6 boolean), (r16v9 boolean) binds: [B:83:0x01b5, B:68:0x0194, B:74:0x01a3, B:79:0x01ad] A[DONT_GENERATE, DONT_INLINE]] */
        @Override // com.android.server.display.util.OplusDisplayXmlParserHelper.DisplayParser
        public boolean parseContent(String content, boolean isRusUpdte, boolean isRusDataDir) {
            boolean z = false;
            if (content == null) {
                return false;
            }
            this.mIsRusDataDir = isRusDataDir;
            this.mIsRusUpdate = isRusUpdte;
            TagParameter tag = new TagParameter();
            StringReader strReader = null;
            try {
                try {
                    XmlPullParser parser = Xml.newPullParser();
                    strReader = new StringReader(content);
                    parser.setInput(strReader);
                    tag.parser = parser;
                    int eventType = parser.getEventType();
                    while (eventType != 1) {
                        switch (eventType) {
                            case 0:
                                parser = parser;
                                z = z;
                                tag.rootTag = parser.getName();
                                tag.topTag = tag.rootTag;
                                tag.startTag = tag.rootTag;
                                tag.orgTopTag = tag.rootTag;
                                break;
                            case 1:
                            default:
                                parser = parser;
                                z = z;
                                break;
                            case 2:
                                tag.topTag = tag.startTag;
                                tag.startTag = parser.getName();
                                if (tag.rootTag == null) {
                                    tag.rootTag = tag.startTag;
                                }
                                if (tag.topTag == null) {
                                    tag.topTag = tag.startTag;
                                }
                                if (OplusDisplayDeviceConfig.TAG_GAME_EDR.equals(tag.topTag) && (OplusDisplayDeviceConfig.TAG_ORIGIN_NIT.equals(tag.startTag) || OplusDisplayDeviceConfig.TAG_ENHANCE_NIT.equals(tag.startTag))) {
                                    XmlUtils.convertValueToInt(parser.getAttributeValue(null, OplusDisplayDeviceConfig.TAG_HDR_FACTOR), -1);
                                }
                                if (OplusDisplayDeviceConfig.TAG_LUX_TABLE.equals(tag.startTag) || OplusDisplayDeviceConfig.TAG_HBM_LUX_TABLE.equals(tag.startTag)) {
                                    OplusDisplayDeviceConfig.this.mCurveConfig.init(getCurrentVersion(), this.mIsRusUpdate, this.mIsRusDataDir);
                                }
                                OplusDisplayDeviceConfig.this.mCurveConfig.parseContent(tag, OplusDisplayDeviceConfig.this.mManufacture);
                                break;
                            case 3:
                                tag.endTag = parser.getName();
                                OplusDisplayDeviceConfig.this.mCurveConfig.parseContent(tag, OplusDisplayDeviceConfig.this.mManufacture);
                                if (tag.startTag != null && tag.startTag.equals(tag.endTag)) {
                                    tag.startTag = tag.topTag;
                                    tag.topTag = tag.rootTag;
                                    parser = parser;
                                    z = z;
                                } else {
                                    parser = parser;
                                    z = z;
                                }
                                break;
                            case 4:
                                String text = parser.getText();
                                tag.text = text;
                                if (OplusDisplayDeviceConfig.TAG_DISPLAY_ID.equals(tag.startTag)) {
                                    OplusDisplayDeviceConfig.this.mDisplayId = Integer.parseInt(text.trim());
                                } else if (OplusDisplayDeviceConfig.TAG_MANUAL_BRIGHTNESS_LIMIT.equals(tag.startTag)) {
                                    OplusDisplayDeviceConfig.this.mManualMaxBrightnessLimit = Integer.parseInt(text);
                                } else if (OplusDisplayDeviceConfig.TAG_WINDOW_MAX_BRIGHTNESS_LIMIT_NIT.equals(tag.startTag)) {
                                    OplusDisplayDeviceConfig.this.mWindowMaxBrightnessLimitNit = Integer.parseInt(text);
                                }
                                OplusDisplayDeviceConfig.this.mCurveConfig.parseContent(tag, OplusDisplayDeviceConfig.this.mManufacture);
                                parser = parser;
                                z = z;
                                break;
                        }
                        try {
                            eventType = parser.next();
                            z = z;
                            parser = parser;
                        } catch (IOException e) {
                            Slog.e(OplusDisplayDeviceConfig.TAG, "OplusDisplayDeviceConfigParser parseContent ", e);
                            if (strReader != null) {
                                strReader.close();
                            }
                            return z;
                        } catch (NumberFormatException e2) {
                            Slog.e(OplusDisplayDeviceConfig.TAG, "OplusDisplayDeviceConfigParser parseContent ", e2);
                            if (strReader != null) {
                                strReader.close();
                            }
                            return z;
                        } catch (IllegalArgumentException e3) {
                            Slog.e(OplusDisplayDeviceConfig.TAG, "OplusDisplayDeviceConfigParser parseContent ", e3);
                            if (strReader != null) {
                                strReader.close();
                            }
                            return z;
                        } catch (XmlPullParserException e4) {
                            Slog.e(OplusDisplayDeviceConfig.TAG, "OplusDisplayDeviceConfigParser parseContent ", e4);
                            if (strReader != null) {
                                strReader.close();
                            }
                            return z;
                        }
                    }
                    strReader.close();
                    return true;
                } catch (Throwable th) {
                    if (strReader != null) {
                        strReader.close();
                    }
                    throw th;
                }
            } catch (Exception e) {
                Slog.e(OplusDisplayDeviceConfig.TAG, "OplusDisplayDeviceConfigParser parseContent error", e);
            }
            return false;
        }
    }

    private String getDeviceRoundcorner() {
        if (this.mManufacture.contains("TM")) {
            return "TM";
        }
        if (!this.mManufacture.contains("BOE")) {
            return null;
        }
        return "BOE";
    }

    
    public int getDisplayId() {
        return ((Integer) this.mLock.executeReadOperation(() -> this.mDisplayId)).intValue();
    }

    
    public String getManufacture() {
        return (String) this.mLock.executeReadOperation(() -> this.mManufacture);
    }

    public void setManufacture(final String manufacture) {
        this.mLock.executeWriteOperation(() -> {
            this.mManufacture = manufacture;
            this.mLowVoltageConfig.setManufacture(this.mManufacture);
        });
    }

    
    public void dump(PrintWriter pw) {
        IndentingPrintWriter ipw = new IndentingPrintWriter(pw, "    ");
        ipw.increaseIndent();
        ipw.println("BrightnessConfig:");
        ipw.increaseIndent();
        ipw.println("displayId=" + this.mDisplayId);
        this.mCurveConfig.dump(ipw);
        this.mLowVoltageConfig.dump(ipw);
        ipw.decreaseIndent();
        ipw.decreaseIndent();
    }

    
    public int getManualMaxBrightnessLimit() {
        return ((Integer) this.mLock.executeReadOperation(() -> this.mManualMaxBrightnessLimit)).intValue();
    }

    
    public int getWindowMaxBrightnessLimitNit() {
        return ((Integer) this.mLock.executeReadOperation(() -> this.mWindowMaxBrightnessLimitNit)).intValue();
    }

    
    public void clearPanelBrightnessMap() {
        this.mLock.executeWriteOperation(() -> this.mCurveConfig.clearPanelBrightnessMap());
    }

    public long getCurrentFileVersion(int fileType) {
        switch (fileType) {
            case 2:
                long version = this.mCurveConfig.getMaxVersion();
                return version;
            case 3:
                long version2 = this.mLowVoltageConfig.getMaxVersion();
                return version2;
            case 4:
            case 6:
            case 7:
            default:
                return -1L;
            case 5:
                long version3 = this.mDMRConfig.getFileMaxVersion();
                return version3;
            case 8:
                long version4 = this.mDMRConfig.getFileMaxVersion();
                return version4;
        }
    }

    
    public CurveConfig getCurveConfig() {
        return (CurveConfig) this.mLock.executeReadOperation(() -> this.mCurveConfig);
    }

    
    public LowVoltageConfig getLowVoltageConfig() {
        return (LowVoltageConfig) this.mLock.executeReadOperation(() -> this.mLowVoltageConfig);
    }

    
    public DMRConfig getDMRConfig() {
        return (DMRConfig) this.mLock.executeReadOperation(() -> this.mDMRConfig);
    }

    private void cloneDMRFileConfigLocked(final OplusDisplayDeviceConfig config) {
        this.mLock.executeWriteOperation(() -> this.mDMRConfig = config.getDMRConfig());
    }

    
    public void cloneConfig(final OplusDisplayDeviceConfig config, final int fileType) {
        if (config == null) {
            Slog.e(TAG, "cloneConfig failed, config is null!! ");
            return;
        }
        Slog.e(TAG, "cloneConfig rus config " + config);
        this.mLock.executeWriteOperation(() -> {
            switch (fileType) {
                case 2:
                    this.mCurveConfig.cloneConfig(config.getCurveConfig());
                    break;
                case 3:
                    this.mLowVoltageConfig.cloneConfig(config.getLowVoltageConfig());
                    break;
                case 5:
                case 8:
                    cloneDMRFileConfigLocked(config);
                    break;
            }
        });
    }
}
