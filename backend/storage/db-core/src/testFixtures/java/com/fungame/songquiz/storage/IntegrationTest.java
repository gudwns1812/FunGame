package com.fungame.songquiz.storage;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest
@Import(MySqlTestContainer.class)
@TestPropertySource(properties = {
        "spring.session.jdbc.initialize-schema=always",
        "app.song-scrape.enabled=false"
})
public @interface IntegrationTest {
}
