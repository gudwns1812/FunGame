package com.fungame.songquiz.storage;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GameRoomRepository extends JpaRepository<GameRoomEntity, Long> {

    @EntityGraph(attributePaths = "members")
    List<GameRoomEntity> findAllBy();

    @EntityGraph(attributePaths = "members")
    Optional<GameRoomEntity> findWithMembersById(Long id);
}
