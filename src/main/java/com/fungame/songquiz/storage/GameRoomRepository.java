package com.fungame.songquiz.storage;

import java.util.List;
import org.springframework.data.repository.CrudRepository;

public interface GameRoomRepository extends CrudRepository<GameRoom, String> {

    List<GameRoom> findAll();
}
