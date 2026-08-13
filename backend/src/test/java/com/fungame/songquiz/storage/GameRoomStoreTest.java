package com.fungame.songquiz.storage;

import com.fungame.songquiz.domain.CSQuizDifficulty;
import com.fungame.songquiz.domain.Category;
import com.fungame.songquiz.domain.GamePlayer;
import com.fungame.songquiz.domain.GameRoom;
import com.fungame.songquiz.domain.GameType;
import com.fungame.songquiz.domain.RoomSettings;
import com.fungame.songquiz.domain.StoredRoom;
import com.fungame.songquiz.domain.member.Member;
import com.fungame.songquiz.domain.member.MemberRepository;
import com.fungame.songquiz.domain.member.Role;
import com.fungame.songquiz.support.MySqlIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@MySqlIntegrationTest
class GameRoomStoreTest {

    private static final RoomSettings SETTINGS =
            new RoomSettings(GameType.SONG, "저장된 방", 8, Category.KPOP, 10, 0, CSQuizDifficulty.HARD);

    @Autowired
    private GameRoomStore gameRoomStore;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("방을 저장했다 불러오면 회원 번호와 닉네임이 그대로 살아난다.")
    void loadRestoresMemberIdAndNickname() {
        // given
        GamePlayer host = saveMemberAsPlayer("방장");
        GamePlayer guest = saveMemberAsPlayer("참가자");

        Long roomId = gameRoomStore.open(SETTINGS, host);
        GameRoom room = GameRoom.create(SETTINGS, host);
        room.join(guest);
        gameRoomStore.save(roomId, room);

        // when
        StoredRoom stored = gameRoomStore.load(roomId).orElseThrow();

        // then
        assertThat(stored.hostId()).isEqualTo(host.memberId());
        assertThat(stored.hostNickname()).isEqualTo("방장");
        assertThat(stored.players())
                .containsExactlyInAnyOrder(
                        new GamePlayer(host.memberId(), "방장", true),
                        new GamePlayer(guest.memberId(), "참가자", false));
    }

    @Test
    @DisplayName("닉네임을 바꾼 뒤 불러오면 바뀐 닉네임으로 살아난다.")
    void loadReadsCurrentNickname() {
        // given
        GamePlayer host = saveMemberAsPlayer("옛닉네임");
        Long roomId = gameRoomStore.open(SETTINGS, host);

        Member member = memberRepository.findById(host.memberId()).orElseThrow();
        member.changeNickname("새닉네임");
        memberRepository.saveAndFlush(member);

        // when
        StoredRoom stored = gameRoomStore.load(roomId).orElseThrow();

        // then
        assertThat(stored.players())
                .extracting(GamePlayer::nickname)
                .containsExactly("새닉네임");
    }

    private GamePlayer saveMemberAsPlayer(String nickname) {
        Member member = memberRepository.save(Member.builder()
                .loginId(nickname)
                .password("password")
                .nickname(nickname)
                .email(nickname + "@fun-game.club")
                .role(Role.USER)
                .build());

        return GamePlayer.createNewPlayer(member.getId(), nickname);
    }
}
