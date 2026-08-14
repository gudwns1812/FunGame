package com.fungame.songquiz.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class GameRoomStore {

    private final GameRoomRepository gameRoomRepository;

    public GameRoomEntity save(GameRoomEntity entity) {
        return gameRoomRepository.save(entity);
    }

    public void deleteById(Long id) {
        gameRoomRepository.deleteById(id);
    }

    public List<GameRoomEntity> findAllBy() {
        return gameRoomRepository.findAllBy();
    }

    public Optional<GameRoomEntity> findWithMembersById(Long id) {
        return gameRoomRepository.findWithMembersById(id);
    }
}
