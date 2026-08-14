package com.fungame.songquiz.domain;

import com.fungame.songquiz.storage.MemberEntity;
import com.fungame.songquiz.storage.MemberRepository;
import com.fungame.songquiz.storage.GameRoomEntity;
import com.fungame.songquiz.storage.GameRoomMemberEntity;
import com.fungame.songquiz.storage.GameRoomRepository;
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
public class GameRoomReader {

    private final GameRoomRepository gameRoomRepository;
    private final MemberRepository memberRepository;

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
                .collect(Collectors.toMap(MemberEntity::getId, MemberEntity::getNickname));
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
                toRoomSettings(entity.toSettings()),
                entity.getStatus(),
                entity.getHostMemberId(),
                players,
                entity.getLastActivityTime());
    }

    private static RoomSettings toRoomSettings(GameRoomEntity.Settings settings) {
        return new RoomSettings(
                settings.gameType(),
                settings.title(),
                settings.maxPlayer(),
                settings.category(),
                settings.totalRound(),
                settings.difficulty(),
                settings.csDifficulty());
    }
}
