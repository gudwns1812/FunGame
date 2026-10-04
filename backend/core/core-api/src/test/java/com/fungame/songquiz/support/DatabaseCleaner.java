package com.fungame.songquiz.support;

import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StreamUtils;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 테스트가 남긴 행을 비운다.
 *
 * <p>통합 테스트가 컨텍스트를 공유하면 DB 도 공유한다. 앞선 테스트가 남긴 행은 외래키에 걸려
 * 다음 테스트의 정리를 막거나, 조회 결과에 섞여 들어와 엉뚱한 실패를 만든다.
 *
 * <p>비울 테이블을 적어두지 않는다. 스키마에서 전부 읽되, 마이그레이션이 심는 참조 데이터는
 * 건드리지 않는다. 어느 테이블이 참조 데이터인지도 적어두지 않고 그 출처인
 * {@code R__reference_data.sql} 에서 읽는다. 목록을 적어두면 테이블이 생기거나 사라질 때
 * 조용히 어긋난다.
 *
 * <p>참조 데이터를 비웠다가 되살리는 쪽이 더 깔끔해 보이지만, 단어 수천 개를 매 테스트마다
 * 다시 넣느라 테스트당 3.5초가 붙었다. 테스트가 쓰지 않는 데이터는 그냥 두는 편이 낫다.
 *
 * <p>{@code truncate} 가 아니라 {@code delete} 를 쓴다. InnoDB 의 truncate 는 테이블을
 * 드롭하고 다시 만드는 DDL 이라 빈 테이블에도 비싸다. 거의 비어 있는 테이블을 수백 번
 * 비우는 이 용도에는 delete 가 훨씬 싸다.
 */
public class DatabaseCleaner {

    private static final String REFERENCE_DATA = "db/migration/R__reference_data.sql";
    private static final String FLYWAY_HISTORY = "flyway_schema_history";
    private static final Pattern TABLE_IN_SCRIPT =
            Pattern.compile("(?:insert into|delete from) ([a-zA-Z_][a-zA-Z0-9_]*)",
                    Pattern.CASE_INSENSITIVE);

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

        Set<String> keep = referenceDataTables();
        keep.add(FLYWAY_HISTORY);

        cached = jdbcTemplate.queryForList(
                        "select table_name from information_schema.tables "
                                + "where table_schema = database() and table_type = 'BASE TABLE'",
                        String.class).stream()
                .filter(table -> !keep.contains(table.toLowerCase()))
                .toList();
        tablesToClear = cached;
        return cached;
    }

    private static Set<String> referenceDataTables() {
        Set<String> tables = new LinkedHashSet<>();
        Matcher matcher = TABLE_IN_SCRIPT.matcher(readReferenceData());
        while (matcher.find()) {
            tables.add(matcher.group(1).toLowerCase());
        }
        return tables;
    }

    private static String readReferenceData() {
        try {
            return StreamUtils.copyToString(
                    new ClassPathResource(REFERENCE_DATA).getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("참조 데이터 스크립트를 읽지 못했다: " + REFERENCE_DATA, e);
        }
    }
}
