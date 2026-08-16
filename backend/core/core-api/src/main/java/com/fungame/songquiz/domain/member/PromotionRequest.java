package com.fungame.songquiz.domain.member;

import com.fungame.songquiz.enums.PromotionStatus;
import com.fungame.songquiz.enums.Role;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class PromotionRequest {

    private static final Role PROMOTED_ROLE = Role.ADMIN;

    private final Long id;
    private final MemberInfo member;
    private final LocalDateTime createdAt;
    private PromotionStatus status;
    private LocalDateTime processedAt;

    private PromotionRequest(Long id, MemberInfo member, PromotionStatus status, LocalDateTime createdAt,
                             LocalDateTime processedAt) {
        this.id = id;
        this.member = member;
        this.status = status;
        this.createdAt = createdAt;
        this.processedAt = processedAt;
    }

    public static PromotionRequest open(MemberInfo member) {
        return new PromotionRequest(null, member, PromotionStatus.PENDING, null, null);
    }

    public static PromotionRequest restore(Long id, MemberInfo member, PromotionStatus status,
                                           LocalDateTime createdAt, LocalDateTime processedAt) {
        return new PromotionRequest(id, member, status, createdAt, processedAt);
    }

    public Long getMemberId() {
        return member.id();
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
