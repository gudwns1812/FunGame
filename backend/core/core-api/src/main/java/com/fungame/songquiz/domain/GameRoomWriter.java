package com.fungame.songquiz.domain;

import com.fungame.songquiz.enums.GameRoomStatus;
import com.fungame.songquiz.storage.GameRoomEntity;
import com.fungame.songquiz.storage.GameRoomStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class GameRoomWriter {

    private final GameRoomStore gameRoomStore;

    @Transactional
    public Long open(RoomSettings settings, GamePlayer host) {
        GameRoomEntity entity = GameRoomEntity.open(toEntitySettings(settings), host.memberId());
        entity.syncMembers(toMemberStates(List.of(host.setReady(true))));

        return gameRoomStore.save(entity).getId();
    }

    @Transactional
    public void save(Long roomId, GameRoom room) {
        gameRoomStore.findWithMembersById(roomId).ifPresent(entity -> {
            entity.applySettings(toEntitySettings(room.getSettings()));
            entity.changeStatus(room.getStatus());
            entity.changeHost(room.getPlayers().getHost());
            entity.touch(room.getLastActivityTime());
            entity.syncMembers(toMemberStates(room.getPlayers().snapshot()));
        });
    }

    @Transactional
    public void markInterruptedGamesWaiting() {
        gameRoomStore.findAllBy().stream()
                .filter(entity -> entity.getStatus() == GameRoomStatus.PLAYING)
                .forEach(entity -> entity.changeStatus(GameRoomStatus.WAITING));
    }

    @Transactional
    public void delete(Long roomId) {
        gameRoomStore.deleteById(roomId);
    }

    private static GameRoomEntity.Settings toEntitySettings(RoomSettings settings) {
        return new GameRoomEntity.Settings(
                settings.gameType(),
                settings.title(),
                settings.maxPlayers(),
                settings.category(),
                settings.totalRound(),
                settings.difficulty(),
                settings.csDifficulty());
    }

    private static List<GameRoomEntity.MemberState> toMemberStates(List<GamePlayer> players) {
        return players.stream()
                .map(player -> new GameRoomEntity.MemberState(player.memberId(), player.isReady()))
                .toList();
    }
}
