package com.fungame.songquiz.storage;

import com.fungame.songquiz.enums.ReportReason;
import com.fungame.songquiz.enums.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReportRepository extends JpaRepository<ReportEntity, Long> {

    boolean existsByMemberIdAndContentIdAndReason(Long memberId, Long contentId, ReportReason reason);

    long countByMemberIdAndCreatedAtAfter(Long memberId, LocalDateTime createdAt);

    @Query("select r from ReportEntity r join fetch r.member"
            + " left join fetch r.comments c left join fetch c.author"
            + " where r.member.id = :memberId order by r.createdAt desc")
    List<ReportEntity> findAllByMemberWithComments(@Param("memberId") Long memberId);

    @Query("select r from ReportEntity r join fetch r.member"
            + " left join fetch r.comments c left join fetch c.author"
            + " order by r.createdAt desc")
    List<ReportEntity> findAllWithComments();

    @Query("select r from ReportEntity r join fetch r.member"
            + " left join fetch r.comments c left join fetch c.author"
            + " where r.status = :status order by r.createdAt desc")
    List<ReportEntity> findAllByStatusWithComments(@Param("status") ReportStatus status);

    @Query("select r from ReportEntity r join fetch r.member"
            + " left join fetch r.comments c left join fetch c.author"
            + " where r.id = :id")
    Optional<ReportEntity> findByIdWithComments(@Param("id") Long id);
}
