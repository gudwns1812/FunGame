package com.fungame.songquiz.storage;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.MySQLContainer;

import java.util.Map;

@TestConfiguration(proxyBeanMethods = false)
public class MySqlTestContainer {

    private static final String IMAGE = "mysql:8.0";
    private static final String DATA_DIR = "/var/lib/mysql";

    private static final String[] OPTIONS = {
            "mysqld",
            "--innodb-lock-wait-timeout=5",
            "--skip-log-bin",
            "--skip-performance-schema",
            "--innodb-flush-log-at-trx-commit=0",
            "--innodb-doublewrite=0",
            "--innodb-flush-method=nosync"
    };

    @Bean
    @ServiceConnection
    static MySQLContainer<?> mysqlContainer() {
        return new MySQLContainer<>(IMAGE)
                .withCommand(OPTIONS)
                .withTmpFs(Map.of(DATA_DIR, "rw"))
                .withReuse(true);
    }
}
