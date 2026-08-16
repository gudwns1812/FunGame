package com.fungame.songquiz.domain.invite;

import com.fungame.songquiz.domain.member.Member;
import com.fungame.songquiz.domain.member.MemberConnectionTracker;
import com.fungame.songquiz.domain.member.MemberPresenceService;
import com.fungame.songquiz.domain.room.GamePlayer;
import com.fungame.songquiz.domain.room.GameRoomService;
import com.fungame.songquiz.domain.room.RoomInfo;
import com.fungame.songquiz.domain.room.RoomSettingsInfo;
import com.fungame.songquiz.enums.CSQuizDifficulty;
import com.fungame.songquiz.enums.Category;
import com.fungame.songquiz.enums.GameRoomStatus;
import com.fungame.songquiz.enums.GameType;
import com.fungame.songquiz.support.MemberFixture;
import com.fungame.songquiz.support.MutableClock;
import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.error.ErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class RoomInviteServiceTest {

    private static final Long ROOM_ID = 7L;
    private static final Long INVITER_ID = 1L;
    private static final Long TARGET_ID = 2L;

    private final GameRoomService gameRoomService = mock(GameRoomService.class);
    private final MemberPresenceService memberPresenceService = mock(MemberPresenceService.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final MemberConnectionTracker memberConnectionTracker = mock(MemberConnectionTracker.class);
    private final MutableClock clock = new MutableClock(Instant.parse("2026-08-13T00:00:00Z"), ZoneId.of("UTC"));

    private RoomInviteService roomInviteService;

    private Member inviter;
    private Member target;

    @BeforeEach
    void setUp() {
        roomInviteService = new RoomInviteService(
                gameRoomService, memberPresenceService, eventPublisher, memberConnectionTracker, clock);

        inviter = MemberFixture.withId(INVITER_ID, "방장");
        inviter.enterWaitingRoom(ROOM_ID);
        target = MemberFixture.withId(TARGET_ID, "손님");

        given(memberPresenceService.findMember(INVITER_ID)).willReturn(inviter);
        given(memberPresenceService.findMember(TARGET_ID)).willReturn(target);
        given(memberConnectionTracker.hasLiveConnection(TARGET_ID)).willReturn(true);
        given(gameRoomService.findRoomInfo(ROOM_ID)).willReturn(waitingRoomInfo());
        given(gameRoomService.findSettings(ROOM_ID)).willReturn(roomSettings());
    }

    @Nested
    @DisplayName("초대 발급")
    class Invite {

        @Test
        @DisplayName("대기실에 있는 사람이 로비에 있는 사람을 초대하면 알림이 나간다.")
        void inviteLobbyMember() {
            RoomInviteNotification notification = roomInviteService.invite(ROOM_ID, INVITER_ID, TARGET_ID);

            assertThat(notification.roomId()).isEqualTo(ROOM_ID);
            assertThat(notification.roomTitle()).isEqualTo("테스트 방");
            assertThat(notification.inviterNickname()).isEqualTo("방장");
            assertThat(notification.expiresInSeconds()).isEqualTo(30);
            verify(eventPublisher).publishEvent(new RoomInviteCreatedEvent(TARGET_ID, notification));
        }

        @Test
        @DisplayName("자기 자신은 초대할 수 없다.")
        void rejectSelfInvite() {
            assertThatThrownBy(() -> roomInviteService.invite(ROOM_ID, INVITER_ID, INVITER_ID))
                    .isInstanceOf(CoreException.class)
                    .hasFieldOrPropertyWithValue("type", ErrorType.INVITE_TO_SELF);
        }

        @Test
        @DisplayName("로비에 있는 사람은 초대를 보낼 수 없다.")
        void rejectInviteFromLobby() {
            inviter.leaveRoom();

            assertThatThrownBy(() -> roomInviteService.invite(ROOM_ID, INVITER_ID, TARGET_ID))
                    .isInstanceOf(CoreException.class)
                    .hasFieldOrPropertyWithValue("type", ErrorType.INVITE_NOT_FROM_WAITING_ROOM);
        }

        @Test
        @DisplayName("게임 중인 사람은 그 방으로 초대를 보낼 수 없다.")
        void rejectInviteFromPlayingMember() {
            inviter.enterPlayingRoom(ROOM_ID);

            assertThatThrownBy(() -> roomInviteService.invite(ROOM_ID, INVITER_ID, TARGET_ID))
                    .isInstanceOf(CoreException.class)
                    .hasFieldOrPropertyWithValue("type", ErrorType.INVITE_NOT_FROM_WAITING_ROOM);
        }

        @Test
        @DisplayName("다른 방에 있는 사람은 이 방으로 초대를 보낼 수 없다.")
        void rejectInviteFromAnotherRoom() {
            inviter.enterWaitingRoom(99L);

            assertThatThrownBy(() -> roomInviteService.invite(ROOM_ID, INVITER_ID, TARGET_ID))
                    .isInstanceOf(CoreException.class)
                    .hasFieldOrPropertyWithValue("type", ErrorType.INVITE_NOT_FROM_WAITING_ROOM);
        }

        @Test
        @DisplayName("방이 이미 게임을 시작했으면 초대할 수 없다.")
        void rejectInviteToPlayingRoom() {
            given(gameRoomService.findRoomInfo(ROOM_ID)).willReturn(playingRoomInfo());

            assertThatThrownBy(() -> roomInviteService.invite(ROOM_ID, INVITER_ID, TARGET_ID))
                    .isInstanceOf(CoreException.class)
                    .hasFieldOrPropertyWithValue("type", ErrorType.GAME_ALREADY_PLAYING);
        }

        @Test
        @DisplayName("접속 중이 아닌 사람은 초대할 수 없다.")
        void rejectOfflineTarget() {
            given(memberConnectionTracker.hasLiveConnection(TARGET_ID)).willReturn(false);

            assertThatThrownBy(() -> roomInviteService.invite(ROOM_ID, INVITER_ID, TARGET_ID))
                    .isInstanceOf(CoreException.class)
                    .hasFieldOrPropertyWithValue("type", ErrorType.INVITE_TARGET_OFFLINE);
        }

        @Test
        @DisplayName("이미 다른 방에 있는 사람은 초대할 수 없다.")
        void rejectTargetInAnotherRoom() {
            target.enterWaitingRoom(99L);

            assertThatThrownBy(() -> roomInviteService.invite(ROOM_ID, INVITER_ID, TARGET_ID))
                    .isInstanceOf(CoreException.class)
                    .hasFieldOrPropertyWithValue("type", ErrorType.INVITE_TARGET_NOT_IN_LOBBY);
        }

        @Test
        @DisplayName("검증에 걸린 초대는 알림을 보내지 않는다.")
        void doNotNotifyOnRejectedInvite() {
            given(memberConnectionTracker.hasLiveConnection(TARGET_ID)).willReturn(false);

            assertThatThrownBy(() -> roomInviteService.invite(ROOM_ID, INVITER_ID, TARGET_ID))
                    .isInstanceOf(CoreException.class);

            verify(eventPublisher, never()).publishEvent(any(RoomInviteCreatedEvent.class));
        }
    }

    @Nested
    @DisplayName("초대 수락")
    class Accept {

        @Test
        @DisplayName("수락하면 그 방에 입장하고 방 정보와 순번을 돌려준다.")
        void acceptJoinsRoom() {
            given(gameRoomService.joinRoom(ROOM_ID, GamePlayer.createNewPlayer(TARGET_ID, "손님"))).willReturn(3);
            String inviteId = invite();

            AcceptedInvite accepted = roomInviteService.accept(inviteId, TARGET_ID);

            assertThat(accepted.playerSequence()).isEqualTo(3);
            assertThat(accepted.room().roomId()).isEqualTo(ROOM_ID);
            assertThat(accepted.room().title()).isEqualTo("테스트 방");
        }

        @Test
        @DisplayName("같은 초대를 두 번 수락할 수 없다.")
        void rejectDoubleAccept() {
            given(gameRoomService.joinRoom(ROOM_ID, GamePlayer.createNewPlayer(TARGET_ID, "손님"))).willReturn(3);
            String inviteId = invite();
            roomInviteService.accept(inviteId, TARGET_ID);

            assertThatThrownBy(() -> roomInviteService.accept(inviteId, TARGET_ID))
                    .isInstanceOf(CoreException.class)
                    .hasFieldOrPropertyWithValue("type", ErrorType.INVITE_NOT_FOUND);
        }

        @Test
        @DisplayName("30초가 지난 초대는 수락할 수 없다.")
        void rejectExpiredInvite() {
            String inviteId = invite();

            clock.plus(Duration.ofSeconds(30));

            assertThatThrownBy(() -> roomInviteService.accept(inviteId, TARGET_ID))
                    .isInstanceOf(CoreException.class)
                    .hasFieldOrPropertyWithValue("type", ErrorType.INVITE_NOT_FOUND);
            verify(gameRoomService, never()).joinRoom(anyLong(), any());
        }

        @Test
        @DisplayName("남에게 온 초대를 가로채 수락할 수 없다.")
        void rejectInviteOfAnotherMember() {
            String inviteId = invite();

            assertThatThrownBy(() -> roomInviteService.accept(inviteId, 99L))
                    .isInstanceOf(CoreException.class)
                    .hasFieldOrPropertyWithValue("type", ErrorType.INVITE_NOT_FOUND);
        }

        @Test
        @DisplayName("남이 가로채기를 시도해도 초대는 살아 있다.")
        void interceptAttemptDoesNotBurnInvite() {
            given(gameRoomService.joinRoom(ROOM_ID, GamePlayer.createNewPlayer(TARGET_ID, "손님"))).willReturn(3);
            String inviteId = invite();

            assertThatThrownBy(() -> roomInviteService.accept(inviteId, 99L))
                    .isInstanceOf(CoreException.class);

            assertThat(roomInviteService.accept(inviteId, TARGET_ID).playerSequence()).isEqualTo(3);
        }

        @Test
        @DisplayName("존재하지 않는 초대는 수락할 수 없다.")
        void rejectUnknownInvite() {
            assertThatThrownBy(() -> roomInviteService.accept("없는-초대", TARGET_ID))
                    .isInstanceOf(CoreException.class)
                    .hasFieldOrPropertyWithValue("type", ErrorType.INVITE_NOT_FOUND);
        }

        @Test
        @DisplayName("이미 다른 방에 들어간 뒤에는 초대를 수락할 수 없다.")
        void rejectAcceptWhileInAnotherRoom() {
            String inviteId = invite();
            target.enterWaitingRoom(99L);

            assertThatThrownBy(() -> roomInviteService.accept(inviteId, TARGET_ID))
                    .isInstanceOf(CoreException.class)
                    .hasFieldOrPropertyWithValue("type", ErrorType.ALREADY_IN_ANOTHER_ROOM);
            verify(gameRoomService, never()).joinRoom(anyLong(), any());
        }

        @Test
        @DisplayName("입장에 실패하면 그 사유가 그대로 올라온다.")
        void propagateJoinFailure() {
            given(gameRoomService.joinRoom(ROOM_ID, GamePlayer.createNewPlayer(TARGET_ID, "손님")))
                    .willThrow(new CoreException(ErrorType.GAME_ROOM_MAX_PLAYER_EXCEED));
            String inviteId = invite();

            assertThatThrownBy(() -> roomInviteService.accept(inviteId, TARGET_ID))
                    .isInstanceOf(CoreException.class)
                    .hasFieldOrPropertyWithValue("type", ErrorType.GAME_ROOM_MAX_PLAYER_EXCEED);
        }
    }

    @Nested
    @DisplayName("초대 거절과 정리")
    class DeclineAndPurge {

        @Test
        @DisplayName("거절한 초대는 다시 수락할 수 없다.")
        void declinedInviteCannotBeAccepted() {
            String inviteId = invite();

            roomInviteService.decline(inviteId, TARGET_ID);

            assertThatThrownBy(() -> roomInviteService.accept(inviteId, TARGET_ID))
                    .isInstanceOf(CoreException.class)
                    .hasFieldOrPropertyWithValue("type", ErrorType.INVITE_NOT_FOUND);
        }

        @Test
        @DisplayName("만료된 초대는 청소된다.")
        void purgeExpired() {
            String inviteId = invite();
            clock.plus(Duration.ofSeconds(31));

            roomInviteService.purgeExpiredInvites();

            assertThatThrownBy(() -> roomInviteService.accept(inviteId, TARGET_ID))
                    .isInstanceOf(CoreException.class)
                    .hasFieldOrPropertyWithValue("type", ErrorType.INVITE_NOT_FOUND);
        }
    }

    private String invite() {
        return roomInviteService.invite(ROOM_ID, INVITER_ID, TARGET_ID).inviteId();
    }

    private static RoomInfo waitingRoomInfo() {
        return new RoomInfo(ROOM_ID, "테스트 방", INVITER_ID, "방장", GameRoomStatus.WAITING, 8, 2, GameType.SONG, CSQuizDifficulty.HARD);
    }

    private static RoomInfo playingRoomInfo() {
        return new RoomInfo(ROOM_ID, "테스트 방", INVITER_ID, "방장", GameRoomStatus.PLAYING, 8, 2, GameType.SONG, CSQuizDifficulty.HARD);
    }

    private static RoomSettingsInfo roomSettings() {
        return new RoomSettingsInfo("테스트 방", GameType.SONG, 8, Category.KPOP, 10, 0, CSQuizDifficulty.HARD, INVITER_ID, "방장");
    }
}
