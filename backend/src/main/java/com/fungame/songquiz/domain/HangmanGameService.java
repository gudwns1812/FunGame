package com.fungame.songquiz.domain;

import com.fungame.songquiz.domain.dto.GameInfo;
import com.fungame.songquiz.domain.dto.GameStateDto;
import com.fungame.songquiz.domain.event.GameResultEvent;
import com.fungame.songquiz.domain.event.GameStartEvent;
import com.fungame.songquiz.domain.event.HangmanActionEvent;
import com.fungame.songquiz.domain.event.RoundStartEvent;
import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HangmanGameService implements GameService {

    private final GameRoomManager gameRoomManager;
    private final GameSessionManager gameSessionManager;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public List<GameType> getSupportTypes() {
        return List.of(GameType.HANGMAN);
    }

    @Override
    public void startGame(Long roomId, String nickname) {
        GameRoom gameRoom = gameRoomManager.startGame(roomId, nickname);

        if (gameRoom.getGame() instanceof HangmanGame hangmanGame) {
            hangmanGame.initPlayers(gameRoom.getRoomPlayers());

            GameInfo gameInfo = gameSessionManager.startGame(roomId, hangmanGame, gameRoom.getRoomPlayers());
            eventPublisher.publishEvent(new GameStartEvent(roomId, gameInfo));

            eventPublisher.publishEvent(new RoundStartEvent(roomId, hangmanGame.getStatus(), 1, 1));

            eventPublisher.publishEvent(new HangmanActionEvent(roomId, nickname, ' ', ActionResult.ACTION_SUCCESS, hangmanGame.getStatus()));
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

        char letter = payload.charAt(0);
        ActionResult result = hangmanGame.guess(action.playerName(), letter);

        eventPublisher.publishEvent(new HangmanActionEvent(roomId, action.playerName(), letter, result, hangmanGame.getStatus()));

        if (result == ActionResult.CORRECT || result == ActionResult.WRONG) {
            submitResult(roomId, hangmanGame);
        }
    }

    private void submitResult(Long roomId, HangmanGame hangmanGame) {
        var result = hangmanGame.getRemainingTries() == 0 ? "실패" : "성공";

        var playerScore = List.of(new PlayerScore(result, hangmanGame.getRemainingTries()), new PlayerScore(hangmanGame.getAnswer().getAnswer(), 0));
        eventPublisher.publishEvent(new GameResultEvent(roomId, playerScore));

        gameRoomManager.endGame(roomId);
    }

    @Override
    public void processAnswer(Long roomId, String playerName, String message) {
    }

    @Override
    public void increaseSkipVote(Long roomId, String playerName) {
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
    public void handlePlayerLeave(Long roomId, String playerName) {
        GameSession session = gameSessionManager.getGameSession(roomId);
        if (session == null) {
            return;
        }

        session.removePlayer(playerName);

        GameRoom room = gameRoomManager.findRoom(roomId);
        if (!(room.getGame() instanceof HangmanGame hangmanGame)) {
            return;
        }

        eventPublisher.publishEvent(new HangmanActionEvent(
                roomId, playerName, ' ', ActionResult.ACTION_SUCCESS, hangmanGame.getStatus()));
    }
}
