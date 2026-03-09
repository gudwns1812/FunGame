package com.fungame.songquiz.domain;

import com.fungame.songquiz.domain.gamecreator.SongGameCreateInfo;
import com.fungame.songquiz.domain.gamecreator.SongGameFactory;
import com.fungame.songquiz.storage.CounterEntity;
import com.fungame.songquiz.storage.CounterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GameRoomServiceTest {

    @Mock
    CounterRepository counterRepository;

    @Mock
    SongReader songReader;

    @Mock
    GameRoomManager gameRoomManager;

    @Mock
    ApplicationEventPublisher applicationEventPublisher;

    GameRoomService service;

    @BeforeEach
    void setUp() {
        SongGameFactory gameFactory = new SongGameFactory(songReader);

        service = new GameRoomService(
                counterRepository,
                List.of(gameFactory),
                gameRoomManager,
                applicationEventPublisher
        );
    }

    @Test
    void gameRoom을_만들면_counter가_증가해야한다() {
        // given
        SongGameCreateInfo info = new SongGameCreateInfo(Category.KPOP, 10);

        CounterEntity counter = new CounterEntity(1L, "GAME_ROOM_COUNTER", 0L);
        Game game = mock(Game.class);
        Song song = mock(Song.class);

        given(songReader.findSongByCategoryWithCount(info.category(),info.songCount())).willReturn(List.of(song));
        given(counterRepository.findByName("GAME_ROOM_COUNTER")).willReturn(counter);

        // when
        Long roomId = service.createRoom(GameType.SONG, "방2", 8, "방장", info);

        // then
        assertThat(roomId).isEqualTo(1L);
        assertThat(counter.getCount()).isEqualTo(1L);

        verify(gameRoomManager).createGameRoom(
                eq(1L),
                eq("방2"),
                any(Game.class),
                eq("방장"),
                eq(8)
        );
    }
}