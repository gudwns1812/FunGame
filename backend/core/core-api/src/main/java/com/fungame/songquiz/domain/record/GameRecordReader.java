package com.fungame.songquiz.domain.record;

import com.fungame.songquiz.storage.GameRecordDao;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class GameRecordReader {

    private final GameRecordDao gameRecordDao;

    @Transactional(readOnly = true)
    public List<GameTypeRecord> countByGameType(Long memberId) {
        return gameRecordDao.countByGameType(memberId).stream()
                .map(count -> new GameTypeRecord(count.gameType(), count.played(), count.won()))
                .toList();
    }
}
