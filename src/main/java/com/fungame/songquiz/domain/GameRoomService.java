package com.fungame.songquiz.domain;

import com.fungame.songquiz.storage.GameRoom;
import com.fungame.songquiz.storage.GameRoomRepository;
import com.fungame.songquiz.storage.GameRoomStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GameRoomService {

    private final GameRoomRepository gameRoomRepository;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String ROOM_ID_COUNTER = "room_id_counter";

    public String createRoom(String title, int maxPlayers) {
        Long roomId = stringRedisTemplate.opsForValue().increment(ROOM_ID_COUNTER);

        GameRoom gameRoom = GameRoom.builder()
                .id(String.valueOf(roomId))
                .title(title)
                .status(GameRoomStatus.WAITING)
                .maxPlayers(maxPlayers)
                .currentPlayers(0)
                .build();

        gameRoomRepository.save(gameRoom);

        return gameRoom.getId();
    }
}
