package android.os;

public class OplusPropertyList {
    public static final String RO_BRIGHTNESS_MODE = SystemProperties.get("ro.display.brightness.mode", "0");
    public static final String DISABLE = "0";
    public static final String ENABLE = "1";
    // Avalon (CPH2661 / OnePlus Nord 4) has an under-screen light sensor (stk36c01 / wise_rgb)
    public static final String SCREEN_UNDERLIGHTSENSOR_REGION = SystemProperties.get("ro.oplus.display.underscreenlight.region", "1");
    public static final String UNDERSCREEN_LIGHTSENSOR_SUPPORT = SystemProperties.get("ro.oplus.display.underscreenlight.support", "1");
}
