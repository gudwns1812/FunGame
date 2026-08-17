package com.fungame.songquiz.domain.room;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

@Component
public class LockContext {

    static final int STRIPE_COUNT = 64;

    private final ReentrantLock[] stripes = newStripes();

    public void processWithLockKey(Long lockKey, Runnable runnable) {
        processWithLockKey(lockKey, () -> {
            runnable.run();
            return null;
        });
    }

    public <T> T processWithLockKey(Long lockKey, Supplier<T> supplier) {
        ReentrantLock lock = lockOf(lockKey);
        lock.lock();

        try {
            return supplier.get();
        } finally {
            lock.unlock();
        }
    }

    private ReentrantLock lockOf(Long lockKey) {
        return stripes[Math.floorMod(lockKey.hashCode(), STRIPE_COUNT)];
    }

    private static ReentrantLock[] newStripes() {
        ReentrantLock[] stripes = new ReentrantLock[STRIPE_COUNT];
        Arrays.setAll(stripes, index -> new ReentrantLock());

        return stripes;
    }
}
