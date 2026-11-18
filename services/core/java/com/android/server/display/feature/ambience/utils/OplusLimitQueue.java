package com.android.server.display.feature.ambience.utils;

import android.os.SystemClock;
import com.android.server.display.feature.ambience.events.delegate.SensorDelegate;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import android.annotation.Nullable;

/* JADX INFO: loaded from: /run/media/sathiya/data/scratch/ambience.dex */
public class OplusLimitQueue<E> {
    public static final int INVALID_VALUE = -1;
    private final Object mQueueLock;
    private final QueueOperator<ObjectNode> mQueueOperator;

    public OplusLimitQueue(@Nullable Integer countLimit, @Nullable Long timeLimit) {
        this.mQueueLock = new Object();
        this.mQueueOperator = new QueueOperator<>(countLimit, timeLimit);
    }

    public OplusLimitQueue() {
        this.mQueueLock = new Object();
        this.mQueueOperator = new QueueOperator<>();
    }

    public boolean add(E e) {
        boolean zAdd;
        synchronized (this.mQueueLock) {
            zAdd = this.mQueueOperator.add(new ObjectNode(this, e));
        }
        return zAdd;
    }

    public void addFirst(E e) {
        synchronized (this.mQueueLock) {
            this.mQueueOperator.addFirst(new ObjectNode(this, e));
        }
    }

    public LinkedList<E> getLinkedList() {
        LinkedList<E> list;
        synchronized (this.mQueueLock) {
            list = new LinkedList<>();
            ListIterator<ObjectNode> listIterator = this.mQueueOperator.listIterator(0);
            while (listIterator.hasNext()) {
                list.add(listIterator.next().getObject());
            }
        }
        return list;
    }

    public E getFirst() {
        synchronized (this.mQueueLock) {
            if (this.mQueueOperator.isEmpty()) {
                return null;
            }
            return (E) ((ObjectNode) this.mQueueOperator.getFirst()).getObject();
        }
    }

    public long getFirstTime() {
        synchronized (this.mQueueLock) {
            if (this.mQueueOperator.isEmpty()) {
                return -1L;
            }
            return ((ObjectNode) this.mQueueOperator.getFirst()).getTime();
        }
    }

    public E getLast() {
        synchronized (this.mQueueLock) {
            if (this.mQueueOperator.isEmpty()) {
                return null;
            }
            return (E) ((ObjectNode) this.mQueueOperator.getLast()).getObject();
        }
    }

    public long getLastTime() {
        synchronized (this.mQueueLock) {
            if (this.mQueueOperator.isEmpty()) {
                return -1L;
            }
            return ((ObjectNode) this.mQueueOperator.getLast()).getTime();
        }
    }

    public E remove() {
        synchronized (this.mQueueLock) {
            if (this.mQueueOperator.isEmpty()) {
                return null;
            }
            return (E) ((ObjectNode) this.mQueueOperator.remove()).getObject();
        }
    }

    public E removeLast() {
        synchronized (this.mQueueLock) {
            if (this.mQueueOperator.isEmpty()) {
                return null;
            }
            return (E) ((ObjectNode) this.mQueueOperator.removeLast()).getObject();
        }
    }

    public int size() {
        int size;
        synchronized (this.mQueueLock) {
            size = this.mQueueOperator.size();
        }
        return size;
    }

    public boolean isEmpty() {
        boolean zIsEmpty;
        synchronized (this.mQueueLock) {
            zIsEmpty = this.mQueueOperator.isEmpty();
        }
        return zIsEmpty;
    }

    public void clear() {
        synchronized (this.mQueueLock) {
            this.mQueueOperator.clear();
        }
    }

    public E get(int i) {
        synchronized (this.mQueueLock) {
            if (i >= 0) {
                if (i < this.mQueueOperator.size()) {
                    return (E) ((ObjectNode) this.mQueueOperator.get(i)).getObject();
                }
            }
            return null;
        }
    }

    public long getTime(int i) {
        synchronized (this.mQueueLock) {
            if (i >= 0) {
                if (i < this.mQueueOperator.size()) {
                    return ((ObjectNode) this.mQueueOperator.get(i)).getTime();
                }
            }
            return -1L;
        }
    }

    public int getCountLimit() {
        int countLimit;
        synchronized (this.mQueueLock) {
            countLimit = this.mQueueOperator.getCountLimit();
        }
        return countLimit;
    }

    public long getTimeLimit() {
        long timeLimit;
        synchronized (this.mQueueLock) {
            timeLimit = this.mQueueOperator.getTimeLimit();
        }
        return timeLimit;
    }

    public LinkedList<E> getLinkedListByPastTime(long pastTime, boolean isFull) {
        LinkedList<E> linkedListByPastTimeLocked;
        synchronized (this.mQueueLock) {
            linkedListByPastTimeLocked = getLinkedListByPastTimeLocked(pastTime, isFull);
        }
        return linkedListByPastTimeLocked;
    }

    public LinkedList<E> getLinkedListByPastTime(long pastTime) {
        LinkedList<E> linkedListByPastTimeLocked;
        synchronized (this.mQueueLock) {
            linkedListByPastTimeLocked = getLinkedListByPastTimeLocked(pastTime, false);
        }
        return linkedListByPastTimeLocked;
    }

    public LinkedList<E> getLinkedListByCount(int count) {
        LinkedList<E> linkedListByCountLocked;
        synchronized (this.mQueueLock) {
            linkedListByCountLocked = getLinkedListByCountLocked(count);
        }
        return linkedListByCountLocked;
    }

    private LinkedList<E> getLinkedListByPastTimeLocked(long j, boolean z) {
        long systemTime = getSystemTime() - j;
        LinkedList<E> queueOperator = new LinkedList<>();
        if (this.mQueueOperator.size() > 0 && systemTime > ((ObjectNode) this.mQueueOperator.getLast()).getTime()) {
            return queueOperator;
        }
        Iterator<ObjectNode> itDescendingIterator = this.mQueueOperator.descendingIterator();
        while (itDescendingIterator.hasNext()) {
            ObjectNode objectNode = itDescendingIterator.next();
            if (objectNode.getTime() > systemTime) {
                queueOperator.addFirst(objectNode.getObject());
            } else {
                if (!z) {
                    break;
                }
                queueOperator.addFirst(objectNode.getObject());
                break;
            }
        }
        return queueOperator;
    }

    private LinkedList<E> getLinkedListByCountLocked(int i) {
        LinkedList<E> queueOperator = new LinkedList<>();
        ListIterator<ObjectNode> listIterator = this.mQueueOperator.listIterator(Math.max(this.mQueueOperator.size() - i, 0));
        while (listIterator.hasNext()) {
            queueOperator.add(listIterator.next().getObject());
        }
        return queueOperator;
    }

    public LinkedList<E> getLinkedListByPastTimeBasedOnCount(long j, int i, boolean z) {
        LinkedList<E> queueOperator;
        synchronized (this.mQueueLock) {
            queueOperator = new LinkedList<>();
            int size = this.mQueueOperator.size() - i;
            long systemTime = getSystemTime() - j;
            ListIterator<ObjectNode> listIterator = this.mQueueOperator.listIterator(Math.max(this.mQueueOperator.size() - i, 0));
            if (z) {
                if (size >= 0) {
                    while (listIterator.hasNext()) {
                        ObjectNode objectNode = listIterator.next();
                        if (objectNode.getTime() >= systemTime) {
                            queueOperator.add(objectNode.getObject());
                        } else {
                            queueOperator.clear();
                            break;
                        }
                    }
                }
            } else {
                while (listIterator.hasNext()) {
                    ObjectNode objectNode2 = listIterator.next();
                    if (objectNode2.getTime() >= systemTime) {
                        queueOperator.add(objectNode2.getObject());
                    }
                }
            }
        }
        return queueOperator;
    }

    public LinkedList<E> getLinkedListByCountBasedOnPastTime(long pastTime, int count, boolean isStrongRequest) {
        LinkedList<E> queue;
        synchronized (this.mQueueLock) {
            queue = getLinkedListByPastTimeLocked(pastTime, false);
            if (isStrongRequest) {
                if (queue.size() < count) {
                    queue.clear();
                }
            } else {
                while (queue.size() > count) {
                    queue.remove();
                }
            }
        }
        return queue;
    }

    public LinkedList<E> getLinkedListByCountAndPastTime(long j, int i) {
        synchronized (this.mQueueLock) {
            int size = this.mQueueOperator.size() - i;
            long systemTime = getSystemTime() - j;
            if (size < 0 || this.mQueueOperator.size() <= 0) {
                return new LinkedList<>();
            }
            if (((ObjectNode) this.mQueueOperator.get(size)).getTime() > systemTime) {
                return getLinkedListByPastTimeLocked(j, false);
            }
            return getLinkedListByCountLocked(i);
        }
    }

    public static double getSum(LinkedList<Float> list) {
        if (list == null || list.size() == 0) {
            return -1.0d;
        }
        float sum = SensorDelegate.DEFAULT_PROXIMITY_NEAR_CHECK_VALUE;
        ListIterator<Float> iterator = list.listIterator(0);
        while (iterator.hasNext()) {
            sum += iterator.next().floatValue();
        }
        return sum;
    }

    public static double getAverage(LinkedList<Float> list) {
        if (list == null || list.size() == 0) {
            return -1.0d;
        }
        return getSum(list) / ((double) list.size());
    }

    public static double getStandardDeviationByExtremum(float min, float max) {
        LinkedList<Float> list = new LinkedList<>();
        list.add(Float.valueOf(min));
        list.add(Float.valueOf(max));
        return getStandardDeviation(list);
    }

    public static double getStandardDeviation(LinkedList<Float> list) {
        if (list == null || list.size() == 0) {
            return -1.0d;
        }
        return Math.sqrt(getVariance(list));
    }

    public static double getVariance(LinkedList<Float> list) {
        if (list == null || list.size() == 0) {
            return -1.0d;
        }
        double average = getAverage(list);
        double sum = 0.0d;
        ListIterator<Float> iterator = list.listIterator(0);
        while (iterator.hasNext()) {
            sum += Math.pow(((double) iterator.next().floatValue()) - average, 2.0d);
        }
        return sum / ((double) list.size());
    }

    public static int getNotMonotonousCount(LinkedList<Float> list) {
        if (list == null || list.size() < 3) {
            return -1;
        }
        int count = 0;
        int lastScore = 0;
        float lastValue = -1.0f;
        boolean start = true;
        ListIterator<Float> iterator = list.listIterator(0);
        while (iterator.hasNext()) {
            float f = iterator.next().floatValue();
            if (!start) {
                int score = Float.compare(f, lastValue);
                if (score != 0 && lastScore != score) {
                    count++;
                    lastScore = score;
                }
            } else {
                start = false;
            }
            lastValue = f;
        }
        return Math.max(count - 1, 0);
    }

    public static int getIncreaseCount(LinkedList<Float> list) {
        if (list == null || list.size() < 2) {
            return -1;
        }
        int count = 0;
        float lastValue = Float.NaN;
        ListIterator<Float> iterator = list.listIterator(0);
        while (iterator.hasNext()) {
            float f = iterator.next().floatValue();
            if (!Float.isNaN(lastValue) && f > lastValue) {
                count++;
            }
            lastValue = f;
        }
        return count;
    }

    public static int getDecreaseCount(LinkedList<Float> list) {
        if (list == null || list.size() < 2) {
            return -1;
        }
        int count = 0;
        float lastValue = Float.NaN;
        ListIterator<Float> iterator = list.listIterator(0);
        while (iterator.hasNext()) {
            float f = iterator.next().floatValue();
            if (!Float.isNaN(lastValue) && f < lastValue) {
                count++;
            }
            lastValue = f;
        }
        return count;
    }

    public static boolean isInRange(LinkedList<Float> list, float min, float max) {
        if (list == null || list.size() == 0) {
            return false;
        }
        ListIterator<Float> iterator = list.listIterator(0);
        while (iterator.hasNext()) {
            float f = iterator.next().floatValue();
            if (!Float.isNaN(f) && (f < min || f > max)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isOutOfMaxLimit(LinkedList<Float> list, float maxLimit) {
        if (list == null || list.size() == 0) {
            return false;
        }
        ListIterator<Float> iterator = list.listIterator(0);
        while (iterator.hasNext()) {
            float f = iterator.next().floatValue();
            if (!Float.isNaN(f) && f < maxLimit) {
                return false;
            }
        }
        return true;
    }

    public static boolean isOutOfMinLimit(LinkedList<Float> list, float minLimit) {
        if (list == null || list.size() == 0) {
            return false;
        }
        ListIterator<Float> iterator = list.listIterator(0);
        while (iterator.hasNext()) {
            float f = iterator.next().floatValue();
            if (!Float.isNaN(f) && f > minLimit) {
                return false;
            }
        }
        return true;
    }

    public long getLastDataDuration() {
        synchronized (this.mQueueLock) {
            if (this.mQueueOperator.isEmpty()) {
                return this.mQueueOperator.getTimeLimit();
            }
            return getSystemTime() - ((ObjectNode) this.mQueueOperator.getLast()).getTime();
        }
    }

    public long getFirstDataDuration() {
        synchronized (this.mQueueLock) {
            if (this.mQueueOperator.isEmpty()) {
                return this.mQueueOperator.getTimeLimit();
            }
            return getSystemTime() - ((ObjectNode) this.mQueueOperator.getFirst()).getTime();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public long getSystemTime() {
        return SystemClock.elapsedRealtime();
    }

    public String toString() {
        String str;
        synchronized (this.mQueueLock) {
            str = "OplusLimitQueue:" + this.mQueueOperator;
        }
        return str;
    }

    private class QueueOperator<E> extends LinkedList<E> {
        private final int mCountLimit;
        private final long mTimeLimit;

        public QueueOperator(@Nullable Integer countLimit, Long timeLimit) {
            this.mCountLimit = countLimit == null ? -1 : countLimit.intValue();
            this.mTimeLimit = timeLimit == null ? -1L : timeLimit.longValue();
        }

        public QueueOperator() {
            this.mCountLimit = -1;
            this.mTimeLimit = -1L;
        }

        public int getCountLimit() {
            return this.mCountLimit;
        }

        public long getTimeLimit() {
            return this.mTimeLimit;
        }

        @Override // java.util.LinkedList, java.util.AbstractList, java.util.AbstractCollection, java.util.Collection, java.util.List, java.util.Deque, java.util.Queue
        public boolean add(E e) {
            super.add(e);
            if (this.mCountLimit != -1) {
                while (size() > this.mCountLimit) {
                    super.remove();
                }
            }
            if (this.mTimeLimit != -1) {
                long systemTime = OplusLimitQueue.this.getSystemTime();
                while (size() > 0 && systemTime - ((ObjectNode) getFirst()).getTime() > this.mTimeLimit) {
                    super.remove();
                }
                return true;
            }
            return true;
        }

        @Override // java.util.AbstractCollection
        public String toString() {
            return "size:" + size() + ", " + super.toString();
        }
    }

    private class ObjectNode {
        final E mObject;
        final long mTime;

        public ObjectNode(OplusLimitQueue oplusLimitQueue, E o) {
            this.mObject = o;
            this.mTime = oplusLimitQueue.getSystemTime();
        }

        public long getTime() {
            return this.mTime;
        }

        public E getObject() {
            return this.mObject;
        }

        public String toString() {
            return this.mObject.toString() + ":" + this.mTime;
        }
    }
}
