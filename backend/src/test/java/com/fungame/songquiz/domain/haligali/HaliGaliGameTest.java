package com.fungame.songquiz.domain.haligali;

import com.fungame.songquiz.domain.*;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class HaliGaliGameTest {

    @Mock
    GameRoomManager roomManager;
    @Mock
    ApplicationEventPublisher publisher;
    @Mock
    GameTimer timer;

    GameSessionManager sessionManager;

    GameService gameService;

    @BeforeEach
    void setUp() {
        sessionManager = new GameSessionManager();
        gameService = new BoardGameService(roomManager, sessionManager, publisher, timer);
    }

    @Test
    void 할리갈리_게임_만들기_테스트() {
        //given
        Long roomId = 15L;
        String name = "테스터";
        given(roomManager.startGame(roomId,name)).willReturn(GameRoom.create("1", new HaliGaliGame(), List.of(),2,""));

        //when
        gameService.startGame(roomId, name);

        //then
        GameSession gameSession = sessionManager.getGameSession(roomId);
        Assertions.assertThat(gameSession).isNotNull();
    }
}
