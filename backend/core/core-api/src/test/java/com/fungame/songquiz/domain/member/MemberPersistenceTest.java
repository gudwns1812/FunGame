package com.fungame.songquiz.domain.member;

import com.fungame.songquiz.enums.PlayerStatus;
import com.fungame.songquiz.enums.Role;
import com.fungame.songquiz.support.MySqlIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import com.fungame.songquiz.storage.MemberEntity;
import com.fungame.songquiz.storage.MemberRepository;

@MySqlIntegrationTest
class MemberPersistenceTest {

    private static final Long ROOM_ID = 777L;

    @Autowired
    private MemberPresenceService memberPresenceService;

    @Autowired
    private AuthService authService;

    @Autowired
    private PromotionService promotionService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Map<String, Object> memberRow(Long memberId) {
        return jdbcTemplate.queryForMap(
                "select status, current_room_id, nickname, role from member where id = ?", memberId);
    }

    private Long saveMember(String name) {
        return memberRepository.save(MemberEntity.builder()
                .loginId(name)
                .password("password")
                .nickname(name)
                .email(name + "@fun-game.club")
                .role(Role.USER)
                .status(PlayerStatus.LOBBY)
                .build()).getId();
    }

    @Test
    @DisplayName("대기실 입장이 DB 행에 남는다.")
    void persistsEnterWaitingRoom() {
        Long memberId = saveMember("대기실입장");

        memberPresenceService.enterWaitingRoom(memberId, ROOM_ID);

        Map<String, Object> row = memberRow(memberId);
        assertThat(row.get("status")).isEqualTo("WAITING");
        assertThat(row.get("current_room_id")).isEqualTo(ROOM_ID);
    }

    @Test
    @DisplayName("게임중 입장이 DB 행에 남는다.")
    void persistsEnterPlayingRoom() {
        Long memberId = saveMember("게임중입장");

        memberPresenceService.enterPlayingRoom(memberId, ROOM_ID);

        Map<String, Object> row = memberRow(memberId);
        assertThat(row.get("status")).isEqualTo("PLAYING");
        assertThat(row.get("current_room_id")).isEqualTo(ROOM_ID);
    }

    @Test
    @DisplayName("방을 나간 것이 DB 행에 남는다.")
    void persistsLeaveRoom() {
        Long memberId = saveMember("방나감");
        memberPresenceService.enterWaitingRoom(memberId, ROOM_ID);

        memberPresenceService.leaveRoom(memberId);

        Map<String, Object> row = memberRow(memberId);
        assertThat(row.get("status")).isEqualTo("LOBBY");
        assertThat(row.get("current_room_id")).isNull();
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
