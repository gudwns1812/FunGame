package com.fungame.songquiz.domain.room;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fungame.songquiz.domain.session.GameSession;
import com.fungame.songquiz.domain.session.GameSnapshot;
import com.fungame.songquiz.storage.redis.GameRoomDao;
import com.fungame.songquiz.storage.redis.StoredRoom;
import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.error.ErrorType;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoomStore {

    private static final int MAX_ATTEMPTS = 16;

    private final GameRoomDao gameRoomDao;
    private final ObjectMapper objectMapper;

    public void create(GameRoom room) {
        if (!gameRoomDao.create(room.getRoomId(), bodyOf(room), memberIdsOf(room))) {
            throw new IllegalStateException("이미 쓰고 있는 방 번호다: " + room.getRoomId());
        }
    }

    public Optional<GameRoom> find(Long roomId) {
        return gameRoomDao.find(roomId).flatMap(this::readableRoomOf);
    }

    public Optional<RoomTable> findTable(Long roomId) {
        return gameRoomDao.find(roomId).flatMap(this::readableTableOf);
    }

    public List<GameRoom> findAll() {
        return gameRoomDao.findAll().stream()
                .map(this::readableRoomOf)
                .flatMap(Optional::stream)
                .toList();
    }

    public Optional<Long> roomIdOf(Long memberId) {
        return gameRoomDao.roomIdOf(memberId);
    }

    public <T> T update(Long roomId, Function<GameRoom, T> change) {
        return updateTable(roomId, table -> change.apply(table.room()));
    }

    public <T> T updateTable(Long roomId, Function<RoomTable, T> change) {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            StoredRoom stored = gameRoomDao.find(roomId)
                    .orElseThrow(() -> new CoreException(ErrorType.GAME_ROOM_NOT_FOUND));
            RoomTable table = readableTableOf(stored)
                    .orElseThrow(() -> new CoreException(ErrorType.GAME_ROOM_NOT_FOUND));
            Set<Long> membersBefore = memberIdsOf(table.room());

            T result = change.apply(table);

            if (isUnchanged(stored, table) || write(stored, table, membersBefore)) {
                return result;
            }
        }

        throw new IllegalStateException("방 " + roomId + " 을 " + MAX_ATTEMPTS + "번 연달아 다른 쓰기에 빼앗겼다");
    }

    public boolean removeIf(Long roomId, Predicate<GameRoom> condition) {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            Optional<StoredRoom> stored = gameRoomDao.find(roomId);
            if (stored.isEmpty()) {
                return false;
            }

            Optional<GameRoom> room = readableRoomOf(stored.get());
            if (room.isEmpty() || !condition.test(room.get())) {
                return false;
            }

            if (gameRoomDao.delete(roomId, stored.get().revision(), memberIdsOf(room.get()))) {
                return true;
            }
        }

        return false;
    }

    public List<Long> removeUnreadable() {
        return gameRoomDao.findAll().stream()
                .filter(stored -> readableTableOf(stored).isEmpty())
                .filter(stored -> gameRoomDao.delete(stored.roomId(), stored.revision(), Set.of()))
                .map(StoredRoom::roomId)
                .toList();
    }

    private boolean isUnchanged(StoredRoom stored, RoomTable table) {
        return stored.body().equals(bodyOf(table.room())) && Objects.equals(stored.game(), gameOf(table));
    }

    private boolean write(StoredRoom stored, RoomTable table, Set<Long> membersBefore) {
        GameRoom room = table.room();
        if (room.isEmpty()) {
            return gameRoomDao.delete(stored.roomId(), stored.revision(), membersBefore);
        }

        Set<Long> membersAfter = memberIdsOf(room);

        return gameRoomDao.replace(stored.roomId(), stored.revision(), bodyOf(room), gameOf(table),
                difference(membersAfter, membersBefore), difference(membersBefore, membersAfter));
    }

    private static Set<Long> difference(Set<Long> from, Set<Long> removing) {
        Set<Long> remaining = new HashSet<>(from);
        remaining.removeAll(removing);
        return remaining;
    }

    private static Set<Long> memberIdsOf(GameRoom room) {
        return room.getRoomPlayers().stream()
                .map(GamePlayer::memberId)
                .collect(Collectors.toSet());
    }

    private String bodyOf(GameRoom room) {
        return json(room.snapshot(), room.getRoomId());
    }

    private String gameOf(RoomTable table) {
        return table.game()
                .map(game -> json(game.snapshot(), table.room().getRoomId()))
                .orElse(null);
    }

    private Optional<RoomTable> readableTableOf(StoredRoom stored) {
        return readable(stored, () -> {
            GameSession game = stored.game() == null
                    ? null
                    : GameSession.restore(objectMapper.readValue(stored.game(), GameSnapshot.class));

            return new RoomTable(roomOf(stored), game);
        });
    }

    private Optional<GameRoom> readableRoomOf(StoredRoom stored) {
        return readable(stored, () -> roomOf(stored));
    }

    private <T> Optional<T> readable(StoredRoom stored, Callable<T> reading) {
        try {
            return Optional.of(reading.call());
        } catch (Exception e) {
            log.warn("저장된 방 {} 을 읽지 못해 없는 방으로 본다", stored.roomId(), e);
            return Optional.empty();
        }
    }

    private GameRoom roomOf(StoredRoom stored) throws JsonProcessingException {
        return GameRoom.restore(objectMapper.readValue(stored.body(), RoomSnapshot.class));
    }

    private String json(Object snapshot, Long roomId) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("방 " + roomId + " 을 저장할 형태로 바꾸지 못했다", e);
        }
    }

}
