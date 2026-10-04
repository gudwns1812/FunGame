package com.fungame.songquiz.support;

import com.fungame.songquiz.client.youtube.YoutubeScraper;
import com.fungame.songquiz.domain.session.GameTimer;
import com.fungame.songquiz.domain.member.PasswordResetMailSender;
import com.fungame.songquiz.storage.IntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import javax.sql.DataSource;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.ZoneId;

/**
 * core-api 통합 테스트의 공통 바탕.
 *
 * <p>스프링은 테스트 클래스가 아니라 **컨텍스트 설정**을 키로 컨텍스트를 캐시한다.
 * 클래스마다 바꿔치기하는 빈이 다르면 키가 갈려 애플리케이션을 통째로 다시 띄운다.
 * 한 번 띄우는 데 30초가 넘으므로, 바꿔치기할 빈을 여기 모아 키를 하나로 묶는다.
 *
 * <p>여기 올리는 것은 **모든 통합 테스트에서 가짜여야 하는 바깥 세계**뿐이다.
 * 특정 테스트만 가짜로 쓰고 싶은 빈을 올리면 다른 테스트의 검증을 조용히 무력화한다.
 *
 * <p>MockMvc 대신 실제 포트를 쓴다. MockMvc 는 {@code webEnvironment = MOCK} 에서만 제공되어,
 * 실제 포트가 필요한 WebSocket 테스트와 컨텍스트를 나눠 갖게 된다. 모두 실제 포트로 맞추면
 * 애플리케이션을 한 번만 띄운다.
 */
@IntegrationTest
@Import(TestEventCapture.class)
public abstract class ApiIntegrationTest {

    /**
     * 시계를 멈추되 실제 현재 시각에서 시작한다. 고정된 과거 시각으로 얼리면, 주입된 시계로
     * 만든 만료 시각과 {@code LocalDateTime.now()} 를 쓰는 코드가 어긋나 엉뚱하게 실패한다.
     *
     * <p>초 단위로 끊는다. DB 의 datetime 은 나노초를 버리므로, 끊지 않으면 저장한 값과
     * 계산한 값이 미세하게 달라진다.
     */
    private static Instant frozenNow() {
        return Instant.now().truncatedTo(ChronoUnit.SECONDS);
    }

    @MockitoBean
    protected PasswordResetMailSender mailSender;

    @MockitoBean
    protected YoutubeScraper youtubeScraper;

    @MockitoSpyBean
    protected GameTimer gameTimer;


    @TestBean
    protected Clock clock;

    static Clock clock() {
        return new MutableClock(frozenNow(), ZoneId.systemDefault());
    }

    protected MutableClock movableClock() {
        return (MutableClock) clock;
    }

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private ApplicationContext applicationContext;

    @AfterEach
    void cleanSharedState() {
        new SharedStateCleaner(applicationContext).clean();
        new DatabaseCleaner(dataSource).clean();
    }
}
