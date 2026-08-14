package com.fungame.songquiz.controller.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fungame.songquiz.domain.member.AuthService;
import com.fungame.songquiz.domain.member.PasswordResetService;
import com.fungame.songquiz.domain.member.Member;
import com.fungame.songquiz.controller.ApiControllerAdvice;
import com.fungame.songquiz.controller.request.LoginRequest;
import com.fungame.songquiz.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

    private MockMvc mockMvc;
    private final AuthService authService = mock(AuthService.class);
    private final PasswordResetService passwordResetService = mock(PasswordResetService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService, passwordResetService))
                .setControllerAdvice(new ApiControllerAdvice())
                .build();

        given(authService.getMyInfo(anyString())).willReturn(Member.builder()
                .loginId("tester")
                .password("encoded")
                .nickname("테스터")
                .email("tester@fun-game.club")
                .role(Role.USER)
                .build());
    }

    private String loginBody() throws Exception {
        return objectMapper.writeValueAsString(Map.of("loginId", "tester", "password", "pw1234"));
    }

    @Test
    @DisplayName("로그인에 성공하면 세션 고정 공격 방지를 위해 세션 ID를 교체한다")
    void login_changesSessionId() throws Exception {
        MockHttpSession session = new MockHttpSession();
        String sessionIdBeforeLogin = session.getId();

        mockMvc.perform(post("/api/auth/login")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody()))
                .andExpect(status().isOk());

        assertThat(session.getId()).isNotEqualTo(sessionIdBeforeLogin);
    }

    @Test
    @DisplayName("세션이 없는 상태로 로그인해도 예외 없이 성공한다")
    void login_withoutExistingSession_succeeds() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("자격 증명이 틀리면 500 이 아니라 401 로 응답한다")
    void login_withBadCredentials_returnsUnauthorized() throws Exception {
        willThrow(new BadCredentialsException("자격 증명에 실패하였습니다."))
                .given(authService).login(anyString(), anyString());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("M010"));
    }
}
