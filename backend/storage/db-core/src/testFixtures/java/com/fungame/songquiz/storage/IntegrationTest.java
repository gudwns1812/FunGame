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
        // 운영은 MySQL 이라 Spring Session 이 스키마를 만들지 않는다. 테스트도 같은 조건으로 둬야
        // 세션 테이블이 마이그레이션에 없다는 것을 여기서 잡는다.
        "spring.session.jdbc.initialize-schema=never",
        "app.instance-id=itest",
        "app.song-scrape.enabled=false",
        "app.room.leave-grace-seconds=1",
        "management.server.port=0"
})
public @interface IntegrationTest {
}
