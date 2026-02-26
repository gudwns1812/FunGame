package com.fungame.songquiz.storage;

import static com.fungame.songquiz.storage.GameRoomStatus.FINISHED;
import static com.fungame.songquiz.storage.GameRoomStatus.PLAYING;

import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.error.ErrorType;
import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.redis.core.RedisHash;

@Getter
@Builder
@RedisHash(value = "GameRoom", timeToLive = 60 * 60 * 24)
public class GameRoom {

    @Id
    private String id;

    private String title;
    private GameRoomStatus status;

    private int maxPlayers;
    private int currentPlayers;

    public void addPlayer() {
        if (currentPlayers == maxPlayers) {
            throw new CoreException(ErrorType.GAME_ROOM_MAX_PLAYER_EXCEED);
        }

        currentPlayers++;
    }

    public void removePlayer() {
        currentPlayers--;
    }

    public void startGame() {
        status = PLAYING;
    }

    public void endGame() {
        status = FINISHED;
    }
}
