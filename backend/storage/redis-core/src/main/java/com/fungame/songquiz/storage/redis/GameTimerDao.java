package com.fungame.songquiz.storage.redis;

import java.util.ArrayList;
import java.util.List;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

@Repository
public class GameTimerDao {

    public static final String KEY_PREFIX = "fungame:timer:";

    private static final String DUE_KEY = KEY_PREFIX + "due";
    private static final int MEMBER_AND_SCORE = 2;

    private final StringRedisTemplate redisTemplate;
    @SuppressWarnings("rawtypes")
    private final RedisScript<List> claimScript =
            RedisScript.of(new ClassPathResource("redis/timer/claim.lua"), List.class);
    private final RedisScript<Long> completeScript =
            RedisScript.of(new ClassPathResource("redis/timer/complete.lua"), Long.class);

    public GameTimerDao(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void schedule(String taskKey, long dueAtMillis) {
        redisTemplate.opsForZSet().add(DUE_KEY, taskKey, dueAtMillis);
    }

    public void cancel(String taskKey) {
        redisTemplate.opsForZSet().remove(DUE_KEY, taskKey);
    }

    public void cancelStartingWith(String taskKeyPrefix) {
        List<String> matching = new ArrayList<>();
        ScanOptions options = ScanOptions.scanOptions().match(taskKeyPrefix + "*").build();
        try (Cursor<TypedTuple<String>> cursor = redisTemplate.opsForZSet().scan(DUE_KEY, options)) {
            cursor.forEachRemaining(entry -> matching.add(entry.getValue()));
        }

        if (!matching.isEmpty()) {
            redisTemplate.opsForZSet().remove(DUE_KEY, matching.toArray());
        }
    }

    public List<ClaimedTimer> claim(long nowMillis, long leaseUntilMillis, int limit) {
        List<?> entries = redisTemplate.execute(claimScript, List.of(DUE_KEY),
                Long.toString(nowMillis), Long.toString(leaseUntilMillis), Integer.toString(limit));
        if (entries == null) {
            return List.of();
        }

        List<ClaimedTimer> claimed = new ArrayList<>();
        for (int i = 0; i + 1 < entries.size(); i += MEMBER_AND_SCORE) {
            claimed.add(new ClaimedTimer((String) entries.get(i), (long) Double.parseDouble((String) entries.get(i + 1)),
                    leaseUntilMillis));
        }
        return claimed;
    }

    public void complete(ClaimedTimer claimed) {
        redisTemplate.execute(completeScript, List.of(DUE_KEY),
                claimed.taskKey(), Long.toString(claimed.leaseUntilMillis()));
    }
}
