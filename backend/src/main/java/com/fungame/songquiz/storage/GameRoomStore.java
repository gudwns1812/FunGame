package com.fungame.songquiz.storage;

import com.fungame.songquiz.domain.GamePlayer;
import com.fungame.songquiz.domain.GameRoomStatus;
import com.fungame.songquiz.domain.GameRoom;
import com.fungame.songquiz.domain.RoomSettings;
import com.fungame.songquiz.domain.StoredRoom;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class GameRoomStore {

    private final GameRoomRepository gameRoomRepository;

    @Transactional
    public Long open(RoomSettings settings, String host) {
        GameRoomEntity entity = GameRoomEntity.open(settings, host);
        entity.syncMembers(List.of(new GamePlayer(host, true)));

        return gameRoomRepository.save(entity).getId();
    }

    @Transactional
    public void save(Long roomId, GameRoom room) {
        gameRoomRepository.findWithMembersById(roomId).ifPresent(entity -> {
            entity.applySettings(room.getSettings());
            entity.changeStatus(room.getStatus());
            entity.changeHost(room.getPlayers().getHost());
            entity.touch(room.getLastActivityTime());
            entity.syncMembers(room.getPlayers().snapshot());
        });
    }

    @Transactional
    public void markInterruptedGamesWaiting() {
        gameRoomRepository.findAllBy().stream()
                .filter(entity -> entity.getStatus() == GameRoomStatus.PLAYING)
                .forEach(entity -> entity.changeStatus(GameRoomStatus.WAITING));
    }

    @Transactional
    public void delete(Long roomId) {
        gameRoomRepository.deleteById(roomId);
    }

    @Transactional(readOnly = true)
    public Optional<StoredRoom> load(Long roomId) {
        return gameRoomRepository.findWithMembersById(roomId).map(GameRoomStore::toStoredRoom);
    }

    @Transactional(readOnly = true)
    public List<StoredRoom> loadAll() {
        return gameRoomRepository.findAllBy().stream()
                .map(GameRoomStore::toStoredRoom)
                .toList();
    }

    private static StoredRoom toStoredRoom(GameRoomEntity entity) {
        List<GamePlayer> players = entity.getMembers().stream()
                .map(member -> new GamePlayer(member.getNickname(), member.isReady()))
                .toList();

        return new StoredRoom(
                entity.getId(),
                entity.toSettings(),
                entity.getStatus(),
                entity.getHostNickname(),
                players,
                entity.getLastActivityTime());
    }
}
