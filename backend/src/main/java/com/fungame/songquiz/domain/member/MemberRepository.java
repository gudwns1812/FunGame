package com.fungame.songquiz.domain.member;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    @Query("select m from Member m where m.loginId = :loginId")
    Optional<Member> findByLoginId(@Param("loginId") String loginId);

    Optional<Member> findByLoginIdAndEmail(String loginId, String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Member m where m.id = :id")
    Optional<Member> findByIdForUpdate(@Param("id") Long id);

    List<Member> findAllByIdInOrderByNicknameAsc(Collection<Long> ids);

    @Modifying(clearAutomatically = true)
    @Query("update Member m set m.status = :status where m.currentRoomId = :roomId")
    void updateStatusOfRoom(@Param("roomId") Long roomId, @Param("status") PlayerStatus status);

    @Modifying(clearAutomatically = true)
    @Query("update Member m set m.status = com.fungame.songquiz.domain.member.PlayerStatus.LOBBY, m.currentRoomId = null "
            + "where m.currentRoomId is not null or m.status <> com.fungame.songquiz.domain.member.PlayerStatus.LOBBY")
    int clearEveryLocation();

    boolean existsByLoginId(String loginId);

    boolean existsByNickname(String nickname);

    boolean existsByEmail(String email);
}
