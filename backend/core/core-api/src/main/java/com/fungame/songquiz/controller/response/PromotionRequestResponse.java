package com.fungame.songquiz.controller.response;

import com.fungame.songquiz.domain.member.PromotionRequestInfo;
import com.fungame.songquiz.enums.PromotionStatus;

import java.time.LocalDateTime;
import java.util.List;

public record PromotionRequestResponse(
        Long id,
        String loginId,
        String nickname,
        PromotionStatus status,
        LocalDateTime createdAt
) {

    public static PromotionRequestResponse from(PromotionRequestInfo info) {
        return new PromotionRequestResponse(
                info.getId(), info.getLoginId(), info.getNickname(), info.getStatus(), info.getCreatedAt());
    }

    public static List<PromotionRequestResponse> listFrom(List<PromotionRequestInfo> requests) {
        return requests.stream()
                .map(PromotionRequestResponse::from)
                .toList();
    }
}
