package com.fungame.songquiz.storage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface SongRepository extends JpaRepository<SongEntity, Long> {

    @Query(value = "SELECT * FROM song_entity WHERE JSON_CONTAINS(categories, CAST(:category AS JSON)) ORDER BY RAND() LIMIT :count",
            nativeQuery = true)
    List<SongEntity> findRandomSongsByCategory(@Param("category") String category, @Param("count") int count);

    @Query(value = "SELECT * FROM song_entity ORDER BY RAND() LIMIT :count",
            nativeQuery = true)
    List<SongEntity> findRandomSongs(@Param("count") int count);

    boolean existsBySingerAndTitle(@Param("singer") String singer, @Param("title") String title);

    boolean existsByTitleContaining(String title);

    boolean existsByTitleContainingAndReleaseDate(String title, LocalDate releaseDate);
}
