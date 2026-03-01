package com.fungame.songquiz.storage;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;

@Entity
@Getter
public class GameRoomCounterEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long counter;
}
