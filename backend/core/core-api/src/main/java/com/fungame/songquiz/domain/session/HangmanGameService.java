package com.fungame.songquiz.domain.session;

import com.fungame.songquiz.domain.quiz.HangmanQuiz;
import com.fungame.songquiz.domain.quiz.Quiz;
import com.fungame.songquiz.domain.quiz.QuizContent;
import com.fungame.songquiz.domain.room.GamePlayer;
import com.fungame.songquiz.domain.room.GameRoomManager;
import com.fungame.songquiz.enums.ActionResult;
import com.fungame.songquiz.enums.GameType;
import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HangmanGameService implements GameService {

    private static final char NO_LETTER = ' ';
    private static final int NO_SCORE = 0;
    private static final Long NO_MEMBER = null;
    private static final long NO_ROUND_CLOCK = 0L;
    private static final int ONLY_ROUND = 1;

    private final GameRoomManager gameRoomManager;
    private final GameSessionManager gameSessionManager;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public List<GameType> getSupportTypes() {
        return List.of(GameType.HANGMAN);
    }

    @Override
    public void startGame(Long roomId, Long memberId) {
        Quiz quiz = gameSessionManager.createQuiz(gameRoomManager.findStartableRoom(roomId, memberId).getSettings());
        if (!(quiz instanceof HangmanQuiz preparedQuiz)) {
            throw new CoreException(ErrorType.GAME_NOT_FOUND);
        }

        GameSession gameSession = gameRoomManager.startGame(roomId, memberId, startedRoom -> {
            preparedQuiz.initPlayers(startedRoom.getRoomPlayers());
            return preparedQuiz;
        });
        HangmanQuiz hangmanQuiz = hangmanQuizOf(gameSession);

        eventPublisher.publishEvent(new GameStartEvent(roomId, gameSession.getQuizInfo()));
        eventPublisher.publishEvent(
                new RoundStartEvent(roomId, hangmanQuiz.getStatus(), ONLY_ROUND, ONLY_ROUND, NO_ROUND_CLOCK));

        GamePlayer starter = hangmanQuiz.getCurrentTurnPlayer();
        eventPublisher.publishEvent(new HangmanActionEvent(roomId, starter.memberId(),
                NO_LETTER, ActionResult.ACTION_SUCCESS, hangmanQuiz.getStatus()));
    }

    private HangmanQuiz hangmanQuizOf(GameSession gameSession) {
        if (gameSession == null || !(gameSession.getQuiz() instanceof HangmanQuiz hangmanQuiz)) {
            throw new CoreException(ErrorType.GAME_NOT_FOUND);
        }
        return hangmanQuiz;
    }

    @Override
    public void handleAction(Long roomId, GameAction action) {
        String payload = action.value();
        if (payload == null || payload.length() != 1) {
            throw new CoreException(ErrorType.INVALID_INPUT_VALUE);
        }
        char letter = payload.charAt(0);

        Guess guess = gameSessionManager.update(roomId, gameSession -> {
                    HangmanQuiz hangmanQuiz = hangmanQuizOf(gameSession);
                    GamePlayer actor = hangmanQuiz.getCurrentTurnPlayer();
                    ActionResult result = hangmanQuiz.guess(action.memberId(), letter);

                    return new Guess(actor, result, hangmanQuiz.getStatus(), hangmanQuiz.getRemainingTries(),
                            hangmanQuiz.getAnswer().answer());
                })
                .orElseThrow(() -> new CoreException(ErrorType.GAME_NOT_FOUND));

        eventPublisher.publishEvent(new HangmanActionEvent(roomId, guess.actor().memberId(), letter, guess.result(),
                guess.status()));

        if (guess.result() == ActionResult.CORRECT || guess.result() == ActionResult.WRONG) {
            submitResult(roomId, guess.remainingTries(), guess.answer());
        }
    }

    private record Guess(GamePlayer actor, ActionResult result, QuizContent status, int remainingTries,
                         String answer) {
    }

    private void submitResult(Long roomId, int remainingTries, String answer) {
        String result = remainingTries == 0 ? "실패" : "성공";

        List<ResultRow> resultRows = List.of(
                ResultRow.labelled(result, remainingTries),
                ResultRow.labelled(answer, NO_SCORE));
        // 브로드캐스트가 터져도 방이 PLAYING 으로 굳지 않게 한다.
        try {
            eventPublisher.publishEvent(new GameResultEvent(roomId, resultRows));
        } finally {
            gameRoomManager.endGame(roomId);
        }
    }

    @Override
    public void processAnswer(Long roomId, Long memberId, String message) {
    }

    @Override
    public void increaseSkipVote(Long roomId, Long memberId) {
    }

    @Override
    public List<PlayerScore> getPlayerRanks(Long roomId) {
        return List.of();
    }

    @Override
    public void startRound(Long roomId) {
    }

    @Override
    public GameStateDto getPlayState(Long roomId) {
        HangmanQuiz hangmanQuiz = hangmanQuizOf(gameSessionManager.find(roomId).orElse(null));

        return new GameStateDto(
                hangmanQuiz.getQuizInfo(),
                ONLY_ROUND,
                ONLY_ROUND,
                null,
                hangmanQuiz.getStatus().data(),
                NO_ROUND_CLOCK
        );
    }

    @Override
    public void handlePlayerLeave(Long roomId, Long memberId) {
        gameSessionManager.find(roomId)
                .filter(gameSession -> gameSession.hasParticipant(memberId))
                .map(GameSession::getQuiz)
                .filter(HangmanQuiz.class::isInstance)
                .map(HangmanQuiz.class::cast)
                .ifPresent(hangmanQuiz -> eventPublisher.publishEvent(new HangmanActionEvent(
                        roomId, memberId, NO_LETTER, ActionResult.ACTION_SUCCESS, hangmanQuiz.getStatus())));
    }
}
