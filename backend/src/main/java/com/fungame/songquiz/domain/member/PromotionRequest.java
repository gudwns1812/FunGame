package com.fungame.songquiz.domain.member;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(name = "promotion_request")
public class PromotionRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PromotionStatus status;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime processedAt;

    @Builder
    private PromotionRequest(Member member) {
        this.member = member;
        this.status = PromotionStatus.PENDING;
    }

    public void approve() {
        this.status = PromotionStatus.APPROVED;
        this.processedAt = LocalDateTime.now();
        this.member.updateRole(Role.ADMIN);
    }

    public void reject() {
        this.status = PromotionStatus.REJECTED;
        this.processedAt = LocalDateTime.now();
    }
}
