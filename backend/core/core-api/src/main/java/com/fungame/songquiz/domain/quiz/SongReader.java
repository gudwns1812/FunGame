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

    private final SongRepository songRepository;

    @Transactional(readOnly = true)
    public List<Song> findSongByCategoryWithCount(Category category, int count) {
        List<SongEntity> songs = songRepository.findRandomSongsByCategory(category.name(), count);

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
        return Song.stored(
                entity.getId(),
                entity.getTitle(),
                entity.getSinger(),
                List.copyOf(entity.getCategories()),
                entity.getReleaseDate(),
                entity.getVideoLink(),
                entity.getPlaySeconds(),
                entity.getAnswers(),
                entity.getHint()
        );
    }
}
