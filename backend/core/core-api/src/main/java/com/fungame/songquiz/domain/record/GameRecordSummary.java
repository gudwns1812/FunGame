package com.fungame.songquiz.domain.record;

import com.fungame.songquiz.enums.GameType;
import java.util.List;

public record GameRecordSummary(List<GameTypeRecord> byGameType) {

    private static final List<GameType> RECORDED_GAME_TYPES = List.of(GameType.SONG, GameType.CS, GameType.HANGMAN);

    public static GameRecordSummary of(List<GameTypeRecord> counted) {
        return new GameRecordSummary(RECORDED_GAME_TYPES.stream()
                .map(gameType -> countedOrUnplayed(counted, gameType))
                .toList());
    }

    private static GameTypeRecord countedOrUnplayed(List<GameTypeRecord> counted, GameType gameType) {
        return counted.stream()
                .filter(record -> record.gameType() == gameType)
                .findFirst()
                .orElseGet(() -> GameTypeRecord.unplayed(gameType));
    }
}
