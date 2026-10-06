package com.fungame.songquiz.domain.room;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fungame.songquiz.storage.redis.GameRoomDao;
import com.fungame.songquiz.storage.redis.StoredRoom;
import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.error.ErrorType;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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
        return gameRoomDao.find(roomId).map(this::roomOf);
    }

    public List<GameRoom> findAll() {
        return gameRoomDao.findAll().stream()
                .map(this::roomOf)
                .toList();
    }

    public Optional<Long> roomIdOf(Long memberId) {
        return gameRoomDao.roomIdOf(memberId);
    }

    public <T> T update(Long roomId, Function<GameRoom, T> change) {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            StoredRoom stored = gameRoomDao.find(roomId)
                    .orElseThrow(() -> new CoreException(ErrorType.GAME_ROOM_NOT_FOUND));
            GameRoom room = roomOf(stored);
            Set<Long> membersBefore = memberIdsOf(room);

            T result = change.apply(room);

            if (write(stored, room, membersBefore)) {
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

            GameRoom room = roomOf(stored.get());
            if (!condition.test(room)) {
                return false;
            }

            if (gameRoomDao.delete(roomId, stored.get().revision(), memberIdsOf(room))) {
                return true;
            }
        }

        return false;
    }

    private boolean write(StoredRoom stored, GameRoom room, Set<Long> membersBefore) {
        if (room.isEmpty()) {
            return gameRoomDao.delete(stored.roomId(), stored.revision(), membersBefore);
        }

        Set<Long> membersAfter = memberIdsOf(room);

        return gameRoomDao.replace(stored.roomId(), stored.revision(), bodyOf(room),
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
        try {
            return objectMapper.writeValueAsString(room.snapshot());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("방 " + room.getRoomId() + " 을 저장할 형태로 바꾸지 못했다", e);
        }
    }

    private GameRoom roomOf(StoredRoom stored) {
        try {
            return GameRoom.restore(objectMapper.readValue(stored.body(), RoomSnapshot.class));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("저장된 방 " + stored.roomId() + " 을 읽지 못했다", e);
        }
    }
}
