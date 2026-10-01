package com.fungame.songquiz.domain.room;

import java.util.function.Supplier;

public interface RoomLock {

    <T> T processWithLockKey(Long roomId, Supplier<T> supplier);

    default void processWithLockKey(Long roomId, Runnable runnable) {
        processWithLockKey(roomId, () -> {
            runnable.run();
            return null;
        });
    }
}
