package com.fungame.songquiz.domain.session;

public record PlayerOutcome(Long memberId, boolean won, Integer rank, Integer score, boolean left) {

    private static final Integer NO_RANK = null;
    private static final Integer NO_SCORE = null;

    static PlayerOutcome finished(PlayerScore score) {
        return new PlayerOutcome(score.memberId(), score.isWinner(), score.rank(), score.score(), false);
    }

    static PlayerOutcome leftScored(Participant leaver, int rank) {
        return new PlayerOutcome(leaver.memberId(), false, rank, leaver.score(), true);
    }

    static PlayerOutcome cooperated(Participant finisher, boolean succeeded) {
        return new PlayerOutcome(finisher.memberId(), succeeded, NO_RANK, NO_SCORE, false);
    }

    static PlayerOutcome leftCooperation(Participant leaver) {
        return new PlayerOutcome(leaver.memberId(), false, NO_RANK, NO_SCORE, true);
    }
}
