package com.fungame.songquiz.controller.api;

import com.fungame.songquiz.controller.request.LoginRequest;
import com.fungame.songquiz.controller.request.NicknameRequest;
import com.fungame.songquiz.controller.request.SignupRequest;
import com.fungame.songquiz.domain.dto.MemberInfo;
import com.fungame.songquiz.domain.member.AuthService;
import com.fungame.songquiz.domain.member.Member;
import com.fungame.songquiz.domain.member.MemberAdapter;
import com.fungame.songquiz.support.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ApiResponse<Long> signup(@RequestBody SignupRequest request) {
        return ApiResponse.success(authService.signup(
                request.getLoginId(),
                request.getPassword(),
                request.getNickname()
        ));
    }

    @GetMapping("/check-id")
    public ApiResponse<Boolean> checkId(@RequestParam String loginId) {
        return ApiResponse.success(authService.checkIdDuplicate(loginId));
    }

    @GetMapping("/check-nickname")
    public ApiResponse<Boolean> checkNickname(@RequestParam String nickname) {
        return ApiResponse.success(authService.checkNicknameDuplicate(nickname));
    }

    @PostMapping("/login")
    public ApiResponse<MemberInfo> login(
            @RequestBody LoginRequest request) {

        authService.login(request.getLoginId(), request.getPassword());

        Member member = authService.getMyInfo(request.getLoginId());
        return ApiResponse.success(MemberInfo.from(member));
    }

    @PatchMapping("/nickname")
    public ApiResponse<Void> updateNickname(
            @RequestBody NicknameRequest request,
            @AuthenticationPrincipal MemberAdapter user) {
        authService.updateNickname(user.getLoginId(), request.getNickname());
        return ApiResponse.success();
    }

    @GetMapping("/me")
    public ApiResponse<MemberInfo> getMe(@AuthenticationPrincipal MemberAdapter user) {
        if (user == null) {
            return ApiResponse.success(null);
        }
        Member member = authService.getMyInfo(user.getLoginId());
        return ApiResponse.success(MemberInfo.from(member));
    }
}
