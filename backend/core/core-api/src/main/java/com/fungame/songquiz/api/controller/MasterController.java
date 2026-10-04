package com.fungame.songquiz.api.controller;

import com.fungame.songquiz.api.controller.response.ApiResponse;
import com.fungame.songquiz.api.controller.response.PromotionRequestResponse;
import com.fungame.songquiz.domain.member.PromotionService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/master")
@RequiredArgsConstructor
public class MasterController {

    private final PromotionService promotionService;

    @GetMapping("/promotions")
    public ApiResponse<List<PromotionRequestResponse>> getPendingPromotions() {
        return ApiResponse.success(PromotionRequestResponse.listFrom(promotionService.getPendingRequests()));
    }

    @PatchMapping("/promotions/{id}/approve")
    public ApiResponse<Void> approvePromotion(@PathVariable Long id) {
        promotionService.approveRequest(id);
        return ApiResponse.success();
    }

    @PatchMapping("/promotions/{id}/reject")
    public ApiResponse<Void> rejectPromotion(@PathVariable Long id) {
        promotionService.rejectRequest(id);
        return ApiResponse.success();
    }
}
