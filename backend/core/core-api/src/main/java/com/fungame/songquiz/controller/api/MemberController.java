package com.fungame.songquiz.controller.api;

import com.fungame.songquiz.domain.member.MemberAdapter;
import com.fungame.songquiz.domain.member.OnlineMemberService;
import com.fungame.songquiz.controller.response.ApiResponse;
import com.fungame.songquiz.controller.response.OnlineMemberResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final OnlineMemberService onlineMemberService;

    @GetMapping("/online")
    public ApiResponse<List<OnlineMemberResponse>> findOnlineMembers(@AuthenticationPrincipal MemberAdapter member) {
        return ApiResponse.success(
                OnlineMemberResponse.listFrom(onlineMemberService.findOthersOnline(member.getId())));
    }
}
