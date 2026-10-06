package com.fungame.songquiz.domain.session;

import com.fungame.songquiz.domain.quiz.Quiz;
import com.fungame.songquiz.domain.quiz.QuizAnswer;
import com.fungame.songquiz.domain.quiz.QuizContent;
import com.fungame.songquiz.domain.room.GameRoomManager;
import com.fungame.songquiz.enums.ActionResult;
import com.fungame.songquiz.enums.GameType;
import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizGameService implements GameService {

    private static final Duration BEFORE_FIRST_ROUND = Duration.ofSeconds(5);
    private static final Duration BETWEEN_ROUNDS = Duration.ofSeconds(3);
    private static final Duration BEFORE_GAME_RESULT = Duration.ofSeconds(3);
    private static final Long NO_WINNER = null;

    private final ApplicationEventPublisher publisher;
    private final GameRoomManager gameRoomManager;
    private final GameSessionManager sessionManager;
    private final GameTimer timer;

    @Override
    public List<GameType> getSupportTypes() {
        return List.of(GameType.SONG, GameType.CS);
    }

    @Override
    public void startGame(Long roomId, Long memberId) {
        Quiz quiz = sessionManager.createQuiz(gameRoomManager.findStartableRoom(roomId, memberId).getSettings());
        validateQuizHasRound(quiz);

        GameSession gameSession = gameRoomManager.startGame(roomId, memberId, startedRoom -> quiz);
        publisher.publishEvent(new GameStartEvent(roomId, gameSession.getQuizInfo()));

        timer.startAfter(roomId, BEFORE_FIRST_ROUND, () -> startRound(roomId));
    }

    private static void validateQuizHasRound(Quiz quiz) {
        if (quiz.getTotalRound() == 0) {
            throw new CoreException(ErrorType.QUIZ_EMPTY);
        }
    }

    @Override
    public void startRound(Long roomId) {
        Optional<RoundStart> started = sessionManager.update(roomId, gameSession -> {
            gameSession.startRound();
            return new RoundStart(gameSession.getContent(), gameSession.getCurrentRound(), gameSession.getTotalRound(),
                    gameSession.getRoundLength(), gameSession.getUntilHintOpens());
        });
        if (started.isEmpty()) {
            return;
        }

        gameRoomManager.touch(roomId);

        RoundStart round = started.get();
        try {
            publisher.publishEvent(new RoundStartEvent(roomId, round.content(), round.number(), round.total(),
                    round.length().toMillis()));
        } finally {
            timer.startAfter(roomId, round.untilHintOpens(), () -> openHint(roomId, round.number()));
            timer.startAfter(roomId, round.length(), () -> endRound(roomId, NO_WINNER, round.number()));
        }
    }

    private record RoundStart(QuizContent content, int number, int total, Duration length, Duration untilHintOpens) {
    }

    private void openHint(Long roomId, int round) {
        sessionManager.find(roomId)
                .filter(gameSession -> gameSession.getCurrentRound() == round)
                .ifPresent(gameSession -> publisher.publishEvent(new QuizGameHintEvent(roomId, gameSession.getHint())));
    }

    private void endRound(Long roomId, Long winnerId, int round) {
        Optional<RoundEnd> ended = sessionManager.update(roomId, gameSession -> {
            if (gameSession.getCurrentRound() != round || !gameSession.startProcessing()) {
                return null;
            }

            if (winnerId != null) {
                gameSession.updatePlayerPoint(winnerId);
            }

            return new RoundEnd(gameSession.getAnswer(), gameSession.isLastRound());
        });
        if (ended.isEmpty()) {
            return;
        }

        timer.stop(roomId);

        try {
            publisher.publishEvent(new RoundEndEvent(roomId, winnerId, ended.get().answer()));
        } finally {
            scheduleNextStep(roomId, ended.get().last());
        }
    }

    private record RoundEnd(QuizAnswer answer, boolean last) {
    }

    private void scheduleNextStep(Long roomId, boolean lastRound) {
        if (lastRound) {
            log.info("게임 종료");
            endGame(roomId);
            return;
        }

        log.info("라운드 종료");
        timer.startAfter(roomId, BETWEEN_ROUNDS, () -> startRound(roomId));
    }

    private void endGame(Long roomId) {
        timer.startAfter(roomId, BEFORE_GAME_RESULT, () -> {
            // 브로드캐스트가 터져도 방이 PLAYING 으로 굳지 않게 한다.
            try {
                List<PlayerScore> ranks = sessionManager.find(roomId)
                        .map(GameSession::getPlayerRanks)
                        .orElse(List.of());
                publisher.publishEvent(new GameResultEvent(roomId, ResultRow.listOf(ranks)));
            } finally {
                gameRoomManager.endGame(roomId);
            }
        });
    }

    @Override
    public void processAnswer(Long roomId, Long memberId, String message) {
        handleAction(roomId, GameAction.submitAnswer(memberId, message));
    }

    @Override
    public void handleAction(Long roomId, GameAction action) {
        Optional<ActionOutcome> outcome = sessionManager.update(roomId, gameSession ->
                new ActionOutcome(gameSession.handleAction(action), gameSession.getCurrentRound()));
        if (outcome.isEmpty()) {
            return;
        }

        ActionResult result = outcome.get().result();
        if (result == ActionResult.CORRECT) {
            endRound(roomId, action.memberId(), outcome.get().round());
        } else if (result == ActionResult.SKIP_VOTE_SUCCESS) {
            endRound(roomId, NO_WINNER, outcome.get().round());
        }
    }

    private record ActionOutcome(ActionResult result, int round) {
    }

    @Override
    public List<PlayerScore> getPlayerRanks(Long roomId) {
        return findGame(roomId).getPlayerRanks();
    }

    @Override
    public void increaseSkipVote(Long roomId, Long memberId) {
        handleAction(roomId, GameAction.skipVote(memberId));
    }

    @Override
    public void handlePlayerLeave(Long roomId, Long memberId) {
        sessionManager.find(roomId)
                .filter(GameSession::isSkipThresholdReached)
                .ifPresent(gameSession -> endRound(roomId, NO_WINNER, gameSession.getCurrentRound()));
        log.info("게임 중 이탈: room {}, member {}", roomId, memberId);
    }

    @Override
    public GameStateDto getPlayState(Long roomId) {
        GameSession gameSession = findGame(roomId);

        int currentRound = gameSession.getCurrentRound();
        String content = currentRound >= 1 ? gameSession.getContent().description() : null;

        return new GameStateDto(
                gameSession.getQuizInfo(),
                currentRound,
                gameSession.getTotalRound(),
                content,
                null,
                gameSession.getRemainingRoundMillis()
        );
    }

    private GameSession findGame(Long roomId) {
        return sessionManager.find(roomId)
                .orElseThrow(() -> new CoreException(ErrorType.GAME_NOT_FOUND));
    }
}
