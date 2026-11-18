package android.common;

public interface IOplusCommonFeature {
    default OplusFeatureList.OplusIndex index() {
        return null;
    }

    default IOplusCommonFeature getDefault() {
        return null;
    }
}
