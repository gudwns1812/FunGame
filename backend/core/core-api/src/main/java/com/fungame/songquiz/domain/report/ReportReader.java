package com.fungame.songquiz.domain.report;

import com.fungame.songquiz.enums.ReportReason;
import com.fungame.songquiz.enums.ReportStatus;
import com.fungame.songquiz.storage.ReportCommentEntity;
import com.fungame.songquiz.storage.ReportEntity;
import com.fungame.songquiz.storage.ReportRepository;
import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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

    @Transactional(readOnly = true)
    public List<Report> findMine(Long memberId) {
        return reportRepository.findAllByMemberWithComments(memberId).stream()
                .map(ReportReader::toDomain)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Report> findAll(ReportStatus status) {
        List<ReportEntity> entities = status == null
                ? reportRepository.findAllWithComments()
                : reportRepository.findAllByStatusWithComments(status);

        return entities.stream()
                .map(ReportReader::toDomain)
                .toList();
    }

    @Transactional(readOnly = true)
    public Report findById(Long reportId) {
        return reportRepository.findByIdWithComments(reportId)
                .map(ReportReader::toDomain)
                .orElseThrow(() -> new CoreException(ErrorType.REPORT_NOT_FOUND));
    }

    private static Report toDomain(ReportEntity entity) {
        return Report.restore(
                entity.getId(),
                entity.getMember().getId(),
                entity.getMember().getNickname(),
                entity.getSource(),
                entity.getReason(),
                entity.getDetail(),
                contextOf(entity),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getComments().stream()
                        .map(ReportReader::toDomain)
                        .toList());
    }

    private static ReportContext contextOf(ReportEntity entity) {
        return new ReportContext(
                entity.getGameType(),
                entity.getQuizCategory(),
                entity.getContentId(),
                entity.getRoomId(),
                entity.getCurrentRound(),
                entity.getTotalRound(),
                entity.getQuizContent(),
                entity.getQuizAnswer(),
                entity.getQuizHint());
    }

    private static ReportComment toDomain(ReportCommentEntity entity) {
        return new ReportComment(
                entity.getId(),
                entity.getReport().getId(),
                entity.getAuthor().getId(),
                entity.getAuthor().getNickname(),
                entity.getContent(),
                entity.getCreatedAt());
    }
}
