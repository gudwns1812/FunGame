package com.fungame.songquiz.support.availability;

import static org.assertj.core.api.Assertions.assertThat;

import com.fungame.songquiz.domain.member.ExpiredTokenCleaner;
import com.fungame.songquiz.domain.quiz.SongScrapeScheduler;
import com.fungame.songquiz.domain.room.IdleRoomCleaner;
import com.fungame.songquiz.domain.session.GameTimerPoller;
import java.lang.reflect.Method;
import java.util.Arrays;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 인스턴스가 여러 대일 때 한 번만 돌아야 하는 예약 작업과, 오히려 나뉘어 돌아야 하는 예약 작업을 가른다.
 *
 * <p>잠가야 하는 것은 <b>바깥 세계를 건드리거나 전역 자원을 긁는</b> 작업이다. 잠그면 안 되는 것은
 * 여러 인스턴스가 나눠 가져갈수록 빨라지는 작업이다. 둘을 반대로 하면, 유튜브가 인스턴스 수만큼
 * 호출되거나 라운드 전환이 한 대 몫으로 느려진다.
 */
class SchedulerLockTest {

    @Test
    @DisplayName("바깥 세계를 긁는 작업은 한 인스턴스만 돈다.")
    void workThatTouchesTheOutsideWorldRunsOnOneInstance() {
        assertThat(lockNameOf(SongScrapeScheduler.class, "fillPendingSongs")).isNotEmpty();
        assertThat(lockNameOf(ExpiredTokenCleaner.class, "deleteExpiredTokens")).isNotEmpty();
    }

    @Test
    @DisplayName("예약 작업 가져가기는 잠그지 않는다. 나눠 가져갈수록 라운드 전환이 제시간에 온다.")
    void claimingTimersIsNotLocked() {
        assertThat(scheduledMethodOf(GameTimerPoller.class, "poll").isAnnotationPresent(SchedulerLock.class))
                .isFalse();
    }

    @Test
    @DisplayName("유휴 방 정리는 잠그지 않는다. 같은 방을 동시에 지워도 revision 비교로 한 곳만 성공한다.")
    void cleaningIdleRoomsIsNotLocked() {
        assertThat(scheduledMethodOf(IdleRoomCleaner.class, "cleanUpIdleRooms")
                .isAnnotationPresent(SchedulerLock.class))
                .isFalse();
    }

    private static String lockNameOf(Class<?> type, String methodName) {
        SchedulerLock lock = scheduledMethodOf(type, methodName).getAnnotation(SchedulerLock.class);
        assertThat(lock).as("%s.%s 에 스케줄 락이 없다", type.getSimpleName(), methodName).isNotNull();

        return lock.name();
    }

    private static Method scheduledMethodOf(Class<?> type, String methodName) {
        Method method = Arrays.stream(type.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals(methodName))
                .findFirst()
                .orElseThrow(() -> new AssertionError(type.getSimpleName() + " 에 " + methodName + " 이 없다"));

        assertThat(method.isAnnotationPresent(Scheduled.class))
                .as("%s.%s 이 예약 작업이 아니다", type.getSimpleName(), methodName)
                .isTrue();

        return method;
    }
}
