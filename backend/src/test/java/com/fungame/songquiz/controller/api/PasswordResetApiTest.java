package com.fungame.songquiz.controller.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fungame.songquiz.domain.member.Member;
import com.fungame.songquiz.domain.member.MemberRepository;
import com.fungame.songquiz.domain.member.PasswordResetTokenRepository;
import com.fungame.songquiz.support.MySqlIntegrationTest;
import com.fungame.songquiz.support.mail.PasswordResetMailSender;
import com.fungame.songquiz.enums.Role;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@MySqlIntegrationTest
@AutoConfigureMockMvc
class PasswordResetApiTest {

    private static final String LOGIN_ID = "sessionuser";
    private static final String EMAIL = "sessionuser@fun-game.club";
    private static final String PASSWORD = "old1234";
    private static final String NEW_PASSWORD = "new1234";
    private static final String SESSION_COOKIE = "SESSION";
    private static final long MAIL_TIMEOUT_MILLIS = 3_000;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private PasswordResetMailSender mailSender;

    @BeforeEach
    void setUp() {
        clearMembers();
        memberRepository.save(Member.builder()
                .loginId(LOGIN_ID)
                .password(passwordEncoder.encode(PASSWORD))
                .nickname("세션테스터")
                .email(EMAIL)
                .role(Role.USER)
                .build());
    }

    @AfterEach
    void tearDown() {
        clearMembers();
    }

    @Test
    @DisplayName("재설정 요청은 인증 없이 호출할 수 있다.")
    void requestIsOpenToAnonymous() throws Exception {
        mockMvc.perform(post("/api/auth/password-reset-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("loginId", LOGIN_ID, "email", EMAIL))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"));
    }

    @Test
    @DisplayName("아이디와 이메일이 일치하지 않아도 성공으로 응답한다.")
    void mismatchedRequestStillReturnsOk() throws Exception {
        mockMvc.perform(post("/api/auth/password-reset-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("loginId", LOGIN_ID, "email", "stranger@fun-game.club"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"));

        assertThat(passwordResetTokenRepository.findAll()).isEmpty();
        verify(mailSender, never()).send(any(), any());
    }

    @Test
    @DisplayName("잘못된 토큰으로 재설정하면 400 으로 응답한다.")
    void invalidTokenReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/password-reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("token", "존재하지-않는-토큰", "newPassword", NEW_PASSWORD))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("재설정에 성공하면 기존 세션으로는 더 이상 인증되지 않는다.")
    void resetExpiresExistingSessions() throws Exception {
        Cookie sessionCookie = login();

        mockMvc.perform(get("/api/auth/me").cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.loginId").value(LOGIN_ID));

        mockMvc.perform(post("/api/auth/password-reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("token", requestToken(), "newPassword", NEW_PASSWORD))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/auth/me").cookie(sessionCookie))
                .andExpect(status().isUnauthorized());
    }

    private Cookie login() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("loginId", LOGIN_ID, "password", PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();

        Cookie sessionCookie = result.getResponse().getCookie(SESSION_COOKIE);
        assertThat(sessionCookie).isNotNull();

        return sessionCookie;
    }

    private String requestToken() throws Exception {
        mockMvc.perform(post("/api/auth/password-reset-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("loginId", LOGIN_ID, "email", EMAIL))))
                .andExpect(status().isOk());

        ArgumentCaptor<String> link = ArgumentCaptor.forClass(String.class);
        verify(mailSender, timeout(MAIL_TIMEOUT_MILLIS).atLeastOnce()).send(any(), link.capture());

        String lastLink = link.getAllValues().get(link.getAllValues().size() - 1);
        return lastLink.substring(lastLink.indexOf("token=") + "token=".length());
    }

    private String body(Map<String, String> fields) throws Exception {
        return objectMapper.writeValueAsString(fields);
    }

    private void clearMembers() {
        passwordResetTokenRepository.deleteAll();
        memberRepository.deleteAll();
    }
}
