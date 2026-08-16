package com.fungame.songquiz.controller.api;

import com.fungame.songquiz.domain.member.AuthService;
import com.fungame.songquiz.domain.member.MemberAdapter;
import com.fungame.songquiz.domain.member.MemberInfo;
import com.fungame.songquiz.domain.member.PasswordResetService;
import com.fungame.songquiz.controller.request.LoginRequest;
import com.fungame.songquiz.controller.request.NicknameRequest;
import com.fungame.songquiz.controller.request.PasswordResetLinkRequest;
import com.fungame.songquiz.controller.request.PasswordResetRequest;
import com.fungame.songquiz.controller.request.SignupRequest;
import com.fungame.songquiz.controller.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/signup")
    public ApiResponse<Long> signup(@RequestBody SignupRequest request) {
        return ApiResponse.success(authService.signup(
                request.getLoginId(),
                request.getPassword(),
                request.getNickname(),
                request.getEmail()
        ));
    }

    @PostMapping("/password-reset-request")
    public ApiResponse<Void> requestPasswordReset(@RequestBody PasswordResetLinkRequest request) {
        passwordResetService.requestReset(request.getLoginId(), request.getEmail());
        return ApiResponse.success();
    }

    @PostMapping("/password-reset")
    public ApiResponse<Void> resetPassword(@RequestBody PasswordResetRequest request) {
        passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
        return ApiResponse.success();
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
            @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        authService.login(request.getLoginId(), request.getPassword());

        // 세션 고정(Session Fixation) 공격 방지를 위해 인증 성공 직후 세션 ID를 교체한다.
        // 필터가 아닌 컨트롤러에서 직접 인증하므로 Spring Security의 SessionAuthenticationStrategy가
        // 동작하지 않아, 로그인 전 익명 세션 ID가 그대로 유지되는 문제가 있었다.
        // 세션이 아직 없다면 인증 정보 저장 시점에 새 세션이 만들어지므로 교체할 필요가 없다.
        if (httpRequest.getSession(false) != null) {
            httpRequest.changeSessionId();
        }

        return ApiResponse.success(authService.getMyInfo(request.getLoginId()));
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
        return ApiResponse.success(authService.getMyInfo(user.getLoginId()));
    }
}
