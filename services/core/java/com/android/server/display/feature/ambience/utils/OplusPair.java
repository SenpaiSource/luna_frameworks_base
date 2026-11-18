package com.android.server.display.feature.ambience.utils;

import android.util.Pair;

/* JADX INFO: loaded from: /run/media/sathiya/data/scratch/ambience.dex */
public class OplusPair<F, S> extends Pair<F, S> {
    public final String TAG;

    public OplusPair(String tag, F first, S second) {
        super(first, second);
        this.TAG = tag;
    }

    public OplusPair(OplusPair<F, S> pair) {
        super(pair.first, pair.second);
        this.TAG = pair.TAG;
    }

    public OplusPair(String tag, Pair<F, S> pair) {
        super(pair.first, pair.second);
        this.TAG = tag;
    }

    public String getTag() {
        return this.TAG;
    }

    public static <A, B> OplusPair<A, B> create(String tag, A a, B b) {
        return new OplusPair<>(tag, a, b);
    }

    @Override // android.util.Pair
    public String toString() {
        return this.TAG + "{" + String.valueOf(this.first) + ", " + String.valueOf(this.second) + "}";
    }
}
