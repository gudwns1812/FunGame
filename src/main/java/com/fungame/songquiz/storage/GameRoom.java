package com.fungame.songquiz.storage;

import static com.fungame.songquiz.storage.GameRoomStatus.PLAYING;
import static com.fungame.songquiz.storage.GameRoomStatus.WAITING;

import com.fungame.songquiz.domain.Category;
import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.error.ErrorType;
import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.redis.core.RedisHash;

import java.util.List;

@Getter
@Builder
@RedisHash(value = "GameRoom", timeToLive = 60 * 60 * 24)
public class GameRoom {

    @Id
    private String id;

    private String title;
    private String hostName;
    private GameRoomStatus status;

    private List<String> playerNames;

    private int maxPlayers;

    private Category category;
    private int count;
    private List<Long> songIds;

    public void addPlayer(String playerName) {
        if (playerNames.size() == maxPlayers) {
            throw new CoreException(ErrorType.GAME_ROOM_MAX_PLAYER_EXCEED);
        }

        playerNames.add(playerName);
    }

    public String removePlayer(String playerName) {
        if (playerNames.isEmpty()) {
            throw new CoreException(ErrorType.GAME_ROOM_PLAYER_EMPTY);
        }

        playerNames.remove(playerName);

        if (playerNames.isEmpty()) {
            return null;
        }

        if (isHostName(playerName)) {
            hostName = playerNames.getFirst();
            return hostName;
        }

        return null;
    }

    public void startGame(List<Long> songIds) {
        if (status != WAITING) {
            throw new CoreException(ErrorType.GAME_ROOM_ALREADY_PLAYING);
        }
        this.songIds = songIds;
        status = PLAYING;
    }

    public void endGame() {
        status = WAITING;
    }

    public boolean isHostName(String nickName) {
        return hostName.equals(nickName);
    }

    public boolean isEmpty() {
        return playerNames.isEmpty();
    }
}
