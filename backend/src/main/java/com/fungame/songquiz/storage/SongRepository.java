package com.fungame.songquiz.storage;

import com.fungame.songquiz.domain.Category;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SongRepository extends JpaRepository<SongEntity, Long> {

    @Query(value = "SELECT * FROM song_entity WHERE JSON_CONTAINS(categories, CAST(:category AS JSON))",
            nativeQuery = true)
    List<SongEntity> findByCategory(@Param("category") String category);
}
