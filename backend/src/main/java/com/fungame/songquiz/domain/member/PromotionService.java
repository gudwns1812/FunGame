package com.fungame.songquiz.domain.member;

import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PromotionService {

    private final PromotionRequestRepository promotionRequestRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public void createPromotionRequest(String loginId) {
        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new CoreException(ErrorType.MEMBER_NOT_FOUND));

        if (promotionRequestRepository.existsByMemberAndStatus(member, PromotionStatus.PENDING)) {
            throw new CoreException(ErrorType.PROMOTION_ALREADY_PENDING);
        }

        PromotionRequest request = PromotionRequest.builder()
                .member(member)
                .build();

        promotionRequestRepository.save(request);
    }

    @Transactional(readOnly = true)
    public List<PromotionRequest> getPendingRequests() {
        return promotionRequestRepository.findAllByStatusWithMember(PromotionStatus.PENDING);
    }

    @Transactional
    public void approveRequest(Long requestId) {
        PromotionRequest request = promotionRequestRepository.findById(requestId)
                .orElseThrow(() -> new CoreException(ErrorType.PROMOTION_NOT_FOUND));
        
        request.approve();
    }

    @Transactional
    public void rejectRequest(Long requestId) {
        PromotionRequest request = promotionRequestRepository.findById(requestId)
                .orElseThrow(() -> new CoreException(ErrorType.PROMOTION_NOT_FOUND));
        
        request.reject();
    }

    @Transactional(readOnly = true)
    public PromotionStatus getCurrentStatus(String loginId) {
        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new CoreException(ErrorType.MEMBER_NOT_FOUND));
        
        return promotionRequestRepository.findTopByMemberOrderByCreatedAtDesc(member)
                .map(PromotionRequest::getStatus)
                .orElse(null);
    }
}
