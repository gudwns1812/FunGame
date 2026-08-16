package com.fungame.songquiz.domain.invite;

import com.fungame.songquiz.domain.member.Member;
import com.fungame.songquiz.domain.member.MemberConnectionTracker;
import com.fungame.songquiz.domain.member.MemberPresenceService;
import com.fungame.songquiz.domain.room.GamePlayer;
import com.fungame.songquiz.domain.room.GameRoomService;
import com.fungame.songquiz.domain.room.RoomInfo;
import com.fungame.songquiz.domain.room.RoomSettingsInfo;
import com.fungame.songquiz.enums.GameRoomStatus;
import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomInviteService {

    private static final Duration INVITE_LIFETIME = Duration.ofSeconds(30);

    private final Map<String, RoomInvite> invitesById = new ConcurrentHashMap<>();

    private final GameRoomService gameRoomService;
    private final MemberPresenceService memberPresenceService;
    private final ApplicationEventPublisher eventPublisher;
    private final MemberConnectionTracker memberConnectionTracker;
    private final Clock clock;

    public RoomInviteNotification invite(Long roomId, Long inviterMemberId, Long targetMemberId) {
        if (inviterMemberId.equals(targetMemberId)) {
            throw new CoreException(ErrorType.INVITE_TO_SELF);
        }

        Member inviter = memberPresenceService.findMember(inviterMemberId);
        if (!inviter.isWaitingIn(roomId)) {
            throw new CoreException(ErrorType.INVITE_NOT_FROM_WAITING_ROOM);
        }

        requireWaitingRoom(roomId);
        requireInvitableTarget(targetMemberId);

        RoomInvite invite = createInvite(roomId, inviter, targetMemberId);
        invitesById.put(invite.inviteId(), invite);

        RoomInviteNotification notification = notificationOf(invite);
        eventPublisher.publishEvent(new RoomInviteCreatedEvent(targetMemberId, notification));

        return notification;
    }

    public AcceptedInvite accept(String inviteId, Long memberId) {
        RoomInvite invite = consume(inviteId, memberId);
        Member member = memberPresenceService.findMember(memberId);

        if (!member.isInLobby()) {
            throw new CoreException(ErrorType.ALREADY_IN_ANOTHER_ROOM);
        }

        int playerSequence = gameRoomService.joinRoom(invite.roomId(),
                GamePlayer.createNewPlayer(memberId, member.getNickname()));
        RoomInfo room = gameRoomService.findRoomInfo(invite.roomId());

        return new AcceptedInvite(room, playerSequence);
    }

    public void decline(String inviteId, Long memberId) {
        consume(inviteId, memberId);
    }

    @Scheduled(fixedDelay = 30000)
    public void purgeExpiredInvites() {
        LocalDateTime now = now();
        invitesById.values().removeIf(invite -> invite.isExpiredAt(now));
    }

    private RoomInvite consume(String inviteId, Long memberId) {
        RoomInvite invite = invitesById.get(inviteId);

        if (invite == null || invite.isNotFor(memberId) || invite.isExpiredAt(now())) {
            throw new CoreException(ErrorType.INVITE_NOT_FOUND);
        }

        if (!invitesById.remove(inviteId, invite)) {
            throw new CoreException(ErrorType.INVITE_NOT_FOUND);
        }

        return invite;
    }

    private void requireWaitingRoom(Long roomId) {
        if (gameRoomService.findRoomInfo(roomId).status() != GameRoomStatus.WAITING) {
            throw new CoreException(ErrorType.GAME_ALREADY_PLAYING);
        }
    }

    private void requireInvitableTarget(Long targetMemberId) {
        if (!memberConnectionTracker.hasLiveConnection(targetMemberId)) {
            throw new CoreException(ErrorType.INVITE_TARGET_OFFLINE);
        }

        if (!memberPresenceService.findMember(targetMemberId).isInLobby()) {
            throw new CoreException(ErrorType.INVITE_TARGET_NOT_IN_LOBBY);
        }
    }

    private RoomInvite createInvite(Long roomId, Member inviter, Long targetMemberId) {
        return new RoomInvite(
                UUID.randomUUID().toString(),
                roomId,
                inviter.getId(),
                inviter.getNickname(),
                targetMemberId,
                now().plus(INVITE_LIFETIME)
        );
    }

    private RoomInviteNotification notificationOf(RoomInvite invite) {
        RoomSettingsInfo settings = gameRoomService.findSettings(invite.roomId());

        return new RoomInviteNotification(
                invite.inviteId(),
                invite.roomId(),
                settings.title(),
                settings.gameType(),
                invite.inviterNickname(),
                INVITE_LIFETIME.toSeconds()
        );
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
