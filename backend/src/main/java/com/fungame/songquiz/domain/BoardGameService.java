package com.fungame.songquiz.domain;

import com.fungame.songquiz.domain.dto.GameInfo;
import com.fungame.songquiz.domain.event.GameResultEvent;
import com.fungame.songquiz.domain.event.GameStartEvent;
import com.fungame.songquiz.domain.event.HaliGaliActionEvent;
import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service("boardGameService")
@RequiredArgsConstructor
public class BoardGameService implements GameService {

    private final GameRoomManager roomManager;
    private final GameSessionManager sessionManager;
    private final ApplicationEventPublisher publisher;

    @Override
    public List<GameType> getSupportTypes() {
        return List.of(GameType.HALLIGALLI);
    }

    @Override
    public void startGame(Long roomId, String nickname) {
        GameRoom gameRoom = roomManager.startGame(roomId, nickname);
        GameInfo gameInfo = sessionManager.startGame(roomId, gameRoom.getGame(), gameRoom.getRoomPlayers());

        publisher.publishEvent(new GameStartEvent(roomId, gameInfo));

        // 보드게임 시작 시, 아직 아무도 카드를 뒤집지 않은 초기 상태를 전파합니다.
        // 이 액션은 도메인 handleAction을 호출하지 않으므로 턴(라운드)이 넘어가지 않습니다.
        GameSession session = sessionManager.getGameSession(roomId);
        publisher.publishEvent(new HaliGaliActionEvent(
                roomId,
                "SYSTEM",
                ActionType.GAME_INIT,
                ActionResult.CORRECT,
                session.getContent()
        ));
    }

    @Override
    public void processAnswer(Long roomId, String playerName, String message) {
    }

    @Override
    public void handleAction(Long roomId, GameAction action) {
        GameSession session = sessionManager.getGameSession(roomId);
        if (session == null) {
            throw new CoreException(ErrorType.GAME_NOT_FOUND);
        }

        ActionResult result = session.handleAction(action);

        publisher.publishEvent(new HaliGaliActionEvent(
                roomId,
                action.playerName(),
                action.type(),
                result,
                session.getContent()
        ));

        // 게임 종료 확인
        if (session.isLastRound()) {
            endGame(roomId);
        }
    }

    private void endGame(Long roomId) {
        GameSession session = sessionManager.getGameSession(roomId);
        log.info("보드게임 종료: {}", roomId);

        publisher.publishEvent(new GameResultEvent(roomId, session.getPlayerRanks()));

        sessionManager.endGameSession(roomId);
        roomManager.endGame(roomId);
    }

    @Override
    public void increaseSkipVote(Long roomId, String playerName) {
        // 보드게임 특성에 맞는 스킵 로직 (필요시 구현)
    }

    @Override
    public void handlePlayerLeave(Long roomId, String playerName) {
        GameSession session = sessionManager.getGameSession(roomId);
        if (session == null) {
            return;
        }

        // 턴이 이탈자에게 고정되어 게임이 멈추지 않도록 진행 상태에서 제거한다.
        session.removePlayer(playerName);
        log.info("보드게임 중 이탈: room {}, player {}", roomId, playerName);

        // 남은 인원이 1명 이하면 더 진행할 수 없으므로 결과를 확정한다.
        if (session.isLastRound()) {
            endGame(roomId);
            return;
        }

        publisher.publishEvent(new HaliGaliActionEvent(
                roomId,
                playerName,
                ActionType.GAME_INIT,
                ActionResult.ACTION_SUCCESS,
                session.getContent()
        ));
    }

    @Override
    public List<PlayerScore> getPlayerRanks(Long roomId) {
        GameSession session = sessionManager.getGameSession(roomId);
        return session != null ? session.getPlayerRanks() : List.of();
    }

    @Override
    public void startRound(Long roomId) {
        // 보드게임은 개별 턴 단위로 진행되므로 퀴즈식 라운드 시작 로직은 사용하지 않음
    }
}
