package com.fungame.songquiz.storage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SongRepository extends JpaRepository<SongEntity, Long> {

    @Query(value = """
            SELECT song_entity.* FROM song_entity
            JOIN song_category ON song_category.song_id = song_entity.id
            WHERE song_category.category = :category
            ORDER BY RAND() LIMIT :count
            """, nativeQuery = true)
    List<SongEntity> findRandomSongsByCategory(@Param("category") String category, @Param("count") int count);

    @Query(value = "SELECT * FROM song_entity ORDER BY RAND() LIMIT :count",
            nativeQuery = true)
    List<SongEntity> findRandomSongs(@Param("count") int count);

    Optional<SongEntity> findByVideoLink(String videoLink);

    boolean existsByTitleAndReleaseDate(String title, LocalDate releaseDate);

    boolean existsBySingerAndTitle(@Param("singer") String singer, @Param("title") String title);

    boolean existsByTitleContaining(String title);

    boolean existsByTitleContainingAndReleaseDate(String title, LocalDate releaseDate);
}
