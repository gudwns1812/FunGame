package com.fungame.songquiz.api;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fungame.songquiz.enums.Role;
import com.fungame.songquiz.storage.MemberEntity;
import com.fungame.songquiz.storage.MemberRepository;
import com.fungame.songquiz.support.ApiIntegrationTest;
import com.fungame.songquiz.support.logging.LogContext;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

class LogContextFilterChainTest extends ApiIntegrationTest {

    private static final String LOGIN_ID = "logcontext";
    private static final String PASSWORD = "password1";
    private static final String MISSING_ROOM_SETTINGS = "/game/rooms/999999/settings";

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();
    private Level originalLevel;

    @BeforeEach
    void listenToAdviceLogs() {
        originalLevel = adviceLogger().getLevel();
        adviceLogger().setLevel(Level.DEBUG);
        logs.start();
        adviceLogger().addAppender(logs);
    }

    @AfterEach
    void stopListening() {
        adviceLogger().detachAppender(logs);
        adviceLogger().setLevel(originalLevel);
        logs.stop();
    }

    @Test
    @DisplayName("보안 체인을 지난 요청의 로그에 요청 id 와 로그인한 회원 id 가 붙는다.")
    void a_request_through_the_security_chain_is_logged_with_its_context() {
        Long memberId = signUp();
        String session = login();

        ResponseEntity<String> response = restTemplate.exchange(MISSING_ROOM_SETTINGS, HttpMethod.GET,
                new HttpEntity<>(sessionHeaders(session)), String.class);

        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
        assertThat(logs.list).singleElement().satisfies(logged -> {
            Map<String, String> context = logged.getMDCPropertyMap();
            assertThat(context.get(LogContext.REQUEST_ID)).hasSize(8);
            assertThat(context).containsEntry(LogContext.MEMBER_ID, String.valueOf(memberId));
        });
    }

    private Long signUp() {
        return memberRepository.save(MemberEntity.builder()
                .loginId(LOGIN_ID)
                .password(passwordEncoder.encode(PASSWORD))
                .nickname(LOGIN_ID)
                .email(LOGIN_ID + "@fun-game.club")
                .role(Role.USER)
                .build()).getId();
    }

    private String login() {
        ResponseEntity<String> response = restTemplate.postForEntity("/api/auth/login",
                Map.of("loginId", LOGIN_ID, "password", PASSWORD), String.class);

        String sessionCookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(sessionCookie).as("로그인 응답에 세션 쿠키가 있어야 한다").isNotNull();

        return sessionCookie.split(";", 2)[0];
    }

    private static HttpHeaders sessionHeaders(String session) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, session);
        return headers;
    }

    private static Logger adviceLogger() {
        return (Logger) LoggerFactory.getLogger(ApiControllerAdvice.class);
    }
}
