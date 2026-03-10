package com.fungame.songquiz.domain;

import com.fungame.songquiz.domain.dto.GameInfo;
import com.fungame.songquiz.domain.dto.GameSkipInfo;
import com.fungame.songquiz.domain.event.GameEndEvent;
import com.fungame.songquiz.domain.event.GameResultEvent;
import com.fungame.songquiz.domain.event.GameSkipEvent;
import com.fungame.songquiz.domain.event.GameStartEvent;
import com.fungame.songquiz.domain.event.RoundEndEvent;
import com.fungame.songquiz.domain.event.RoundStartEvent;
import com.fungame.songquiz.domain.event.TimerTickEvent;
import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.error.ErrorType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Slf4j
@Service("quizGameService")
@RequiredArgsConstructor
public class QuizGameService implements GameService {

    private final ApplicationEventPublisher publisher;
    private final GameRoomManager gameRoomManager;
    private final GameSessionManager sessionManager;
    private final GameTimer timer;

    @Override
    public List<GameType> getSupportTypes() {
        return List.of(GameType.SONG, GameType.CS);
    }

    @Override
    public void startGame(Long roomId, String nickname) {
        GameRoom gameRoom = gameRoomManager.startGame(roomId, nickname);
        GameInfo gameInfo = sessionManager.startGame(roomId, gameRoom.getGame(), gameRoom.getRoomPlayers());
        publisher.publishEvent(new GameStartEvent(roomId, gameInfo));

        timer.startAfter(roomId, 5, () -> startRound(roomId));
    }

    @Override
    public void startRound(Long roomId) {
        GameSession gameSession = sessionManager.getGameSession(roomId);
        if (gameSession == null) {
            return;
        }

        gameSession.startRound();
        publisher.publishEvent(new RoundStartEvent(roomId, gameSession.getContent(), gameSession.getCurrentRound(),
                gameSession.getTotalRound()));

        timer.startCountDown(roomId, 30, remain -> {
            publisher.publishEvent(new TimerTickEvent(roomId, remain));
            if (remain <= 0) {
                endRound(roomId, null);
            }
        });
    }

    private void endRound(Long roomId, String winner) {
        GameSession gameSession = sessionManager.getGameSession(roomId);

        if (gameSession == null || !gameSession.startProcessing()) {
            return;
        }

        timer.stop(roomId);

        processRoundResult(roomId, winner, gameSession);

        scheduleNextStep(roomId, gameSession);
    }

    private void processRoundResult(Long roomId, String winner, GameSession gameSession) {
        if (winner != null) {
            gameSession.updatePlayerPoint(winner);
        }

        publisher.publishEvent(new RoundEndEvent(roomId, winner, gameSession.getAnswer()));
    }

    private void scheduleNextStep(Long roomId, GameSession gameSession) {
        if (gameSession.isLastRound()) {
            log.info("게임 종료");
            endGame(roomId);
            return;
        }

        log.info("라운드 종료");
        gameSession.endRound();
        timer.startAfter(roomId, 3, () -> startRound(roomId));
    }

    private void endGame(Long roomId) {
        GameSession gameSession = sessionManager.getGameSession(roomId);
        timer.startAfter(roomId, 3, () -> {
            publisher.publishEvent(new GameResultEvent(roomId, gameSession.getPlayerRanks()));

            timer.startAfter(roomId, 5, () -> {
                publisher.publishEvent(new GameEndEvent(roomId));
                sessionManager.endGameSession(roomId);
                gameRoomManager.endGame(roomId);
            });
        });
    }

    @Override
    public void processAnswer(Long roomId, String playerName, String message) {
        handleAction(roomId, GameAction.submitAnswer(playerName, message));
    }

    @Override
    public void handleAction(Long roomId, GameAction action) {
        GameSession gameSession = sessionManager.getGameSession(roomId);
        if (gameSession == null) {
            throw new CoreException(ErrorType.GAME_NOT_FOUND);
        }

        ActionResult result = gameSession.handleAction(action);
        if (result == ActionResult.CORRECT) {
            endRound(roomId, action.playerName());
        } else if (result == ActionResult.SKIP_VOTE_SUCCESS) {
            endRound(roomId, null);
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
    public void increaseSkipVote(Long roomId, String playerName) {
        handleAction(roomId, GameAction.skipVote(playerName));
    }
}
