package com.fungame.songquiz.domain;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GameRoomManager {
    private final Map<Long, GameRoom> gameRooms = new ConcurrentHashMap<>();

    public GameRoom getRoom(Long roomId) {
        return gameRooms.get(roomId);
    }

    public Map<Long, GameRoom> getRooms() {
        return Map.copyOf(gameRooms);
    }

    public void createGameRoom(Long roomId, String title, Game game, String host, int maxPlayer) {
        GameRoom gameRoom = GameRoom.create(title, game, List.of(host), maxPlayer, host);
        gameRooms.put(roomId, gameRoom);
    }

    public int joinRoom(Long roomId, String playerName) {
        GameRoom gameRoom = gameRooms.get(roomId);
        return gameRoom.addPlayer(playerName);
    }

    public void leaveRoom(Long roomId, String playerName) {
        GameRoom gameRoom = gameRooms.get(roomId);
        gameRoom.removePlayer(playerName);
    }
}
