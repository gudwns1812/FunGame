package com.fungame.songquiz.domain.member;

import com.fungame.songquiz.enums.PlayerStatus;
import com.fungame.songquiz.support.MemberFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MemberPresenceTest {

    @Test
    @DisplayName("새로 만든 회원은 어느 방에도 속하지 않는다.")
    void startsOutsideAnyRoom() {
        Member member = MemberFixture.withId(1L, "짱구");

        assertThat(member.getStatus()).isEqualTo(PlayerStatus.LOBBY);
        assertThat(member.getCurrentRoomId()).isNull();
        assertThat(member.isInLobby()).isTrue();
    }

    @Test
    @DisplayName("대기실에 들어가면 방 번호와 대기 상태를 갖는다.")
    void enterWaitingRoom() {
        Member member = MemberFixture.withId(1L, "짱구");

        member.enterWaitingRoom(7L);

        assertThat(member.getStatus()).isEqualTo(PlayerStatus.WAITING);
        assertThat(member.getCurrentRoomId()).isEqualTo(7L);
        assertThat(member.isInLobby()).isFalse();
    }

    @Test
    @DisplayName("진행 중인 방에 재입장하면 게임중 상태가 된다.")
    void enterPlayingRoom() {
        Member member = MemberFixture.withId(1L, "짱구");

        member.enterPlayingRoom(7L);

        assertThat(member.getStatus()).isEqualTo(PlayerStatus.PLAYING);
        assertThat(member.getCurrentRoomId()).isEqualTo(7L);
    }

    @Test
    @DisplayName("방을 나가면 로비로 돌아간다.")
    void leaveRoom() {
        Member member = MemberFixture.withId(1L, "짱구");
        member.enterPlayingRoom(7L);

        member.leaveRoom();

        assertThat(member.getStatus()).isEqualTo(PlayerStatus.LOBBY);
        assertThat(member.getCurrentRoomId()).isNull();
        assertThat(member.isInLobby()).isTrue();
    }

    @Test
    @DisplayName("특정 방의 대기실에 있는지 확인한다.")
    void waitingInRoom() {
        Member member = MemberFixture.withId(1L, "짱구");
        member.enterWaitingRoom(7L);

        assertThat(member.isWaitingIn(7L)).isTrue();
        assertThat(member.isWaitingIn(8L)).isFalse();
    }

    @Test
    @DisplayName("게임 중이면 그 방의 대기실에 있는 것이 아니다.")
    void playingIsNotWaiting() {
        Member member = MemberFixture.withId(1L, "짱구");
        member.enterPlayingRoom(7L);

        assertThat(member.isWaitingIn(7L)).isFalse();
    }
}
