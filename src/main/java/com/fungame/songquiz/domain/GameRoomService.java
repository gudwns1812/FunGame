package com.fungame.songquiz.domain;

import com.fungame.songquiz.domain.dto.RoomInfo;
import com.fungame.songquiz.storage.CounterEntity;
import com.fungame.songquiz.storage.CounterRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GameRoomService {

    private static final String GAME_ROOM_COUNTER = "GAME_ROOM_COUNTER";
    private final CounterRepository counterRepository;
    private final SongReader songReader;
    private final ApplicationEventPublisher publisher;

    private final GameRoomManager gameRoomManager = new GameRoomManager();

    private static final String ROOM_ID_COUNTER = "room_id_counter";
    private static final String ROOM_LOCK_PREFIX = "room_lock:";

    @Transactional
    public Long createRoom(String title, int maxPlayers, String hostName, Category category, int songCount) {
        List<Song> songs = songReader.findSongByCategoryWithCount(category, songCount);
        SongQuiz game = new SongQuiz(songs);

        CounterEntity counter = counterRepository.findByName(GAME_ROOM_COUNTER);
        counter.increment();

        gameRoomManager.createGameRoom(counter.getCount(), title, game, hostName, maxPlayers);
        return counter.getCount();
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
