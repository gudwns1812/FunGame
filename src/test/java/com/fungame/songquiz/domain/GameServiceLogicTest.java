package com.fungame.songquiz.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fungame.songquiz.domain.event.CorrectAnswerEvent;
import com.fungame.songquiz.domain.event.GameEndEvent;
import com.fungame.songquiz.domain.event.RoundTimeoutEvent;
import com.fungame.songquiz.domain.event.TimerTickEvent;
import com.fungame.songquiz.storage.GameRoomEntity;
import com.fungame.songquiz.storage.GameRoomRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

class GameServiceLogicTest {

    private GameService gameService;
    private GameRoomRepository gameRoomRepository;
    private SongReader songReader;
    private ApplicationEventPublisher publisher;
    private RedissonClient redissonClient;
    private RedisTemplate<String, Object> redisTemplate;

    @BeforeEach
    void setUp() {
        gameRoomRepository = mock(GameRoomRepository.class);
        songReader = mock(SongReader.class);
        publisher = mock(ApplicationEventPublisher.class);
        redissonClient = mock(RedissonClient.class);
        redisTemplate = mock(RedisTemplate.class);
        gameService = new GameService(gameRoomRepository, songReader, publisher, redissonClient, redisTemplate);

        RLock lock = mock(RLock.class);
        given(redissonClient.getLock(anyString())).willReturn(lock);
        try {
            given(lock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).willReturn(true);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    @DisplayName("라운드 타임아웃 발생 시 5초 대기 후 다음 노래로 넘어간다")
    void handleRoundTimeoutTest() {
        // given
        String roomId = "1";
        GameRoomEntity gameRoomEntity = GameRoomEntity.builder()
                .id(roomId)
                .status(GameRoomStatus.PLAYING)
                .songIds(List.of(101L, 102L))
                .currentSongIndex(0)
                .playerNames(new ArrayList<>(List.of("p1")))
                .build();

        given(gameRoomRepository.findById(roomId)).willReturn(Optional.of(gameRoomEntity));

        // when - 타임아웃 발생
        gameService.handleRoundTimeout(roomId, 0);

        // then - 바로 저장되지는 않음 (스케줄링됨)
        verify(gameRoomRepository, never()).save(any());

        // when - 5초 대기 후 handleNextRound 호출 시뮬레이션
        gameService.handleNextRound(roomId, 0);

        // then
        assertThat(gameRoomEntity.getCurrentSongIndex()).isEqualTo(1);
        verify(gameRoomRepository).save(gameRoomEntity);
        verify(publisher).publishEvent(any(RoundTimeoutEvent.class));
    }

    @Test
    @DisplayName("마지막 라운드 타임아웃 발생 시 5초 대기 후 게임이 종료된다")
    void gameEndOnTimeoutTest() {
        // given
        String roomId = "1";
        GameRoomEntity gameRoomEntity = GameRoomEntity.builder()
                .id(roomId)
                .status(GameRoomStatus.PLAYING)
                .songIds(List.of(101L))
                .currentSongIndex(0)
                .playerNames(new ArrayList<>(List.of("p1")))
                .build();

        given(gameRoomRepository.findById(roomId)).willReturn(Optional.of(gameRoomEntity));
        ZSetOperations zSetOperations = mock(ZSetOperations.class);
        given(redisTemplate.opsForZSet()).willReturn(zSetOperations);

        // when - 라운드 종료 시뮬레이션 (타임아웃 후 5초 대기 경과)
        gameService.handleNextRound(roomId, 0);

        // then - 상태는 FINISHED가 되지만 아직 GameEndEvent는 안나감
        assertThat(gameRoomEntity.isFinished()).isTrue();
        verify(publisher, never()).publishEvent(any(GameEndEvent.class));

        // when - 다시 5초 대기 후 handleNextRound 호출 (최종 종료 처리)
        gameService.handleNextRound(roomId, 1);

        // then - 이제서야 GameEndEvent 발행
        verify(publisher).publishEvent(any(GameEndEvent.class));
    }

    @Test
    @DisplayName("정답을 맞췄을 때 바로 다음 노래로 넘어가지 않고 이벤트를 발행한다")
    void processCorrectAnswerTest() {
        // given
        String roomId = "1";
        String nickname = "p1";
        String message = "정답";
        GameRoomEntity gameRoomEntity = GameRoomEntity.builder()
                .id(roomId)
                .status(GameRoomStatus.PLAYING)
                .songIds(List.of(101L, 102L))
                .currentSongIndex(0)
                .playerNames(new ArrayList<>(List.of(nickname)))
                .build();

        Song song = mock(Song.class);
        given(song.isCorrect(message)).willReturn(true);
        given(songReader.findById(101L)).willReturn(song);
        given(gameRoomRepository.findById(roomId)).willReturn(Optional.of(gameRoomEntity));

        ZSetOperations<String, Object> zSetOperations = mock(ZSetOperations.class);
        given(redisTemplate.opsForZSet()).willReturn(zSetOperations);
        doReturn(10.0).when(zSetOperations).score(anyString(), any(Object.class));

        // when
        gameService.processAnswer(roomId, nickname, message);

        // then
        assertThat(gameRoomEntity.getCurrentSongIndex()).isEqualTo(0); // 바로 넘어가지 않음
        verify(publisher).publishEvent(any(CorrectAnswerEvent.class));
    }

    @Test
    @DisplayName("라운드 시작 시 1초마다 타이머 이벤트를 발행한다")
    void scheduleRoundTimeoutTimerTest() throws InterruptedException {
        // given
        String roomId = "1";
        GameRoomEntity gameRoomEntity = GameRoomEntity.builder()
                .id(roomId)
                .status(GameRoomStatus.PLAYING)
                .songIds(List.of(101L, 102L))
                .currentSongIndex(0)
                .playerNames(new ArrayList<>(List.of("p1")))
                .build();

        given(gameRoomRepository.findById(roomId)).willReturn(Optional.of(gameRoomEntity));

        // when
        gameService.handleNextRound(roomId, 0); // songIndex가 0인 상태에서 다음 라운드로 넘어가는 요청

        // then
        Thread.sleep(1500); // 1초 대기 (0초에 한 번, 1초에 한 번 발생 예상)
        verify(publisher, atLeast(1)).publishEvent(any(TimerTickEvent.class));
    }
}
