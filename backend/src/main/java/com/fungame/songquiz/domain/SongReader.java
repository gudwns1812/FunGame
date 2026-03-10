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
    public static final String JSON_PREFIX = "\"";
    public static final String JSON_SUFFIX = "\"";
    private final SongRepository songRepository;

    public List<Song> findSongByCategoryWithCount(Category category, int count) {
        String jsonCategory = JSON_PREFIX + category.name() + JSON_SUFFIX;
        List<SongEntity> songs = songRepository.findRandomSongsByCategory(jsonCategory, count);

        return songs.stream()
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
                entity.getCategories(),
                entity.getReleaseDate(),
                entity.getVideoLink(),
                entity.getPlaySeconds(),
                entity.getAnswers()
        );
    }

    public List<Song> findSongWithCount(int songCount) {
        List<SongEntity> findSongs = songRepository.findRandomSongs(songCount);;

        return findSongs.stream()
                .map(SongEntity::toDomain)
                .toList();
    }
}
