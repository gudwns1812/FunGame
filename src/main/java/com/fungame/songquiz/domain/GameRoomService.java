package com.fungame.songquiz.domain;

import com.fungame.songquiz.domain.dto.RoomInfo;
import com.fungame.songquiz.domain.event.HostChangeEvent;
import com.fungame.songquiz.domain.event.PlayerJoinEvent;
import com.fungame.songquiz.domain.event.PlayerLeaveEvent;
import com.fungame.songquiz.storage.GameRoom;
import com.fungame.songquiz.storage.GameRoomRepository;
import com.fungame.songquiz.storage.GameRoomStatus;
import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class GameRoomService {

    private final GameRoomRepository gameRoomRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final ApplicationEventPublisher publisher;
    private final RedissonClient redissonClient;

    private static final String ROOM_ID_COUNTER = "room_id_counter";
    private static final String ROOM_LOCK_PREFIX = "room_lock:";

    public String createRoom(String title, int maxPlayers , String hostName, Category category) {
        Long roomId = stringRedisTemplate.opsForValue().increment(ROOM_ID_COUNTER);

        GameRoom gameRoom = GameRoom.builder()
                .id(String.valueOf(roomId))
                .title(title)
                .hostName(hostName)
                .category(category)
                .playerNames(new ArrayList<>(List.of(hostName)))
                .status(GameRoomStatus.WAITING)
                .maxPlayers(maxPlayers)
                .build();

        gameRoomRepository.save(gameRoom);

        return gameRoom.getId();
    }

    public void joinRoom(String roomId, String playerName) {
        RLock lock = redissonClient.getLock(ROOM_LOCK_PREFIX + roomId);
        try {
            if (!lock.tryLock(5, 1, TimeUnit.SECONDS)) {
                throw new CoreException(ErrorType.GAME_ROOM_LOCK_FAILED);
            }

            GameRoom gameRoom = gameRoomRepository.findById(roomId)
                    .orElseThrow(() -> new CoreException(ErrorType.GAME_ROOM_NOT_FOUND));

            gameRoom.addPlayer(playerName);
            gameRoomRepository.save(gameRoom);
            publisher.publishEvent(new PlayerJoinEvent(roomId, gameRoom.getPlayerNames()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CoreException(ErrorType.DEFAULT_ERROR);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    public void leaveRoom(String roomId, String nickName) {
        RLock lock = redissonClient.getLock(ROOM_LOCK_PREFIX + roomId);
        try {
            if (!lock.tryLock(5, 1, TimeUnit.SECONDS)) {
                throw new CoreException(ErrorType.GAME_ROOM_LOCK_FAILED);
            }

            GameRoom gameRoom = gameRoomRepository.findById(roomId)
                    .orElseThrow(() -> new CoreException(ErrorType.GAME_ROOM_NOT_FOUND));

            String newHost = gameRoom.removePlayer(nickName);
            publisher.publishEvent(new PlayerLeaveEvent(roomId, gameRoom.getPlayerNames()));
            if (gameRoom.isEmpty()) {
                gameRoomRepository.delete(gameRoom);
                return;
            }

            gameRoomRepository.save(gameRoom);
            if (newHost != null) {
                publisher.publishEvent(new HostChangeEvent(roomId, newHost));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CoreException(ErrorType.DEFAULT_ERROR);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    public List<RoomInfo> findAll() {
        List<GameRoom> rooms = gameRoomRepository.findAll();
        return rooms.stream()
                .map(RoomInfo::from)
                .toList();
    }

    public List<String> findUsers(String roomId) {
        GameRoom gameRoom = gameRoomRepository.findById(roomId)
                .orElseThrow(() -> new CoreException(ErrorType.GAME_ROOM_NOT_FOUND));

        return gameRoom.getPlayerNames();
    }
}
