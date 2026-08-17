package com.fungame.songquiz.domain.report;

import com.fungame.songquiz.enums.ReportReason;
import com.fungame.songquiz.storage.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class ReportReader {

    private final ReportRepository reportRepository;

    @Transactional(readOnly = true)
    public boolean existsSameReport(Long memberId, Long contentId, ReportReason reason) {
        return reportRepository.existsByMemberIdAndContentIdAndReason(memberId, contentId, reason);
    }

    @Transactional(readOnly = true)
    public long countSince(Long memberId, LocalDateTime since) {
        return reportRepository.countByMemberIdAndCreatedAtAfter(memberId, since);
    }
}
