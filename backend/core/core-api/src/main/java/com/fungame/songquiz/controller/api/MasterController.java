package com.fungame.songquiz.controller.api;

import com.fungame.songquiz.domain.member.PromotionRequestInfo;
import com.fungame.songquiz.domain.member.PromotionService;
import com.fungame.songquiz.controller.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/master")
@RequiredArgsConstructor
public class MasterController {

    private final PromotionService promotionService;

    @GetMapping("/promotions")
    public ApiResponse<List<PromotionRequestInfo>> getPendingPromotions() {
        return ApiResponse.success(promotionService.getPendingRequests());
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
