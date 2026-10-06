package com.fungame.songquiz.domain.invite;

import com.fungame.songquiz.domain.member.MemberConnectionTracker;
import com.fungame.songquiz.domain.member.MemberProfiles;
import com.fungame.songquiz.domain.room.GamePlayer;
import com.fungame.songquiz.domain.room.GameRoomService;
import com.fungame.songquiz.domain.room.RoomInfo;
import com.fungame.songquiz.domain.room.RoomSettings;
import com.fungame.songquiz.enums.GameRoomStatus;
import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomInviteService {

    private final GameRoomService gameRoomService;
    private final MemberProfiles memberProfiles;
    private final ApplicationEventPublisher eventPublisher;
    private final MemberConnectionTracker memberConnectionTracker;
    private final RoomInvites roomInvites;

    public RoomInviteNotification invite(Long roomId, Long inviterMemberId, Long targetMemberId) {
        if (inviterMemberId.equals(targetMemberId)) {
            throw new CoreException(ErrorType.INVITE_TO_SELF);
        }

        if (!gameRoomService.findLocationOf(inviterMemberId).isWaitingIn(roomId)) {
            throw new CoreException(ErrorType.INVITE_NOT_FROM_WAITING_ROOM);
        }

        requireWaitingRoom(roomId);
        requireInvitableTarget(targetMemberId);

        RoomInvite invite = roomInvites.issue(roomId, inviterMemberId, targetMemberId);

        RoomInviteNotification notification = notificationOf(invite);
        eventPublisher.publishEvent(new RoomInviteCreatedEvent(targetMemberId, notification));

        return notification;
    }

    public AcceptedInvite accept(String inviteId, Long memberId) {
        RoomInvite invite = consume(inviteId, memberId);

        if (!gameRoomService.findLocationOf(memberId).isInLobby()) {
            throw new CoreException(ErrorType.ALREADY_IN_ANOTHER_ROOM);
        }

        int playerSequence = gameRoomService.joinRoom(invite.roomId(), GamePlayer.createNewPlayer(memberId));
        RoomInfo room = gameRoomService.findRoomInfo(invite.roomId());

        return new AcceptedInvite(room, playerSequence);
    }

    public void decline(String inviteId, Long memberId) {
        consume(inviteId, memberId);
    }

    private RoomInvite consume(String inviteId, Long memberId) {
        return roomInvites.take(inviteId, memberId)
                .orElseThrow(() -> new CoreException(ErrorType.INVITE_NOT_FOUND));
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

        if (!gameRoomService.findLocationOf(targetMemberId).isInLobby()) {
            throw new CoreException(ErrorType.INVITE_TARGET_NOT_IN_LOBBY);
        }
    }

    private RoomInviteNotification notificationOf(RoomInvite invite) {
        RoomSettings settings = gameRoomService.findRoomState(invite.roomId()).settings();

        return new RoomInviteNotification(
                invite.inviteId(),
                invite.roomId(),
                settings.title(),
                settings.gameType(),
                memberProfiles.of(invite.inviterMemberId()).nickname(),
                RoomInvites.LIFETIME.toSeconds()
        );
    }
}
