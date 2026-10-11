package com.fungame.songquiz.domain.record;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GameRecordService {

    private final GameRecordReader gameRecordReader;

    public GameRecordSummary findMine(Long memberId) {
        return GameRecordSummary.of(gameRecordReader.countByGameType(memberId));
    }
}
