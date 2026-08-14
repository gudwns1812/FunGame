package com.fungame.songquiz.domain;

import com.fungame.songquiz.domain.member.MemberPresenceService;
import com.fungame.songquiz.storage.GameRoomStore;
import com.fungame.songquiz.enums.CSQuizDifficulty;
import com.fungame.songquiz.enums.Category;
import com.fungame.songquiz.enums.GameType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.mock;

@SpringJUnitConfig(GameRoomServiceTransactionBoundaryTest.TransactionalContext.class)
@DisplayName("방을 잠그는 동안에는 트랜잭션을 열어두지 않는다")
class GameRoomServiceTransactionBoundaryTest {

    private static final Long ROOM_ID = 1L;
    private static final GamePlayer HOST = GamePlayer.createNewPlayer(7L, "방장");
    private static final GamePlayer GUEST = GamePlayer.createNewPlayer(9L, "참가자");

    @Autowired
    GameRoomService gameRoomService;

    @Autowired
    GameRoomManager gameRoomManager;

    @Test
    @DisplayName("방을 만드는 동안 트랜잭션이 열려 있지 않다.")
    void createRoomHoldsNoTransaction() {
        AtomicBoolean transactionActive = new AtomicBoolean(true);
        willAnswer(record(transactionActive, null))
                .given(gameRoomManager).createGameRoom(any(), any(), any());

        gameRoomService.createRoom(settings(), HOST);

        assertThat(transactionActive).isFalse();
    }

    @Test
    @DisplayName("방에 들어가는 동안 트랜잭션이 열려 있지 않다.")
    void joinRoomHoldsNoTransaction() {
        AtomicBoolean transactionActive = new AtomicBoolean(true);
        given(gameRoomManager.joinRoom(any(), any()))
                .willAnswer(record(transactionActive, new JoinResult(1, true)));
        given(gameRoomManager.findRoom(ROOM_ID)).willReturn(GameRoom.create(settings(), HOST));

        gameRoomService.joinRoom(ROOM_ID, GUEST);

        assertThat(transactionActive).isFalse();
    }

    @Test
    @DisplayName("방에서 나가는 동안 트랜잭션이 열려 있지 않다.")
    void leaveRoomHoldsNoTransaction() {
        AtomicBoolean transactionActive = new AtomicBoolean(true);
        given(gameRoomManager.leaveRoom(any(), any()))
                .willAnswer(record(transactionActive, new LeaveResult(true, false, null)));

        gameRoomService.leaveRoom(ROOM_ID, GUEST.memberId());

        assertThat(transactionActive).isFalse();
    }

    private static Answer<Object> record(AtomicBoolean transactionActive, Object result) {
        return invocation -> {
            transactionActive.set(TransactionSynchronizationManager.isActualTransactionActive());
            return result;
        };
    }

    private static RoomSettings settings() {
        return new RoomSettings(GameType.SONG, "방 제목", 8, Category.KPOP, 10, 0, CSQuizDifficulty.HARD);
    }

    @Configuration
    @EnableTransactionManagement
    static class TransactionalContext {

        @Bean
        PlatformTransactionManager transactionManager() {
            return new TransactionManagerWithoutResource();
        }

        @Bean
        GameRoomManager gameRoomManager() {
            return mock(GameRoomManager.class);
        }

        @Bean
        GameRoomStore gameRoomStore() {
            return mock(GameRoomStore.class);
        }

        @Bean
        GameService gameService() {
            return mock(GameService.class);
        }

        @Bean
        RoomPresence roomPresence() {
            return new RoomPresence();
        }

        @Bean
        MemberPresenceService memberPresenceService() {
            return mock(MemberPresenceService.class);
        }

        @Bean
        GameRoomService gameRoomService(
                GameRoomManager gameRoomManager,
                GameRoomStore gameRoomStore,
                GameService gameService,
                RoomPresence roomPresence,
                MemberPresenceService memberPresenceService,
                ApplicationEventPublisher applicationEventPublisher) {
            return new GameRoomService(
                    gameRoomManager,
                    gameRoomStore,
                    gameService,
                    roomPresence,
                    memberPresenceService,
                    applicationEventPublisher);
        }
    }

    static class TransactionManagerWithoutResource extends AbstractPlatformTransactionManager {

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
        }
    }
}
