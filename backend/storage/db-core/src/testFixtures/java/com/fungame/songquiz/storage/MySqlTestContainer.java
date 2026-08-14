package com.fungame.songquiz.storage;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.MySQLContainer;

@TestConfiguration(proxyBeanMethods = false)
public class MySqlTestContainer {

    private static final String IMAGE = "mysql:8.0";
    private static final String LOCK_WAIT_TIMEOUT_OPTION = "--innodb-lock-wait-timeout=5";

    @Bean
    @ServiceConnection
    static MySQLContainer<?> mysqlContainer() {
        return new MySQLContainer<>(IMAGE)
                .withCommand("mysqld", LOCK_WAIT_TIMEOUT_OPTION)
                .withReuse(true);
    }
}
