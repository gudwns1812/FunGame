package com.fungame.songquiz.domain.member;

import com.fungame.songquiz.storage.MemberStore;
import com.fungame.songquiz.storage.PromotionRequestEntity;
import com.fungame.songquiz.storage.PromotionRequestStore;
import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PromotionRequestWriter {

    private final PromotionRequestStore promotionRequestStore;
    private final MemberStore memberStore;

    public void append(PromotionRequest request) {
        promotionRequestStore.save(PromotionRequestEntity.open(
                memberStore.getReferenceById(request.getMemberId()),
                request.getStatus()));
    }

    public void update(PromotionRequest request) {
        PromotionRequestEntity entity = promotionRequestStore.findById(request.getId())
                .orElseThrow(() -> new CoreException(ErrorType.PROMOTION_NOT_FOUND));

        entity.changeStatus(request.getStatus(), request.getProcessedAt());
    }
}
