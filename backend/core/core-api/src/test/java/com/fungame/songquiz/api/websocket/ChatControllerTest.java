package com.fungame.songquiz.api.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fungame.songquiz.api.controller.request.ChatRequest;
import com.fungame.songquiz.api.controller.request.GameActionRequest;
import com.fungame.songquiz.domain.member.Member;
import com.fungame.songquiz.domain.member.MemberAdapter;
import com.fungame.songquiz.domain.room.ChatMessageEvent;
import com.fungame.songquiz.domain.room.GameRoomManager;
import com.fungame.songquiz.domain.session.GameAction;
import com.fungame.songquiz.domain.session.GameService;
import com.fungame.songquiz.enums.ActionType;
import com.fungame.songquiz.support.MemberFixture;
import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.error.ErrorType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

class ChatControllerTest {

    private static final Long ROOM_ID = 7L;
    private static final Long MEMBER_ID = 1L;

    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final GameRoomManager gameRoomManager = mock(GameRoomManager.class);
    private final GameService gameService = mock(GameService.class);
    private final ChatController chatController =
            new ChatController(eventPublisher, gameRoomManager, gameService);
    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();
    private Level originalLevel;

    @BeforeEach
    void listenToLogs() {
        originalLevel = controllerLogger().getLevel();
        controllerLogger().setLevel(Level.DEBUG);
        logs.start();
        controllerLogger().addAppender(logs);
    }

    @AfterEach
    void stopListening() {
        controllerLogger().detachAppender(logs);
        controllerLogger().setLevel(originalLevel);
        logs.stop();
    }

    private static Logger controllerLogger() {
        return (Logger) LoggerFactory.getLogger(ChatController.class);
    }

    private static MemberAdapter principal() {
        Member member = MemberFixture.withId(MEMBER_ID, "반달");

        return new MemberAdapter(member);
    }

    @Test
    @DisplayName("내 말이 화면에 뜨고 나서 정답 판정이 돈다. 순서가 뒤집히면 정답 안내가 내 말보다 먼저 보인다.")
    void theEchoGoesOutBeforeTheAnswerIsJudged() {
        chatController.chat(ROOM_ID, principal(), new ChatRequest("정답"));

        InOrder inOrder = inOrder(eventPublisher, gameService);
        inOrder.verify(eventPublisher).publishEvent(new ChatMessageEvent(ROOM_ID, MEMBER_ID, "정답"));
        inOrder.verify(gameService).processAnswer(ROOM_ID, MEMBER_ID, "정답");
    }

    @Test
    @DisplayName("게임이 없는 방에서도 내 말은 뜬다.")
    void theEchoSurvivesARoomWithoutAGame() {
        willThrow(new CoreException(ErrorType.GAME_ROOM_NOT_FOUND))
                .given(gameRoomManager).touch(ROOM_ID);

        chatController.chat(ROOM_ID, principal(), new ChatRequest("안녕"));

        verify(eventPublisher).publishEvent(eq(new ChatMessageEvent(ROOM_ID, MEMBER_ID, "안녕")));
        verify(gameService, never()).processAnswer(any(), any(), any());
    }

    @Test
    @DisplayName("방이 막 사라져 처리하지 못한 채팅은 흔한 경합이라 DEBUG 로만 남긴다.")
    void a_chat_for_a_vanished_room_is_logged_at_debug() {
        willThrow(new CoreException(ErrorType.GAME_ROOM_NOT_FOUND))
                .given(gameRoomManager).touch(ROOM_ID);

        chatController.chat(ROOM_ID, principal(), new ChatRequest("안녕"));

        assertThat(logs.list).singleElement()
                .extracting(ILoggingEvent::getLevel).isEqualTo(Level.DEBUG);
    }

    @Test
    @DisplayName("처리하지 못한 행동은 오류 종류가 정한 레벨로 남긴다. 방이 없는 것과 섞어 묻지 않는다.")
    void a_rejected_action_is_logged_at_the_error_type_level() {
        willThrow(new CoreException(ErrorType.INVALID_INPUT_VALUE))
                .given(gameService).handleAction(eq(ROOM_ID), any(GameAction.class));

        chatController.handleAction(ROOM_ID, principal(), new GameActionRequest(ActionType.SUBMIT_ANSWER, "가나"));

        assertThat(logs.list).singleElement()
                .extracting(ILoggingEvent::getLevel).isEqualTo(Level.WARN);
    }
}
