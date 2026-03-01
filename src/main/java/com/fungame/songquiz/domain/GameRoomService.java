package com.fungame.songquiz.domain;

import com.fungame.songquiz.domain.dto.RoomInfo;
import com.fungame.songquiz.storage.GameRoomCounterEntity;
import com.fungame.songquiz.storage.GameRoomCounterRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GameRoomService {

    private final GameRoomCounterRepository gameRoomCounterRepository;
    private final SongReader songReader;
    private final ApplicationEventPublisher publisher;

    private final GameRoomManager gameRoomManager = new GameRoomManager();

    private static final String ROOM_ID_COUNTER = "room_id_counter";
    private static final String ROOM_LOCK_PREFIX = "room_lock:";

    public Long createRoom(String title, int maxPlayers, String hostName, Category category, int songCount) {
        List<Song> songs = songReader.findSongByCategoryWithCount(category, songCount);
        SongQuiz game = new SongQuiz(songs);

        GameRoomCounterEntity counter = gameRoomCounterRepository.save(new GameRoomCounterEntity());

        gameRoomManager.createGameRoom(counter.getCounter(), title, game, hostName, maxPlayers);
        return counter.getCounter();
    }

    public int joinRoom(Long roomId, String playerName) {
        return gameRoomManager.joinRoom(roomId, playerName);
    }

    public void leaveRoom(Long roomId, String playerName) {
        gameRoomManager.leaveRoom(roomId, playerName);
    }

    public List<RoomInfo> findAllRooms() {
        var rooms = gameRoomManager.getRooms();
        return rooms.entrySet().stream()
                .map(room -> RoomInfo.from(room.getKey(), room.getValue()))
                .toList();
    }

    public List<String> findUsers(Long roomId) {
        GameRoom room = gameRoomManager.getRoom(roomId);

        return room.getRoomPlayers();
    }
}
