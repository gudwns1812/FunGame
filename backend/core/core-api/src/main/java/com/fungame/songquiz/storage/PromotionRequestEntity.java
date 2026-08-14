package com.fungame.songquiz.storage;

import com.fungame.songquiz.enums.PromotionStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
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
public class PromotionRequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private MemberEntity member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PromotionStatus status;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime processedAt;

    private PromotionRequestEntity(MemberEntity member, PromotionStatus status) {
        this.member = member;
        this.status = status;
    }

    public static PromotionRequestEntity open(MemberEntity member, PromotionStatus status) {
        return new PromotionRequestEntity(member, status);
    }

    public void changeStatus(PromotionStatus status, LocalDateTime processedAt) {
        this.status = status;
        this.processedAt = processedAt;
    }
}
