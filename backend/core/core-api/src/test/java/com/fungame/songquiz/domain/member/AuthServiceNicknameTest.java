package com.fungame.songquiz.domain.member;

import org.mockito.ArgumentMatchers;

import com.fungame.songquiz.support.MemberFixture;
import com.fungame.songquiz.support.error.CoreException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class AuthServiceNicknameTest {

    private static final Long MEMBER_ID = 1L;
    private static final String LOGIN_ID = "login" + MEMBER_ID;
    private static final String OLD_NICKNAME = "반달";
    private static final String NEW_NICKNAME = "보름달";

    private final MemberReader memberReader = mock(MemberReader.class);
    private final MemberWriter memberWriter = mock(MemberWriter.class);
    private final MemberProfiles memberProfiles = mock(MemberProfiles.class);
    private final CacheManager cacheManager = new ConcurrentMapCacheManager(MemberProfiles.CACHE_NAME);
    private final AuthService authService = new AuthService(
            memberReader,
            memberWriter,
            mock(PasswordEncoder.class),
            mock(AuthenticationManager.class),
            mock(LoginMetrics.class),
            memberProfiles);

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("닉네임을 바꾸면 프로필 캐시도 새 닉네임으로 갱신한다.")
    void writeThroughOnNicknameChange() {
        given(memberReader.existsByNickname(NEW_NICKNAME)).willReturn(false);
        given(memberReader.findByLoginId(LOGIN_ID)).willReturn(Optional.of(member()));

        authService.updateNickname(LOGIN_ID, NEW_NICKNAME);

        ArgumentCaptor<Member> refreshed = ArgumentCaptor.forClass(Member.class);
        verify(memberProfiles).refresh(refreshed.capture());
        assertThat(refreshed.getValue().getId()).isEqualTo(MEMBER_ID);
        assertThat(refreshed.getValue().getNickname()).isEqualTo(NEW_NICKNAME);
    }

    @Test
    @DisplayName("이미 쓰는 닉네임이면 캐시를 건드리지 않는다.")
    void leaveCacheAloneWhenNicknameIsTaken() {
        given(memberReader.existsByNickname(NEW_NICKNAME)).willReturn(true);

        assertThatThrownBy(() -> authService.updateNickname(LOGIN_ID, NEW_NICKNAME))
                .isInstanceOf(CoreException.class);

        verify(memberProfiles, never()).refresh(ArgumentMatchers.any());
    }

    @Test
    @DisplayName("캐시를 거쳐 읽으면 변경 직후에도 새 닉네임이 나온다.")
    void cacheServesNewNicknameRightAfterChange() {
        MemberProfiles realProfiles = new MemberProfiles(memberReader, cacheManager);
        AuthService service = new AuthService(
                memberReader,
                memberWriter,
                mock(PasswordEncoder.class),
                mock(AuthenticationManager.class),
                mock(LoginMetrics.class),
                realProfiles);

        Member member = member();
        given(memberReader.findMember(MEMBER_ID)).willReturn(member);
        given(memberReader.findByLoginId(LOGIN_ID)).willReturn(Optional.of(member));
        given(memberReader.existsByNickname(NEW_NICKNAME)).willReturn(false);

        assertThat(realProfiles.of(MEMBER_ID).nickname()).isEqualTo(OLD_NICKNAME);

        service.updateNickname(LOGIN_ID, NEW_NICKNAME);

        assertThat(realProfiles.of(MEMBER_ID).nickname()).isEqualTo(NEW_NICKNAME);
    }

    private static Member member() {
        return MemberFixture.withId(MEMBER_ID, OLD_NICKNAME);
    }
}
