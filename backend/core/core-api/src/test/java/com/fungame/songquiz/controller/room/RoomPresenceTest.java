package com.fungame.songquiz.controller.room;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RoomPresenceTest {

    private static final Long ROOM_ID = 1L;
    private static final Long MEMBER_ID = 11L;
    private static final RoomMember MEMBER = RoomMember.of(ROOM_ID, MEMBER_ID, "참가자");

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
        roomPresence.arrive("session-2", RoomMember.of(2L, MEMBER_ID, "참가자"));

        // then
        assertThat(roomPresence.countSessionsOf(MEMBER)).isEqualTo(1);
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
