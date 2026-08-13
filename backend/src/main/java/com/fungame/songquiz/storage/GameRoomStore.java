package com.fungame.songquiz.storage;

import com.fungame.songquiz.domain.GamePlayer;
import com.fungame.songquiz.domain.GameRoom;
import com.fungame.songquiz.domain.GameRoomStatus;
import com.fungame.songquiz.domain.RoomSettings;
import com.fungame.songquiz.domain.StoredRoom;
import com.fungame.songquiz.domain.member.Member;
import com.fungame.songquiz.domain.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class GameRoomStore {

    private final GameRoomRepository gameRoomRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public Long open(RoomSettings settings, GamePlayer host) {
        GameRoomEntity entity = GameRoomEntity.open(settings, host.memberId());
        entity.syncMembers(List.of(host.setReady(true)));

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
        return gameRoomRepository.findWithMembersById(roomId)
                .map(entity -> toStoredRoom(entity, findNicknames(List.of(entity))));
    }

    @Transactional(readOnly = true)
    public List<StoredRoom> loadAll() {
        List<GameRoomEntity> entities = gameRoomRepository.findAllBy();
        Map<Long, String> nicknames = findNicknames(entities);

        return entities.stream()
                .map(entity -> toStoredRoom(entity, nicknames))
                .toList();
    }

    private Map<Long, String> findNicknames(Collection<GameRoomEntity> entities) {
        Set<Long> memberIds = entities.stream()
                .flatMap(entity -> entity.getMembers().stream())
                .map(GameRoomMemberEntity::getMemberId)
                .collect(Collectors.toSet());

        return memberRepository.findAllById(memberIds).stream()
                .collect(Collectors.toMap(Member::getId, Member::getNickname));
    }

    private static StoredRoom toStoredRoom(GameRoomEntity entity, Map<Long, String> nicknames) {
        List<GamePlayer> players = entity.getMembers().stream()
                .map(member -> new GamePlayer(
                        member.getMemberId(),
                        nicknames.get(member.getMemberId()),
                        member.isReady()))
                .filter(player -> player.nickname() != null)
                .toList();

        return new StoredRoom(
                entity.getId(),
                entity.toSettings(),
                entity.getStatus(),
                entity.getHostMemberId(),
                players,
                entity.getLastActivityTime());
    }
}
