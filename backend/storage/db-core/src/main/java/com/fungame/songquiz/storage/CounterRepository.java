package com.fungame.songquiz.storage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CounterRepository extends JpaRepository<CounterEntity, Long> {

    CounterEntity findByName(String name);

    /** 읽고-더하고-쓰기가 아니라 한 문장이라 같은 번호가 두 번 나가지 않는다. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update CounterEntity c set c.count = c.count + 1 where c.name = :name")
    int increment(@Param("name") String name);
}
