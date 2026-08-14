package com.fungame.songquiz.storage;

import com.fungame.songquiz.enums.PromotionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PromotionRequestRepository extends JpaRepository<PromotionRequestEntity, Long> {
    
    @Query("SELECT pr FROM PromotionRequestEntity pr JOIN FETCH pr.member WHERE pr.status = :status")
    List<PromotionRequestEntity> findAllByStatusWithMember(@Param("status") PromotionStatus status);

    boolean existsByMemberAndStatus(MemberEntity member, PromotionStatus status);

    Optional<PromotionRequestEntity> findTopByMemberOrderByCreatedAtDesc(MemberEntity member);
}
