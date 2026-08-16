package com.fungame.songquiz.domain.member;

import com.fungame.songquiz.enums.PromotionStatus;
import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PromotionService {

    private final PromotionRequestReader promotionRequestReader;
    private final PromotionRequestWriter promotionRequestWriter;
    private final MemberReader memberReader;
    private final MemberWriter memberWriter;

    public void createPromotionRequest(String loginId) {
        Member member = readMember(loginId);

        if (promotionRequestReader.existsPendingOf(member.getId())) {
            throw new CoreException(ErrorType.PROMOTION_ALREADY_PENDING);
        }

        promotionRequestWriter.append(PromotionRequest.open(member.getInfo()));
    }

    public List<PromotionRequestInfo> getPendingRequests() {
        return promotionRequestReader.findAllByStatus(PromotionStatus.PENDING).stream()
                .map(PromotionRequestInfo::from)
                .toList();
    }

    @Transactional
    public void approveRequest(Long requestId) {
        PromotionRequest request = readRequest(requestId);

        request.approve();
        promotionRequestWriter.update(request);

        Member member = memberReader.findById(request.getMemberId())
                .orElseThrow(() -> new CoreException(ErrorType.MEMBER_NOT_FOUND));
        member.updateRole(request.promotedRole());
        memberWriter.update(member);
    }

    public void rejectRequest(Long requestId) {
        PromotionRequest request = readRequest(requestId);

        request.reject();
        promotionRequestWriter.update(request);
    }

    public PromotionStatus getCurrentStatus(String loginId) {
        Member member = readMember(loginId);

        return promotionRequestReader.findLatestOf(member.getId())
                .map(PromotionRequest::getStatus)
                .orElse(null);
    }

    private Member readMember(String loginId) {
        return memberReader.findByLoginId(loginId)
                .orElseThrow(() -> new CoreException(ErrorType.MEMBER_NOT_FOUND));
    }

    private PromotionRequest readRequest(Long requestId) {
        return promotionRequestReader.findById(requestId)
                .orElseThrow(() -> new CoreException(ErrorType.PROMOTION_NOT_FOUND));
    }
}
