package com.fungame.songquiz.domain.member;

import com.fungame.songquiz.enums.PromotionStatus;
import com.fungame.songquiz.enums.Role;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class PromotionRequest {

    private static final Role PROMOTED_ROLE = Role.ADMIN;

    private final Long id;
    private final Long memberId;
    private final String memberLoginId;
    private final String memberNickname;
    private final LocalDateTime createdAt;
    private PromotionStatus status;
    private LocalDateTime processedAt;

    private PromotionRequest(Long id, Long memberId, String memberLoginId, String memberNickname,
                            PromotionStatus status, LocalDateTime createdAt, LocalDateTime processedAt) {
        this.id = id;
        this.memberId = memberId;
        this.memberLoginId = memberLoginId;
        this.memberNickname = memberNickname;
        this.status = status;
        this.createdAt = createdAt;
        this.processedAt = processedAt;
    }

    public static PromotionRequest open(Long memberId) {
        return new PromotionRequest(null, memberId, null, null, PromotionStatus.PENDING, null, null);
    }

    public static PromotionRequest restore(Long id, Long memberId, String memberLoginId, String memberNickname,
                                           PromotionStatus status, LocalDateTime createdAt,
                                           LocalDateTime processedAt) {
        return new PromotionRequest(id, memberId, memberLoginId, memberNickname, status, createdAt, processedAt);
    }

    public Role promotedRole() {
        return PROMOTED_ROLE;
    }

    public void approve() {
        this.status = PromotionStatus.APPROVED;
        this.processedAt = LocalDateTime.now();
    }

    public void reject() {
        this.status = PromotionStatus.REJECTED;
        this.processedAt = LocalDateTime.now();
    }
}
