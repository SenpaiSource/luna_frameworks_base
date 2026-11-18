package com.android.server.display.util;

import java.util.concurrent.locks.ReentrantReadWriteLock;

/* JADX INFO: loaded from: /run/media/sathiya/data/scratch/deps.dex */
public class AutoReleaseReadWriteLock {
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public interface ReadOperation<T> {
        T execute();
    }

    public interface WriteOperation {
        void execute();
    }

    public class ReadLock implements AutoCloseable {
        public ReadLock() {
            AutoReleaseReadWriteLock.this.lock.readLock().lock();
        }

        @Override // java.lang.AutoCloseable
        public void close() {
            AutoReleaseReadWriteLock.this.lock.readLock().unlock();
        }
    }

    public class WriteLock implements AutoCloseable {
        public WriteLock() {
            AutoReleaseReadWriteLock.this.lock.writeLock().lock();
        }

        @Override // java.lang.AutoCloseable
        public void close() {
            AutoReleaseReadWriteLock.this.lock.writeLock().unlock();
        }
    }

    public <T> T executeReadOperation(ReadOperation<T> operation) {
        ReadLock readLock = new ReadLock();
        try {
            T tExecute = operation.execute();
            readLock.close();
            return tExecute;
        } catch (Throwable th) {
            try {
                readLock.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
            throw th;
        }
    }

    public void executeWriteOperation(WriteOperation operation) {
        WriteLock writeLock = new WriteLock();
        try {
            operation.execute();
            writeLock.close();
        } catch (Throwable th) {
            try {
                writeLock.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
            throw th;
        }
    }
}
