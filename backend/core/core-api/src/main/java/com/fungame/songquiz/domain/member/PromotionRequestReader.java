package com.fungame.songquiz.domain.member;

import com.fungame.songquiz.enums.PromotionStatus;
import com.fungame.songquiz.storage.MemberEntity;
import com.fungame.songquiz.storage.MemberRepository;
import com.fungame.songquiz.storage.PromotionRequestEntity;
import com.fungame.songquiz.storage.PromotionRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PromotionRequestReader {

    private final PromotionRequestRepository promotionRequestRepository;
    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public List<PromotionRequest> findAllByStatus(PromotionStatus status) {
        return promotionRequestRepository.findAllByStatusWithMember(status).stream()
                .map(PromotionRequestReader::toRequest)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<PromotionRequest> findById(Long requestId) {
        return promotionRequestRepository.findById(requestId).map(PromotionRequestReader::toRequest);
    }

    @Transactional(readOnly = true)
    public Optional<PromotionRequest> findLatestOf(Long memberId) {
        return memberRepository.findById(memberId)
                .flatMap(promotionRequestRepository::findTopByMemberOrderByCreatedAtDesc)
                .map(PromotionRequestReader::toRequest);
    }

    @Transactional(readOnly = true)
    public boolean existsPendingOf(Long memberId) {
        return memberRepository.findById(memberId)
                .map(member -> promotionRequestRepository.existsByMemberAndStatus(member, PromotionStatus.PENDING))
                .orElse(false);
    }

    private static PromotionRequest toRequest(PromotionRequestEntity entity) {
        MemberEntity member = entity.getMember();

        return PromotionRequest.restore(
                entity.getId(),
                new MemberInfo(member.getId(), member.getLoginId(), member.getNickname(), member.getEmail(),
                        member.getRole()),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getProcessedAt());
    }
}
