package com.fungame.songquiz.domain;

import com.fungame.songquiz.domain.dto.GameInfo;
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

            // 초기 라운드 시작 이벤트 발행 (프론트엔드 상태 전환용)
            eventPublisher.publishEvent(new RoundStartEvent(roomId, hangmanGame.getStatus(), 1, 1));

            // 초기 행맨 상태 이벤트 발행 (프론트엔드 hangmanStatus 초기화용)
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

        // 행맨 액션 이벤트 발행
        eventPublisher.publishEvent(new HangmanActionEvent(roomId, action.playerName(), letter, result, hangmanGame.getStatus()));

        if (result == ActionResult.CORRECT || result == ActionResult.WRONG) {
            submitResult(roomId, hangmanGame);
        }
    }

    private void submitResult(Long roomId, HangmanGame hangmanGame) {
        gameRoomManager.endGame(roomId);
        gameSessionManager.endGameSession(roomId);

        var result = hangmanGame.getRemainingTries() == 0 ? "실패" : "성공";

        var playerScore = List.of(new PlayerScore(result, hangmanGame.getRemainingTries()), new PlayerScore(hangmanGame.getAnswer().getAnswer(), 0));
        eventPublisher.publishEvent(new GameResultEvent(roomId, playerScore));
    }

    @Override
    public void processAnswer(Long roomId, String playerName, String message) {
    }

    @Override
    public void increaseSkipVote(Long roomId, String playerName) {
        // 행맨 전용 스킵 로직 필요시 구현
    }

    @Override
    public List<PlayerScore> getPlayerRanks(Long roomId) {
        return List.of();
    }

    @Override
    public void startRound(Long roomId) {
        // 단판 게임이므로 별도 구현 없음
    }
}
