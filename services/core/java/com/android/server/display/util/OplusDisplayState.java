package com.android.server.display.util;

import android.util.Slog;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/* JADX INFO: loaded from: /run/media/sathiya/data/scratch/deps.dex */
public class OplusDisplayState {
    private static final int ALL_INTERNAL_DISPLAY_SIZE = 2;
    private static final int INDEX_PRIMARY_DISPLAY = 0;
    private static final int INDEX_SECONDARY_DISPLAY = 1;
    public static final int INVALID_DISPLAY_ID = -1;
    private static final String TAG = "OplusDisplayState";
    private static final String UNIQUE_DISPLAYID_PREFIX = "local:";
    private AtomicInteger mCurrentEdrType;
    private Object mDisplayStateArrayListLock;
    private boolean mNeedScreenOffWhenDeviceStateClose;
    private final Object mNeedScreenOffWhenDeviceStateCloseLock;
    private ArrayList<OplusDisplayStateInfo> mOplusDisplayStateInfos;

    private OplusDisplayState() {
        this.mCurrentEdrType = new AtomicInteger(0);
        this.mNeedScreenOffWhenDeviceStateCloseLock = new Object();
        this.mNeedScreenOffWhenDeviceStateClose = false;
        this.mDisplayStateArrayListLock = new Object();
        this.mOplusDisplayStateInfos = new ArrayList<>();
        synchronized (this.mDisplayStateArrayListLock) {
            for (int i = 0; i < 2; i++) {
                this.mOplusDisplayStateInfos.add(new OplusDisplayStateInfo(this));
            }
        }
    }

    private static class OplusDisplayStateHolder {
        private static final OplusDisplayState INSTANCE = new OplusDisplayState();

        private OplusDisplayStateHolder() {
        }
    }

    public static final OplusDisplayState getInstance() {
        return OplusDisplayStateHolder.INSTANCE;
    }

    public void setLogicalDisplayId(boolean isPrimary, int displayId) {
        synchronized (this.mDisplayStateArrayListLock) {
            Slog.d(TAG, "setLogicalDisplayId = " + displayId + " isPrimary = " + isPrimary);
            this.mOplusDisplayStateInfos.get(getIndex(isPrimary)).setLogicalId(displayId);
        }
    }

    public void setPhysicalDisplayId(boolean isPrimary, long physicalDisplayId) {
        synchronized (this.mDisplayStateArrayListLock) {
            Slog.d(TAG, "setPhysicalDisplayId = " + physicalDisplayId + " isPrimary = " + isPrimary);
            this.mOplusDisplayStateInfos.get(getIndex(isPrimary)).setPhysicalId(physicalDisplayId);
        }
    }

    public int getIndexByPhysicalId(long physicalDisplayId) {
        synchronized (this.mDisplayStateArrayListLock) {
            return (this.mOplusDisplayStateInfos.get(1).isValid() && this.mOplusDisplayStateInfos.get(1).getPhysicalId() == physicalDisplayId) ? 1 : 0;
        }
    }

    public int getIndexByLogicalId(int displayId) {
        synchronized (this.mDisplayStateArrayListLock) {
            for (int i = 0; i < 2; i++) {
                if (this.mOplusDisplayStateInfos.get(i).isValid() && this.mOplusDisplayStateInfos.get(i).getLogicalId() == displayId) {
                    return i;
                }
            }
            return 0;
        }
    }

    public int getLogicalIdByIndex(int index) {
        synchronized (this.mDisplayStateArrayListLock) {
            if (index >= this.mOplusDisplayStateInfos.size()) {
                return 0;
            }
            return this.mOplusDisplayStateInfos.get(index).getLogicalId();
        }
    }

    public int getScreenOnLogicalId() {
        synchronized (this.mDisplayStateArrayListLock) {
            for (OplusDisplayStateInfo info : this.mOplusDisplayStateInfos) {
                if (info.isValid() && info.getScreenState() == 2) {
                    return info.getLogicalId();
                }
            }
            return 0;
        }
    }

    public void setEdrType(int type) {
        this.mCurrentEdrType.set(type);
    }

    public int getEdrType() {
        return this.mCurrentEdrType.get();
    }

    public boolean isScreenOn(int id) {
        synchronized (this.mDisplayStateArrayListLock) {
            for (OplusDisplayStateInfo info : this.mOplusDisplayStateInfos) {
                if (info.isValid() && (info.getScreenState() == 2 || info.getScreenState() == 6) && id == info.getLogicalId()) {
                    return true;
                }
            }
            return false;
        }
    }

    public boolean isScreenDoze(int id) {
        synchronized (this.mDisplayStateArrayListLock) {
            for (OplusDisplayStateInfo info : this.mOplusDisplayStateInfos) {
                if (info.isValid() && (info.getScreenState() == 3 || info.getScreenState() == 4) && id == info.getLogicalId()) {
                    return true;
                }
            }
            return false;
        }
    }

    public boolean isScreenOn() {
        synchronized (this.mDisplayStateArrayListLock) {
            for (OplusDisplayStateInfo info : this.mOplusDisplayStateInfos) {
                if (info.isValid() && (info.getScreenState() == 2 || info.getScreenState() == 6)) {
                    return true;
                }
            }
            return false;
        }
    }

    public boolean isScreenDoze() {
        synchronized (this.mDisplayStateArrayListLock) {
            for (OplusDisplayStateInfo info : this.mOplusDisplayStateInfos) {
                if (info.isValid() && (info.getScreenState() == 3 || info.getScreenState() == 4)) {
                    return true;
                }
            }
            return false;
        }
    }

    public boolean isPrimaryDisplay(String uniqueId) {
        boolean zEquals;
        synchronized (this.mDisplayStateArrayListLock) {
            zEquals = uniqueId.equals(UNIQUE_DISPLAYID_PREFIX + this.mOplusDisplayStateInfos.get(0).getPhysicalId());
        }
        return zEquals;
    }

    public boolean isPrimaryDisplay(int displayId) {
        synchronized (this.mDisplayStateArrayListLock) {
            for (int i = 0; i < this.mOplusDisplayStateInfos.size(); i++) {
                OplusDisplayStateInfo info = this.mOplusDisplayStateInfos.get(i);
                if (info.isValid() && info.getLogicalId() == displayId) {
                    return i == 0;
                }
            }
            return displayId == 0;
        }
    }

    public boolean isOnlySecondaryDisplayON() {
        boolean z;
        synchronized (this.mDisplayStateArrayListLock) {
            z = false;
            if ((!this.mOplusDisplayStateInfos.get(0).isValid() || this.mOplusDisplayStateInfos.get(0).getScreenState() != 2) && this.mOplusDisplayStateInfos.get(1).isValid() && this.mOplusDisplayStateInfos.get(1).getScreenState() == 2) {
                z = true;
            }
        }
        return z;
    }

    public boolean isAllScreenOff() {
        synchronized (this.mDisplayStateArrayListLock) {
            boolean hasValid = false;
            for (OplusDisplayStateInfo info : this.mOplusDisplayStateInfos) {
                if (info.isValid()) {
                    hasValid = true;
                    if (info.getScreenState() != 1) {
                        return false;
                    }
                }
            }
            return hasValid;
        }
    }

    public boolean hasScreenOn() {
        return isScreenOn();
    }

    public boolean isOnlyOneDisplayON() {
        boolean onlyOne;
        synchronized (this.mDisplayStateArrayListLock) {
            onlyOne = true;
            if (this.mOplusDisplayStateInfos.get(0).isValid() && this.mOplusDisplayStateInfos.get(1).isValid()) {
                onlyOne = (this.mOplusDisplayStateInfos.get(0).getScreenState() == 2) ^ (this.mOplusDisplayStateInfos.get(1).getScreenState() == 2);
            }
        }
        return onlyOne;
    }

    public void setScreenState(boolean isPrimary, int state) {
        synchronized (this.mDisplayStateArrayListLock) {
            Slog.d(TAG, "setScreenState = " + state + " isPrimary = " + isPrimary);
            this.mOplusDisplayStateInfos.get(getIndex(isPrimary)).setScreenState(state);
        }
    }

    public void dump(PrintWriter pw) {
        synchronized (this.mDisplayStateArrayListLock) {
            pw.println("OplusDisplayState:" + this.mOplusDisplayStateInfos.toString());
        }
    }

    public boolean getNeedScreenOffWhenDeviceStateClose() {
        boolean z;
        synchronized (this.mNeedScreenOffWhenDeviceStateCloseLock) {
            z = this.mNeedScreenOffWhenDeviceStateClose;
        }
        return z;
    }

    public void setNeedScreenOffWhenDeviceStateClose(boolean flag) {
        synchronized (this.mNeedScreenOffWhenDeviceStateCloseLock) {
            this.mNeedScreenOffWhenDeviceStateClose = flag;
        }
    }

    private int getIndex(boolean z) {
        return !z ? 1 : 0;
    }

    private class OplusDisplayStateInfo {
        private int mLogicalId;
        private long mPhysicalId;
        private int mState;

        private OplusDisplayStateInfo(OplusDisplayState oplusDisplayState) {
            this.mLogicalId = -1;
            this.mPhysicalId = 0L;
            this.mState = 0;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("OplusDisplayStateInfo:");
            sb.append(" logicalId = ").append(this.mLogicalId);
            sb.append(" physicalId = ").append(this.mPhysicalId);
            sb.append(" mState = ").append(this.mState);
            return sb.toString();
        }

        public boolean isValid() {
            return this.mLogicalId != -1;
        }

        public int getScreenState() {
            return this.mState;
        }

        public void setScreenState(int state) {
            this.mState = state;
        }

        public int getLogicalId() {
            return this.mLogicalId;
        }

        public void setLogicalId(int logicalId) {
            this.mLogicalId = logicalId;
        }

        public long getPhysicalId() {
            return this.mPhysicalId;
        }

        public void setPhysicalId(long physicalId) {
            this.mPhysicalId = physicalId;
        }
    }
}
