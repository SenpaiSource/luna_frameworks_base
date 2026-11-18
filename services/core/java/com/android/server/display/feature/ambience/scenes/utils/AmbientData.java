package com.android.server.display.feature.ambience.scenes.utils;

import com.android.server.display.feature.ambience.events.utils.PhoneState;

/* JADX INFO: loaded from: /run/media/sathiya/data/scratch/ambience.dex */
public class AmbientData {
    private final String TAG = "AmbientData";
    private final PhoneState mPhoneState;
    private final SceneBase mScene;

    public AmbientData(PhoneState state, SceneBase scene) {
        this.mPhoneState = state;
        this.mScene = scene;
    }

    public PhoneState getPhoneState() {
        return this.mPhoneState;
    }

    public SceneBase getScene() {
        return this.mScene;
    }

    public String toString() {
        return "AmbientData{mPhoneState=" + this.mPhoneState + ", mScene=" + this.mScene.getSceneName() + '}';
    }
}
