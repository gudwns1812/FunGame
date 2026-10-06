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
public class QuizGameService implements GameService, GameTimerHandler {

    private static final Duration BEFORE_FIRST_ROUND = Duration.ofSeconds(5);
    private static final Duration BETWEEN_ROUNDS = Duration.ofSeconds(3);
    private static final Duration BEFORE_GAME_RESULT = Duration.ofSeconds(3);
    private static final Long NO_WINNER = null;
    private static final int FIRST_ROUND = 1;

    private final ApplicationEventPublisher publisher;
    private final GameRoomManager gameRoomManager;
    private final GameSessionManager sessionManager;
    private final GameTimer timer;

    @Override
    public List<GameType> getSupportTypes() {
        return List.of(GameType.SONG, GameType.CS);
    }

    @Override
    public List<GameTimerTask.Kind> timerKinds() {
        return List.of(GameTimerTask.Kind.START_ROUND, GameTimerTask.Kind.OPEN_HINT, GameTimerTask.Kind.END_ROUND,
                GameTimerTask.Kind.SHOW_RESULT);
    }

    @Override
    public void onTimer(GameTimerTask task) {
        switch (task.kind()) {
            case START_ROUND -> startRound(task.targetId(), task.round());
            case OPEN_HINT -> openHint(task.targetId(), task.round());
            case END_ROUND -> endRound(task.targetId(), NO_WINNER, task.round());
            case SHOW_RESULT -> showResult(task.targetId());
            default -> log.warn("다루지 않는 예약 작업이다: {}", task);
        }
    }

    @Override
    public void startGame(Long roomId, Long memberId) {
        Quiz quiz = sessionManager.createQuiz(gameRoomManager.findStartableRoom(roomId, memberId).getSettings());
        validateQuizHasRound(quiz);

        GameSession gameSession = gameRoomManager.startGame(roomId, memberId, startedRoom -> quiz);
        publisher.publishEvent(new GameStartEvent(roomId, gameSession.getQuizInfo()));

        timer.startAfter(BEFORE_FIRST_ROUND, GameTimerTask.startRound(roomId, FIRST_ROUND));
    }

    private static void validateQuizHasRound(Quiz quiz) {
        if (quiz.getTotalRound() == 0) {
            throw new CoreException(ErrorType.QUIZ_EMPTY);
        }
    }

    @Override
    public void startRound(Long roomId) {
        sessionManager.find(roomId)
                .ifPresent(gameSession -> startRound(roomId, gameSession.getCurrentRound() + 1));
    }

    private void startRound(Long roomId, int round) {
        Optional<RoundStart> started = sessionManager.update(roomId, gameSession -> {
            if (gameSession.getCurrentRound() + 1 != round) {
                return null;
            }

            gameSession.startRound();
            return new RoundStart(gameSession.getContent(), gameSession.getCurrentRound(), gameSession.getTotalRound(),
                    gameSession.getRoundLength(), gameSession.getUntilHintOpens());
        });
        if (started.isEmpty()) {
            return;
        }

        gameRoomManager.touch(roomId);

        RoundStart opened = started.get();
        try {
            publisher.publishEvent(new RoundStartEvent(roomId, opened.content(), opened.number(), opened.total(),
                    opened.length().toMillis()));
        } finally {
            timer.startAfter(opened.untilHintOpens(), GameTimerTask.openHint(roomId, opened.number()));
            timer.startAfter(opened.length(), GameTimerTask.endRound(roomId, opened.number()));
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
            scheduleNextStep(roomId, round, ended.get().last());
        }
    }

    private record RoundEnd(QuizAnswer answer, boolean last) {
    }

    private void scheduleNextStep(Long roomId, int endedRound, boolean lastRound) {
        if (lastRound) {
            log.info("게임 종료");
            timer.startAfter(BEFORE_GAME_RESULT, GameTimerTask.showResult(roomId));
            return;
        }

        log.info("라운드 종료");
        timer.startAfter(BETWEEN_ROUNDS, GameTimerTask.startRound(roomId, endedRound + 1));
    }

    private void showResult(Long roomId) {
        Optional<List<PlayerScore>> ranks = sessionManager.find(roomId).map(GameSession::getPlayerRanks);
        if (ranks.isEmpty()) {
            return;
        }

        try {
            publisher.publishEvent(new GameResultEvent(roomId, ResultRow.listOf(ranks.get())));
        } finally {
            gameRoomManager.endGame(roomId);
        }
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
