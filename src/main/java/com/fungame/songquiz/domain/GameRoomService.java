package com.fungame.songquiz.domain;

import com.fungame.songquiz.domain.dto.RoomInfo;
import com.fungame.songquiz.domain.event.HostChangeEvent;
import com.fungame.songquiz.domain.event.PlayerJoinEvent;
import com.fungame.songquiz.domain.event.PlayerLeaveEvent;
import com.fungame.songquiz.storage.GameRoomEntity;
import com.fungame.songquiz.storage.GameRoomRepository;
import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.error.ErrorType;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GameRoomService {

    private final GameRoomRepository gameRoomRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final ApplicationEventPublisher publisher;
    private final RedissonClient redissonClient;

    private static final String ROOM_ID_COUNTER = "room_id_counter";
    private static final String ROOM_LOCK_PREFIX = "room_lock:";

    public String createRoom(String title, int maxPlayers, String hostName, Category category) {
        Long roomId = stringRedisTemplate.opsForValue().increment(ROOM_ID_COUNTER);

        GameRoomEntity gameRoomEntity = GameRoomEntity.builder()
                .id(String.valueOf(roomId))
                .title(title)
                .hostName(hostName)
                .category(category)
                .playerNames(new ArrayList<>(List.of(hostName)))
                .status(GameRoomStatus.WAITING)
                .maxPlayers(maxPlayers)
                .build();

        gameRoomRepository.save(gameRoomEntity);

        return gameRoomEntity.getId();
    }

    public void joinRoom(String roomId, String playerName) {
        RLock lock = redissonClient.getLock(ROOM_LOCK_PREFIX + roomId);
        try {
            if (!lock.tryLock(5, 1, TimeUnit.SECONDS)) {
                throw new CoreException(ErrorType.GAME_ROOM_LOCK_FAILED);
            }

            GameRoomEntity gameRoomEntity = gameRoomRepository.findById(roomId)
                    .orElseThrow(() -> new CoreException(ErrorType.GAME_ROOM_NOT_FOUND));

            gameRoomEntity.addPlayer(playerName);
            gameRoomRepository.save(gameRoomEntity);
            publisher.publishEvent(new PlayerJoinEvent(roomId, gameRoomEntity.getPlayerNames()));
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

            GameRoomEntity gameRoomEntity = gameRoomRepository.findById(roomId)
                    .orElseThrow(() -> new CoreException(ErrorType.GAME_ROOM_NOT_FOUND));

            String newHost = gameRoomEntity.removePlayer(nickName);
            publisher.publishEvent(new PlayerLeaveEvent(roomId, gameRoomEntity.getPlayerNames()));
            if (gameRoomEntity.isEmpty()) {
                gameRoomRepository.delete(gameRoomEntity);
                return;
            }

            gameRoomRepository.save(gameRoomEntity);
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
        List<GameRoomEntity> rooms = gameRoomRepository.findAll();
        return rooms.stream()
                .map(RoomInfo::from)
                .toList();
    }

    public List<String> findUsers(String roomId) {
        GameRoomEntity gameRoomEntity = gameRoomRepository.findById(roomId)
                .orElseThrow(() -> new CoreException(ErrorType.GAME_ROOM_NOT_FOUND));

        return gameRoomEntity.getPlayerNames();
    }
}
