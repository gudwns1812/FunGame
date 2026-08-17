package com.fungame.songquiz.domain.member;

import com.fungame.songquiz.enums.Role;
import com.fungame.songquiz.storage.IntegrationTest;
import com.fungame.songquiz.storage.MemberEntity;
import com.fungame.songquiz.storage.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class MemberPersistenceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private PromotionService promotionService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Map<String, Object> memberRow(Long memberId) {
        return jdbcTemplate.queryForMap("select nickname, role from member where id = ?", memberId);
    }

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
    @DisplayName("닉네임 변경이 DB 행에 남는다.")
    void persistsNicknameChange() {
        Long memberId = saveMember("닉네임바꿀사람");

        authService.updateNickname("닉네임바꿀사람", "바뀐닉네임");

        assertThat(memberRow(memberId).get("nickname")).isEqualTo("바뀐닉네임");
    }

    @Test
    @DisplayName("승급 승인이 요청 상태와 회원 권한 양쪽 DB 행에 남는다.")
    void persistsPromotionApproval() {
        Long memberId = saveMember("승급대상");
        promotionService.createPromotionRequest("승급대상");
        Long requestId = jdbcTemplate.queryForObject(
                "select id from promotion_request where member_id = ?", Long.class, memberId);

        promotionService.approveRequest(requestId);

        String requestStatus = jdbcTemplate.queryForObject(
                "select status from promotion_request where id = ?", String.class, requestId);
        assertThat(requestStatus).isEqualTo("APPROVED");
        assertThat(memberRow(memberId).get("role")).isEqualTo("ADMIN");
    }
}
