package com.fungame.songquiz.domain.member;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthServiceLoginMetricsTest {

    private MeterRegistry registry;
    private boolean 인증이_통과한다;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        인증이_통과한다 = true;
        SecurityContextHolder.clearContext();
    }

    private AuthService authService() {
        AuthenticationManager authenticationManager = authentication -> {
            if (!인증이_통과한다) {
                throw new BadCredentialsException("비밀번호가 다르다");
            }

            return new UsernamePasswordAuthenticationToken("놀이왕", "password");
        };

        return new AuthService(null, null, null, authenticationManager, new LoginMetrics(registry));
    }

    private double count(String result) {
        return registry.get(LoginMetrics.LOGIN_METER).tag(LoginMetrics.RESULT_TAG, result).counter().count();
    }

    @Test
    @DisplayName("로그인 시도가 없어도 성공·실패 카운터가 0 으로 존재한다.")
    void registersBothCountersUpFront() {
        authService();

        assertThat(count(LoginMetrics.SUCCESS)).isZero();
        assertThat(count(LoginMetrics.FAIL)).isZero();
    }

    @Test
    @DisplayName("로그인에 성공하면 success 가 오른다.")
    void countsSuccess() {
        authService().login("놀이왕", "password");

        assertThat(count(LoginMetrics.SUCCESS)).isEqualTo(1);
        assertThat(count(LoginMetrics.FAIL)).isZero();
    }

    @Test
    @DisplayName("인증에 실패하면 fail 이 오르고 예외는 그대로 밖으로 나간다.")
    void countsFailureAndRethrows() {
        인증이_통과한다 = false;

        assertThatThrownBy(() -> authService().login("놀이왕", "틀린비밀번호"))
                .isInstanceOf(BadCredentialsException.class);

        assertThat(count(LoginMetrics.FAIL)).isEqualTo(1);
        assertThat(count(LoginMetrics.SUCCESS)).isZero();
    }

    @Test
    @DisplayName("로그인에 성공하면 인증 정보가 시큐리티 컨텍스트에 남는다.")
    void keepsAuthenticationInContext() {
        authService().login("놀이왕", "password");

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getName()).isEqualTo("놀이왕");
    }
}
