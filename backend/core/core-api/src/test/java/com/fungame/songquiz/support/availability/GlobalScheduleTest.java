package com.fungame.songquiz.support.availability;

import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.fungame.songquiz.domain.room.GameRoomManager;
import com.fungame.songquiz.domain.room.IdleRoomCleaner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 전역 상태를 바꾸는 예약 작업은 트래픽을 받는 동안에만 돈다.
 *
 * <p>블루-그린 배포에서 기동만 해 둔 쪽은 트래픽을 한 줄도 받지 않는데, 그래도 같은 Redis · DB 를 본다.
 * 막지 않으면 그쪽이 상대 버전의 방을 지운다.
 *
 * <p>스케줄 락이 걸린 작업은 여기서 보지 않는다. 락을 내주는 쪽이 게이트를 보므로 대기 서버는
 * 애초에 락을 쥐지 못한다({@code SchedulerLockProviderTest}).
 */
@ExtendWith(MockitoExtension.class)
class GlobalScheduleTest {

    private final TrafficGate closedGate = new TrafficGate(false, event -> { });
    private final TrafficGate openGate = new TrafficGate(true, event -> { });


    @Nested
    class 유휴_방_정리 {

        @Mock
        GameRoomManager gameRoomManager;

        @Test
        @DisplayName("트래픽을 받지 않으면 방을 정리하지 않는다. 상대 버전이 쓴 방을 지우게 된다.")
        void doesNotCleanWhileRefusingTraffic() {
            new IdleRoomCleaner(gameRoomManager, closedGate).cleanUpIdleRooms();

            then(gameRoomManager).should(never()).cleanupIdleRooms();
        }

        @Test
        @DisplayName("트래픽을 받으면 방을 정리한다.")
        void cleansWhileAcceptingTraffic() {
            new IdleRoomCleaner(gameRoomManager, openGate).cleanUpIdleRooms();

            then(gameRoomManager).should().cleanupIdleRooms();
        }
    }

}
