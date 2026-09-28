package com.fungame.songquiz.storage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CounterRepository extends JpaRepository<CounterEntity, Long> {

    CounterEntity findByName(String name);

    /**
     * 읽고-더하고-쓰기 대신 한 문장으로 올립니다. 동시에 두 요청이 들어와도 뒤엣것이 행 잠금에
     * 걸렸다가 갱신된 값 위에 더하므로 같은 번호가 두 번 나가지 않습니다.
     * <p>
     * 영속성 컨텍스트에 남아 있는 옛 값을 읽지 않도록 실행 전후로 flush · clear 합니다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update CounterEntity c set c.count = c.count + 1 where c.name = :name")
    int increment(@Param("name") String name);
}
