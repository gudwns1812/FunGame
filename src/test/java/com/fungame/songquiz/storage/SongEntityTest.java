package com.fungame.songquiz.storage;

import com.fungame.songquiz.domain.Category;
import java.time.LocalDate;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SongEntityTest {

    @Autowired
    SongRepository songRepository;

    @Test
    void song엔티티_저장시_컨버터로_복수정답을_저장한다() {
        //given
        SongEntity song = new SongEntity(null, "A", "a", Category.BALLADE, LocalDate.of(2015, 2, 12), "Dd", 2,
                List.of("test", "test2"));
        //when
        SongEntity save = songRepository.save(song);
        //then
        SongEntity findSong = songRepository.findById(save.getId()).orElse(null);
        Assertions.assertThat(findSong.getAnswers()).isNotNull()
                .hasSize(2);
    }
}
