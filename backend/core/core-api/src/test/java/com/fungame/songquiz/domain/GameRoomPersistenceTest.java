package com.fungame.songquiz.domain;

import com.fungame.songquiz.enums.CSQuizDifficulty;
import com.fungame.songquiz.enums.Category;
import com.fungame.songquiz.enums.GameRoomStatus;
import com.fungame.songquiz.enums.GameType;
import com.fungame.songquiz.storage.MemberEntity;
import com.fungame.songquiz.storage.MemberRepository;
import com.fungame.songquiz.enums.PlayerStatus;
import com.fungame.songquiz.enums.Role;
import com.fungame.songquiz.support.MySqlIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@MySqlIntegrationTest
class GameRoomPersistenceTest {

    private static final RoomSettings SETTINGS =
            new RoomSettings(GameType.SONG, "저장된 방", 8, Category.KPOP, 10, 0, CSQuizDifficulty.HARD);

    @Autowired
    private GameRoomReader gameRoomReader;

    @Autowired
    private GameRoomWriter gameRoomWriter;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("방을 저장했다 불러오면 회원 번호와 닉네임이 그대로 살아난다.")
    void loadRestoresMemberIdAndNickname() {
        // given
        GamePlayer host = saveMemberAsPlayer("방장");
        GamePlayer guest = saveMemberAsPlayer("참가자");

        Long roomId = gameRoomWriter.open(SETTINGS, host);
        GameRoom room = GameRoom.create(SETTINGS, host);
        room.join(guest);
        gameRoomWriter.save(roomId, room);

        // when
        StoredRoom stored = gameRoomReader.load(roomId).orElseThrow();

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
        Long roomId = gameRoomWriter.open(SETTINGS, host);

        MemberEntity member = memberRepository.findById(host.memberId()).orElseThrow();
        member.changeNickname("새닉네임");
        memberRepository.saveAndFlush(member);

        // when
        StoredRoom stored = gameRoomReader.load(roomId).orElseThrow();

        // then
        assertThat(stored.players())
                .extracting(GamePlayer::nickname)
                .containsExactly("새닉네임");
    }

    @Test
    @DisplayName("설정을 바꿔 저장하면 게임 종류까지 함께 저장된다.")
    void saveKeepsChangedGameType() {
        // given
        GamePlayer host = saveMemberAsPlayer("설정바꾸는방장");
        Long roomId = gameRoomWriter.open(SETTINGS, host);

        GameRoom room = GameRoom.create(SETTINGS, host);
        room.changeSettings(SETTINGS.changeTo(GameType.CS, 8, Category.DEFAULT, 5, 0, CSQuizDifficulty.EASY));

        // when
        gameRoomWriter.save(roomId, room);
        RoomSettings reloaded = gameRoomReader.load(roomId).orElseThrow().settings();

        // then
        assertThat(reloaded.gameType()).isEqualTo(GameType.CS);
        assertThat(reloaded.category()).isEqualTo(Category.DEFAULT);
        assertThat(reloaded.totalRound()).isEqualTo(5);
        assertThat(reloaded.csDifficulty()).isEqualTo(CSQuizDifficulty.EASY);
    }

    @Test
    @DisplayName("방을 열었다 불러오면 설정 일곱 필드가 모두 그대로 살아난다.")
    void openRestoresEverySettingField() {
        // given
        RoomSettings everyFieldDistinct =
                new RoomSettings(GameType.CS, "일곱필드", 7, Category.POP, 9, 3, CSQuizDifficulty.NORMAL);
        GamePlayer host = saveMemberAsPlayer("일곱필드방장");

        // when
        Long roomId = gameRoomWriter.open(everyFieldDistinct, host);
        RoomSettings reloaded = gameRoomReader.load(roomId).orElseThrow().settings();

        // then
        assertThat(reloaded).isEqualTo(everyFieldDistinct);
    }

    @Test
    @DisplayName("설정을 바꿔 저장하면 일곱 필드가 모두 바뀐 값으로 저장된다.")
    void saveRestoresEverySettingField() {
        // given
        GamePlayer host = saveMemberAsPlayer("일곱필드바꾸는방장");
        Long roomId = gameRoomWriter.open(SETTINGS, host);
        RoomSettings changed = SETTINGS.changeTo(GameType.HANGMAN, 6, Category.OST, 4, 2, CSQuizDifficulty.EASY);

        GameRoom room = GameRoom.create(SETTINGS, host);
        room.changeSettings(changed);

        // when
        gameRoomWriter.save(roomId, room);
        RoomSettings reloaded = gameRoomReader.load(roomId).orElseThrow().settings();

        // then
        assertThat(reloaded).isEqualTo(changed);
    }

    @Test
    @DisplayName("모두 불러오면 방마다 참가자가 함께 살아난다.")
    void loadAllRestoresPlayersOfEachRoom() {
        // given
        GamePlayer firstHost = saveMemberAsPlayer("첫째방장");
        GamePlayer secondHost = saveMemberAsPlayer("둘째방장");
        Long firstRoomId = gameRoomWriter.open(SETTINGS, firstHost);
        Long secondRoomId = gameRoomWriter.open(SETTINGS, secondHost);

        // when
        List<StoredRoom> rooms = gameRoomReader.loadAll();

        // then
        assertThat(rooms)
                .filteredOn(room -> List.of(firstRoomId, secondRoomId).contains(room.roomId()))
                .hasSize(2)
                .allSatisfy(room -> assertThat(room.players()).hasSize(1))
                .extracting(StoredRoom::hostNickname)
                .containsExactlyInAnyOrder("첫째방장", "둘째방장");
    }

    @Test
    @DisplayName("저장하면 준비 상태 변경과 나간 참가자가 함께 반영된다.")
    void saveSyncsReadyChangeAndLeaver() {
        // given
        GamePlayer host = saveMemberAsPlayer("동기화방장");
        GamePlayer stayer = saveMemberAsPlayer("남는사람");
        GamePlayer leaver = saveMemberAsPlayer("나가는사람");

        Long roomId = gameRoomWriter.open(SETTINGS, host);
        GameRoom room = GameRoom.create(SETTINGS, host);
        room.join(stayer);
        room.join(leaver);
        gameRoomWriter.save(roomId, room);

        // when
        room.readyPlayer(stayer.memberId());
        room.leave(leaver.memberId());
        gameRoomWriter.save(roomId, room);

        // then
        assertThat(gameRoomReader.load(roomId).orElseThrow().players())
                .containsExactlyInAnyOrder(
                        new GamePlayer(host.memberId(), "동기화방장", true),
                        new GamePlayer(stayer.memberId(), "남는사람", true));
    }

    @Test
    @DisplayName("진행 중이던 방은 대기 상태로 되돌린다.")
    void markInterruptedGamesWaiting() {
        // given
        GamePlayer host = saveMemberAsPlayer("중단된방장");
        Long roomId = gameRoomWriter.open(SETTINGS, host);

        GameRoom room = GameRoom.create(SETTINGS, host);
        room.start(host.memberId(), mock(Game.class));
        gameRoomWriter.save(roomId, room);
        assertThat(gameRoomReader.load(roomId).orElseThrow().status()).isEqualTo(GameRoomStatus.PLAYING);

        // when
        gameRoomWriter.markInterruptedGamesWaiting();

        // then
        assertThat(gameRoomReader.load(roomId).orElseThrow().status()).isEqualTo(GameRoomStatus.WAITING);
    }

    @Test
    @DisplayName("삭제한 방은 불러오지 못한다.")
    void deleteRemovesRoom() {
        // given
        GamePlayer host = saveMemberAsPlayer("삭제될방장");
        Long roomId = gameRoomWriter.open(SETTINGS, host);

        // when
        gameRoomWriter.delete(roomId);

        // then
        assertThat(gameRoomReader.load(roomId)).isEmpty();
    }

    private GamePlayer saveMemberAsPlayer(String nickname) {
        MemberEntity member = memberRepository.save(MemberEntity.builder()
                .loginId(nickname)
                .password("password")
                .nickname(nickname)
                .email(nickname + "@fun-game.club")
                .role(Role.USER)
                .status(PlayerStatus.LOBBY)
                .build());

        return GamePlayer.createNewPlayer(member.getId(), nickname);
    }
}
