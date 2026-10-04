package com.fungame.songquiz.api.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.fungame.songquiz.api.controller.response.ApiResponse;
import com.fungame.songquiz.domain.invite.RoomInviteCreatedEvent;
import com.fungame.songquiz.domain.invite.RoomInviteNotification;
import com.fungame.songquiz.domain.member.MemberAdapter;
import com.fungame.songquiz.enums.GameType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class InviteNotifierTest {

    private static final Long TARGET_ID = 7L;

    private final StompBroadcaster broadcaster = mock(StompBroadcaster.class);
    private final InviteNotifier inviteNotifier = new InviteNotifier(broadcaster);

    @Test
    @DisplayName("초대는 초대받은 사람에게만 간다.")
    void sendInviteOnlyToTarget() {
        RoomInviteNotification notification =
                new RoomInviteNotification("invite-1", 9L, "방 제목", GameType.SONG, "방장", 30);

        inviteNotifier.handleRoomInviteCreated(new RoomInviteCreatedEvent(TARGET_ID, notification));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<ApiResponse<Object>> sent = ArgumentCaptor.forClass(ApiResponse.class);
        verify(broadcaster).sendToUser(
                eq(MemberAdapter.principalNameOf(TARGET_ID)), eq(StompDestination.INVITE), sent.capture());

        assertThat(sent.getValue().getData()).isEqualTo(notification);
    }
}
