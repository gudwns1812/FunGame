package com.fungame.songquiz.domain.member;

import com.fungame.songquiz.enums.Role;
import com.fungame.songquiz.storage.IntegrationTest;
import com.fungame.songquiz.storage.MemberEntity;
import com.fungame.songquiz.storage.MemberRepository;
import com.fungame.songquiz.storage.PasswordResetTokenEntity;
import com.fungame.songquiz.storage.PasswordResetTokenRepository;
import com.fungame.songquiz.support.MutableClock;
import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.error.ErrorType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@IntegrationTest
class PasswordResetServiceTest {

    private static final String LOGIN_ID = "resetter";
    private static final String EMAIL = "resetter@fun-game.club";
    private static final String OLD_PASSWORD = "old1234";
    private static final String NEW_PASSWORD = "new1234";
    private static final Instant FIXED_INSTANT = Instant.parse("2026-08-13T00:00:00Z");
    private static final Duration MAIL_TIMEOUT = Duration.ofSeconds(3);

    @Autowired
    private PasswordResetService passwordResetService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private PasswordResetTokenGenerator passwordResetTokenGenerator;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @TestBean
    private Clock clock;

    static Clock clock() {
        return new MutableClock(FIXED_INSTANT, ZoneId.systemDefault());
    }

    @MockitoBean
    private PasswordResetMailSender mailSender;

    private MutableClock mutableClock;

    @BeforeEach
    void setUp() {
        mutableClock = (MutableClock) clock;
        clearMembers();
        memberRepository.save(MemberEntity.builder()
                .loginId(LOGIN_ID)
                .password(passwordEncoder.encode(OLD_PASSWORD))
                .nickname("재설정테스터")
                .email(EMAIL)
                .role(Role.USER)
                .build());
    }

    @AfterEach
    void tearDown() {
        clearMembers();
    }

    @Test
    @DisplayName("발급된 토큰은 원문이 아니라 해시로 저장된다.")
    void storesHashedToken() {
        passwordResetService.requestReset(LOGIN_ID, EMAIL);

        String rawToken = capturedRawToken();
        List<PasswordResetTokenEntity> tokens = passwordResetTokenRepository.findAll();

        assertThat(tokens).hasSize(1);
        assertThat(tokens.get(0).getTokenHash())
                .isNotEqualTo(rawToken)
                .isEqualTo(passwordResetTokenGenerator.hash(rawToken))
                .hasSize(64);
    }

    @Test
    @DisplayName("만료 시각은 발급 시점으로부터 5분 뒤로 잡힌다.")
    void expiresInFiveMinutes() {
        passwordResetService.requestReset(LOGIN_ID, EMAIL);

        LocalDateTime expected = LocalDateTime.now(clock).plus(PasswordResetTokenGenerator.TOKEN_TTL);

        assertThat(PasswordResetTokenGenerator.TOKEN_TTL).isEqualTo(Duration.ofMinutes(5));
        assertThat(passwordResetTokenRepository.findAll().get(0).getExpiresAt()).isEqualTo(expected);
    }

    @Test
    @DisplayName("만료된 토큰으로는 재설정할 수 없다.")
    void rejectsExpiredToken() {
        passwordResetService.requestReset(LOGIN_ID, EMAIL);
        String rawToken = capturedRawToken();

        mutableClock.plus(PasswordResetTokenGenerator.TOKEN_TTL.plusSeconds(1));

        assertThatThrownBy(() -> passwordResetService.resetPassword(rawToken, NEW_PASSWORD))
                .isInstanceOf(CoreException.class)
                .hasFieldOrPropertyWithValue("type", ErrorType.INVALID_PASSWORD_RESET_TOKEN);
    }

    @Test
    @DisplayName("한 번 쓴 토큰은 두 번째에 거부된다.")
    void rejectsUsedToken() {
        passwordResetService.requestReset(LOGIN_ID, EMAIL);
        String rawToken = capturedRawToken();

        passwordResetService.resetPassword(rawToken, NEW_PASSWORD);

        assertThatThrownBy(() -> passwordResetService.resetPassword(rawToken, "another1234"))
                .isInstanceOf(CoreException.class)
                .hasFieldOrPropertyWithValue("type", ErrorType.INVALID_PASSWORD_RESET_TOKEN);
    }

    @Test
    @DisplayName("재요청하면 이전 토큰은 무효가 된다.")
    void reissueInvalidatesPreviousToken() {
        passwordResetService.requestReset(LOGIN_ID, EMAIL);
        String firstToken = capturedRawToken();

        passwordResetService.requestReset(LOGIN_ID, EMAIL);

        assertThat(passwordResetTokenRepository.findAll()).hasSize(1);
        assertThatThrownBy(() -> passwordResetService.resetPassword(firstToken, NEW_PASSWORD))
                .isInstanceOf(CoreException.class)
                .hasFieldOrPropertyWithValue("type", ErrorType.INVALID_PASSWORD_RESET_TOKEN);
    }

    @Test
    @DisplayName("아이디와 이메일이 어긋나면 토큰도 메일도 생기지 않지만 예외는 던지지 않는다.")
    void mismatchedRequestIsSilentlyIgnored() {
        passwordResetService.requestReset(LOGIN_ID, "somebody-else@fun-game.club");

        assertThat(passwordResetTokenRepository.findAll()).isEmpty();
        verify(mailSender, never()).send(anyString(), anyString());
    }

    @Test
    @DisplayName("재설정에 성공하면 새 비밀번호로 로그인할 수 있다.")
    void changesPassword() {
        passwordResetService.requestReset(LOGIN_ID, EMAIL);

        passwordResetService.resetPassword(capturedRawToken(), NEW_PASSWORD);

        MemberEntity member = memberRepository.findByLoginId(LOGIN_ID).orElseThrow();
        assertThat(passwordEncoder.matches(NEW_PASSWORD, member.getPassword())).isTrue();
        assertThat(passwordEncoder.matches(OLD_PASSWORD, member.getPassword())).isFalse();
    }

    @Test
    @DisplayName("정책에 못 미치는 비밀번호로는 재설정할 수 없다.")
    void rejectsTooShortPassword() {
        passwordResetService.requestReset(LOGIN_ID, EMAIL);
        String rawToken = capturedRawToken();

        assertThatThrownBy(() -> passwordResetService.resetPassword(rawToken, "1"))
                .isInstanceOf(CoreException.class)
                .hasFieldOrPropertyWithValue("type", ErrorType.PASSWORD_POLICY_VIOLATION);
    }

    @Test
    @DisplayName("트랜잭션이 롤백되면 메일이 나가지 않는다.")
    void doesNotSendMailWhenTransactionRollsBack() {
        transactionTemplate.executeWithoutResult(status -> {
            passwordResetService.requestReset(LOGIN_ID, EMAIL);
            status.setRollbackOnly();
        });

        assertThat(passwordResetTokenRepository.findAll()).isEmpty();
        verify(mailSender, never()).send(anyString(), anyString());
    }

    @Test
    @DisplayName("만료된 지 하루가 지난 토큰은 청소된다.")
    void deletesLongExpiredTokens() {
        passwordResetService.requestReset(LOGIN_ID, EMAIL);

        mutableClock.plus(Duration.ofDays(2));
        passwordResetService.deleteExpiredTokens();

        assertThat(passwordResetTokenRepository.findAll()).isEmpty();
    }

    private String capturedRawToken() {
        ArgumentCaptor<String> link = ArgumentCaptor.forClass(String.class);
        verify(mailSender, timeout(MAIL_TIMEOUT.toMillis()).atLeastOnce()).send(any(), link.capture());

        String lastLink = link.getAllValues().get(link.getAllValues().size() - 1);
        return lastLink.substring(lastLink.indexOf("token=") + "token=".length());
    }

    private void clearMembers() {
        passwordResetTokenRepository.deleteAll();
        memberRepository.deleteAll();
    }
}
