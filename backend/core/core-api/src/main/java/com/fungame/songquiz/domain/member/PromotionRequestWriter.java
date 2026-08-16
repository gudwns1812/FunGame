package com.fungame.songquiz.domain.member;

import com.fungame.songquiz.storage.MemberRepository;
import com.fungame.songquiz.storage.PromotionRequestEntity;
import com.fungame.songquiz.storage.PromotionRequestRepository;
import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PromotionRequestWriter {

    private final PromotionRequestRepository promotionRequestRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public void append(PromotionRequest request) {
        promotionRequestRepository.save(PromotionRequestEntity.open(
                memberRepository.getReferenceById(request.getMemberId()),
                request.getStatus()));
    }

    @Transactional
    public void update(PromotionRequest request) {
        PromotionRequestEntity entity = promotionRequestRepository.findById(request.getId())
                .orElseThrow(() -> new CoreException(ErrorType.PROMOTION_NOT_FOUND));

        entity.changeStatus(request.getStatus(), request.getProcessedAt());
    }
}
