package com.fungame.songquiz.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import com.fungame.songquiz.storage.GameRoom;
import com.fungame.songquiz.storage.GameRoomRepository;
import com.fungame.songquiz.storage.GameRoomStatus;
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

import static org.mockito.ArgumentMatchers.*;

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
    @DisplayName("라운드 타임아웃 발생 시 다음 노래로 넘어가고 이벤트를 발행한다")
    void handleRoundTimeoutTest() {
        // given
        String roomId = "1";
        GameRoom gameRoom = GameRoom.builder()
                .id(roomId)
                .status(GameRoomStatus.PLAYING)
                .songIds(List.of(101L, 102L))
                .currentSongIndex(0)
                .playerNames(new ArrayList<>(List.of("p1")))
                .build();

        given(gameRoomRepository.findById(roomId)).willReturn(Optional.of(gameRoom));

        // when
        gameService.handleRoundTimeout(roomId, 0);

        // then
        assertThat(gameRoom.getCurrentSongIndex()).isEqualTo(1);
        verify(gameRoomRepository).save(gameRoom);
        verify(publisher).publishEvent(any(RoundTimeoutEvent.class));
    }

    @Test
    @DisplayName("마지막 라운드 타임아웃 발생 시 게임이 종료되고 결과 이벤트를 발행한다")
    void gameEndOnTimeoutTest() {
        // given
        String roomId = "1";
        GameRoom gameRoom = GameRoom.builder()
                .id(roomId)
                .status(GameRoomStatus.PLAYING)
                .songIds(List.of(101L))
                .currentSongIndex(0)
                .playerNames(new ArrayList<>(List.of("p1")))
                .build();

        given(gameRoomRepository.findById(roomId)).willReturn(Optional.of(gameRoom));
        ZSetOperations zSetOperations = mock(ZSetOperations.class);
        given(redisTemplate.opsForZSet()).willReturn(zSetOperations);

        // when
        gameService.handleRoundTimeout(roomId, 0);

        // then
        assertThat(gameRoom.isFinished()).isTrue();
        verify(publisher).publishEvent(any(GameEndEvent.class));
    }

    @Test
    @DisplayName("정답을 맞췄을 때 바로 다음 노래로 넘어가지 않고 이벤트를 발행한다")
    void processCorrectAnswerTest() {
        // given
        String roomId = "1";
        String nickname = "p1";
        String message = "정답";
        GameRoom gameRoom = GameRoom.builder()
                .id(roomId)
                .status(GameRoomStatus.PLAYING)
                .songIds(List.of(101L, 102L))
                .currentSongIndex(0)
                .playerNames(new ArrayList<>(List.of(nickname)))
                .build();

        Song song = mock(Song.class);
        given(song.isCorrect(message)).willReturn(true);
        given(songReader.findById(101L)).willReturn(song);
        given(gameRoomRepository.findById(roomId)).willReturn(Optional.of(gameRoom));
        
        ZSetOperations<String, Object> zSetOperations = mock(ZSetOperations.class);
        given(redisTemplate.opsForZSet()).willReturn(zSetOperations);
        doReturn(10.0).when(zSetOperations).score(anyString(), any(Object.class));

        // when
        gameService.processAnswer(roomId, nickname, message);

        // then
        assertThat(gameRoom.getCurrentSongIndex()).isEqualTo(0); // 바로 넘어가지 않음
        verify(publisher).publishEvent(any(CorrectAnswerEvent.class));
    }

    @Test
    @DisplayName("라운드 시작 시 1초마다 타이머 이벤트를 발행한다")
    void scheduleRoundTimeoutTimerTest() throws InterruptedException {
        // given
        String roomId = "1";
        GameRoom gameRoom = GameRoom.builder()
                .id(roomId)
                .status(GameRoomStatus.PLAYING)
                .songIds(List.of(101L, 102L))
                .currentSongIndex(0)
                .playerNames(new ArrayList<>(List.of("p1")))
                .build();

        given(gameRoomRepository.findById(roomId)).willReturn(Optional.of(gameRoom));

        // when
        gameService.handleNextRound(roomId, -1); // 처음 시작 시나 다음 라운드 진입 시 호출됨

        // then
        Thread.sleep(1500); // 1초 대기 (0초에 한 번, 1초에 한 번 발생 예상)
        verify(publisher, atLeast(1)).publishEvent(any(TimerTickEvent.class));
    }
}
