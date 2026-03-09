package com.fungame.songquiz.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;

import com.fungame.songquiz.domain.Category;
import com.fungame.songquiz.domain.GameRoomService;
import com.fungame.songquiz.domain.GameService;
import com.fungame.songquiz.domain.GameTimer;
import com.fungame.songquiz.domain.GameType;
import com.fungame.songquiz.domain.event.GameEndEvent;
import com.fungame.songquiz.domain.event.GameResultEvent;
import com.fungame.songquiz.domain.event.GameStartEvent;
import com.fungame.songquiz.domain.event.RoundEndEvent;
import com.fungame.songquiz.domain.event.RoundStartEvent;
import com.fungame.songquiz.domain.gamecreator.CsQuizGameCreateInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ActiveProfiles("test")
@Import(GameServiceIntegrationTest.TestEventCapture.class)
public class GameServiceIntegrationTest {

    @Autowired
    private GameService gameService;

    @Autowired
    private GameRoomService gameRoomService;

    @MockBean
    private GameTimer gameTimer;

    @Autowired
    private TestEventCapture eventCapture;

    @Autowired
    private com.fungame.songquiz.storage.CounterRepository counterRepository;

    @Autowired
    private com.fungame.songquiz.storage.ComputerScienceRepository computerScienceRepository;

    private Long roomId;
    private final String hostName = "host";
    private final String player1 = "player1";

    @BeforeEach
    void setUp() {
        eventCapture.clear();
        
        // 데이터 초기화
        counterRepository.save(new com.fungame.songquiz.storage.CounterEntity(null, "GAME_ROOM_COUNTER", 0L));
        
        // CS 문제 데이터 추가 (정답을 명시적으로 알기 위해 고정)
        computerScienceRepository.save(com.fungame.songquiz.storage.ComputerScienceEntity.builder()
                .field("OS")
                .content("문제1")
                .answers(List.of("정답1"))
                .explanation("설명1")
                .difficulty(com.fungame.songquiz.domain.CSQuizDifficulty.EASY)
                .build());
        computerScienceRepository.save(com.fungame.songquiz.storage.ComputerScienceEntity.builder()
                .field("DB")
                .content("문제2")
                .answers(List.of("정답2"))
                .explanation("설명2")
                .difficulty(com.fungame.songquiz.domain.CSQuizDifficulty.NORMAL)
                .build());

        // 방 생성 및 입장
        roomId = gameRoomService.createRoom(
                GameType.CS, 
                "테스트 방", 
                5, 
                hostName, 
                new CsQuizGameCreateInfo(2)
        );
        gameRoomService.joinRoom(roomId, player1);
        gameRoomService.readyPlayer(roomId, player1); // player1도 준비 완료!

        // 타이머 동작 모킹: 순서 제어를 위해 콜백을 보관하거나 즉시 실행(약간의 지연 추가)
        doAnswer(invocation -> {
            Runnable callback = invocation.getArgument(2);
            // 약간의 딜레이를 주어 스레드 경쟁이나 순서 꼬임 방지
            new Thread(() -> {
                try { Thread.sleep(10); } catch (InterruptedException e) {}
                callback.run();
            }).start();
            return null;
        }).when(gameTimer).startAfter(any(), anyInt(), any());
    }

    @Test
    @DisplayName("전체 게임 흐름(시작-정답-종료)이 올바르게 동작하는지 검증한다")
    void fullGameFlowTest() {
        // 1. 게임 시작
        gameService.startGame(roomId, hostName);

        // GameStartEvent 및 첫 라운드 시작 대기
        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(eventCapture.getEvents(GameStartEvent.class)).hasSize(1);
            assertThat(eventCapture.getEvents(RoundStartEvent.class)).hasSize(1);
        });

        // 2. 1라운드 정답 입력 ("정답1" 또는 "정답2" - shuffle 때문)
        RoundStartEvent currentRound = eventCapture.getEvents(RoundStartEvent.class).get(0);
        String question = currentRound.content().data().get(2);
        String answer = question.equals("문제1") ? "정답1" : "정답2";

        gameService.processAnswer(roomId, player1, answer);

        // 1라운드 종료 확인 및 2라운드 시작 대기
        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(eventCapture.getEvents(RoundEndEvent.class)).hasSize(1);
            assertThat(eventCapture.getEvents(RoundStartEvent.class)).hasSize(2);
        });

        // 3. 2라운드 정답 입력
        RoundStartEvent nextRound = eventCapture.getEvents(RoundStartEvent.class).get(1);
        String nextAnswer = nextRound.content().data().get(2).equals("문제1") ? "정답1" : "정답2";

        gameService.processAnswer(roomId, player1, nextAnswer);

        // 4. 게임 전체 결과 및 종료 확인
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(eventCapture.getEvents(RoundEndEvent.class)).hasSize(2);
            assertThat(eventCapture.getEvents(GameResultEvent.class)).hasSize(1);
            assertThat(eventCapture.getEvents(GameEndEvent.class)).hasSize(1);
        });
    }

    @Component
    public static class TestEventCapture {
        private final List<Object> events = java.util.Collections.synchronizedList(new ArrayList<>());

        @EventListener
        public void capture(Object event) {
            if (event.getClass().getPackageName().startsWith("com.fungame.songquiz.domain.event")) {
                events.add(event);
            }
        }

        public void clear() {
            events.clear();
        }

        @SuppressWarnings("unchecked")
        public <T> List<T> getEvents(Class<T> type) {
            synchronized (events) {
                return events.stream()
                        .filter(type::isInstance)
                        .map(e -> (T) e)
                        .toList();
            }
        }
    }
}
