package com.fungame.songquiz.storage.redis;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RoomInviteDao {

    public static final String KEY_PREFIX = "fungame:invite:";

    private static final String SEPARATOR = ":";
    private static final int ROOM_ID = 0;
    private static final int INVITER_MEMBER_ID = 1;
    private static final int EXPIRES_AT = 2;

    private final StringRedisTemplate redisTemplate;

    public RoomInviteDao(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void save(StoredRoomInvite invite, Duration lifetime) {
        redisTemplate.opsForValue().set(keyOf(invite.targetMemberId(), invite.inviteId()), valueOf(invite), lifetime);
    }

    public Optional<StoredRoomInvite> take(Long targetMemberId, String inviteId) {
        String value = redisTemplate.opsForValue().getAndDelete(keyOf(targetMemberId, inviteId));

        return Optional.ofNullable(value)
                .map(stored -> stored.split(SEPARATOR))
                .map(fields -> new StoredRoomInvite(
                        inviteId,
                        Long.valueOf(fields[ROOM_ID]),
                        Long.valueOf(fields[INVITER_MEMBER_ID]),
                        targetMemberId,
                        Instant.ofEpochMilli(Long.parseLong(fields[EXPIRES_AT]))));
    }

    private static String keyOf(Long targetMemberId, String inviteId) {
        return KEY_PREFIX + targetMemberId + SEPARATOR + inviteId;
    }

    private static String valueOf(StoredRoomInvite invite) {
        return String.join(SEPARATOR,
                invite.roomId().toString(),
                invite.inviterMemberId().toString(),
                Long.toString(invite.expiresAt().toEpochMilli()));
    }
}
