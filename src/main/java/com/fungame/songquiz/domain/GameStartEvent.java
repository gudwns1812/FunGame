package com.fungame.songquiz.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class GameStartEvent {
    private final String roomId;
    private final List<Long> songIds;
}
