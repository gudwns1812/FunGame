package com.fungame.songquiz.controller.api;

import com.fungame.songquiz.domain.member.PromotionService;
import com.fungame.songquiz.domain.member.PromotionStatus;
import com.fungame.songquiz.support.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/promotions")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionService promotionService;

    @PostMapping
    public ApiResponse<Void> requestPromotion(@AuthenticationPrincipal User user) {
        promotionService.createPromotionRequest(user.getUsername());
        return ApiResponse.success();
    }

    @GetMapping("/status")
    public ApiResponse<PromotionStatus> getPromotionStatus(@AuthenticationPrincipal User user) {
        return ApiResponse.success(promotionService.getCurrentStatus(user.getUsername()));
    }
}
