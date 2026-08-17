package com.fungame.songquiz.storage;

import com.fungame.songquiz.enums.ReportReason;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface ReportRepository extends JpaRepository<ReportEntity, Long> {

    boolean existsByMemberIdAndContentIdAndReason(Long memberId, Long contentId, ReportReason reason);

    long countByMemberIdAndCreatedAtAfter(Long memberId, LocalDateTime createdAt);
}
