package com.fungame.songquiz.acceptance;

import com.fungame.songquiz.domain.room.RoomSettings;

import com.fungame.songquiz.storage.MemberEntity;

import com.fungame.songquiz.storage.ComputerScienceEntity;

import com.fungame.songquiz.enums.Role;

import com.fungame.songquiz.enums.CSQuizDifficulty;

import com.fungame.songquiz.storage.ComputerScienceRepository;
import com.fungame.songquiz.storage.MemberRepository;
import com.fungame.songquiz.support.TestEventCapture;
import com.fungame.songquiz.support.ApiIntegrationTest;
import com.fungame.songquiz.domain.room.GamePlayer;
import com.fungame.songquiz.domain.room.GameRoomService;
import com.fungame.songquiz.domain.session.GameResultEvent;
import com.fungame.songquiz.domain.session.GameService;
import com.fungame.songquiz.domain.session.GameStartEvent;
import com.fungame.songquiz.domain.session.RoundEndEvent;
import com.fungame.songquiz.domain.session.RoundStartEvent;
import com.fungame.songquiz.enums.GameType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

public class GameServiceIntegrationTest extends ApiIntegrationTest {

    private static final Duration LONGEST_GAME_FLOW_TRANSITION = Duration.ofSeconds(5);
    private static final long SETTLE_MILLIS = 10L;

    @Autowired
    private GameService gameService;

    @Autowired
    private GameRoomService gameRoomService;

    @Autowired
    private TestEventCapture eventCapture;

    @Autowired
    private ComputerScienceRepository computerScienceRepository;

    @Autowired
    private MemberRepository memberRepository;

    private Long roomId;
    private final String hostName = "host";
    private final String player1 = "player1";
    private GamePlayer host;
    private GamePlayer guest;

    @BeforeEach
    void setUp() {
        eventCapture.clear();

        // CS 문제 데이터 추가 (정답을 명시적으로 알기 위해 고정)
        computerScienceRepository.save(ComputerScienceEntity.builder()
                .field("OS")
                .content("문제1")
                .answers(List.of("정답1"))
                .explanation("설명1")
                .difficulty(CSQuizDifficulty.EASY)
                .build());
        computerScienceRepository.save(ComputerScienceEntity.builder()
                .field("DB")
                .content("문제2")
                .answers(List.of("정답2"))
                .explanation("설명2")
                .difficulty(CSQuizDifficulty.NORMAL)
                .build());

        // 방 생성 및 입장
        Long hostId = saveMember(hostName);
        Long player1Id = saveMember(player1);

        host = GamePlayer.createNewPlayer(hostId);
        guest = GamePlayer.createNewPlayer(player1Id);

        roomId = gameRoomService.createRoom(
                new RoomSettings(GameType.CS, "테스트 방", 5, null, 2, 0, CSQuizDifficulty.HARD),
                host
        );
        gameRoomService.joinRoom(roomId, guest);
        gameRoomService.readyPlayer(roomId, guest.memberId()); // player1도 준비 완료!

        doAnswer(invocation -> {
            if (isGameFlowTransition(invocation.getArgument(1))) {
                runShortlyAfter(invocation.getArgument(2));
            }
            return null;
        }).when(gameTimer).startAfter(any(), any(Duration.class), any());
    }

    private static boolean isGameFlowTransition(Duration delay) {
        return delay.compareTo(LONGEST_GAME_FLOW_TRANSITION) <= 0;
    }

    private static void runShortlyAfter(Runnable callback) {
        new Thread(() -> {
            try {
                Thread.sleep(SETTLE_MILLIS);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return;
            }
            callback.run();
        }).start();
    }

    @Test
    @DisplayName("전체 게임 흐름(시작-정답-종료)이 올바르게 동작하는지 검증한다")
    void fullGameFlowTest() {
        // 1. 게임 시작
        gameService.startGame(roomId, host.memberId());

        // GameStartEvent 및 첫 라운드 시작 대기
        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(eventCapture.getEvents(GameStartEvent.class)).hasSize(1);
            assertThat(eventCapture.getEvents(RoundStartEvent.class)).hasSize(1);
        });

        // 2. 1라운드 정답 입력 ("정답1" 또는 "정답2" - shuffle 때문)
        RoundStartEvent currentRound = eventCapture.getEvents(RoundStartEvent.class).get(0);
        String question = currentRound.content().data().get(2);
        String answer = question.equals("문제1") ? "정답1" : "정답2";

        gameService.processAnswer(roomId, guest.memberId(), answer);

        // 1라운드 종료 확인 및 2라운드 시작 대기
        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(eventCapture.getEvents(RoundEndEvent.class)).hasSize(1);
            assertThat(eventCapture.getEvents(RoundStartEvent.class)).hasSize(2);
        });

        // 3. 2라운드 정답 입력
        RoundStartEvent nextRound = eventCapture.getEvents(RoundStartEvent.class).get(1);
        String nextAnswer = nextRound.content().data().get(2).equals("문제1") ? "정답1" : "정답2";

        gameService.processAnswer(roomId, guest.memberId(), nextAnswer);

        // 4. 게임 전체 결과 및 종료 확인
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(eventCapture.getEvents(RoundEndEvent.class)).hasSize(2);
            assertThat(eventCapture.getEvents(GameResultEvent.class)).hasSize(1);
        });
    }



    private Long saveMember(String nickname) {
        return memberRepository.save(MemberEntity.builder()
                .loginId(nickname)
                .password("password")
                .nickname(nickname)
                .email(nickname + "@fun-game.club")
                .role(Role.USER)
                .build()).getId();
    }
}
