package com.fungame.songquiz.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SongStore {

    private final SongRepository songRepository;

    public Optional<SongEntity> findById(Long id) {
        return songRepository.findById(id);
    }

    public List<SongEntity> findAll() {
        return songRepository.findAll();
    }

    public SongEntity save(SongEntity entity) {
        return songRepository.save(entity);
    }

    public List<SongEntity> findRandomSongsByCategory(String category, int count) {
        return songRepository.findRandomSongsByCategory(category, count);
    }

    public List<SongEntity> findRandomSongs(int count) {
        return songRepository.findRandomSongs(count);
    }

    public boolean existsBySingerAndTitle(String singer, String title) {
        return songRepository.existsBySingerAndTitle(singer, title);
    }

    public boolean existsByTitleContaining(String title) {
        return songRepository.existsByTitleContaining(title);
    }

    public boolean existsByTitleContainingAndReleaseDate(String title, LocalDate releaseDate) {
        return songRepository.existsByTitleContainingAndReleaseDate(title, releaseDate);
    }
}
