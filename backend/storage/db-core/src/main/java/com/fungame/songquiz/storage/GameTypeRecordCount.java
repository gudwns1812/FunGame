package com.fungame.songquiz.storage;

import com.fungame.songquiz.enums.GameType;

public record GameTypeRecordCount(GameType gameType, long played, long won) {
}
