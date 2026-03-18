package com.fungame.songquiz.domain.dto;

import com.fungame.songquiz.domain.member.PromotionRequest;
import com.fungame.songquiz.domain.member.PromotionStatus;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PromotionRequestInfo {
    private Long id;
    private String loginId;
    private String nickname;
    private PromotionStatus status;
    private LocalDateTime createdAt;

    @Builder
    private PromotionRequestInfo(Long id, String loginId, String nickname, PromotionStatus status, LocalDateTime createdAt) {
        this.id = id;
        this.loginId = loginId;
        this.nickname = nickname;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static PromotionRequestInfo from(PromotionRequest request) {
        return PromotionRequestInfo.builder()
                .id(request.getId())
                .loginId(request.getMember().getLoginId())
                .nickname(request.getMember().getNickname())
                .status(request.getStatus())
                .createdAt(request.getCreatedAt())
                .build();
    }
}
