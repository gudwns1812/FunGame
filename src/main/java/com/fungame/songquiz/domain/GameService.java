package com.fungame.songquiz.domain;

import com.fungame.songquiz.storage.GameRoom;
import com.fungame.songquiz.storage.GameRoomRepository;
import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GameService {

    private final GameRoomRepository gameRoomRepository;
    private final SongReader songReader;
    private final ApplicationEventPublisher publisher;
    private final RedissonClient redissonClient;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final Map<String, ScheduledFuture<?>> roomTimers = new ConcurrentHashMap<>();

    private static final String ROOM_LOCK_PREFIX = "room_lock:";
    private static final String RANKING_KEY_PREFIX = "ranking:";
    private static final int ROUND_TIMEOUT_SECONDS = 30;
    private static final int ANSWER_DELAY_SECONDS = 5;

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

            // 랭킹 초기화
            String rankingKey = RANKING_KEY_PREFIX + roomId;
            redisTemplate.delete(rankingKey);
            for (String playerName : gameRoom.getPlayerNames()) {
                redisTemplate.opsForZSet().add(rankingKey, playerName, 0);
            }

            publisher.publishEvent(new GameStartEvent(roomId, songs));
            scheduleRoundTimeout(roomId, 0);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CoreException(ErrorType.DEFAULT_ERROR);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    public void processAnswer(String roomId, String nickname, String message) {
        RLock lock = redissonClient.getLock(ROOM_LOCK_PREFIX + roomId);
        try {
            if (!lock.tryLock(5, 1, TimeUnit.SECONDS)) {
                return;
            }

            GameRoom gameRoom = gameRoomRepository.findById(roomId)
                    .orElseThrow(() -> new CoreException(ErrorType.GAME_ROOM_NOT_FOUND));

            Long currentSongId = gameRoom.getCurrentSongId();
            if (currentSongId == null) {
                return;
            }

            Song song = songReader.findById(currentSongId);
            if (song != null && song.isCorrect(message)) {
                // 정답 처리
                String rankingKey = RANKING_KEY_PREFIX + roomId;
                redisTemplate.opsForZSet().incrementScore(rankingKey, nickname, 10);
                Double score = redisTemplate.opsForZSet().score(rankingKey, nickname);

                publisher.publishEvent(new CorrectAnswerEvent(roomId, nickname, message, score.intValue()));
                scheduleNextRound(roomId, gameRoom.getCurrentSongIndex());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private void scheduleRoundTimeout(String roomId, int songIndex) {
        cancelTimer(roomId);
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(new Runnable() {
            int remaining = ROUND_TIMEOUT_SECONDS;

            @Override
            public void run() {
                if (remaining > 0) {
                    publisher.publishEvent(new TimerTickEvent(roomId, remaining));
                    remaining--;
                } else {
                    handleRoundTimeout(roomId, songIndex);
                    cancelTimer(roomId);
                }
            }
        }, 0, 1, TimeUnit.SECONDS);
        roomTimers.put(roomId, future);
    }

    private void scheduleNextRound(String roomId, int songIndex) {
        cancelTimer(roomId);
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(new Runnable() {
            int remaining = ANSWER_DELAY_SECONDS;

            @Override
            public void run() {
                if (remaining > 0) {
                    publisher.publishEvent(new TimerTickEvent(roomId, remaining));
                    remaining--;
                } else {
                    handleNextRound(roomId, songIndex);
                    cancelTimer(roomId);
                }
            }
        }, 0, 1, TimeUnit.SECONDS);
        roomTimers.put(roomId, future);
    }

    private void cancelTimer(String roomId) {
        ScheduledFuture<?> future = roomTimers.remove(roomId);
        if (future != null) {
            future.cancel(false);
        }
    }

    public void handleRoundTimeout(String roomId, int songIndex) {
        handleNextRound(roomId, songIndex);
    }

    public void handleNextRound(String roomId, int songIndex) {
        RLock lock = redissonClient.getLock(ROOM_LOCK_PREFIX + roomId);
        try {
            if (!lock.tryLock(5, 1, TimeUnit.SECONDS)) {
                return;
            }

            GameRoom gameRoom = gameRoomRepository.findById(roomId).orElse(null);
            if (gameRoom == null || gameRoom.getCurrentSongIndex() != songIndex || gameRoom.isFinished()) {
                return;
            }

            boolean isFinished = gameRoom.nextSong();
            gameRoomRepository.save(gameRoom);

            if (isFinished) {
                Map<String, Integer> rankings = getRankings(roomId);
                publisher.publishEvent(new GameEndEvent(roomId, rankings));
            } else {
                publisher.publishEvent(new RoundTimeoutEvent(roomId, gameRoom.getCurrentSongIndex()));
                scheduleRoundTimeout(roomId, gameRoom.getCurrentSongIndex());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private Map<String, Integer> getRankings(String roomId) {
        String rankingKey = RANKING_KEY_PREFIX + roomId;
        Set<ZSetOperations.TypedTuple<Object>> range = redisTemplate.opsForZSet()
                .reverseRangeWithScores(rankingKey, 0, -1);

        if (range == null) {
            return Map.of();
        }

        return range.stream()
                .collect(Collectors.toMap(
                        tuple -> String.valueOf(tuple.getValue()),
                        tuple -> tuple.getScore() != null ? tuple.getScore().intValue() : 0,
                        (v1, v2) -> v1
                ));
    }
}
