package com.fungame.songquiz.controller.api;

import com.fungame.songquiz.controller.request.LoginRequest;
import com.fungame.songquiz.controller.request.NicknameRequest;
import com.fungame.songquiz.controller.request.SignupRequest;
import com.fungame.songquiz.domain.dto.MemberInfo;
import com.fungame.songquiz.domain.member.AuthService;
import com.fungame.songquiz.domain.member.Member;
import com.fungame.songquiz.support.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final SecurityContextRepository securityContextRepository;

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

    @PostMapping("/login")
    public ApiResponse<MemberInfo> login(
            @RequestBody LoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {

        authService.login(request.getLoginId(), request.getPassword());

        SecurityContext context = SecurityContextHolder.getContext();
        securityContextRepository.saveContext(context, servletRequest, servletResponse);

        Member member = authService.getMyInfo(request.getLoginId());
        return ApiResponse.success(MemberInfo.from(member));
    }

    @PatchMapping("/nickname")
    public ApiResponse<Void> updateNickname(
            @RequestBody NicknameRequest request,
            @AuthenticationPrincipal User user) {
        authService.updateNickname(user.getUsername(), request.getNickname());
        return ApiResponse.success();
    }

    @GetMapping("/me")
    public ApiResponse<MemberInfo> getMe(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ApiResponse.success(null);
        }
        Member member = authService.getMyInfo(user.getUsername());
        return ApiResponse.success(MemberInfo.from(member));
    }
}
