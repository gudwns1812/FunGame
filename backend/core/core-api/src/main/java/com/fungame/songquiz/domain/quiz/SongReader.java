package com.fungame.songquiz.domain.quiz;

import com.fungame.songquiz.enums.Category;
import com.fungame.songquiz.storage.SongEntity;
import com.fungame.songquiz.storage.SongRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SongReader {
    public static final String JSON_PREFIX = "\"";
    public static final String JSON_SUFFIX = "\"";
    private final SongRepository songRepository;

    @Transactional(readOnly = true)
    public List<Song> findSongByCategoryWithCount(Category category, int count) {
        String jsonCategory = JSON_PREFIX + category.name() + JSON_SUFFIX;
        List<SongEntity> songs = songRepository.findRandomSongsByCategory(jsonCategory, count);

        return songs.stream()
                .map(this::toDomain)
                .toList();
    }

    @Transactional(readOnly = true)
    public Song findById(Long id) {
        return songRepository.findById(id)
                .map(this::toDomain)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<Song> findSongWithCount(int songCount) {
        List<SongEntity> findSongs = songRepository.findRandomSongs(songCount);

        return findSongs.stream()
                .map(this::toDomain)
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean existsSameSong(Song song) {
        return songRepository.existsBySingerAndTitle(song.getSinger(), song.getTitle())
                || songRepository.existsByTitleAndReleaseDate(song.getTitle(), song.getReleaseDate());
    }

    @Transactional(readOnly = true)
    public boolean existsByTitleLike(String title, LocalDate releaseDate) {
        if (releaseDate == null) {
            return songRepository.existsByTitleContaining(title);
        }

        return songRepository.existsByTitleContainingAndReleaseDate(title, releaseDate);
    }

    private Song toDomain(SongEntity entity) {
        return Song.of(
                entity.getTitle(),
                entity.getSinger(),
                entity.getCategories(),
                entity.getReleaseDate(),
                entity.getVideoLink(),
                entity.getPlaySeconds(),
                entity.getAnswers(),
                entity.getHint()
        );
    }
}
