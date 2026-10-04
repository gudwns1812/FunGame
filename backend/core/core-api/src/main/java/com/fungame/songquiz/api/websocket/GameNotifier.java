package com.fungame.songquiz.api.websocket;

import com.fungame.songquiz.api.controller.response.ApiResponse;
import com.fungame.songquiz.api.controller.response.RoomSettingsResponse;
import com.fungame.songquiz.api.controller.response.RoomStateResponse;
import com.fungame.songquiz.domain.member.MemberProfiles;
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
import com.fungame.songquiz.domain.session.QuizGameHintEvent;
import com.fungame.songquiz.domain.session.ResultRow;
import com.fungame.songquiz.domain.session.RoundEndEvent;
import com.fungame.songquiz.domain.session.RoundStartEvent;
import com.fungame.songquiz.support.error.CoreException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class GameNotifier {

    private final StompBroadcaster broadcaster;
    private final MemberProfiles memberProfiles;

    @EventListener
    public void handleHangmanAction(HangmanActionEvent event) {
        String destination = StompDestination.room(event.roomId());

        Object payload = Map.of(
                "type", "HANGMAN_ACTION",
                "memberId", event.memberId(),
                "letter", String.valueOf(event.letter()),
                "result", event.result().name(),
                "status", event.status().data()
        );
        broadcaster.send(destination, ApiResponse.success(payload));
    }

    @EventListener
    public void handleRoomSettingsChanged(RoomSettingsChangedEvent event) {
        log.info("Broadcasting room settings change in room {}", event.roomId());
        sendRoomState(event.roomId(), Map.of(
                "type", "ROOM_SETTINGS_CHANGED",
                "settings", RoomSettingsResponse.from(event.state(), memberProfiles)
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
        broadcaster.send(destination, ApiResponse.success(payload));
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
                "content", event.content().description(),
                "remainingMillis", event.remainingMillis()
        );
        broadcaster.send(destination, ApiResponse.success(payload));
    }

    @EventListener
    public void handleGameHint(QuizGameHintEvent event) {
        log.info("Broadcasting round hint in room {}", event.roomId());
        String destination = StompDestination.room(event.roomId());
        Object payload = Map.of(
                "type", "ROUND_HINT",
                "hint", event.hint()
        );

        broadcaster.send(destination, ApiResponse.success(payload));
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
        broadcaster.send(destination, ApiResponse.success(payload));
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
        payload.put("winnerNickname", nicknameOrNull(event.winnerMemberId()));
        broadcaster.send(destination, ApiResponse.success(payload));
    }

    @EventListener
    public void handleGameResult(GameResultEvent event) {
        log.info("Broadcasting game end in room {}", event.roomId());
        String destination = StompDestination.room(event.roomId());

        List<Map<String, Object>> rankings = event.rankings().stream()
                .map(this::toRankingPayload)
                .toList();

        Object payload = Map.of(
                "type", "GAME_RESULT",
                "rankings", rankings,
                "message", "5초 뒤 게임이 종료됩니다."
        );
        broadcaster.send(destination, ApiResponse.success(payload));
    }

    private void sendRoomState(Long roomId, Map<String, Object> payload, RoomStateInfo state) {
        Map<String, Object> withRoom = new HashMap<>(payload);
        withRoom.put("room", RoomStateResponse.from(state, memberProfiles));

        broadcaster.send(StompDestination.room(roomId), ApiResponse.success(withRoom));
    }

    private Map<String, Object> whoDidIt(String type, GamePlayer player) {
        Map<String, Object> who = new HashMap<>();
        who.put("type", type);
        who.put("memberId", player.memberId());
        who.put("nickname", nicknameOrNull(player.memberId()));
        return who;
    }

    private String nicknameOrNull(Long memberId) {
        if (memberId == null) {
            return null;
        }

        try {
            return memberProfiles.of(memberId).nickname();
        } catch (CoreException e) {
            log.info("닉네임을 찾지 못했다: member {}", memberId);
            return null;
        }
    }

    private Map<String, Object> toRankingPayload(ResultRow row) {
        Map<String, Object> ranking = new HashMap<>();
        ranking.put("memberId", row.memberId());
        ranking.put("nickname", row.label() != null ? row.label() : nicknameOrNull(row.memberId()));
        ranking.put("score", row.score());
        return ranking;
    }
}
