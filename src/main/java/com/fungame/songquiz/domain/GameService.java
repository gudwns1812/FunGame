package com.fungame.songquiz.domain;

import com.fungame.songquiz.domain.event.TimerTickEvent;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RedissonClient;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GameService {

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

    }

    public void processAnswer(String roomId, String nickname, String message) {

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
        scheduleNextRound(roomId, songIndex);
    }

    public void handleNextRound(String roomId, int songIndex) {

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
