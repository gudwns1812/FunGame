package com.fungame.songquiz.domain.member;

import com.fungame.songquiz.enums.Role;
import com.fungame.songquiz.support.MemberFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class MemberProfilesTest {

    private static final Long ONE = 1L;
    private static final Long TWO = 2L;
    private static final Long THREE = 3L;

    private final MemberReader memberReader = mock(MemberReader.class);
    private final CacheManager cacheManager = new ConcurrentMapCacheManager(MemberProfiles.CACHE_NAME);
    private final MemberProfiles memberProfiles = new MemberProfiles(memberReader, cacheManager);

    @Test
    @DisplayName("처음 묻는 회원은 저장소에서 읽어온다.")
    void readFromStorageOnFirstAsk() {
        given(memberReader.findMember(ONE)).willReturn(member(ONE, "반달"));

        MemberProfile profile = memberProfiles.of(ONE);

        assertThat(profile.memberId()).isEqualTo(ONE);
        assertThat(profile.nickname()).isEqualTo("반달");
        assertThat(profile.role()).isEqualTo(Role.USER);
    }

    @Test
    @DisplayName("한 번 읽은 회원은 저장소를 다시 묻지 않는다.")
    void askStorageOnlyOnce() {
        given(memberReader.findMember(ONE)).willReturn(member(ONE, "반달"));

        memberProfiles.of(ONE);
        memberProfiles.of(ONE);
        memberProfiles.of(ONE);

        verify(memberReader, times(1)).findMember(ONE);
    }

    @Test
    @DisplayName("여럿을 물으면 캐시에 없는 사람만 모아 한 번에 읽는다.")
    void readMissingOnesInOneGo() {
        given(memberReader.findMember(ONE)).willReturn(member(ONE, "반달"));
        memberProfiles.of(ONE);

        given(memberReader.findAllInOrderByNickname(any()))
                .willReturn(List.of(member(TWO, "초승달"), member(THREE, "보름달")));

        Map<Long, MemberProfile> profiles = memberProfiles.allOf(List.of(ONE, TWO, THREE));

        assertThat(profiles).containsOnlyKeys(ONE, TWO, THREE);
        assertThat(profiles.get(ONE).nickname()).isEqualTo("반달");
        assertThat(profiles.get(THREE).nickname()).isEqualTo("보름달");
        verify(memberReader).findAllInOrderByNickname(argThat(ids -> Set.copyOf(ids).equals(Set.of(TWO, THREE))));
    }

    @Test
    @DisplayName("전부 캐시에 있으면 저장소를 묻지 않는다.")
    void skipStorageWhenEveryoneIsCached() {
        given(memberReader.findMember(anyLong())).willReturn(member(ONE, "반달"), member(TWO, "초승달"));
        memberProfiles.of(ONE);
        memberProfiles.of(TWO);

        memberProfiles.allOf(List.of(ONE, TWO));

        verify(memberReader, never()).findAllInOrderByNickname(any());
    }

    @Test
    @DisplayName("여럿을 물은 뒤에는 그 사람들도 캐시에 남는다.")
    void keepWhatWasReadInBulk() {
        given(memberReader.findAllInOrderByNickname(any())).willReturn(List.of(member(TWO, "초승달")));
        memberProfiles.allOf(List.of(TWO));

        MemberProfile profile = memberProfiles.of(TWO);

        assertThat(profile.nickname()).isEqualTo("초승달");
        verify(memberReader, never()).findMember(TWO);
    }

    @Test
    @DisplayName("갱신하면 저장소를 다시 읽지 않고도 새 닉네임이 보인다.")
    void writeThroughOnRefresh() {
        given(memberReader.findMember(ONE)).willReturn(member(ONE, "반달"));
        memberProfiles.of(ONE);

        memberProfiles.refresh(member(ONE, "보름달"));

        assertThat(memberProfiles.of(ONE).nickname()).isEqualTo("보름달");
        verify(memberReader, times(1)).findMember(ONE);
    }

    @Test
    @DisplayName("한 번도 읽지 않은 회원을 갱신해도 캐시에 담긴다.")
    void refreshAlsoFillsEmptyCache() {
        memberProfiles.refresh(member(ONE, "보름달"));

        assertThat(memberProfiles.of(ONE).nickname()).isEqualTo("보름달");
        verify(memberReader, never()).findMember(ONE);
    }

    @Test
    @DisplayName("잊으라고 하면 다음에 저장소를 다시 읽는다.")
    void readAgainAfterForget() {
        given(memberReader.findMember(ONE)).willReturn(member(ONE, "반달"), member(ONE, "보름달"));
        memberProfiles.of(ONE);

        memberProfiles.forget(ONE);

        assertThat(memberProfiles.of(ONE).nickname()).isEqualTo("보름달");
        verify(memberReader, times(2)).findMember(ONE);
    }

    private static Member member(Long memberId, String nickname) {
        return MemberFixture.withId(memberId, nickname);
    }
}
