package com.fungame.songquiz.controller.api;

import com.fungame.songquiz.domain.member.MemberAdapter;
import com.fungame.songquiz.domain.member.PromotionService;
import com.fungame.songquiz.domain.member.PromotionStatus;
import com.fungame.songquiz.support.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/promotions")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionService promotionService;

    @PostMapping
    public ApiResponse<Void> requestPromotion(@AuthenticationPrincipal MemberAdapter user) {
        promotionService.createPromotionRequest(user.getLoginId());
        return ApiResponse.success();
    }

    @GetMapping("/status")
    public ApiResponse<PromotionStatus> getPromotionStatus(@AuthenticationPrincipal MemberAdapter user) {
        return ApiResponse.success(promotionService.getCurrentStatus(user.getUsername()));
    }
}
