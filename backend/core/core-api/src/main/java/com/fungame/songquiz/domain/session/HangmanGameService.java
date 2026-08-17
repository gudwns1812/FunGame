package com.fungame.songquiz.domain.session;

import com.fungame.songquiz.domain.quiz.HangmanQuiz;
import com.fungame.songquiz.domain.room.GamePlayer;
import com.fungame.songquiz.domain.room.GameRoom;
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
        GameRoom gameRoom = gameRoomManager.startGame(roomId, memberId);
        GameSession gameSession =
                gameSessionManager.startGame(roomId, gameRoom.getSettings(), gameRoom.getRoomPlayers());

        HangmanQuiz hangmanQuiz = hangmanQuizOf(gameSession);
        hangmanQuiz.initPlayers(gameRoom.getRoomPlayers());

        eventPublisher.publishEvent(new GameStartEvent(roomId, gameSession.getQuizInfo()));
        eventPublisher.publishEvent(
                new RoundStartEvent(roomId, hangmanQuiz.getStatus(), ONLY_ROUND, ONLY_ROUND, NO_ROUND_CLOCK));

        GamePlayer starter = hangmanQuiz.getCurrentTurnPlayer();
        eventPublisher.publishEvent(new HangmanActionEvent(roomId, starter.memberId(), starter.nickname(),
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
        HangmanQuiz hangmanQuiz = hangmanQuizOf(gameSessionManager.getGameSession(roomId));

        String payload = action.value();
        if (payload == null || payload.length() != 1) {
            throw new CoreException(ErrorType.INVALID_INPUT_VALUE);
        }

        GamePlayer actor = hangmanQuiz.getCurrentTurnPlayer();
        char letter = payload.charAt(0);
        ActionResult result = hangmanQuiz.guess(action.memberId(), letter);

        eventPublisher.publishEvent(new HangmanActionEvent(roomId, actor.memberId(), actor.nickname(), letter, result,
                hangmanQuiz.getStatus()));

        if (result == ActionResult.CORRECT || result == ActionResult.WRONG) {
            submitResult(roomId, hangmanQuiz);
        }
    }

    private void submitResult(Long roomId, HangmanQuiz hangmanQuiz) {
        String result = hangmanQuiz.getRemainingTries() == 0 ? "실패" : "성공";

        List<PlayerScore> resultRows = List.of(
                resultRow(result, hangmanQuiz.getRemainingTries()),
                resultRow(hangmanQuiz.getAnswer().answer(), NO_SCORE));
        eventPublisher.publishEvent(new GameResultEvent(roomId, resultRows));

        gameRoomManager.endGame(roomId);
    }

    private static PlayerScore resultRow(String label, int value) {
        return new PlayerScore(GamePlayer.createNewPlayer(NO_MEMBER, label), value);
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
        HangmanQuiz hangmanQuiz = hangmanQuizOf(gameSessionManager.getGameSession(roomId));

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
        GameSession session = gameSessionManager.getGameSession(roomId);
        if (session == null) {
            return;
        }

        String leaverNickname = session.nicknameOf(memberId);
        session.removePlayer(memberId);

        if (leaverNickname == null) {
            return;
        }

        if (!(session.getQuiz() instanceof HangmanQuiz hangmanQuiz)) {
            return;
        }

        eventPublisher.publishEvent(new HangmanActionEvent(
                roomId, memberId, leaverNickname, NO_LETTER, ActionResult.ACTION_SUCCESS, hangmanQuiz.getStatus()));
    }
}
