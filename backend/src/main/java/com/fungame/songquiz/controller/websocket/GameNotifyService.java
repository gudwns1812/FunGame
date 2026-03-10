package com.fungame.songquiz.controller.websocket;

import com.fungame.songquiz.domain.PlayerScore;
import com.fungame.songquiz.domain.dto.GameInfo;
import com.fungame.songquiz.domain.event.GameEndEvent;
import com.fungame.songquiz.domain.event.GameResultEvent;
import com.fungame.songquiz.domain.event.GameSkipEvent;
import com.fungame.songquiz.domain.event.GameStartEvent;
import com.fungame.songquiz.domain.event.HostChangeEvent;
import com.fungame.songquiz.domain.event.PlayerJoinEvent;
import com.fungame.songquiz.domain.event.PlayerLeaveEvent;
import com.fungame.songquiz.domain.event.PlayerReadyEvent;
import com.fungame.songquiz.domain.event.RoundEndEvent;
import com.fungame.songquiz.domain.event.RoundStartEvent;
import com.fungame.songquiz.domain.event.TimerTickEvent;
import com.fungame.songquiz.domain.event.HaliGaliActionEvent;
import com.fungame.songquiz.support.response.ApiResponse;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameNotifyService {

    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void handleHaliGaliAction(HaliGaliActionEvent event) {
        log.info("Broadcasting HaliGali action: {} in room {}", event.actionType(), event.roomId());
        String destination = "/subscribe/room/" + event.roomId();
        
        Object payload = Map.of(
                "type", "HALIGALI_ACTION",
                "playerName", event.playerName(),
                "actionType", event.actionType().name(),
                "result", event.result().name(),
                "status", event.status().data()
        );
        messagingTemplate.convertAndSend(destination, ApiResponse.success(payload));
    }

    @EventListener
    public void handlePlayerJoin(PlayerJoinEvent event) {
        log.info("Broadcasting player join: {} in room {}", event.playerName(), event.roomId());
        String destination = "/subscribe/room/" + event.roomId();
        Object payload = Map.of("type", "PLAYER_JOIN", "player", event.playerName());
        messagingTemplate.convertAndSend(destination, ApiResponse.success(payload));
    }

    @EventListener
    public void handlePlayerLeave(PlayerLeaveEvent event) {
        log.info("Broadcasting player leave: {} in room {}", event.playerName(), event.roomId());
        String destination = "/subscribe/room/" + event.roomId();
        Object payload = Map.of("type", "PLAYER_LEAVE", "player", event.playerName());
        messagingTemplate.convertAndSend(destination, ApiResponse.success(payload));
    }

    @EventListener
    public void handleHostChange(HostChangeEvent event) {
        log.info("Broadcasting host change: new host {} in room {}", event.newHost(), event.roomId());
        String destination = "/subscribe/room/" + event.roomId();
        Object payload = Map.of("type", "HOST_CHANGE", "newHost", event.newHost());
        messagingTemplate.convertAndSend(destination, ApiResponse.success(payload));
    }

    @EventListener
    public void handlePlayerReady(PlayerReadyEvent event) {
        log.info("Broadcasting player ready: player {} is now {} in room {}", event.player(), event.ready(), event.roomId());
        String destination = "/subscribe/room/" + event.roomId();
        Object payload = Map.of(
                "type", "PLAYER_READY", 
                "player", event.player(), 
                "ready", event.ready(), 
                "isAllReady", event.isAllReady()
        );
        messagingTemplate.convertAndSend(destination, ApiResponse.success(payload));
    }

    @EventListener
    public void handleGameStart(GameStartEvent event) {
        log.info("Broadcasting game start in room {}", event.roomId());
        String destination = "/subscribe/room/" + event.roomId();
        GameInfo gameInfo = event.gameInfo();

        String message = gameInfo.category();
        if (message == null) {
            message = "";
        }

        Object payload = Map.of(
                "type", "GAME_START",
                "gameType", gameInfo.gameType(),
                "category", message,
                "songCount", gameInfo.totalCount(),
                "message", "채팅에 정답을 입력하면 됩니다. 띄어쓰기 없이 입력해주시고 영어이름은 소문자로 입력해주세요. 게임이 5초 뒤 시작됩니다."
        );
        messagingTemplate.convertAndSend(destination, ApiResponse.success(payload));
    }

    @Async
    @EventListener
    public void handleRoundStart(RoundStartEvent event) {
        log.info("Broadcasting round start in room {}", event.roomId());
        String destination = "/subscribe/room/" + event.roomId();
        Object payload = Map.of(
                "type", "ROUND_START",
                "round", event.currentRound(),
                "totalRound", event.totalRound(),
                "content", event.content().toString()
        );
        messagingTemplate.convertAndSend(destination, ApiResponse.success(payload));
    }

    @EventListener
    public void handleGameSkip(GameSkipEvent event) {
        log.info("Broadcasting round skip in room {}", event.roomId());
        String destination = "/subscribe/room/" + event.roomId();
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
        String destination = "/subscribe/room/" + event.roomId();

        String winnerName = (event.winner() != null) ? event.winner() : "없음";

        Object payload = Map.of(
                "type", "ROUND_END",
                "answer", event.answer().toString(),
                "winner", winnerName
        );
        messagingTemplate.convertAndSend(destination, ApiResponse.success(payload));
    }

    @Async
    @EventListener
    public void handleTimerTicker(TimerTickEvent event) {
        String destination = "/subscribe/room/" + event.roomId();
        Object payload = Map.of(
                "type", "TIMER_TICK",
                "remainingSeconds", event.remainingSeconds()
        );
        messagingTemplate.convertAndSend(destination, ApiResponse.success(payload));
    }

    @EventListener
    public void handleGameResult(GameResultEvent event) {
        log.info("Broadcasting game end in room {}", event.roomId());
        String destination = "/subscribe/room/" + event.roomId();

        StringBuilder builder = new StringBuilder();

        List<PlayerScore> rankings = event.rankings();
        rankings.forEach(score -> builder.append(score.player()).append(":").append(score.score()).append("\n"));

        Object payload = Map.of(
                "type", "GAME_RESULT",
                "rankings", builder.toString(),
                "message", "5초 뒤 게임이 종료됩니다."
        );
        messagingTemplate.convertAndSend(destination, ApiResponse.success(payload));
    }

    @EventListener
    public void handleGameEnd(GameEndEvent event) {
        String destination = "/subscribe/room/" + event.roomId();

        Object payload = Map.of(
                "type", "GAME_END"
        );
        messagingTemplate.convertAndSend(destination, ApiResponse.success(payload));
    }
}
