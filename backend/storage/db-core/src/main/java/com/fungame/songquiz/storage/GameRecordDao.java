package com.fungame.songquiz.storage;

import com.fungame.songquiz.enums.GameType;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class GameRecordDao {

    private static final String RECORD_GAME = """
            insert ignore into game_record (game_id, game_type, started_at, ended_at, started_player_count)
            values (?, ?, ?, ?, ?)
            """;

    private static final String RECORD_PLAYER = """
            insert into game_record_player (game_id, member_id, game_type, won, player_rank, score, left_game)
            values (?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String COUNT_BY_GAME_TYPE = """
            select game_type, count(*) as played, sum(case when won then 1 else 0 end) as won
            from game_record_player
            where member_id = ?
            group by game_type
            """;

    private static final int NOT_INSERTED = 0;

    private final JdbcTemplate jdbcTemplate;

    public boolean record(GameRecordRow game, List<GameRecordPlayerRow> players) {
        if (insertGame(game) == NOT_INSERTED) {
            return false;
        }

        jdbcTemplate.batchUpdate(RECORD_PLAYER, players, players.size(),
                (statement, player) -> bindPlayer(statement, game, player));
        return true;
    }

    private int insertGame(GameRecordRow game) {
        return jdbcTemplate.update(RECORD_GAME,
                game.gameId(),
                game.gameType().name(),
                Timestamp.from(game.startedAt()),
                Timestamp.from(game.endedAt()),
                game.startedPlayerCount());
    }

    private static void bindPlayer(PreparedStatement statement, GameRecordRow game, GameRecordPlayerRow player)
            throws SQLException {
        statement.setString(1, game.gameId());
        statement.setLong(2, player.memberId());
        statement.setString(3, game.gameType().name());
        statement.setBoolean(4, player.won());
        statement.setObject(5, player.rank(), Types.INTEGER);
        statement.setObject(6, player.score(), Types.INTEGER);
        statement.setBoolean(7, player.left());
    }

    public List<GameTypeRecordCount> countByGameType(Long memberId) {
        return jdbcTemplate.query(COUNT_BY_GAME_TYPE,
                (resultSet, rowNum) -> new GameTypeRecordCount(
                        GameType.valueOf(resultSet.getString("game_type")),
                        resultSet.getLong("played"),
                        resultSet.getLong("won")),
                memberId);
    }
}
