package com.fungame.songquiz.storage;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(name = "report_comment")
public class ReportCommentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false)
    private ReportEntity report;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private MemberEntity author;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private ReportCommentEntity(ReportEntity report, MemberEntity author, String content) {
        this.report = report;
        this.author = author;
        this.content = content;
    }

    public static ReportCommentEntity write(ReportEntity report, MemberEntity author, String content) {
        return new ReportCommentEntity(report, author, content);
    }
}
