package com.fungame.songquiz.storage.redis;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class MemberPresenceDao {

    public static final String KEY_PREFIX = "fungame:presence:";

    private static final String ONLINE_KEY = KEY_PREFIX + "online";
    private static final String INSTANCES_KEY = KEY_PREFIX + "instances";
    private static final String MEMBER_CONNECTIONS_KEY_PREFIX = KEY_PREFIX + "connections:";
    private static final String ALIVE_KEY_PREFIX = KEY_PREFIX + "instance:";
    private static final String INSTANCE_CONNECTIONS_KEY_SUFFIX = ":connections";
    private static final String SEPARATOR = ":";
    private static final String ALIVE = "1";
    private static final int MAX_REMOVE_ATTEMPTS = 10;
    private static final int ONLINE_RESULT_INDEX = 2;

    private final StringRedisTemplate redisTemplate;
    private final SetOperations<String, String> sets;

    public MemberPresenceDao(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.sets = redisTemplate.opsForSet();
    }

    public boolean connect(Long memberId, String instanceId, String sessionId) {
        List<Object> results = inSession(operations -> {
            operations.multi();
            operations.opsForSet().add(memberConnectionsKeyOf(memberId), connectionOf(instanceId, sessionId));
            operations.opsForSet().add(instanceConnectionsKeyOf(instanceId), instanceEntryOf(memberId, sessionId));
            operations.opsForSet().add(ONLINE_KEY, memberId.toString());
            return operations.exec();
        });

        return isOne(results.get(ONLINE_RESULT_INDEX));
    }

    public boolean disconnect(Long memberId, String instanceId, String sessionId) {
        return removeConnection(memberId, instanceId, sessionId);
    }

    public boolean hasLiveConnection(Long memberId) {
        return membersOf(memberConnectionsKeyOf(memberId)).stream()
                .map(MemberPresenceDao::instanceOf)
                .distinct()
                .anyMatch(this::isAlive);
    }

    public Set<Long> onlineMemberIds() {
        return membersOf(ONLINE_KEY).stream()
                .map(Long::valueOf)
                .collect(Collectors.toUnmodifiableSet());
    }

    public long countOnline() {
        Long count = sets.size(ONLINE_KEY);

        return count == null ? 0 : count;
    }

    public void keepAlive(String instanceId, Duration ttl) {
        redisTemplate.opsForValue().set(aliveKeyOf(instanceId), ALIVE, ttl);
        sets.add(INSTANCES_KEY, instanceId);
    }

    public void markDead(String instanceId) {
        redisTemplate.delete(aliveKeyOf(instanceId));
    }

    public long forgetDeadInstances() {
        return membersOf(INSTANCES_KEY).stream()
                .filter(instanceId -> !isAlive(instanceId))
                .mapToLong(this::forget)
                .sum();
    }

    private long forget(String instanceId) {
        if (!isOne(sets.remove(INSTANCES_KEY, instanceId))) {
            return 0;
        }

        String instanceConnectionsKey = instanceConnectionsKeyOf(instanceId);
        long wentOffline = membersOf(instanceConnectionsKey).stream()
                .filter(entry -> removeConnection(memberOf(entry), instanceId, sessionOf(entry)))
                .count();
        redisTemplate.delete(instanceConnectionsKey);

        return wentOffline;
    }

    private boolean removeConnection(Long memberId, String instanceId, String sessionId) {
        for (int attempt = 0; attempt < MAX_REMOVE_ATTEMPTS; attempt++) {
            List<Object> results = tryRemoveConnection(memberId, instanceId, sessionId);
            if (!results.isEmpty()) {
                return results.size() > ONLINE_RESULT_INDEX && isOne(results.get(ONLINE_RESULT_INDEX));
            }
        }

        throw new IllegalStateException("회원 " + memberId + " 의 연결을 지우는 동안 계속 충돌했다");
    }

    private List<Object> tryRemoveConnection(Long memberId, String instanceId, String sessionId) {
        String memberConnectionsKey = memberConnectionsKeyOf(memberId);
        String connection = connectionOf(instanceId, sessionId);

        return inSession(operations -> {
            operations.watch(memberConnectionsKey);
            Set<String> connections = operations.opsForSet().members(memberConnectionsKey);
            boolean lastConnection = connections == null || connections.stream().allMatch(connection::equals);

            operations.multi();
            operations.opsForSet().remove(memberConnectionsKey, connection);
            operations.opsForSet().remove(instanceConnectionsKeyOf(instanceId), instanceEntryOf(memberId, sessionId));
            if (lastConnection) {
                operations.opsForSet().remove(ONLINE_KEY, memberId.toString());
            }
            return operations.exec();
        });
    }

    private List<Object> inSession(Function<RedisOperations<String, String>, List<Object>> commands) {
        List<Object> results = redisTemplate.execute(new SessionCallback<>() {
            @Override
            @SuppressWarnings("unchecked")
            public <K, V> List<Object> execute(RedisOperations<K, V> operations) throws DataAccessException {
                return commands.apply((RedisOperations<String, String>) operations);
            }
        });

        return results == null ? List.of() : results;
    }

    private boolean isAlive(String instanceId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(aliveKeyOf(instanceId)));
    }

    private Set<String> membersOf(String key) {
        Set<String> members = sets.members(key);

        return members == null ? Set.of() : members;
    }

    private static String memberConnectionsKeyOf(Long memberId) {
        return MEMBER_CONNECTIONS_KEY_PREFIX + memberId;
    }

    private static String aliveKeyOf(String instanceId) {
        return ALIVE_KEY_PREFIX + instanceId;
    }

    private static String instanceConnectionsKeyOf(String instanceId) {
        return aliveKeyOf(instanceId) + INSTANCE_CONNECTIONS_KEY_SUFFIX;
    }

    private static String connectionOf(String instanceId, String sessionId) {
        return instanceId + SEPARATOR + sessionId;
    }

    private static String instanceOf(String connection) {
        return connection.substring(0, connection.lastIndexOf(SEPARATOR));
    }

    private static String instanceEntryOf(Long memberId, String sessionId) {
        return memberId + SEPARATOR + sessionId;
    }

    private static Long memberOf(String instanceEntry) {
        return Long.valueOf(instanceEntry.substring(0, instanceEntry.indexOf(SEPARATOR)));
    }

    private static String sessionOf(String instanceEntry) {
        return instanceEntry.substring(instanceEntry.indexOf(SEPARATOR) + 1);
    }

    private static boolean isOne(Object result) {
        return result instanceof Long count && count == 1;
    }
}
