package com.fungame.songquiz.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
    }

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(new ThrowingController())
                .setControllerAdvice(new ApiControllerAdvice())
                .build();
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
}
