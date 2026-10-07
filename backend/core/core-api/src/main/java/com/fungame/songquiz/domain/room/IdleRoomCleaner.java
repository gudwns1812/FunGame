package com.fungame.songquiz.domain.room;

import com.fungame.songquiz.support.availability.TrafficGate;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IdleRoomCleaner {

    private final GameRoomManager gameRoomManager;
    private final TrafficGate trafficGate;

    @Scheduled(fixedDelay = 60000)
    public void cleanUpIdleRooms() {
        if (!trafficGate.isAccepting()) {
            return;
        }

        gameRoomManager.cleanupIdleRooms();
    }
}
