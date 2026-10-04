package com.fungame.songquiz.support;

import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.List;
import java.util.Set;

/**
 * 테스트가 남긴 행을 비운다.
 *
 * <p>통합 테스트가 컨텍스트를 공유하면 DB 도 공유한다. 앞선 테스트가 남긴 행은 외래키에 걸려
 * 다음 테스트의 정리를 막거나, 조회 결과에 섞여 들어와 엉뚱한 실패를 만든다.
 *
 * <p>비울 테이블을 적어두지 않는다. 스키마에서 전부 읽는다. 목록을 적어두면 테이블이 생기거나
 * 사라질 때 조용히 어긋난다.
 *
 * <p>마이그레이션이 심어둔 데이터도 함께 비운다. 테스트가 쓰는 데이터는 테스트가 심는 것이
 * 맞다 — 운영 데이터에 기대면 거기에 한 줄 더하는 것만으로 테스트가 빨개진다.
 * {@link #KEEP} 에 적은 둘만 남긴다.
 *
 * <p>{@code truncate} 가 아니라 {@code delete} 를 쓴다. InnoDB 의 truncate 는 테이블을
 * 드롭하고 다시 만드는 DDL 이라 빈 테이블에도 비싸다. 거의 비어 있는 테이블을 수백 번
 * 비우는 이 용도에는 delete 가 훨씬 싸다.
 */
public class DatabaseCleaner {

    private static final String FLYWAY_HISTORY = "flyway_schema_history";

    /**
     * 방 번호 채번 카운터. 비우면 count 가 0 으로 되감겨 이미 나간 방 번호를 다시 발급한다.
     * 마이그레이션은 행이 없을 때만 심으므로 한 번 지우면 돌아오지 않는다.
     */
    private static final String ROOM_NUMBER_COUNTER = "counter_entity";

    private static final Set<String> KEEP = Set.of(FLYWAY_HISTORY, ROOM_NUMBER_COUNTER);

    private static volatile List<String> tablesToClear;

    private final JdbcTemplate jdbcTemplate;

    public DatabaseCleaner(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public void clean() {
        List<String> tables = tablesToClear();

        jdbcTemplate.execute("set foreign_key_checks = 0");
        try {
            tables.forEach(table -> jdbcTemplate.update("delete from " + table));
        } finally {
            jdbcTemplate.execute("set foreign_key_checks = 1");
        }
    }

    /**
     * 테이블 목록은 한 실행 안에서 바뀌지 않는다. 매 테스트마다 information_schema 를 다시
     * 묻는 것만으로도 수백 번의 조회가 쌓이므로 한 번만 읽어 둔다.
     */
    private List<String> tablesToClear() {
        List<String> cached = tablesToClear;
        if (cached != null) {
            return cached;
        }

        cached = jdbcTemplate.queryForList(
                        "select table_name from information_schema.tables "
                                + "where table_schema = database() and table_type = 'BASE TABLE'",
                        String.class).stream()
                .filter(table -> !KEEP.contains(table.toLowerCase()))
                .toList();
        tablesToClear = cached;
        return cached;
    }
}
