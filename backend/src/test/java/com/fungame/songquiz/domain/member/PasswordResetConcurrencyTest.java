package com.fungame.songquiz.domain.member;

import com.fungame.songquiz.support.MySqlIntegrationTest;
import com.fungame.songquiz.support.mail.PasswordResetMailSender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@MySqlIntegrationTest
class PasswordResetConcurrencyTest {

    private static final String LOGIN_ID = "racer";
    private static final String EMAIL = "racer@fun-game.club";
    private static final String PASSWORD = "old1234";
    private static final int CONCURRENCY = 2;
    private static final int AWAIT_SECONDS = 20;
    private static final long MAIL_TIMEOUT_MILLIS = 3_000;

    @Autowired
    private PasswordResetService passwordResetService;

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
                .nickname("동시성테스터")
                .email(EMAIL)
                .role(Role.USER)
                .build());
    }

    @AfterEach
    void tearDown() {
        clearMembers();
    }

    @Test
    @DisplayName("같은 토큰으로 동시에 재설정하면 정확히 하나만 성공한다.")
    void onlyOneResetSucceedsForTheSameToken() throws Exception {
        String rawToken = issueToken();

        List<Throwable> failures = new CopyOnWriteArrayList<>();
        AtomicInteger successes = runConcurrently(index -> passwordResetService.resetPassword(rawToken, "new" + index), failures);

        assertThat(successes.get()).isEqualTo(1);
        assertThat(failures).hasSize(CONCURRENCY - 1);
        assertThat(usableTokenCount()).isZero();
    }

    @Test
    @DisplayName("동시에 재설정을 요청해도 유효한 토큰은 하나만 남는다.")
    void onlyOneTokenSurvivesConcurrentRequests() throws Exception {
        List<Throwable> failures = new CopyOnWriteArrayList<>();
        runConcurrently(index -> passwordResetService.requestReset(LOGIN_ID, EMAIL), failures);

        assertThat(failures).isEmpty();
        assertThat(passwordResetTokenRepository.findAll()).hasSize(1);
        assertThat(usableTokenCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("토큰 발급과 재설정이 동시에 일어나도 데드락이 나지 않는다.")
    void concurrentIssueAndResetDoNotDeadlock() throws Exception {
        String rawToken = issueToken();

        List<Throwable> failures = new CopyOnWriteArrayList<>();
        runConcurrently(index -> {
            if (index == 0) {
                passwordResetService.requestReset(LOGIN_ID, EMAIL);
            } else {
                passwordResetService.resetPassword(rawToken, "new1234");
            }
        }, failures);

        assertThat(failures).noneMatch(DeadlockLoserDataAccessException.class::isInstance);
    }

    private AtomicInteger runConcurrently(ConcurrentAttempt attempt, List<Throwable> failures) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENCY);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(CONCURRENCY);
        AtomicInteger successes = new AtomicInteger();

        for (int i = 0; i < CONCURRENCY; i++) {
            int index = i;
            executor.submit(() -> {
                try {
                    start.await();
                    attempt.run(index);
                    successes.incrementAndGet();
                } catch (Throwable e) {
                    failures.add(e);
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        assertThat(done.await(AWAIT_SECONDS, TimeUnit.SECONDS)).isTrue();
        executor.shutdownNow();

        return successes;
    }

    private String issueToken() {
        passwordResetService.requestReset(LOGIN_ID, EMAIL);

        ArgumentCaptor<String> link = ArgumentCaptor.forClass(String.class);
        verify(mailSender, timeout(MAIL_TIMEOUT_MILLIS).atLeastOnce()).send(any(), link.capture());

        String lastLink = link.getAllValues().get(link.getAllValues().size() - 1);
        return lastLink.substring(lastLink.indexOf("token=") + "token=".length());
    }

    private long usableTokenCount() {
        LocalDateTime now = LocalDateTime.now();
        return passwordResetTokenRepository.findAll().stream()
                .filter(token -> token.isUsable(now))
                .count();
    }

    private void clearMembers() {
        passwordResetTokenRepository.deleteAll();
        memberRepository.deleteAll();
    }

    private interface ConcurrentAttempt {
        void run(int index);
    }
}
