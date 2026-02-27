package com.fungame.songquiz.storage;

import com.fungame.songquiz.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SongRepository extends JpaRepository<SongEntity, Long> {

    @Query("select s.id from SongEntity s where s.category = :category")
    List<Long> findByCategory(@Param("category") Category category);
}
