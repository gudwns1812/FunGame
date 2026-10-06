package com.fungame.songquiz.storage.redis;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

@Repository
public class MemberPresenceDao {

    public static final String KEY_PREFIX = "fungame:presence:";

    private static final String ONLINE_KEY = KEY_PREFIX + "online";
    private static final String CONNECTIONS_KEY_PREFIX = KEY_PREFIX + "connections:";
    private static final double EXCLUSIVE_STEP = 1;

    private final StringRedisTemplate redisTemplate;
    private final RedisScript<Long> connectScript = script("connect");
    private final RedisScript<Long> disconnectScript = script("disconnect");
    private final RedisScript<Long> renewScript = script("renew");

    public MemberPresenceDao(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean connect(Long memberId, String connectionId, Instant now, Duration lease) {
        Long wasOnline = redisTemplate.execute(connectScript, keysOf(memberId),
                memberId.toString(), connectionId, millis(now), millis(now.plus(lease)), Long.toString(lease.toMillis()));

        return wasOnline != null && wasOnline == 1;
    }

    public void disconnect(Long memberId, String connectionId, Instant now, Instant graceUntil) {
        redisTemplate.execute(disconnectScript, keysOf(memberId),
                memberId.toString(), connectionId, millis(now), millis(graceUntil));
    }

    public void renew(Map<Long, Set<String>> connectionIdsByMember, Instant now, Duration lease) {
        connectionIdsByMember.forEach((memberId, connectionIds) -> {
            List<String> args = new ArrayList<>();
            args.add(memberId.toString());
            args.add(millis(now.plus(lease)));
            args.add(Long.toString(lease.toMillis()));
            args.addAll(connectionIds);

            redisTemplate.execute(renewScript, keysOf(memberId), args.toArray());
        });
    }

    public boolean hasLiveConnection(Long memberId, Instant now) {
        Long live = redisTemplate.opsForZSet()
                .count(connectionsKeyOf(memberId), afterNow(now), Double.POSITIVE_INFINITY);

        return live != null && live > 0;
    }

    public Set<Long> onlineMemberIds(Instant now) {
        Set<String> memberIds = redisTemplate.opsForZSet()
                .rangeByScore(ONLINE_KEY, afterNow(now), Double.POSITIVE_INFINITY);

        if (memberIds == null) {
            return Set.of();
        }

        return memberIds.stream()
                .map(Long::valueOf)
                .collect(Collectors.toUnmodifiableSet());
    }

    public long countOnline(Instant now) {
        Long count = redisTemplate.opsForZSet().count(ONLINE_KEY, afterNow(now), Double.POSITIVE_INFINITY);

        return count == null ? 0 : count;
    }

    public boolean expireOffline(Instant now) {
        Long removed = redisTemplate.opsForZSet().removeRangeByScore(ONLINE_KEY, Double.NEGATIVE_INFINITY, now.toEpochMilli());

        return removed != null && removed > 0;
    }

    private static List<String> keysOf(Long memberId) {
        return List.of(ONLINE_KEY, connectionsKeyOf(memberId));
    }

    private static String connectionsKeyOf(Long memberId) {
        return CONNECTIONS_KEY_PREFIX + memberId;
    }

    private static double afterNow(Instant now) {
        return now.toEpochMilli() + EXCLUSIVE_STEP;
    }

    private static String millis(Instant instant) {
        return Long.toString(instant.toEpochMilli());
    }

    private static RedisScript<Long> script(String name) {
        return RedisScript.of(new ClassPathResource("redis/presence/" + name + ".lua"), Long.class);
    }
}
