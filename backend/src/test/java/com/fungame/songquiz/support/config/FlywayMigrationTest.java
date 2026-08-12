package com.fungame.songquiz.support.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("local")
@TestPropertySource(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate"
})
class FlywayMigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("마이그레이션으로 만든 스키마가 엔티티 매핑과 일치한다.")
    void migrationsMatchEntityMappings() {
        List<String> appliedVersions = jdbcTemplate.queryForList(
                "select \"version\" from \"flyway_schema_history\" where \"success\" = true and \"version\" is not null",
                String.class);

        assertThat(appliedVersions).contains("1");
    }

    @Test
    @DisplayName("엔티티가 기대하는 테이블이 모두 생성된다.")
    void everyMappedTableExists() {
        List<String> tables = jdbcTemplate.queryForList(
                "select lower(table_name) from information_schema.tables where table_schema = 'PUBLIC'",
                String.class);

        assertThat(tables).contains(
                "member",
                "promotion_request",
                "song_entity",
                "computer_science_entity",
                "counter_entity");
    }
}
