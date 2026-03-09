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
@Service
@RequiredArgsConstructor
public class GameService {

    private final ApplicationEventPublisher publisher;
    private final GameRoomManager gameRoomManager;
    private final GameSessionManager sessionManager;
    private final GameTimer timer;

    public void startGame(Long roomId, String nickname) {
        GameRoom gameRoom = gameRoomManager.startGame(roomId, nickname);
        GameInfo gameInfo = sessionManager.startGame(roomId, gameRoom.getGame(), gameRoom.getRoomPlayers());
        publisher.publishEvent(new GameStartEvent(roomId, gameInfo));

        timer.startAfter(roomId, 5, () -> startRound(roomId));
    }

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

        //딱 하나의 스레드만 통과할 수 있도록 락을 AtomicBoolean으로 만듬
        if (!gameSession.startProcessing()) {
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

    public void processAnswer(Long roomId, String playerName, String message) {
        GameSession gameSession = sessionManager.getGameSession(roomId);
        if (gameSession == null) {
            return;
        }

        if (gameSession.isCorrectAnswer(message)) {
            endRound(roomId, playerName);
        }
    }

    public List<PlayerScore> getPlayerRanks(Long roomId) {
        GameSession gameSession = sessionManager.getGameSession(roomId);
        if (gameSession == null) {
            throw new CoreException(ErrorType.GAME_NOT_FOUND);
        }

        return gameSession.getPlayerRanks();
    }

    public void increaseSkipVote(Long roomId, String playerName) {
        GameSkipInfo skipInfo = sessionManager.increaseSkipVote(roomId, playerName);
        publisher.publishEvent(new GameSkipEvent(roomId, skipInfo.skipCount(), skipInfo.totalCount()));
        if (!skipInfo.isSkip()) {
            return;
        }

        endRound(roomId, null);
    }
}
