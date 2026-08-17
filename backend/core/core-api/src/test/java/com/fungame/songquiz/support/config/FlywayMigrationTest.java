package com.fungame.songquiz.support.config;

import com.fungame.songquiz.storage.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class FlywayMigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("마이그레이션이 적용된 상태로 기동한다.")
    void migrationsAreApplied() {
        List<String> appliedVersions = jdbcTemplate.queryForList(
                "select version from flyway_schema_history where success = true and version is not null",
                String.class);

        assertThat(appliedVersions).contains("1", "4");
    }

    @Test
    @DisplayName("엔티티가 기대하는 테이블이 모두 생성된다.")
    void everyMappedTableExists() {
        List<String> tables = jdbcTemplate.queryForList(
                "select lower(table_name) from information_schema.tables where table_schema = database()",
                String.class);

        assertThat(tables).contains(
                "member",
                "promotion_request",
                "song_entity",
                "computer_science_entity",
                "counter_entity",
                "password_reset_token",
                "report",
                "report_comment");
    }

    @Test
    @DisplayName("member.email 은 NOT NULL 이고 유일하다.")
    void memberEmailIsNotNullAndUnique() {
        String nullable = jdbcTemplate.queryForObject(
                "select is_nullable from information_schema.columns "
                        + "where table_schema = database() and table_name = 'member' and column_name = 'email'",
                String.class);

        List<String> uniqueIndexes = jdbcTemplate.queryForList(
                "select index_name from information_schema.statistics "
                        + "where table_schema = database() and table_name = 'member' and non_unique = 0",
                String.class);

        assertThat(nullable).isEqualTo("NO");
        assertThat(uniqueIndexes).contains("uk_member_email");
    }
}
