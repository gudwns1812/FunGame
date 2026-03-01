package com.fungame.songquiz.domain;

import com.fungame.songquiz.storage.SongEntity;
import com.fungame.songquiz.storage.SongRepository;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SongReader {
    private final SongRepository songRepository;

    public List<Song> findSongByCategoryWithCount(Category category, int count) {
        List<SongEntity> songs = songRepository.findByCategory(category);

        Collections.shuffle(songs);

        return songs.stream()
                .limit(count)
                .map(SongEntity::toDomain)
                .toList();
    }

    public Song findById(Long id) {
        return songRepository.findById(id)
                .map(this::toDomain)
                .orElse(null);
    }

    private Song toDomain(SongEntity entity) {
        return Song.of(
                entity.getTitle(),
                entity.getSinger(),
                entity.getCategory(),
                entity.getReleaseDate(),
                entity.getVideoLink(),
                entity.getPlaySeconds(),
                entity.getAnswers()
        );
    }
}
