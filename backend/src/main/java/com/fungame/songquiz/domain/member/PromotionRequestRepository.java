package com.fungame.songquiz.domain.member;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PromotionRequestRepository extends JpaRepository<PromotionRequest, Long> {
    
    @Query("SELECT pr FROM PromotionRequest pr JOIN FETCH pr.member WHERE pr.status = :status")
    List<PromotionRequest> findAllByStatusWithMember(@Param("status") PromotionStatus status);

    boolean existsByMemberAndStatus(Member member, PromotionStatus status);

    Optional<PromotionRequest> findTopByMemberOrderByCreatedAtDesc(Member member);
}
