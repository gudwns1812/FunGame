package com.fungame.songquiz.controller.websocket;

import com.fungame.songquiz.domain.event.CorrectAnswerEvent;
import com.fungame.songquiz.domain.event.GameEndEvent;
import com.fungame.songquiz.domain.event.GameStartEvent;
import com.fungame.songquiz.domain.event.HostChangeEvent;
import com.fungame.songquiz.domain.event.PlayerJoinEvent;
import com.fungame.songquiz.domain.event.PlayerLeaveEvent;
import com.fungame.songquiz.domain.event.RoundTimeoutEvent;
import com.fungame.songquiz.domain.event.TimerTickEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameNotifyService {

    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void handlePlayerJoin(PlayerJoinEvent event) {
        log.info("Broadcasting player join: {} in room {}", event.nickname(), event.roomId());
        String destination = "/subscribe/room/" + event.roomId();
        Object payload = Map.of("type", "PLAYER_JOIN", "nickname", event.nickname());
        messagingTemplate.convertAndSend(destination, payload);
    }

    @EventListener
    public void handlePlayerLeave(PlayerLeaveEvent event) {
        log.info("Broadcasting player leave: {} in room {}", event.nickname(), event.roomId());
        String destination = "/subscribe/room/" + event.roomId();
        Object payload = Map.of("type", "PLAYER_LEAVE", "nickname", event.nickname());
        messagingTemplate.convertAndSend(destination, payload);
    }

    @EventListener
    public void handleHostChange(HostChangeEvent event) {
        log.info("Broadcasting host change: new host {} in room {}", event.newHost(), event.roomId());
        String destination = "/subscribe/room/" + event.roomId();
        Object payload = Map.of("type", "HOST_CHANGE", "newHost", event.newHost());
        messagingTemplate.convertAndSend(destination, payload);
    }

    @EventListener
    public void handleGameStart(GameStartEvent event) {
        log.info("Broadcasting game start in room {}", event.roomId());
        String destination = "/subscribe/room/" + event.roomId();
        Object payload = Map.of("type", "GAME_START", "songCount", event.songIds().size());
        messagingTemplate.convertAndSend(destination, payload);
    }

    @EventListener
    public void handleCorrectAnswer(CorrectAnswerEvent event) {
        log.info("Broadcasting correct answer: {} by {} in room {}", event.answer(), event.nickname(), event.roomId());
        String destination = "/subscribe/room/" + event.roomId();
        Object payload = Map.of(
                "type", "CORRECT_ANSWER",
                "nickname", event.nickname(),
                "answer", event.answer(),
                "score", event.score()
        );
        messagingTemplate.convertAndSend(destination, payload);
    }

    @EventListener
    public void handleRoundTimeout(RoundTimeoutEvent event) {
        log.info("Broadcasting round timeout in room {}", event.roomId());
        String destination = "/subscribe/room/" + event.roomId();
        Object payload = Map.of(
                "type", "ROUND_TIMEOUT",
                "nextSongIndex", event.nextSongIndex()
        );
        messagingTemplate.convertAndSend(destination, payload);
    }

    @EventListener
    public void handleTimerTick(TimerTickEvent event) {
        String destination = "/subscribe/room/" + event.roomId();
        Object payload = Map.of(
                "type", "TIMER_TICK",
                "remainingSeconds", event.remainingSeconds()
        );
        messagingTemplate.convertAndSend(destination, payload);
    }

    @EventListener
    public void handleGameEnd(GameEndEvent event) {
        log.info("Broadcasting game end in room {}", event.roomId());
        String destination = "/subscribe/room/" + event.roomId();
        Object payload = Map.of(
                "type", "GAME_END",
                "rankings", event.rankings()
        );
        messagingTemplate.convertAndSend(destination, payload);
    }
}
