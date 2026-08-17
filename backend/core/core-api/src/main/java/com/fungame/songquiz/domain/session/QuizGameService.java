package com.fungame.songquiz.domain.session;

import com.fungame.songquiz.domain.quiz.Quiz;
import com.fungame.songquiz.domain.quiz.QuizInfo;
import com.fungame.songquiz.domain.room.GameRoom;
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

        GameRoom gameRoom = gameRoomManager.startGame(roomId, memberId);
        GameSession gameSession = sessionManager.startGame(roomId, quiz, gameRoom.getRoomPlayers());
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
        GameSession gameSession = sessionManager.getGameSession(roomId);
        if (gameSession == null) {
            return;
        }

        gameRoomManager.touch(roomId);

        gameSession.startRound();
        publisher.publishEvent(new RoundStartEvent(roomId, gameSession.getContent(), gameSession.getCurrentRound(),
                gameSession.getTotalRound(), gameSession.getRoundLength().toMillis()));

        timer.startAfter(roomId, gameSession.getUntilHintOpens(), () -> openHint(roomId, gameSession));
        timer.startAfter(roomId, gameSession.getRoundLength(), () -> endRound(roomId, NO_WINNER));
    }

    private void openHint(Long roomId, GameSession gameSession) {
        publisher.publishEvent(new QuizGameHintEvent(roomId, gameSession.getHint()));
    }

    private void endRound(Long roomId, Long winnerId) {
        GameSession gameSession = sessionManager.getGameSession(roomId);

        if (gameSession == null || !gameSession.startProcessing()) {
            return;
        }

        timer.stop(roomId);

        processRoundResult(roomId, winnerId, gameSession);

        scheduleNextStep(roomId, gameSession);
    }

    private void processRoundResult(Long roomId, Long winnerId, GameSession gameSession) {
        if (winnerId == null) {
            publisher.publishEvent(new RoundEndEvent(roomId, null, null, gameSession.getAnswer()));
            return;
        }

        gameSession.updatePlayerPoint(winnerId);
        publisher.publishEvent(
                new RoundEndEvent(roomId, winnerId, gameSession.nicknameOf(winnerId), gameSession.getAnswer()));
    }

    private void scheduleNextStep(Long roomId, GameSession gameSession) {
        if (gameSession.isLastRound()) {
            log.info("게임 종료");
            endGame(roomId);
            return;
        }

        log.info("라운드 종료");
        timer.startAfter(roomId, BETWEEN_ROUNDS, () -> startRound(roomId));
    }

    private void endGame(Long roomId) {
        GameSession gameSession = sessionManager.getGameSession(roomId);
        timer.startAfter(roomId, BEFORE_GAME_RESULT, () -> {
            publisher.publishEvent(new GameResultEvent(roomId, gameSession.getPlayerRanks()));

            gameRoomManager.endGame(roomId);
        });
    }

    @Override
    public void processAnswer(Long roomId, Long memberId, String message) {
        handleAction(roomId, GameAction.submitAnswer(memberId, message));
    }

    @Override
    public void handleAction(Long roomId, GameAction action) {
        GameSession gameSession = sessionManager.getGameSession(roomId);
        if (gameSession == null) {
            return;
        }

        ActionResult result = gameSession.handleAction(action);
        if (result == ActionResult.CORRECT) {
            endRound(roomId, action.memberId());
        } else if (result == ActionResult.SKIP_VOTE_SUCCESS) {
            endRound(roomId, NO_WINNER);
        }
    }

    @Override
    public List<PlayerScore> getPlayerRanks(Long roomId) {
        GameSession gameSession = sessionManager.getGameSession(roomId);
        if (gameSession == null) {
            throw new CoreException(ErrorType.GAME_NOT_FOUND);
        }

        return gameSession.getPlayerRanks();
    }

    @Override
    public void increaseSkipVote(Long roomId, Long memberId) {
        handleAction(roomId, GameAction.skipVote(memberId));
    }

    @Override
    public void handlePlayerLeave(Long roomId, Long memberId) {
        GameSession gameSession = sessionManager.getGameSession(roomId);
        if (gameSession == null) {
            return;
        }

        gameSession.removePlayer(memberId);
        log.info("게임 중 이탈: room {}, member {}", roomId, memberId);

        if (gameSession.isSkipThresholdReached()) {
            endRound(roomId, NO_WINNER);
        }
    }

    @Override
    public GameStateDto getPlayState(Long roomId) {
        GameSession gameSession = sessionManager.getGameSession(roomId);
        if (gameSession == null) {
            throw new CoreException(ErrorType.GAME_NOT_FOUND);
        }

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
}
