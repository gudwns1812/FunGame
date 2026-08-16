package com.fungame.songquiz.controller.api;

import com.fungame.songquiz.domain.invite.AcceptedInvite;
import com.fungame.songquiz.domain.invite.RoomInviteNotification;
import com.fungame.songquiz.domain.invite.RoomInviteService;
import com.fungame.songquiz.domain.invite.SentInvite;
import com.fungame.songquiz.domain.member.MemberAdapter;
import com.fungame.songquiz.controller.request.InviteMemberRequest;
import com.fungame.songquiz.controller.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RoomInviteController {

    private final RoomInviteService roomInviteService;

    @PostMapping("/api/rooms/{roomId}/invites")
    public ApiResponse<SentInvite> invite(
            @PathVariable Long roomId,
            @RequestBody InviteMemberRequest request,
            @AuthenticationPrincipal MemberAdapter member) {
        RoomInviteNotification sent = roomInviteService.invite(roomId, member.getId(), request.targetMemberId());
        return ApiResponse.success(SentInvite.from(sent));
    }

    @PostMapping("/api/invites/{inviteId}/accept")
    public ApiResponse<AcceptedInvite> accept(
            @PathVariable String inviteId,
            @AuthenticationPrincipal MemberAdapter member) {
        return ApiResponse.success(roomInviteService.accept(inviteId, member.getId()));
    }

    @PostMapping("/api/invites/{inviteId}/decline")
    public ApiResponse<Void> decline(
            @PathVariable String inviteId,
            @AuthenticationPrincipal MemberAdapter member) {
        roomInviteService.decline(inviteId, member.getId());
        return ApiResponse.success();
    }
}
