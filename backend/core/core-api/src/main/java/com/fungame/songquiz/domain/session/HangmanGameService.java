package com.fungame.songquiz.domain.session;

import com.fungame.songquiz.domain.quiz.Game;
import com.fungame.songquiz.domain.quiz.GameInfo;
import com.fungame.songquiz.domain.quiz.HangmanGame;
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

        if (gameRoom.getGame() instanceof HangmanGame hangmanGame) {
            hangmanGame.initPlayers(gameRoom.getRoomPlayers());

            GameInfo gameInfo = gameSessionManager.startGame(roomId, hangmanGame, gameRoom.getRoomPlayers());
            eventPublisher.publishEvent(new GameStartEvent(roomId, gameInfo));

            eventPublisher.publishEvent(new RoundStartEvent(roomId, hangmanGame.getStatus(), 1, 1));

            GamePlayer starter = hangmanGame.getCurrentTurnPlayer();
            eventPublisher.publishEvent(new HangmanActionEvent(roomId, starter.memberId(), starter.nickname(),
                    NO_LETTER, ActionResult.ACTION_SUCCESS, hangmanGame.getStatus()));
        }
    }

    @Override
    public void handleAction(Long roomId, GameAction action) {
        GameRoom room = gameRoomManager.findRoom(roomId);
        Game game = room.getGame();

        if (!(game instanceof HangmanGame hangmanGame)) {
            throw new CoreException(ErrorType.GAME_NOT_FOUND);
        }

        String payload = action.value();
        if (payload == null || payload.length() != 1) {
            throw new CoreException(ErrorType.INVALID_INPUT_VALUE);
        }

        GamePlayer actor = hangmanGame.getCurrentTurnPlayer();
        char letter = payload.charAt(0);
        ActionResult result = hangmanGame.guess(action.memberId(), letter);

        eventPublisher.publishEvent(new HangmanActionEvent(roomId, actor.memberId(), actor.nickname(), letter, result,
                hangmanGame.getStatus()));

        if (result == ActionResult.CORRECT || result == ActionResult.WRONG) {
            submitResult(roomId, hangmanGame);
        }
    }

    private void submitResult(Long roomId, HangmanGame hangmanGame) {
        var result = hangmanGame.getRemainingTries() == 0 ? "실패" : "성공";

        var playerScore = List.of(
                new PlayerScore(null, result, hangmanGame.getRemainingTries()),
                new PlayerScore(null, hangmanGame.getAnswer().getAnswer(), NO_SCORE));
        eventPublisher.publishEvent(new GameResultEvent(roomId, playerScore));

        gameRoomManager.endGame(roomId);
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
        GameRoom room = gameRoomManager.findRoom(roomId);

        if (!(room.getGame() instanceof HangmanGame hangmanGame)) {
            throw new CoreException(ErrorType.GAME_NOT_FOUND);
        }

        GameInfo gameInfo = hangmanGame.getGameInfo();
        return new GameStateDto(
                gameInfo.gameType(),
                gameInfo.category(),
                gameInfo.totalCount(),
                1,
                1,
                null,
                hangmanGame.getStatus().data()
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

        GameRoom room = gameRoomManager.findRoom(roomId);
        if (!(room.getGame() instanceof HangmanGame hangmanGame)) {
            return;
        }

        eventPublisher.publishEvent(new HangmanActionEvent(
                roomId, memberId, leaverNickname, NO_LETTER, ActionResult.ACTION_SUCCESS, hangmanGame.getStatus()));
    }
}
