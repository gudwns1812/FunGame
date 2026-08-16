package com.fungame.songquiz.domain.member;

import com.fungame.songquiz.enums.Role;
import com.fungame.songquiz.support.MemberFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MemberAdapterTest {

    @Test
    @DisplayName("인증 주체 이름은 닉네임이 아니라 회원 ID 다.")
    void principalNameIsMemberId() {
        Member member = MemberFixture.withId(42L, "짱구");

        MemberAdapter adapter = new MemberAdapter(member);

        assertThat(adapter.getUsername()).isEqualTo("42");
        assertThat(MemberAdapter.principalNameOf(member)).isEqualTo("42");
    }

    @Test
    @DisplayName("닉네임을 바꿔도 주체 이름은 그대로다.")
    void principalNameSurvivesNicknameChange() {
        Member member = MemberFixture.withId(42L, "짱구");
        String before = MemberAdapter.principalNameOf(member);

        member.changeNickname("철수");

        assertThat(MemberAdapter.principalNameOf(member)).isEqualTo(before);
    }

    @Test
    @DisplayName("아직 저장되지 않아 ID 가 없는 회원은 인증 주체가 될 수 없다.")
    void rejectMemberWithoutId() {
        Member notPersisted = Member.builder()
                .loginId("login")
                .password("password")
                .nickname("짱구")
                .email("test@fun-game.club")
                .role(Role.USER)
                .build();

        assertThatThrownBy(() -> new MemberAdapter(notPersisted))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("닉네임은 주체 이름과 별개로 그대로 노출한다.")
    void keepsNicknameAccessible() {
        MemberAdapter adapter = new MemberAdapter(MemberFixture.withId(1L, "짱구"));

        assertThat(adapter.getNickName()).isEqualTo("짱구");
        assertThat(adapter.getId()).isEqualTo(1L);
    }
}
