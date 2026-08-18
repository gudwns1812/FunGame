package com.fungame.songquiz.domain.member;

import com.fungame.songquiz.enums.Role;
import com.fungame.songquiz.storage.IntegrationTest;
import com.fungame.songquiz.storage.MemberActivityDao;
import com.fungame.songquiz.storage.MemberEntity;
import com.fungame.songquiz.storage.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class MemberActivityDaoTest {

    // 테스트끼리 행을 되돌리지 않으므로 날짜를 겹치지 않게 나눠 쓴다.
    private static final LocalDate 첫방문일 = LocalDate.of(2026, 1, 1);
    private static final LocalDate 재방문일 = LocalDate.of(2026, 1, 2);
    private static final LocalDate 이틀연속_첫날 = LocalDate.of(2026, 1, 3);
    private static final LocalDate 이틀연속_다음날 = LocalDate.of(2026, 1, 4);
    private static final LocalDate 두명이_온_날 = LocalDate.of(2026, 1, 5);

    @Autowired
    private MemberActivityDao memberActivityDao;

    @Autowired
    private MemberRepository memberRepository;

    private Long saveMember(String name) {
        return memberRepository.save(MemberEntity.builder()
                .loginId(name)
                .password("password")
                .nickname(name)
                .email(name + "@fun-game.club")
                .role(Role.USER)
                .build()).getId();
    }

    @Test
    @DisplayName("그날 처음 기록하면 행이 새로 생긴다.")
    void insertsOnFirstVisit() {
        Long memberId = saveMember("첫방문자");

        assertThat(memberActivityDao.recordActive(memberId, 첫방문일)).isTrue();
    }

    @Test
    @DisplayName("같은 날 다시 기록하면 행이 생기지 않는다.")
    void ignoresSecondVisitOfSameDay() {
        Long memberId = saveMember("재방문자");
        memberActivityDao.recordActive(memberId, 재방문일);

        assertThat(memberActivityDao.recordActive(memberId, 재방문일)).isFalse();
    }

    @Test
    @DisplayName("날짜가 바뀌면 같은 회원이라도 행이 새로 생긴다.")
    void insertsAgainOnNextDay() {
        Long memberId = saveMember("이틀연속방문자");
        memberActivityDao.recordActive(memberId, 이틀연속_첫날);

        assertThat(memberActivityDao.recordActive(memberId, 이틀연속_다음날)).isTrue();
    }

    @Test
    @DisplayName("회원이 다르면 같은 날이라도 행이 각각 생긴다.")
    void countsEachMemberSeparately() {
        memberActivityDao.recordActive(saveMember("갑"), 두명이_온_날);
        memberActivityDao.recordActive(saveMember("을"), 두명이_온_날);

        assertThat(memberActivityDao.countActiveOn(두명이_온_날)).isEqualTo(2);
    }
}
