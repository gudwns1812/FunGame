package com.fungame.songquiz.storage;

import static com.fungame.songquiz.domain.GameRoomStatus.FINISHED;
import static com.fungame.songquiz.domain.GameRoomStatus.PLAYING;
import static com.fungame.songquiz.domain.GameRoomStatus.WAITING;

import com.fungame.songquiz.domain.Category;
import com.fungame.songquiz.domain.GameRoomStatus;
import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.error.ErrorType;
import jakarta.persistence.Id;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.redis.core.RedisHash;

@Getter
@Builder
@RedisHash(value = "GameRoom", timeToLive = 60 * 60 * 24)
public class GameRoomEntity {

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
    private int currentSongIndex;

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
        this.currentSongIndex = 0;
        status = PLAYING;
    }

    public Long getCurrentSongId() {
        if (status != PLAYING || songIds == null || currentSongIndex >= songIds.size()) {
            return null;
        }
        return songIds.get(currentSongIndex);
    }

    public boolean nextSong() {
        currentSongIndex++;
        if (currentSongIndex >= songIds.size()) {
            status = FINISHED;
            return true;
        }
        return false;
    }

    public void endGame() {
        status = FINISHED;
    }

    public boolean isFinished() {
        return status == FINISHED;
    }

    public boolean isHostName(String nickName) {
        return hostName.equals(nickName);
    }

    public boolean isEmpty() {
        return playerNames.isEmpty();
    }
}
