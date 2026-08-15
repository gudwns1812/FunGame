package com.fungame.songquiz.support.lock;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

@Component
public class LockContext {

    private final Map<Long, ReentrantLock> locks = new ConcurrentHashMap<>();

    public void processWithLockKey(Long lockKey, Runnable runnable) {
        ReentrantLock lock = locks.computeIfAbsent(lockKey, k -> new ReentrantLock());
        lock.lock();

        try {
            runnable.run();
        } finally {
            lock.unlock();
        }
    }

    public <T> T processWithLockKey(Long lockKey, Supplier<T> supplier) {
        ReentrantLock lock = locks.computeIfAbsent(lockKey, k -> new ReentrantLock());
        lock.lock();

        try {
            return supplier.get();
        } finally {
            lock.unlock();
        }
    }

    public void createLockWithLockKey(Long roomId) {
        locks.put(roomId, new ReentrantLock());
    }

    public void deleteLock(Long roomId) {
        locks.remove(roomId);
    }
}
