package com.fungame.songquiz.domain.member;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    @Query("select m from Member m where m.loginId = :loginId")
    Optional<Member> findByLoginId(@Param("loginId") String loginId);

    boolean existsByLoginId(String loginId);

    boolean existsByNickname(String nickname);
}
