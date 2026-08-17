package com.fungame.songquiz.controller.websocket;

import com.fungame.songquiz.domain.quiz.QuizInfo;
import com.fungame.songquiz.domain.room.GamePlayer;
import com.fungame.songquiz.domain.room.PlayerJoinEvent;
import com.fungame.songquiz.domain.room.PlayerKickedEvent;
import com.fungame.songquiz.domain.room.PlayerLeaveEvent;
import com.fungame.songquiz.domain.room.PlayerReadyEvent;
import com.fungame.songquiz.domain.room.RoomSettingsChangedEvent;
import com.fungame.songquiz.domain.room.RoomStateInfo;
import com.fungame.songquiz.domain.session.GameResultEvent;
import com.fungame.songquiz.domain.session.GameSkipEvent;
import com.fungame.songquiz.domain.session.GameStartEvent;
import com.fungame.songquiz.domain.session.HangmanActionEvent;
import com.fungame.songquiz.domain.session.PlayerScore;
import com.fungame.songquiz.domain.session.QuizGameHintEvent;
import com.fungame.songquiz.domain.session.RoundEndEvent;
import com.fungame.songquiz.domain.session.RoundStartEvent;
import com.fungame.songquiz.domain.session.TimerTickEvent;
import com.fungame.songquiz.controller.response.ApiResponse;
import com.fungame.songquiz.controller.response.RoomSettingsResponse;
import com.fungame.songquiz.controller.response.RoomStateResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameNotifyService {

    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void handleHangmanAction(HangmanActionEvent event) {
        String destination = StompDestination.room(event.roomId());

        Object payload = Map.of(
                "type", "HANGMAN_ACTION",
                "memberId", event.memberId(),
                "nickname", event.nickname(),
                "letter", String.valueOf(event.letter()),
                "result", event.result().name(),
                "status", event.status().data()
        );
        messagingTemplate.convertAndSend(destination, ApiResponse.success(payload));
    }

    @EventListener
    public void handleRoomSettingsChanged(RoomSettingsChangedEvent event) {
        log.info("Broadcasting room settings change in room {}", event.roomId());
        sendRoomState(event.roomId(), Map.of(
                "type", "ROOM_SETTINGS_CHANGED",
                "settings", RoomSettingsResponse.from(event.state())
        ), event.state());
    }

    @EventListener
    public void handlePlayerJoin(PlayerJoinEvent event) {
        log.info("Broadcasting player join: {} in room {}", event.player().memberId(), event.roomId());
        sendRoomState(event.roomId(), whoDidIt("PLAYER_JOIN", event.player()), event.state());
    }

    @EventListener
    public void handlePlayerLeave(PlayerLeaveEvent event) {
        log.info("Broadcasting player leave: {} in room {}", event.player().memberId(), event.roomId());
        sendRoomState(event.roomId(), whoDidIt("PLAYER_LEAVE", event.player()), event.state());
    }

    @EventListener
    public void handlePlayerKicked(PlayerKickedEvent event) {
        log.info("Broadcasting player kicked: {} in room {}", event.player().memberId(), event.roomId());
        sendRoomState(event.roomId(), whoDidIt("PLAYER_KICKED", event.player()), event.state());
    }

    @EventListener
    public void handlePlayerReady(PlayerReadyEvent event) {
        log.info("Broadcasting player ready: member {} is now {} in room {}",
                event.player().memberId(), event.player().isReady(), event.roomId());
        Map<String, Object> payload = new HashMap<>(whoDidIt("PLAYER_READY", event.player()));
        payload.put("ready", event.player().isReady());
        payload.put("isAllReady", event.isAllReady());

        sendRoomState(event.roomId(), payload, event.state());
    }

    @EventListener
    public void handleGameStart(GameStartEvent event) {
        log.info("Broadcasting game start in room {}", event.roomId());
        String destination = StompDestination.room(event.roomId());
        QuizInfo quizInfo = event.quizInfo();

        String message = quizInfo.category();
        if (message == null) {
            message = "";
        }

        Object payload = Map.of(
                "type", "GAME_START",
                "gameType", quizInfo.gameType(),
                "category", message,
                "songCount", quizInfo.totalCount(),
                "message", "채팅에 정답을 입력하면 됩니다. 띄어쓰기 없이 입력해주시고 영어이름은 소문자로 입력해주세요. 게임이 5초 뒤 시작됩니다."
        );
        messagingTemplate.convertAndSend(destination, ApiResponse.success(payload));
    }

    @Async
    @EventListener
    public void handleRoundStart(RoundStartEvent event) {
        log.info("Broadcasting round start in room {}", event.roomId());
        String destination = StompDestination.room(event.roomId());
        Object payload = Map.of(
                "type", "ROUND_START",
                "round", event.currentRound(),
                "totalRound", event.totalRound(),
                "content", event.content().description()
        );
        messagingTemplate.convertAndSend(destination, ApiResponse.success(payload));
    }

    @EventListener
    public void handleGameHint(QuizGameHintEvent event) {
        log.info("Broadcasting round hint in room {}", event.roomId());
        String destination = StompDestination.room(event.roomId());
        Object payload = Map.of(
                "type", "ROUND_HINT",
                "hint", event.hint()
        );

        messagingTemplate.convertAndSend(destination, ApiResponse.success(payload));
    }

    @EventListener
    public void handleGameSkip(GameSkipEvent event) {
        log.info("Broadcasting round skip in room {}", event.roomId());
        String destination = StompDestination.room(event.roomId());
        Object payload = Map.of(
                "type", "ROUND_SKIP",
                "skipCount", event.skipCount(),
                "totalCount", event.totalCount()
        );
        messagingTemplate.convertAndSend(destination, ApiResponse.success(payload));
    }

    @Async
    @EventListener
    public void handleRoundEnd(RoundEndEvent event) {
        log.info("Broadcasting round end in room {}", event.roomId());
        String destination = StompDestination.room(event.roomId());

        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "ROUND_END");
        payload.put("answer", event.answer().answer());
        payload.put("explanation", event.answer().explanation());
        payload.put("winnerMemberId", event.winnerMemberId());
        payload.put("winnerNickname", event.winnerNickname());
        messagingTemplate.convertAndSend(destination, ApiResponse.success(payload));
    }

    @Async
    @EventListener
    public void handleTimerTicker(TimerTickEvent event) {
        String destination = StompDestination.room(event.roomId());
        Object payload = Map.of(
                "type", "TIMER_TICK",
                "remainingSeconds", event.remainingSeconds()
        );
        messagingTemplate.convertAndSend(destination, ApiResponse.success(payload));
    }

    @EventListener
    public void handleGameResult(GameResultEvent event) {
        log.info("Broadcasting game end in room {}", event.roomId());
        String destination = StompDestination.room(event.roomId());

        List<Map<String, Object>> rankings = event.rankings().stream()
                .map(GameNotifyService::toRankingPayload)
                .toList();

        Object payload = Map.of(
                "type", "GAME_RESULT",
                "rankings", rankings,
                "message", "5초 뒤 게임이 종료됩니다."
        );
        messagingTemplate.convertAndSend(destination, ApiResponse.success(payload));
    }

    /**
     * 방 상태가 바뀐 이벤트는 무엇이 바뀌었는지가 아니라 바뀐 뒤의 방 전체를 싣는다.
     * 구독 완료와 스냅샷 조회 사이에 온 이벤트가 두 번 적용돼도 깨지지 않도록,
     * 받는 쪽은 version 이 자기 것보다 낮은 payload 를 버리면 된다.
     */
    private void sendRoomState(Long roomId, Map<String, Object> payload, RoomStateInfo state) {
        Map<String, Object> withRoom = new HashMap<>(payload);
        withRoom.put("room", RoomStateResponse.from(state));

        messagingTemplate.convertAndSend(StompDestination.room(roomId), ApiResponse.success(withRoom));
    }

    private static Map<String, Object> whoDidIt(String type, GamePlayer player) {
        return Map.of("type", type, "memberId", player.memberId(), "nickname", player.nickname());
    }

    private static Map<String, Object> toRankingPayload(PlayerScore score) {
        Map<String, Object> ranking = new HashMap<>();
        ranking.put("memberId", score.memberId());
        ranking.put("nickname", score.nickname());
        ranking.put("score", score.score());
        return ranking;
    }
}
