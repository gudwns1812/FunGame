package com.fungame.songquiz.domain.record;

import com.fungame.songquiz.domain.session.FinishedGame;
import com.fungame.songquiz.domain.session.GameOutcome;
import com.fungame.songquiz.domain.session.PlayerOutcome;
import com.fungame.songquiz.storage.GameRecordDao;
import com.fungame.songquiz.storage.GameRecordPlayerRow;
import com.fungame.songquiz.storage.GameRecordRow;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class GameRecordWriter {

    private final GameRecordDao gameRecordDao;
    private final Clock clock;

    @Transactional
    public void write(FinishedGame game) {
        if (!gameRecordDao.record(gameRowOf(game), playerRowsOf(game.outcome()))) {
            log.info("이미 전적으로 남은 판이다: {}", game.gameId());
        }
    }

    private GameRecordRow gameRowOf(FinishedGame game) {
        return new GameRecordRow(game.gameId(), game.gameType(), game.startedAt(), Instant.now(clock),
                game.outcome().startedPlayerCount());
    }

    private static List<GameRecordPlayerRow> playerRowsOf(GameOutcome outcome) {
        return outcome.players().stream()
                .map(GameRecordWriter::playerRowOf)
                .toList();
    }

    private static GameRecordPlayerRow playerRowOf(PlayerOutcome player) {
        return new GameRecordPlayerRow(player.memberId(), player.won(), player.rank(), player.score(), player.left());
    }
}
