package com.fungame.songquiz.domain.room;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;

class LockContextTest {

    private final LockContext lockContext = new LockContext();

    private ReentrantLock[] stripesOf(LockContext context) {
        return (ReentrantLock[]) ReflectionTestUtils.getField(context, "stripes");
    }

    @Test
    @DisplayName("사라진 방 번호로 계속 들어와도 보관하는 락 수가 늘지 않는다.")
    void deadRoomsDoNotAccumulateLocks() {
        int lockCountBefore = stripesOf(lockContext).length;

        LongStream.rangeClosed(1, 100_000)
                .forEach(deadRoomId -> lockContext.processWithLockKey(deadRoomId, () -> {
                }));

        assertThat(stripesOf(lockContext)).hasSize(lockCountBefore);
        assertThat(lockCountBefore).isEqualTo(LockContext.STRIPE_COUNT);
    }

    @Test
    @DisplayName("같은 방 번호의 작업은 서로 겹치지 않는다.")
    void sameRoomIsMutuallyExclusive() throws Exception {
        int threadCount = 16;
        int repeatCount = 200;
        AtomicInteger insideCriticalSection = new AtomicInteger();
        AtomicInteger overlapCount = new AtomicInteger();
        AtomicInteger totalCount = new AtomicInteger();

        runConcurrently(threadCount, () -> {
            for (int i = 0; i < repeatCount; i++) {
                lockContext.processWithLockKey(7L, () -> {
                    if (insideCriticalSection.incrementAndGet() > 1) {
                        overlapCount.incrementAndGet();
                    }
                    totalCount.incrementAndGet();
                    insideCriticalSection.decrementAndGet();
                });
            }
        });

        assertThat(overlapCount).hasValue(0);
        assertThat(totalCount).hasValue(threadCount * repeatCount);
    }

    @Test
    @DisplayName("같은 스트라이프를 쓰는 다른 방들도 서로 겹치지 않는다.")
    void roomsSharingAStripeAreMutuallyExclusive() throws Exception {
        long roomId = 3L;
        long sameStripeRoomId = roomId + LockContext.STRIPE_COUNT;
        AtomicInteger insideCriticalSection = new AtomicInteger();
        AtomicInteger overlapCount = new AtomicInteger();

        runConcurrently(8, () -> {
            for (int i = 0; i < 200; i++) {
                lockContext.processWithLockKey(i % 2 == 0 ? roomId : sameStripeRoomId, () -> {
                    if (insideCriticalSection.incrementAndGet() > 1) {
                        overlapCount.incrementAndGet();
                    }
                    insideCriticalSection.decrementAndGet();
                });
            }
        });

        assertThat(overlapCount).hasValue(0);
    }

    @Test
    @DisplayName("락을 들고 같은 방을 다시 잠가도 막히지 않는다.")
    void reentrantOnTheSameRoom() {
        String result = lockContext.processWithLockKey(7L,
                () -> lockContext.processWithLockKey(7L, () -> "안쪽까지 들어왔다"));

        assertThat(result).isEqualTo("안쪽까지 들어왔다");
    }

    @Test
    @DisplayName("방을 만들고 지우기를 반복해도 락이 방 수에 비례해 늘지 않는다.")
    void repeatedRoomLifecycleDoesNotGrowLocks() {
        LongStream.rangeClosed(1, 10_000).forEach(roomId -> {
            lockContext.processWithLockKey(roomId, () -> {
            });
            lockContext.processWithLockKey(roomId, () -> {
            });
        });

        assertThat(stripesOf(lockContext)).hasSize(LockContext.STRIPE_COUNT);
    }

    private void runConcurrently(int threadCount, Runnable work) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startTogether = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(threadCount);

        try {
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        startTogether.await();
                        work.run();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        finished.countDown();
                    }
                });
            }

            startTogether.countDown();
            assertThat(finished.await(30, TimeUnit.SECONDS)).isTrue();
        } finally {
            executor.shutdownNow();
        }
    }
}
