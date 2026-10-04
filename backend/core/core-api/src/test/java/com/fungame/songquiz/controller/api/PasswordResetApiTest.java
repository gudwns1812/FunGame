package com.fungame.songquiz.controller.api;

import com.fungame.songquiz.enums.Role;
import com.fungame.songquiz.storage.MemberEntity;
import com.fungame.songquiz.storage.MemberRepository;
import com.fungame.songquiz.storage.PasswordResetTokenRepository;
import com.fungame.songquiz.support.ApiIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

class PasswordResetApiTest extends ApiIntegrationTest {

    private static final String LOGIN_ID = "sessionuser";
    private static final String EMAIL = "sessionuser@fun-game.club";
    private static final String PASSWORD = "old1234";
    private static final String NEW_PASSWORD = "new1234";
    private static final long MAIL_TIMEOUT_MILLIS = 3_000;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        memberRepository.save(MemberEntity.builder()
                .loginId(LOGIN_ID)
                .password(passwordEncoder.encode(PASSWORD))
                .nickname("세션테스터")
                .email(EMAIL)
                .role(Role.USER)
                .build());
    }

    @Test
    @DisplayName("재설정 요청은 인증 없이 호출할 수 있다.")
    void requestIsOpenToAnonymous() {
        ResponseEntity<String> response = post("/api/auth/password-reset-request",
                Map.of("loginId", LOGIN_ID, "email", EMAIL));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"result\":\"SUCCESS\"");
    }

    @Test
    @DisplayName("아이디와 이메일이 일치하지 않아도 성공으로 응답한다.")
    void mismatchedRequestStillReturnsOk() {
        ResponseEntity<String> response = post("/api/auth/password-reset-request",
                Map.of("loginId", LOGIN_ID, "email", "stranger@fun-game.club"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"result\":\"SUCCESS\"");
        assertThat(passwordResetTokenRepository.findAll()).isEmpty();
        verify(mailSender, never()).send(any(), any());
    }

    @Test
    @DisplayName("잘못된 토큰으로 재설정하면 400 으로 응답한다.")
    void invalidTokenReturnsBadRequest() {
        ResponseEntity<String> response = post("/api/auth/password-reset",
                Map.of("token", "존재하지-않는-토큰", "newPassword", NEW_PASSWORD));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("재설정에 성공하면 기존 세션으로는 더 이상 인증되지 않는다.")
    void resetExpiresExistingSessions() {
        String sessionCookie = login();

        assertThat(me(sessionCookie).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(me(sessionCookie).getBody()).contains(LOGIN_ID);

        ResponseEntity<String> reset = post("/api/auth/password-reset",
                Map.of("token", requestToken(), "newPassword", NEW_PASSWORD));
        assertThat(reset.getStatusCode()).isEqualTo(HttpStatus.OK);

        assertThat(me(sessionCookie).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private ResponseEntity<String> post(String path, Map<String, String> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        return restTemplate.postForEntity(path, new HttpEntity<>(body, headers), String.class);
    }

    private ResponseEntity<String> me(String sessionCookie) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, sessionCookie);

        return restTemplate.exchange("/api/auth/me", HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }

    private String login() {
        ResponseEntity<String> response = post("/api/auth/login",
                Map.of("loginId", LOGIN_ID, "password", PASSWORD));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        String setCookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).isNotNull();

        return setCookie.split(";", 2)[0];
    }

    private String requestToken() {
        post("/api/auth/password-reset-request", Map.of("loginId", LOGIN_ID, "email", EMAIL));

        ArgumentCaptor<String> link = ArgumentCaptor.forClass(String.class);
        verify(mailSender, timeout(MAIL_TIMEOUT_MILLIS).atLeastOnce()).send(any(), link.capture());

        String lastLink = link.getAllValues().get(link.getAllValues().size() - 1);
        return lastLink.substring(lastLink.indexOf("token=") + "token=".length());
    }
}
