package com.fungame.songquiz.domain.record;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.fungame.songquiz.domain.session.FinishedGame;
import com.fungame.songquiz.domain.session.GameOutcome;
import com.fungame.songquiz.domain.session.PlayerOutcome;
import com.fungame.songquiz.enums.GameType;
import com.fungame.songquiz.enums.Role;
import com.fungame.songquiz.storage.GameRecordDao;
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
import org.springframework.dao.DataAccessException;

class GameRecordWriterTest extends ApiIntegrationTest {

    private static final Instant STARTED_AT = Instant.parse("2026-10-12T10:00:00Z");
    private static final Long MISSING_MEMBER_ID = Long.MAX_VALUE;

    @Autowired
    private GameRecordWriter gameRecordWriter;

    @Autowired
    private GameRecordDao gameRecordDao;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("끝난 판을 전적으로 남긴다.")
    void writesAFinishedGame() {
        Long winner = saveMember();
        Long loser = saveMember();

        gameRecordWriter.write(finishedSongGame(
                new PlayerOutcome(winner, true, 1, 2, false),
                new PlayerOutcome(loser, false, 2, 0, false)));

        assertThat(gameRecordDao.countByGameType(winner))
                .extracting(GameTypeRecordCount::gameType, GameTypeRecordCount::played, GameTypeRecordCount::won)
                .containsExactly(tuple(GameType.SONG, 1L, 1L));
    }

    @Test
    @DisplayName("같은 판을 두 번 써도 전적은 한 번이다.")
    void writesTheSameGameOnce() {
        Long winner = saveMember();
        FinishedGame game = finishedSongGame(
                new PlayerOutcome(winner, true, 1, 2, false),
                new PlayerOutcome(saveMember(), false, 2, 0, false));

        gameRecordWriter.write(game);
        gameRecordWriter.write(game);

        assertThat(gameRecordDao.countByGameType(winner))
                .extracting(GameTypeRecordCount::played)
                .containsExactly(1L);
    }

    @Test
    @DisplayName("참가자 한 명이라도 저장에 실패하면 그 판은 하나도 남지 않는다.")
    void leavesNothingOfAGameWhenAnyPlayerFails() {
        Long member = saveMember();
        FinishedGame game = finishedSongGame(
                new PlayerOutcome(member, true, 1, 2, false),
                new PlayerOutcome(MISSING_MEMBER_ID, false, 2, 0, false));

        assertThatThrownBy(() -> gameRecordWriter.write(game)).isInstanceOf(DataAccessException.class);

        assertThat(gameRecordDao.countByGameType(member)).isEmpty();
        assertThat(gameRecordDao.record(gameRowOf(game), List.of())).isTrue();
    }

    private static FinishedGame finishedSongGame(PlayerOutcome... players) {
        return new FinishedGame(UUID.randomUUID().toString(), GameType.SONG, STARTED_AT,
                new GameOutcome(List.of(players)));
    }

    private static GameRecordRow gameRowOf(FinishedGame game) {
        return new GameRecordRow(game.gameId(), game.gameType(), game.startedAt(), STARTED_AT,
                game.outcome().startedPlayerCount());
    }

    private Long saveMember() {
        String name = "기록" + UUID.randomUUID().toString().substring(0, 8);
        return memberRepository.save(MemberEntity.builder()
                .loginId(name)
                .password("password")
                .nickname(name)
                .email(name + "@fun-game.club")
                .role(Role.USER)
                .build()).getId();
    }
}
