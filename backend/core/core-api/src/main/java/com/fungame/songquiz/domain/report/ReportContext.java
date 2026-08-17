package com.fungame.songquiz.domain.report;

import com.fungame.songquiz.domain.session.GameSession;
import com.fungame.songquiz.enums.GameType;

public record ReportContext(
        GameType gameType,
        String category,
        Long contentId,
        Long roomId,
        Integer currentRound,
        Integer totalRound,
        String content,
        String answer,
        String hint
) {

    public static ReportContext outsideGame(Long roomId, GameType gameType) {
        return new ReportContext(gameType, null, null, roomId, null, null, null, null, null);
    }

    public static ReportContext of(Long roomId, GameSession session) {
        if (!session.isRoundStarted()) {
            return outsideGame(roomId, session.getGameType());
        }

        return new ReportContext(
                session.getGameType(),
                session.getQuizInfo().category(),
                session.getCurrentContentId(),
                roomId,
                session.getCurrentRound(),
                session.getTotalRound(),
                session.getContent().description(),
                session.getAnswer().answer(),
                session.getHint());
    }
}
