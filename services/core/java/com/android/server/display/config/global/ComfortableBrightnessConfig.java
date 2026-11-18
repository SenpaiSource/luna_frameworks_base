package com.android.server.display.config.global;

import android.util.ArrayMap;
import com.android.server.display.feature.ambience.scenes.utils.SceneConfig;
import com.android.server.display.feature.ambience.utils.AmbienceLog;
import com.oplus.content.OplusFeatureConfigManager;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;

/* JADX INFO: loaded from: /run/media/sathiya/data/scratch/deps.dex */
public class ComfortableBrightnessConfig {
    private static final int DEFAULT_CONFIG_KEY_VALUE_SIZE = 2;
    private static final int DEFAULT_MIN_LABEL_LEVEL = 3;
    private static final int DEFAULT_SCENE_SIZE = 3;
    private static final String TAG = "ComfortableBrightnessConfig";
    private static final String TAG_AllSceneLuxSmallChange = "AllSceneLuxSmallChangeSupport";
    private static final String TAG_AutoModeProximitySensor = "AutoModeProximitySensorSupport";
    private static final String TAG_DarkestMode = "DarkestModeSupport";
    private static final String TAG_DayNightMode = "DayNightModeSupport";
    private static final String TAG_ErraticSceneLargeRangeStrategyReduceNot = "ErraticSceneLargeRangeStrategyReduceNotSupport";
    private static final String TAG_ErraticSceneLuxFilter = "ErraticSceneLuxFilterSupport";
    private static final String TAG_ExtremeDragRecovery = "ExtremeDragRecoverySupport";
    private static final String TAG_INOUTDOOR_EVENT = "InOutDoorEventSupport";
    private static final String TAG_InterimMode = "InterimModeSupport";
    private static final String TAG_LightSensorAlwaysOn = "LightSensorAlwaysOnSupport";
    private static final String TAG_MotionMode = "MotionModeSupport";
    private static final String TAG_SecondaryProximitySensor = "SecondaryProximitySensorSupport";
    private static final String TAG_WeakLightSpecialShakeRange = "WeakLightSpecialShakeRangeSupport";
    private static final String XML_LABEL_APP_TAG = "app";
    private static final String XML_LABEL_FEATURES_TAG = "features";
    private static final String XML_LABEL_SCENE_TAG = "scene";
    private static final String XML_LABEL_TOP_TAG = "comfortable_brightness";
    public boolean mLightSensorAlwaysOnSupport = false;
    public boolean mAutoModeProximitySensorSupport = true;
    public boolean mSecondaryProximitySensorSupport = false;
    public boolean mInterimModeSupport = true;
    public boolean mDayNightModeSupport = true;
    public boolean mDarkestModeSupport = true;
    public boolean mMotionModeSupport = false;
    public boolean mInOutDoorEventSupport = false;
    public boolean mExtremeDragRecoverySupport = true;
    public boolean mWeakLightSpecialShakeRangeSupport = true;
    public boolean mAllSceneLuxSmallChangeSupport = false;
    public boolean mErraticSceneLuxFilterSupport = true;
    public boolean mErraticSceneLargeRangeStrategyReduceNotSupport = false;
    private boolean mForceUseXmlConfigForSensorAlwaysOn = false;
    private boolean mForceUseXmlConfigForMotionMode = false;
    private boolean mForceUseXmlConfigForProximity = false;
    private boolean mForceUseXmlConfigForSecondaryProximity = false;
    private ArrayMap<String, SceneConfig> mSceneConfig = new ArrayMap<>(3);
    private ArrayMap<String, ArrayMap<String, String>> mSceneConfigFromXml = new ArrayMap<>(3);
    private ArrayMap<String, String> mCurrentOperateScene = null;

    public void parseFeaturesConfig(LinkedList<String> labelList) {
        if (labelList.size() < 3) {
            AmbienceLog.d(TAG, "features config is wrong, label minium level is 3");
            return;
        }
        labelList.remove();
        if (labelList.size() == 2) {
            updateFeaturesKeyValueConfig(labelList.getFirst(), labelList.getLast());
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code duplicated, block: B:18:0x007a  */
    public void parseComfortableConfig(LinkedList<String> list) {
        byte b = -1;
        LinkedList<String> labelList = (LinkedList) list.clone();
        if (labelList.size() < 3) {
            return;
        }
        if (!labelList.getFirst().equals(XML_LABEL_TOP_TAG)) {
            AmbienceLog.e(TAG, "not comfortable brightness config, current top tag is " + labelList.getFirst());
            return;
        }
        AmbienceLog.d(TAG, "setConfig: " + labelList);
        labelList.remove();
        String first = labelList.getFirst();
        switch (first.hashCode()) {
            case -290659267:
                if (!first.equals(XML_LABEL_FEATURES_TAG)) {
                    b = -1;
                } else {
                    b = 0;
                }
                break;
            case 109254796:
                if (!first.equals(XML_LABEL_SCENE_TAG)) {
                    b = -1;
                } else {
                    b = 1;
                }
                break;
            default:
                b = -1;
                break;
        }
        switch (b) {
            case 0:
                parseFeaturesConfig(labelList);
                break;
            case 1:
                parseSceneConfig(labelList);
                break;
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code duplicated, block: B:5:0x000b  */
    private void updateFeaturesKeyValueConfig(String key, String value) {
        byte b = -1;
        boolean isSupport;
        try {
            switch (key.hashCode()) {
                case -1904815494:
                    b = !key.equals(TAG_ExtremeDragRecovery) ? (byte) -1 : (byte) 8;
                    break;
                case -1823322654:
                    if (key.equals(TAG_ErraticSceneLuxFilter)) {
                        b = 11;
                    }
                    break;
                case -1622922836:
                    if (key.equals(TAG_InterimMode)) {
                        b = 3;
                    }
                    break;
                case -1552290852:
                    if (key.equals(TAG_AllSceneLuxSmallChange)) {
                        b = 10;
                    }
                    break;
                case -1541968624:
                    if (key.equals(TAG_DayNightMode)) {
                        b = 4;
                    }
                    break;
                case -1537894616:
                    if (key.equals(TAG_AutoModeProximitySensor)) {
                        b = 1;
                    }
                    break;
                case -1245795594:
                    if (key.equals(TAG_MotionMode)) {
                        b = 6;
                    }
                    break;
                case -769178484:
                    if (key.equals(TAG_INOUTDOOR_EVENT)) {
                        b = 7;
                    }
                    break;
                case 377603164:
                    if (key.equals(TAG_DarkestMode)) {
                        b = 5;
                    }
                    break;
                case 748620081:
                    if (key.equals(TAG_LightSensorAlwaysOn)) {
                        b = 0;
                    }
                    break;
                case 854223594:
                    if (key.equals(TAG_SecondaryProximitySensor)) {
                        b = 2;
                    }
                    break;
                case 859668465:
                    if (key.equals(TAG_ErraticSceneLargeRangeStrategyReduceNot)) {
                        b = 12;
                    }
                    break;
                case 882261181:
                    if (key.equals(TAG_WeakLightSpecialShakeRange)) {
                        b = 9;
                    }
                    break;
                default:
                    break;
            }
            switch (b) {
                case 0:
                    isSupport = Integer.parseInt(value) != 0;
                    this.mLightSensorAlwaysOnSupport = isSupport;
                    this.mForceUseXmlConfigForSensorAlwaysOn = true;
                    break;
                case 1:
                    isSupport = Integer.parseInt(value) != 0;
                    this.mAutoModeProximitySensorSupport = isSupport;
                    this.mForceUseXmlConfigForProximity = true;
                    break;
                case 2:
                    isSupport = Integer.parseInt(value) != 0;
                    this.mSecondaryProximitySensorSupport = isSupport;
                    this.mForceUseXmlConfigForSecondaryProximity = true;
                    break;
                case 3:
                    isSupport = Integer.parseInt(value) != 0;
                    this.mInterimModeSupport = isSupport;
                    break;
                case 4:
                    isSupport = Integer.parseInt(value) != 0;
                    this.mDayNightModeSupport = isSupport;
                    break;
                case 5:
                    isSupport = Integer.parseInt(value) != 0;
                    this.mDarkestModeSupport = isSupport;
                    break;
                case 6:
                    isSupport = Integer.parseInt(value) != 0;
                    this.mMotionModeSupport = isSupport;
                    this.mForceUseXmlConfigForMotionMode = true;
                    break;
                case 7:
                    isSupport = Integer.parseInt(value) != 0;
                    this.mInOutDoorEventSupport = isSupport;
                    break;
                case 8:
                    isSupport = Integer.parseInt(value) != 0;
                    this.mExtremeDragRecoverySupport = isSupport;
                    break;
                case 9:
                    isSupport = Integer.parseInt(value) != 0;
                    this.mWeakLightSpecialShakeRangeSupport = isSupport;
                    break;
                case 10:
                    isSupport = Integer.parseInt(value) != 0;
                    this.mAllSceneLuxSmallChangeSupport = isSupport;
                    break;
                case 11:
                    isSupport = Integer.parseInt(value) != 0;
                    this.mErraticSceneLuxFilterSupport = isSupport;
                    break;
                case 12:
                    isSupport = Integer.parseInt(value) != 0;
                    this.mErraticSceneLargeRangeStrategyReduceNotSupport = isSupport;
                    break;
                default:
                    AmbienceLog.e(TAG, "undefine parameter:" + key + "=" + value);
                    break;
            }
            AmbienceLog.f(TAG, key + "=" + value);
        } catch (NumberFormatException e) {
            AmbienceLog.e(TAG, "[NumberFormatException]feature updateFeaturesKeyValueConfig fail. key:" + key + " value:" + value);
        }
    }

    public void updateConfigForOldProject() {
        boolean autoModeProxDisable = false;
        boolean secondaryProxEnable = false;
        try {
            autoModeProxDisable = OplusFeatureConfigManager.getInstance().hasFeature("oplus.software.display.automatic_brightness_proximity_disable");
            secondaryProxEnable = OplusFeatureConfigManager.getInstance().hasFeature("oplus.software.display.secondary_proximity_enable");
        } catch (Throwable t) {
            AmbienceLog.e(TAG, "Failed to query OplusFeatureConfigManager: " + t);
        }
        if (!this.mForceUseXmlConfigForProximity) {
            this.mAutoModeProximitySensorSupport = !autoModeProxDisable;
        }
        if (!this.mForceUseXmlConfigForSecondaryProximity) {
            this.mSecondaryProximitySensorSupport = secondaryProxEnable;
        }
        if (!this.mForceUseXmlConfigForSensorAlwaysOn) {
            this.mLightSensorAlwaysOnSupport = OplusDisplayGlobalConfig.getInstance().isLightSensorAlwaysOnSupport();
        }
        if (!this.mForceUseXmlConfigForMotionMode) {
            this.mMotionModeSupport = OplusDisplayGlobalConfig.getInstance().isAiEventMinimumBrightnessControlSupport();
        }
        AmbienceLog.f(TAG, "updateConfigForOldProject: mAutoModeProximitySensorSupport=" + this.mAutoModeProximitySensorSupport + " mSecondaryProximitySensorSupport=" + this.mSecondaryProximitySensorSupport + " mLightSensorAlwaysOnSupport=" + this.mLightSensorAlwaysOnSupport + " mMotionModeSupport=" + this.mMotionModeSupport);
    }

    public void parseSceneConfig(LinkedList<String> labelList) {
        if (labelList.size() < 3) {
            AmbienceLog.d(TAG, "scene config is wrong, label minium level is 3");
            return;
        }
        labelList.remove();
        if (labelList.size() == 2) {
            updateSceneKeyValueConfig(labelList.getFirst(), labelList.getLast());
        } else {
            parseSubSceneConfig(labelList);
        }
    }

    private void updateSceneKeyValueConfig(String key, String value) {
        if (this.mCurrentOperateScene != null) {
            this.mCurrentOperateScene.put(key, value);
            AmbienceLog.f(TAG, key + "=" + value);
        }
        this.mCurrentOperateScene = null;
    }

    private void parseSubSceneConfig(LinkedList<String> labelList) {
        byte b = -1;
        String firstTag = labelList.getFirst();
        ArrayMap<String, ArrayMap<String, String>> config = this.mSceneConfigFromXml;
        switch (firstTag.hashCode()) {
            case 96801:
                if (firstTag.equals("app")) {
                    b = 0;
                    break;
                }
            default:
                b = -1;
                break;
        }
        switch (b) {
            case 0:
                break;
            default:
                if (!config.containsKey(firstTag)) {
                    config.put(firstTag, new ArrayMap<>());
                }
                this.mCurrentOperateScene = config.get(firstTag);
                break;
        }
        labelList.remove();
        if (labelList.size() == 2) {
            updateSceneKeyValueConfig(labelList.getFirst(), labelList.getLast());
        } else {
            parseSubSceneConfig(labelList);
        }
    }

    public void applySceneXmlConfig(String sceneName, SceneConfig sceneConfig) {
        for (Map.Entry<String, ArrayMap<String, String>> xmlConfig : this.mSceneConfigFromXml.entrySet()) {
            if (sceneName.toLowerCase(Locale.ROOT).contains(xmlConfig.getKey().toLowerCase(Locale.ROOT))) {
                if (sceneConfig != null) {
                    ArrayMap<String, String> config = xmlConfig.getValue();
                    AmbienceLog.f(TAG, sceneName + " applyFileConfig:" + config);
                    sceneConfig.updateKeyValueConfig(config);
                    return;
                }
                return;
            }
        }
    }

    public void onParseStart() {
        this.mSceneConfigFromXml.clear();
        AmbienceLog.f(TAG, "onParseStart");
    }

    public void onParseEnd() {
        AmbienceLog.f(TAG, "onParseEnd : " + getString());
    }

    public void onBootCompleted() {
        AmbienceLog.f(TAG, "onBootCompleted.\n" + toString());
    }

    public void updateAllSence() {
        for (Map.Entry<String, SceneConfig> entry : this.mSceneConfig.entrySet()) {
            applySceneXmlConfig(entry.getKey(), entry.getValue());
        }
    }

    public void mountSceneConfig(final String sceneName, final SceneConfig sceneConfig) {
        AmbienceLog.logic(TAG, () -> oplusLambda$mountSceneConfig$0(sceneName, sceneConfig), "mount config success, name:" + sceneName, "replace config success, name:" + sceneName);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ boolean oplusLambda$mountSceneConfig$0(String sceneName, SceneConfig sceneConfig) {
        boolean isNewScene = true;
        if (this.mSceneConfig.put(sceneName, sceneConfig) != null) {
            isNewScene = false;
        }
        applySceneXmlConfig(sceneName, sceneConfig);
        return isNewScene;
    }

    private String getString() {
        StringBuilder sb = new StringBuilder(TAG);
        sb.append("Features:").append("\n");
        sb.append("mLightSensorAlwaysOnSupport:").append(this.mLightSensorAlwaysOnSupport).append(",");
        sb.append("mAutoModeProximitySensorSupport:").append(this.mAutoModeProximitySensorSupport).append(",");
        sb.append("mSecondaryProximitySensorSupport:").append(this.mSecondaryProximitySensorSupport).append(",");
        sb.append("mInterimModeSupport:").append(this.mInterimModeSupport).append(",");
        sb.append("mDayNightModeSupport:").append(this.mDayNightModeSupport).append(",");
        sb.append("mDarkestModeSupport:").append(this.mDarkestModeSupport).append(",");
        sb.append("mMotionModeSupport:").append(this.mMotionModeSupport).append(",");
        sb.append("mInOutDoorEventSupport:").append(this.mInOutDoorEventSupport).append(",");
        sb.append("mExtremeDragRecoverySupport:").append(this.mExtremeDragRecoverySupport).append(",");
        sb.append("mWeakLightSpecialShakeRangeSupport:").append(this.mWeakLightSpecialShakeRangeSupport).append(",");
        sb.append("mAllSceneLuxSmallChangeSupport:").append(this.mAllSceneLuxSmallChangeSupport);
        sb.append("mErraticSceneLuxFilterSupport:").append(this.mErraticSceneLuxFilterSupport);
        sb.append("mErraticSceneLargeRangeStrategyReduceNotSupport:").append(this.mErraticSceneLargeRangeStrategyReduceNotSupport);
        sb.append("Scenes:").append("\n");
        for (Map.Entry<String, SceneConfig> entry : this.mSceneConfig.entrySet()) {
            sb.append(entry.getKey()).append(entry.getValue().toString()).append("\n");
        }
        return sb.toString();
    }
}
