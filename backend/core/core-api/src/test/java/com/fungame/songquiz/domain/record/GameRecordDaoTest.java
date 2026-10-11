package com.fungame.songquiz.domain.record;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.fungame.songquiz.enums.GameType;
import com.fungame.songquiz.enums.Role;
import com.fungame.songquiz.storage.GameRecordDao;
import com.fungame.songquiz.storage.GameRecordPlayerRow;
import com.fungame.songquiz.storage.GameRecordRow;
import com.fungame.songquiz.storage.GameTypeRecordCount;
import com.fungame.songquiz.storage.MemberEntity;
import com.fungame.songquiz.storage.MemberRepository;
import com.fungame.songquiz.support.ApiIntegrationTest;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class GameRecordDaoTest extends ApiIntegrationTest {

    private static final Instant STARTED_AT = Instant.parse("2026-10-12T10:00:00Z");
    private static final Instant ENDED_AT = Instant.parse("2026-10-12T10:05:00Z");
    private static final Integer NO_RANK = null;
    private static final Integer NO_SCORE = null;

    @Autowired
    private GameRecordDao gameRecordDao;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("처음 저장하는 판은 참가자 결과와 함께 남는다.")
    void recordsANewGame() {
        Long winner = saveMember();
        Long loser = saveMember();

        boolean recorded = gameRecordDao.record(newGame(GameType.SONG, 2), List.of(
                new GameRecordPlayerRow(winner, true, 1, 3, false),
                new GameRecordPlayerRow(loser, false, 2, 1, false)));

        assertThat(recorded).isTrue();
        assertThat(gameRecordDao.countByGameType(winner))
                .extracting(GameTypeRecordCount::gameType, GameTypeRecordCount::played, GameTypeRecordCount::won)
                .containsExactly(tuple(GameType.SONG, 1L, 1L));
        assertThat(gameRecordDao.countByGameType(loser))
                .extracting(GameTypeRecordCount::played, GameTypeRecordCount::won)
                .containsExactly(tuple(1L, 0L));
    }

    @Test
    @DisplayName("같은 판을 다시 저장하면 아무것도 남지 않는다. 결과 발행이 다른 서버에서 한 번 더 돌아도 전적은 한 번이다.")
    void ignoresTheSameGameTwice() {
        Long first = saveMember();
        Long second = saveMember();
        GameRecordRow game = newGame(GameType.CS, 2);
        List<GameRecordPlayerRow> players = List.of(
                new GameRecordPlayerRow(first, true, 1, 2, false),
                new GameRecordPlayerRow(second, false, 2, 0, false));
        gameRecordDao.record(game, players);

        boolean recordedAgain = gameRecordDao.record(game, players);

        assertThat(recordedAgain).isFalse();
        assertThat(gameRecordDao.countByGameType(first))
                .extracting(GameTypeRecordCount::played)
                .containsExactly(1L);
    }

    @Test
    @DisplayName("순위와 점수가 없는 협동 판도 남는다.")
    void recordsACooperativeGameWithoutRankOrScore() {
        Long finisher = saveMember();
        Long leaver = saveMember();

        boolean recorded = gameRecordDao.record(newGame(GameType.HANGMAN, 2), List.of(
                new GameRecordPlayerRow(finisher, true, NO_RANK, NO_SCORE, false),
                new GameRecordPlayerRow(leaver, false, NO_RANK, NO_SCORE, true)));

        assertThat(recorded).isTrue();
        assertThat(gameRecordDao.countByGameType(leaver))
                .extracting(GameTypeRecordCount::gameType, GameTypeRecordCount::played, GameTypeRecordCount::won)
                .containsExactly(tuple(GameType.HANGMAN, 1L, 0L));
    }

    @Test
    @DisplayName("전적은 게임 종류마다 따로 센다.")
    void countsEachGameTypeSeparately() {
        Long member = saveMember();
        Long rival = saveMember();
        gameRecordDao.record(newGame(GameType.SONG, 2), List.of(
                new GameRecordPlayerRow(member, true, 1, 2, false),
                new GameRecordPlayerRow(rival, false, 2, 1, false)));
        gameRecordDao.record(newGame(GameType.SONG, 2), List.of(
                new GameRecordPlayerRow(member, false, 2, 0, false),
                new GameRecordPlayerRow(rival, true, 1, 1, false)));
        gameRecordDao.record(newGame(GameType.HANGMAN, 2), List.of(
                new GameRecordPlayerRow(member, true, NO_RANK, NO_SCORE, false),
                new GameRecordPlayerRow(rival, true, NO_RANK, NO_SCORE, false)));

        assertThat(gameRecordDao.countByGameType(member))
                .extracting(GameTypeRecordCount::gameType, GameTypeRecordCount::played, GameTypeRecordCount::won)
                .containsExactlyInAnyOrder(
                        tuple(GameType.SONG, 2L, 1L),
                        tuple(GameType.HANGMAN, 1L, 1L));
    }

    @Test
    @DisplayName("한 판도 하지 않은 회원은 센 것이 없다.")
    void countsNothingForAMemberWithoutGames() {
        assertThat(gameRecordDao.countByGameType(saveMember())).isEmpty();
    }

    private static GameRecordRow newGame(GameType gameType, int startedPlayerCount) {
        return new GameRecordRow(UUID.randomUUID().toString(), gameType, STARTED_AT, ENDED_AT, startedPlayerCount);
    }

    private Long saveMember() {
        String name = "전적" + UUID.randomUUID().toString().substring(0, 8);
        return memberRepository.save(MemberEntity.builder()
                .loginId(name)
                .password("password")
                .nickname(name)
                .email(name + "@fun-game.club")
                .role(Role.USER)
                .build()).getId();
    }
}
