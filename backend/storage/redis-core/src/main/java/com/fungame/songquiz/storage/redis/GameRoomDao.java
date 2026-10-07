package com.fungame.songquiz.storage.redis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

@Repository
public class GameRoomDao {

    public static final String KEY_PREFIX = "fungame:room:";

    private static final String ROOM_IDS_KEY = KEY_PREFIX + "ids";
    private static final String MEMBER_KEY_PREFIX = KEY_PREFIX + "member:";
    private static final String BODY = "body";
    private static final String REVISION = "revision";
    private static final String GAME = "game";
    private static final String UPDATED_AT = "updatedAt";
    private static final String NO_GAME = "";
    private static final long WRITTEN = 1;

    private final StringRedisTemplate redisTemplate;
    private final RedisScript<Long> createScript = script("create");
    private final RedisScript<Long> replaceScript = script("replace");
    private final RedisScript<Long> deleteScript = script("delete");

    public GameRoomDao(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean create(Long roomId, String body, Collection<Long> memberIds, long updatedAtMillis) {
        List<String> args = new ArrayList<>(
                List.of(roomId.toString(), body, MEMBER_KEY_PREFIX, Long.toString(updatedAtMillis)));
        memberIds.forEach(memberId -> args.add(memberId.toString()));

        return written(redisTemplate.execute(createScript, List.of(roomKeyOf(roomId), ROOM_IDS_KEY), args.toArray()));
    }

    public Optional<StoredRoom> find(Long roomId) {
        List<Object> fields = hashOperations().multiGet(roomKeyOf(roomId), List.of(BODY, REVISION, GAME, UPDATED_AT));

        return storedRoomOf(roomId, fields);
    }

    public List<StoredRoom> findAll() {
        Set<String> roomIds = redisTemplate.opsForSet().members(ROOM_IDS_KEY);
        if (roomIds == null) {
            return List.of();
        }

        return roomIds.stream()
                .map(Long::valueOf)
                .map(this::find)
                .flatMap(Optional::stream)
                .toList();
    }

    public Optional<Long> roomIdOf(Long memberId) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(MEMBER_KEY_PREFIX + memberId))
                .map(Long::valueOf);
    }

    public boolean replace(Long roomId, long expectedRevision, String body, String game,
                           Collection<Long> joinedMemberIds, Collection<Long> leftMemberIds, long updatedAtMillis) {
        List<String> args = new ArrayList<>(List.of(
                roomId.toString(), Long.toString(expectedRevision), body, game == null ? NO_GAME : game,
                MEMBER_KEY_PREFIX, Integer.toString(joinedMemberIds.size()), Long.toString(updatedAtMillis)));
        joinedMemberIds.forEach(memberId -> args.add(memberId.toString()));
        leftMemberIds.forEach(memberId -> args.add(memberId.toString()));

        return written(redisTemplate.execute(replaceScript, List.of(roomKeyOf(roomId)), args.toArray()));
    }

    public boolean delete(Long roomId, long expectedRevision, Collection<Long> memberIds) {
        List<String> args = new ArrayList<>(List.of(roomId.toString(), Long.toString(expectedRevision), MEMBER_KEY_PREFIX));
        memberIds.forEach(memberId -> args.add(memberId.toString()));

        return written(redisTemplate.execute(deleteScript, List.of(roomKeyOf(roomId), ROOM_IDS_KEY), args.toArray()));
    }

    private Optional<StoredRoom> storedRoomOf(Long roomId, List<Object> fields) {
        if (fields == null || fields.get(0) == null || fields.get(1) == null) {
            return Optional.empty();
        }

        return Optional.of(new StoredRoom(roomId, (String) fields.get(0), (String) fields.get(2),
                Long.parseLong((String) fields.get(1)), millisOrNull(fields.get(3))));
    }

    private static Long millisOrNull(Object field) {
        return field == null ? null : Long.parseLong((String) field);
    }

    private HashOperations<String, String, Object> hashOperations() {
        return redisTemplate.opsForHash();
    }

    private static boolean written(Long result) {
        return result != null && result == WRITTEN;
    }

    private static String roomKeyOf(Long roomId) {
        return KEY_PREFIX + roomId;
    }

    private static RedisScript<Long> script(String name) {
        return RedisScript.of(new ClassPathResource("redis/room/" + name + ".lua"), Long.class);
    }
}
