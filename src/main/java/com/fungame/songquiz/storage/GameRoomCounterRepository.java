package com.fungame.songquiz.storage;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GameRoomCounterRepository extends JpaRepository<GameRoomCounterEntity, Long> {
}
