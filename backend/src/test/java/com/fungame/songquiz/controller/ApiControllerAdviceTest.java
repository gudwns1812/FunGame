package com.fungame.songquiz.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApiControllerAdviceTest {

    @RestController
    static class ThrowingController {

        @GetMapping("/async-timeout")
        void asyncTimeout() {
            throw new AsyncRequestTimeoutException();
        }

        @GetMapping("/async-not-usable")
        void asyncNotUsable() throws AsyncRequestNotUsableException {
            throw new AsyncRequestNotUsableException("연결이 끊겼습니다.");
        }

        @GetMapping("/boom")
        void boom() {
            throw new IllegalStateException("예상하지 못한 오류");
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
    @DisplayName("SSE 타임아웃은 이미 커밋된 응답에 본문을 덧붙이지 않는다.")
    void asyncTimeoutWritesNothing() throws Exception {
        mockMvc.perform(get("/async-timeout").accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    @Test
    @DisplayName("끊긴 연결에 쓰지 못한 경우에도 본문을 덧붙이지 않는다.")
    void asyncNotUsableWritesNothing() throws Exception {
        mockMvc.perform(get("/async-not-usable").accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    @Test
    @DisplayName("그 밖의 예외는 여전히 500 과 실패 응답을 내려준다.")
    void unexpectedExceptionStillFails() throws Exception {
        mockMvc.perform(get("/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.result").value("FAIL"))
                .andExpect(jsonPath("$.error.code").value("E500"));
    }
}
