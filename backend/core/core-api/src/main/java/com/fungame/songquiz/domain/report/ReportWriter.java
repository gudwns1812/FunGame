package com.fungame.songquiz.domain.report;

import com.fungame.songquiz.storage.MemberRepository;
import com.fungame.songquiz.storage.ReportEntity;
import com.fungame.songquiz.storage.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ReportWriter {

    private final ReportRepository reportRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public void append(Report report) {
        reportRepository.save(ReportEntity.open(
                memberRepository.getReferenceById(report.getMemberId()),
                report.getSource(),
                report.getReason(),
                report.getDetail(),
                gameContextOf(report.getContext())));
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
