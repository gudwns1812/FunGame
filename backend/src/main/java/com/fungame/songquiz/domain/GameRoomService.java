package com.fungame.songquiz.domain;

import com.fungame.songquiz.domain.dto.PlayersInfo;
import com.fungame.songquiz.domain.dto.RoomInfo;
import com.fungame.songquiz.domain.event.PlayerJoinEvent;
import com.fungame.songquiz.domain.event.PlayerLeaveEvent;
import com.fungame.songquiz.domain.event.PlayerReadyEvent;
import com.fungame.songquiz.domain.gamecreator.GameCreateInfo;
import com.fungame.songquiz.storage.CounterEntity;
import com.fungame.songquiz.storage.CounterRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class GameRoomService {

    private static final String GAME_ROOM_COUNTER = "GAME_ROOM_COUNTER";

    private final CounterRepository counterRepository;
    private final Map<GameType, GameFactory> creators;
    private final GameRoomManager gameRoomManager;
    private final ApplicationEventPublisher applicationEventPublisher;

    public GameRoomService(CounterRepository counterRepository, List<GameFactory> creators, GameRoomManager gameRoomManager, ApplicationEventPublisher applicationEventPublisher) {
        this.counterRepository = counterRepository;
        this.gameRoomManager = gameRoomManager;
        this.applicationEventPublisher = applicationEventPublisher;
        this.creators = creators.stream().collect(Collectors.toMap(GameFactory::getSupportedType, creator -> creator));
    }

    @Transactional
    public Long createRoom(GameType gameType, String title, int maxPlayers, String hostName, GameCreateInfo createInfo) {
        Game game = creators.get(gameType).create(createInfo);
        CounterEntity counter = counterRepository.findByName(GAME_ROOM_COUNTER);
        counter.increment();

        gameRoomManager.createGameRoom(counter.getCount(), title, game, hostName, maxPlayers);
        return counter.getCount();
    }

    public int joinRoom(Long roomId, String playerName) {
        log.info("roomId : {} , playerName : {}", roomId, playerName);
        int playerNumber = gameRoomManager.joinRoom(roomId, playerName);
        applicationEventPublisher.publishEvent(new PlayerJoinEvent(roomId, playerName));

        return playerNumber;
    }

    public void leaveRoom(Long roomId, String playerName) {
        boolean isDestroy = gameRoomManager.leaveRoom(roomId, playerName);

        if (!isDestroy) {
            applicationEventPublisher.publishEvent(new PlayerLeaveEvent(roomId, playerName));
        }
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
        boolean isAllReady = gameRoomManager.readyPlayer(roomId, playerName);

        applicationEventPublisher.publishEvent(new PlayerReadyEvent(roomId, playerName, isAllReady));
    }
}
