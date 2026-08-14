package com.fungame.songquiz.storage;

import com.fungame.songquiz.enums.PromotionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PromotionRequestStore {

    private final PromotionRequestRepository promotionRequestRepository;

    public Optional<PromotionRequestEntity> findById(Long id) {
        return promotionRequestRepository.findById(id);
    }

    public PromotionRequestEntity save(PromotionRequestEntity entity) {
        return promotionRequestRepository.save(entity);
    }

    public List<PromotionRequestEntity> findAllByStatusWithMember(PromotionStatus status) {
        return promotionRequestRepository.findAllByStatusWithMember(status);
    }

    public boolean existsByMemberAndStatus(MemberEntity member, PromotionStatus status) {
        return promotionRequestRepository.existsByMemberAndStatus(member, status);
    }

    public Optional<PromotionRequestEntity> findTopByMemberOrderByCreatedAtDesc(MemberEntity member) {
        return promotionRequestRepository.findTopByMemberOrderByCreatedAtDesc(member);
    }
}
