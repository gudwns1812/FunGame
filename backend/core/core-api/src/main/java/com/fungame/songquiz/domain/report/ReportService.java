package com.fungame.songquiz.domain.report;

import com.fungame.songquiz.domain.room.GameRoomManager;
import com.fungame.songquiz.domain.session.GameSession;
import com.fungame.songquiz.domain.session.GameSessionManager;
import com.fungame.songquiz.enums.GameType;
import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReportService {

    static final int MAX_REPORTS_PER_MINUTE = 5;

    private final GameRoomManager gameRoomManager;
    private final GameSessionManager gameSessionManager;
    private final ReportReader reportReader;
    private final ReportWriter reportWriter;
    private final ReportNotifier reportNotifier;
    private final Clock clock;

    public void receive(Long memberId, ReportCommand command) {
        validateReporterIsInRoom(memberId, command.roomId());
        validateNotTooFrequent(memberId);

        Report report = Report.open(memberId, command.source(), command.reason(), command.detail(),
                contextOf(command.roomId(), command.gameType()));
        boolean alreadyFiled = alreadyFiled(memberId, report);

        reportWriter.append(report);

        if (!alreadyFiled) {
            reportNotifier.notifyReport(report);
        }
    }

    private boolean alreadyFiled(Long memberId, Report report) {
        return report.pointsAtContent()
                && reportReader.existsSameReport(memberId, report.getContext().contentId(), report.getReason());
    }

    private void validateReporterIsInRoom(Long memberId, Long roomId) {
        if (roomId != null && !gameRoomManager.hasPlayer(roomId, memberId)) {
            throw new CoreException(ErrorType.REPORT_NOT_IN_ROOM);
        }
    }

    private void validateNotTooFrequent(Long memberId) {
        LocalDateTime aMinuteAgo = LocalDateTime.now(clock).minusMinutes(1);

        if (reportReader.countSince(memberId, aMinuteAgo) >= MAX_REPORTS_PER_MINUTE) {
            throw new CoreException(ErrorType.REPORT_RATE_LIMIT_EXCEEDED);
        }
    }

    private ReportContext contextOf(Long roomId, GameType declaredGameType) {
        if (roomId == null) {
            return ReportContext.outsideGame(null, declaredGameType);
        }

        GameSession session = gameSessionManager.getGameSession(roomId);
        if (session == null) {
            return ReportContext.outsideGame(roomId, gameRoomManager.getGameType(roomId));
        }

        return ReportContext.of(roomId, session);
    }
}
