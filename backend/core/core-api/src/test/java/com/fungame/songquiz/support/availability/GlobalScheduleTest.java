package com.fungame.songquiz.support.availability;

import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.fungame.songquiz.domain.member.ExpiredTokenCleaner;
import com.fungame.songquiz.domain.member.PasswordResetService;
import com.fungame.songquiz.domain.quiz.SongScrapeScheduler;
import com.fungame.songquiz.domain.quiz.SongScrapeService;
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
 * 막지 않으면 그쪽이 상대 버전의 방을 지우고 예약 작업을 제 규칙으로 돌린다.
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

    @Nested
    class 노래_채우기 {

        @Mock
        SongScrapeService songScrapeService;

        @Test
        @DisplayName("트래픽을 받지 않으면 노래를 긁지 않는다. 유튜브 호출이 인스턴스 수만큼 는다.")
        void doesNotScrapeWhileRefusingTraffic() {
            new SongScrapeScheduler(songScrapeService, closedGate).fillPendingSongs();

            then(songScrapeService).should(never()).fillPendingSongs();
        }

        @Test
        @DisplayName("트래픽을 받으면 노래를 긁는다.")
        void scrapesWhileAcceptingTraffic() {
            new SongScrapeScheduler(songScrapeService, openGate).fillPendingSongs();

            then(songScrapeService).should().fillPendingSongs();
        }
    }

    @Nested
    class 만료_토큰_정리 {

        @Mock
        PasswordResetService passwordResetService;

        @Test
        @DisplayName("트래픽을 받지 않으면 만료 토큰을 지우지 않는다.")
        void doesNotDeleteWhileRefusingTraffic() {
            new ExpiredTokenCleaner(passwordResetService, closedGate).deleteExpiredTokens();

            then(passwordResetService).should(never()).deleteExpiredTokens();
        }

        @Test
        @DisplayName("트래픽을 받으면 만료 토큰을 지운다.")
        void deletesWhileAcceptingTraffic() {
            new ExpiredTokenCleaner(passwordResetService, openGate).deleteExpiredTokens();

            then(passwordResetService).should().deleteExpiredTokens();
        }
    }
}
