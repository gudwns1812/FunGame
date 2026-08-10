package com.fungame.songquiz.domain;

import com.fungame.songquiz.domain.dto.PlayersInfo;
import com.fungame.songquiz.domain.dto.RoomInfo;
import com.fungame.songquiz.domain.event.PlayerJoinEvent;
import com.fungame.songquiz.domain.event.PlayerLeaveEvent;
import com.fungame.songquiz.domain.event.PlayerReadyEvent;
import com.fungame.songquiz.domain.event.RoomChangedEvent;
import com.fungame.songquiz.domain.gamecreator.GameCreateInfo;
import com.fungame.songquiz.storage.CounterEntity;
import com.fungame.songquiz.storage.CounterRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GameRoomService {

    private static final String GAME_ROOM_COUNTER = "GAME_ROOM_COUNTER";

    private final CounterRepository counterRepository;
    private final Map<GameType, GameFactory> creators;
    private final GameRoomManager gameRoomManager;
    private final GameService gameService;
    private final ApplicationEventPublisher applicationEventPublisher;

    public GameRoomService(CounterRepository counterRepository, List<GameFactory> creators, GameRoomManager gameRoomManager, GameService gameService, ApplicationEventPublisher applicationEventPublisher) {
        this.counterRepository = counterRepository;
        this.gameRoomManager = gameRoomManager;
        this.gameService = gameService;
        this.applicationEventPublisher = applicationEventPublisher;
        this.creators = creators.stream().collect(Collectors.toMap(GameFactory::getSupportedType, creator -> creator));
    }

    @Transactional
    public Long createRoom(GameType gameType, String title, int maxPlayers, String hostName, GameCreateInfo createInfo) {
        Game game = creators.get(gameType).create(createInfo);
        CounterEntity counter = counterRepository.findByName(GAME_ROOM_COUNTER);
        counter.increment();

        gameRoomManager.createGameRoom(counter.getCount(), title, game, hostName, maxPlayers);
        applicationEventPublisher.publishEvent(new RoomChangedEvent());
        return counter.getCount();
    }

    public int joinRoom(Long roomId, String playerName) {
        log.info("roomId : {} , playerName : {}", roomId, playerName);
        JoinResult result = gameRoomManager.joinRoom(roomId, playerName);

        // 새로고침·재연결로 join 이 다시 호출될 수 있다. 실제로 방에 추가됐을 때만 알린다.
        if (result.newlyJoined()) {
            applicationEventPublisher.publishEvent(new PlayerJoinEvent(roomId, playerName));
        }

        return result.playerNumber();
    }

    public void leaveRoom(Long roomId, String playerName) {
        LeaveResult result = gameRoomManager.leaveRoom(roomId, playerName);

        if (result.destroyed()) {
            return;
        }

        if (result.wasPlaying()) {
            gameService.handlePlayerLeave(roomId, playerName);
        }

        applicationEventPublisher.publishEvent(new PlayerLeaveEvent(roomId, playerName));
    }

    public List<RoomInfo> findAllRooms() {
        var rooms = gameRoomManager.getRooms();
        return rooms.entrySet().stream()
                .map(room -> RoomInfo.from(room.getKey(), room.getValue()))
                .toList();
    }

    public PlayersInfo findUsers(Long roomId) {
        return gameRoomManager.findRoomUsers(roomId);
    }

    public void readyPlayer(Long roomId, String playerName) {
        ReadyResult result = gameRoomManager.readyPlayer(roomId, playerName);

        applicationEventPublisher.publishEvent(new PlayerReadyEvent(roomId, playerName, result.ready(), result.isAllReady()));
    }

    public void healthCheck(Long roomId) {
        gameRoomManager.healthCheck(roomId);
    }
}
