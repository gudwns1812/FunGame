package com.fungame.songquiz.domain.session;

import com.fungame.songquiz.domain.quiz.QuizSnapshot;
import java.time.Instant;
import java.util.List;
import java.util.Set;

public record GameSnapshot(
        QuizSnapshot quiz,
        List<Participant> participants,
        Set<Long> skipVoters,
        Instant roundStartedAt
) {
}
