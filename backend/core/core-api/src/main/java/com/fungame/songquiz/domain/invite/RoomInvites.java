package com.fungame.songquiz.domain.invite;

import com.fungame.songquiz.storage.redis.RoomInviteDao;
import com.fungame.songquiz.storage.redis.StoredRoomInvite;
import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoomInvites {

    static final Duration LIFETIME = Duration.ofSeconds(30);

    private final RoomInviteDao roomInviteDao;
    private final Clock clock;

    public RoomInvite issue(Long roomId, Long inviterMemberId, Long targetMemberId) {
        RoomInvite invite = new RoomInvite(
                UUID.randomUUID().toString(),
                roomId,
                inviterMemberId,
                targetMemberId,
                clock.instant().plus(LIFETIME));

        roomInviteDao.save(new StoredRoomInvite(
                invite.inviteId(), roomId, inviterMemberId, targetMemberId, invite.expiresAt()), LIFETIME);

        return invite;
    }

    public Optional<RoomInvite> take(String inviteId, Long targetMemberId) {
        return roomInviteDao.take(targetMemberId, inviteId)
                .map(stored -> new RoomInvite(
                        stored.inviteId(),
                        stored.roomId(),
                        stored.inviterMemberId(),
                        stored.targetMemberId(),
                        stored.expiresAt()))
                .filter(invite -> !invite.isExpiredAt(clock.instant()));
    }
}
