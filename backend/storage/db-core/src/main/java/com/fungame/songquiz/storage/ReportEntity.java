package com.fungame.songquiz.storage;

import com.fungame.songquiz.enums.GameType;
import com.fungame.songquiz.enums.ReportReason;
import com.fungame.songquiz.enums.ReportSource;
import com.fungame.songquiz.enums.ReportStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(name = "report")
public class ReportEntity {

    public record GameContext(
            GameType gameType,
            String quizCategory,
            Long contentId,
            Long roomId,
            Integer currentRound,
            Integer totalRound,
            String quizContent,
            String quizAnswer,
            String quizHint
    ) {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private MemberEntity member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportSource source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportReason reason;

    @Column(columnDefinition = "TEXT")
    private String detail;

    @Enumerated(EnumType.STRING)
    private GameType gameType;

    private String quizCategory;

    private Long contentId;

    private Long roomId;

    private Integer currentRound;

    private Integer totalRound;

    @Column(columnDefinition = "TEXT")
    private String quizContent;

    @Column(columnDefinition = "TEXT")
    private String quizAnswer;

    @Column(columnDefinition = "TEXT")
    private String quizHint;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportStatus status;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "report")
    @OrderBy("createdAt asc")
    private List<ReportCommentEntity> comments = new ArrayList<>();

    private ReportEntity(MemberEntity member, ReportSource source, ReportReason reason, String detail,
                         GameContext context) {
        this.member = member;
        this.source = source;
        this.reason = reason;
        this.detail = detail;
        this.gameType = context.gameType();
        this.quizCategory = context.quizCategory();
        this.contentId = context.contentId();
        this.roomId = context.roomId();
        this.currentRound = context.currentRound();
        this.totalRound = context.totalRound();
        this.quizContent = context.quizContent();
        this.quizAnswer = context.quizAnswer();
        this.quizHint = context.quizHint();
        this.status = ReportStatus.OPEN;
    }

    public static ReportEntity open(MemberEntity member, ReportSource source, ReportReason reason, String detail,
                                    GameContext context) {
        return new ReportEntity(member, source, reason, detail, context);
    }

    public void changeStatus(ReportStatus status) {
        this.status = status;
    }
}
