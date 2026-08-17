package com.fungame.songquiz.controller.room;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RoomPresenceTest {

    private static final Long ROOM_ID = 1L;
    private static final Long MEMBER_ID = 11L;
    private static final RoomMember MEMBER = new RoomMember(ROOM_ID, MEMBER_ID, "참가자");

    private final RoomPresence roomPresence = new RoomPresence();

    @Test
    void 세션이_없으면_0_을_센다() {
        assertThat(roomPresence.countSessionsOf(MEMBER)).isZero();
    }

    @Test
    void 같은_사람이_한_방에_열어둔_세션을_모두_센다() {
        // given
        roomPresence.arrive("session-1", MEMBER);
        roomPresence.arrive("session-2", MEMBER);

        // then
        assertThat(roomPresence.countSessionsOf(MEMBER)).isEqualTo(2);
    }

    @Test
    void 다른_방의_세션은_세지_않는다() {
        // given
        roomPresence.arrive("session-1", MEMBER);
        roomPresence.arrive("session-2", new RoomMember(2L, MEMBER_ID, "참가자"));

        // then
        assertThat(roomPresence.countSessionsOf(MEMBER)).isEqualTo(1);
    }

    @Test
    void 닉네임이_달라도_같은_방의_같은_회원이면_같은_연결로_본다() {
        // given: 닉네임을 바꾸기 전에 연 세션과 바꾼 뒤에 연 세션
        roomPresence.arrive("session-1", MEMBER);
        roomPresence.arrive("session-2", new RoomMember(ROOM_ID, MEMBER_ID, "바뀐닉네임"));

        // then
        assertThat(roomPresence.countSessionsOf(MEMBER)).isEqualTo(2);
        assertThat(roomPresence.isConnected(new RoomMember(ROOM_ID, MEMBER_ID, "바뀐닉네임"))).isTrue();
    }

    @Test
    void 떠난_세션은_세지_않는다() {
        // given
        roomPresence.arrive("session-1", MEMBER);
        roomPresence.arrive("session-2", MEMBER);

        // when
        roomPresence.depart("session-1");

        // then
        assertThat(roomPresence.countSessionsOf(MEMBER)).isEqualTo(1);
    }
}
