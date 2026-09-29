package com.fungame.songquiz.controller.config;

import com.fungame.songquiz.controller.websocket.StompSessions;
import com.fungame.songquiz.domain.member.MemberConnectionTracker;
import com.fungame.songquiz.domain.room.GameRoomService;
import com.fungame.songquiz.domain.session.GameSessionManager;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    @Bean
    public MeterBinder gameRoomMetrics(GameRoomService gameRoomService, GameSessionManager gameSessionManager) {
        return registry -> {
            Gauge.builder("fungame.rooms.active", gameRoomService, service -> service.findAllRooms().size())
                    .description("살아 있는 방 수")
                    .register(registry);

            Gauge.builder("fungame.games.in.progress", gameSessionManager, GameSessionManager::count)
                    .description("진행 중인 게임 수")
                    .register(registry);

            Gauge.builder("fungame.room.players", gameRoomService, MetricsConfig::playersInRooms)
                    .description("방 안에 있는 사람 수")
                    .register(registry);
        };
    }

    @Bean
    public MeterBinder connectionMetrics(StompSessions stompSessions, MemberConnectionTracker connectionTracker) {
        return registry -> {
            Gauge.builder("fungame.stomp.sessions", stompSessions, StompSessions::count)
                    .description("열려 있는 STOMP 세션 수")
                    .register(registry);

            Gauge.builder("fungame.members.online", connectionTracker, MemberConnectionTracker::onlineCount)
                    .description("접속 중인 회원 수. 재접속 유예 안에 있는 회원을 포함한다")
                    .register(registry);
        };
    }

    private static double playersInRooms(GameRoomService gameRoomService) {
        return gameRoomService.findAllRooms().stream()
                .mapToInt(room -> room.currentPlayers())
                .sum();
    }
}
