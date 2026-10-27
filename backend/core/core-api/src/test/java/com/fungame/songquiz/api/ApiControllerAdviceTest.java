package com.fungame.songquiz.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RestController;

class ApiControllerAdviceTest {

    @RestController
    static class ThrowingController {

        @GetMapping("/boom")
        void boom() {
            throw new IllegalStateException("예상하지 못한 오류");
        }

        @GetMapping("/bad-credentials")
        void badCredentials() {
            throw new BadCredentialsException("자격 증명에 실패하였습니다.");
        }

        @GetMapping("/bind")
        void bind(@ModelAttribute AgeForm form) {
        }
    }

    record AgeForm(Integer age) {
    }

    private static final String TYPED_BY_USER = "leak@fun-game.club";

    private MockMvc mockMvc;
    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(new ThrowingController())
                .setControllerAdvice(new ApiControllerAdvice())
                .build();
        logs.start();
        adviceLogger().addAppender(logs);
    }

    @AfterEach
    void stopListening() {
        adviceLogger().detachAppender(logs);
        logs.stop();
    }

    private static Logger adviceLogger() {
        return (Logger) LoggerFactory.getLogger(ApiControllerAdvice.class);
    }

    @Test
    @DisplayName("그 밖의 예외는 여전히 500 과 실패 응답을 내려준다.")
    void unexpectedExceptionStillFails() throws Exception {
        mockMvc.perform(get("/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.result").value("FAIL"))
                .andExpect(jsonPath("$.error.code").value("E500"));
    }

    @Test
    @DisplayName("로그인 실패는 서버 오류가 아니라 401 로 응답한다.")
    void badCredentialsReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/bad-credentials"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.result").value("FAIL"))
                .andExpect(jsonPath("$.error.code").value("M010"));
    }

    @Test
    @DisplayName("입력값 바인딩 실패는 어느 필드가 왜 거부됐는지만 남기고, 사용자가 넣은 값은 남기지 않는다.")
    void a_bind_failure_is_logged_without_the_rejected_value() throws Exception {
        mockMvc.perform(get("/bind").param("age", TYPED_BY_USER))
                .andExpect(status().isBadRequest());

        assertThat(logs.list).singleElement().satisfies(logged -> {
            assertThat(logged.getLevel()).isEqualTo(Level.WARN);
            assertThat(logged.getFormattedMessage())
                    .contains("age", "typeMismatch")
                    .doesNotContain(TYPED_BY_USER);
        });
    }
}
