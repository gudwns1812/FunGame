package com.fungame.songquiz.storage;

import com.fungame.songquiz.domain.Category;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SongRepository extends JpaRepository<SongEntity, Long> {

    @Query("select s from SongEntity s where s.category = :category")
    List<SongEntity> findByCategory(@Param("category") Category category);
}
