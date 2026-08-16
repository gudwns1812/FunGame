package com.fungame.songquiz.controller.api;

import com.fungame.songquiz.domain.member.PromotionService;
import com.fungame.songquiz.controller.response.ApiResponse;
import com.fungame.songquiz.controller.response.PromotionRequestResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
