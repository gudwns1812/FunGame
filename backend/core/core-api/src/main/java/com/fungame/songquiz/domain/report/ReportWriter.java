package com.fungame.songquiz.domain.report;

import com.fungame.songquiz.storage.MemberRepository;
import com.fungame.songquiz.storage.ReportCommentEntity;
import com.fungame.songquiz.storage.ReportCommentRepository;
import com.fungame.songquiz.storage.ReportEntity;
import com.fungame.songquiz.storage.ReportRepository;
import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ReportWriter {

    private final ReportRepository reportRepository;
    private final ReportCommentRepository reportCommentRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public Long append(Report report) {
        ReportEntity saved = reportRepository.save(ReportEntity.open(
                memberRepository.getReferenceById(report.getMemberId()),
                report.getSource(),
                report.getReason(),
                report.getDetail(),
                gameContextOf(report.getContext())));

        return saved.getId();
    }

    @Transactional
    public void appendComment(ReportComment comment) {
        reportCommentRepository.save(ReportCommentEntity.write(
                reportRepository.getReferenceById(comment.reportId()),
                memberRepository.getReferenceById(comment.authorId()),
                comment.content()));
    }

    @Transactional
    public void changeStatus(Report report) {
        ReportEntity entity = reportRepository.findById(report.getId())
                .orElseThrow(() -> new CoreException(ErrorType.REPORT_NOT_FOUND));

        entity.changeStatus(report.getStatus());
    }

    private static ReportEntity.GameContext gameContextOf(ReportContext context) {
        return new ReportEntity.GameContext(
                context.gameType(),
                context.category(),
                context.contentId(),
                context.roomId(),
                context.currentRound(),
                context.totalRound(),
                context.content(),
                context.answer(),
                context.hint());
    }
}
