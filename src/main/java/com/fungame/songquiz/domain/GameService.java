package com.fungame.songquiz.domain;

import com.fungame.songquiz.storage.GameRoom;
import com.fungame.songquiz.storage.GameRoomRepository;
import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class GameService {

    private final GameRoomRepository gameRoomRepository;
    private final SongReader songReader;
    private final ApplicationEventPublisher publisher;
    private final RedissonClient redissonClient;

    private static final String ROOM_LOCK_PREFIX = "room_lock:";

    public void startGame(String roomId, String nickname) {
        RLock lock = redissonClient.getLock(ROOM_LOCK_PREFIX + roomId);
        try {
            if (!lock.tryLock(5, 1, TimeUnit.SECONDS)) {
                throw new CoreException(ErrorType.GAME_ROOM_LOCK_FAILED);
            }

            GameRoom gameRoom = gameRoomRepository.findById(roomId)
                    .orElseThrow(() -> new CoreException(ErrorType.GAME_ROOM_NOT_FOUND));

            if (!gameRoom.isHostName(nickname)) {
                throw new CoreException(ErrorType.GAME_ROOM_NOT_HOST);
            }

            List<Long> songs = songReader.findSongByCategoryWithCount(gameRoom.getCategory(), gameRoom.getCount());
            gameRoom.startGame(songs);
            gameRoomRepository.save(gameRoom);

            publisher.publishEvent(new GameStartEvent(roomId, songs));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CoreException(ErrorType.DEFAULT_ERROR);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
