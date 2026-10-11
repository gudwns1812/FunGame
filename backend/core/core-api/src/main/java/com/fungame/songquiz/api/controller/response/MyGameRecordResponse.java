package com.fungame.songquiz.api.controller.response;

import com.fungame.songquiz.domain.record.GameRecordSummary;
import com.fungame.songquiz.domain.record.GameTypeRecord;
import com.fungame.songquiz.enums.GameType;
import java.util.List;

public record MyGameRecordResponse(
        GameType gameType,
        long played,
        long won,
        long lost,
        Integer winRate
) {

    public static MyGameRecordResponse from(GameTypeRecord record) {
        return new MyGameRecordResponse(record.gameType(), record.played(), record.won(), record.lost(),
                record.winRate());
    }

    public static List<MyGameRecordResponse> listFrom(GameRecordSummary summary) {
        return summary.byGameType().stream()
                .map(MyGameRecordResponse::from)
                .toList();
    }
}
