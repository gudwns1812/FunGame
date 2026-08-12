package com.fungame.songquiz.domain.member;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    @Query("select m from Member m where m.loginId = :loginId")
    Optional<Member> findByLoginId(@Param("loginId") String loginId);

    Optional<Member> findByLoginIdAndEmail(String loginId, String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Member m where m.id = :id")
    Optional<Member> findByIdForUpdate(@Param("id") Long id);

    boolean existsByLoginId(String loginId);

    boolean existsByNickname(String nickname);

    boolean existsByEmail(String email);
}
