package com.fungame.songquiz.domain.member;

import com.fungame.songquiz.enums.PromotionStatus;
import com.fungame.songquiz.storage.MemberStore;
import com.fungame.songquiz.storage.PromotionRequestEntity;
import com.fungame.songquiz.storage.PromotionRequestStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PromotionRequestReader {

    private final PromotionRequestStore promotionRequestStore;
    private final MemberStore memberStore;

    public List<PromotionRequest> findAllByStatus(PromotionStatus status) {
        return promotionRequestStore.findAllByStatusWithMember(status).stream()
                .map(PromotionRequestReader::toRequest)
                .toList();
    }

    public Optional<PromotionRequest> findById(Long requestId) {
        return promotionRequestStore.findById(requestId).map(PromotionRequestReader::toRequest);
    }

    public Optional<PromotionRequest> findLatestOf(Long memberId) {
        return memberStore.findById(memberId)
                .flatMap(promotionRequestStore::findTopByMemberOrderByCreatedAtDesc)
                .map(PromotionRequestReader::toRequest);
    }

    public boolean existsPendingOf(Long memberId) {
        return memberStore.findById(memberId)
                .map(member -> promotionRequestStore.existsByMemberAndStatus(member, PromotionStatus.PENDING))
                .orElse(false);
    }

    private static PromotionRequest toRequest(PromotionRequestEntity entity) {
        return PromotionRequest.restore(
                entity.getId(),
                entity.getMember().getId(),
                entity.getMember().getLoginId(),
                entity.getMember().getNickname(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getProcessedAt());
    }
}
