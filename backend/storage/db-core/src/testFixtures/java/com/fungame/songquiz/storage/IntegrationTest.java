package com.fungame.songquiz.storage;

import com.fungame.songquiz.storage.redis.RedisTestContainer;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureObservability
@Import({MySqlTestContainer.class, RedisTestContainer.class})
@TestPropertySource(properties = {
        "spring.session.jdbc.initialize-schema=always",
        "app.song-scrape.enabled=false",
        "app.room.leave-grace-seconds=1",
        "management.server.port=0"
})
public @interface IntegrationTest {
}
