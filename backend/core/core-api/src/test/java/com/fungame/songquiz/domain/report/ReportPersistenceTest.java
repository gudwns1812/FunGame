package com.fungame.songquiz.domain.report;

import com.fungame.songquiz.enums.GameType;
import com.fungame.songquiz.enums.ReportReason;
import com.fungame.songquiz.enums.ReportSource;
import com.fungame.songquiz.enums.ReportStatus;
import com.fungame.songquiz.storage.IntegrationTest;
import com.fungame.songquiz.storage.MemberRepository;
import com.fungame.songquiz.support.MemberFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class ReportPersistenceTest {

    private static final Long ROOM_ID = 7L;
    private static final Long CONTENT_ID = 4321L;

    @Autowired
    private ReportWriter reportWriter;

    @Autowired
    private ReportReader reportReader;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long saveMember(String name) {
        return memberRepository.save(MemberFixture.entityOf(name)).getId();
    }

    private Map<String, Object> reportRow(Long memberId) {
        return jdbcTemplate.queryForMap("select * from report where member_id = ?", memberId);
    }

    private static Report reportOf(Long memberId, ReportReason reason) {
        return Report.open(memberId, ReportSource.IN_GAME, reason, null,
                new ReportContext(GameType.SONG, "KPOP", CONTENT_ID, ROOM_ID, 2, 5,
                        "https://youtu.be/BzYnNdJhZQw", "아이유 - 밤편지", "아이유 - ㅂㅍㅈ"));
    }

    @Test
    @DisplayName("접수한 신고가 컨텍스트 스냅샷과 함께 DB 행에 남는다.")
    void persistsReportWithContextSnapshot() {
        Long memberId = saveMember("신고한사람");

        reportWriter.append(reportOf(memberId, ReportReason.HINT_WRONG));

        Map<String, Object> row = reportRow(memberId);
        assertThat(row.get("source")).isEqualTo("IN_GAME");
        assertThat(row.get("reason")).isEqualTo("HINT_WRONG");
        assertThat(row.get("status")).isEqualTo("OPEN");
        assertThat(row.get("game_type")).isEqualTo("SONG");
        assertThat(row.get("quiz_category")).isEqualTo("KPOP");
        assertThat(row.get("content_id")).isEqualTo(CONTENT_ID);
        assertThat(row.get("room_id")).isEqualTo(ROOM_ID);
        assertThat(row.get("current_round")).isEqualTo(2);
        assertThat(row.get("total_round")).isEqualTo(5);
        assertThat(row.get("quiz_content")).isEqualTo("https://youtu.be/BzYnNdJhZQw");
        assertThat(row.get("quiz_answer")).isEqualTo("아이유 - 밤편지");
        assertThat(row.get("quiz_hint")).isEqualTo("아이유 - ㅂㅍㅈ");
        assertThat(row.get("created_at")).isNotNull();
    }

    @Test
    @DisplayName("같은 회원이 같은 문제를 같은 사유로 또 신고하면 이미 있는 신고로 본다.")
    void findsSameReportAlreadyFiled() {
        Long memberId = saveMember("같은신고두번");

        reportWriter.append(reportOf(memberId, ReportReason.ANSWER_WRONG));

        assertThat(reportReader.existsSameReport(memberId, CONTENT_ID, ReportReason.ANSWER_WRONG)).isTrue();
        assertThat(reportReader.existsSameReport(memberId, CONTENT_ID, ReportReason.HINT_WRONG)).isFalse();
    }

    @Test
    @DisplayName("접수한 신고를 신고자별로 최근 순으로 읽는다.")
    void readsMyReports() {
        Long memberId = saveMember("내신고보는사람");
        Long otherMemberId = saveMember("남의신고");
        reportWriter.append(reportOf(memberId, ReportReason.HINT_WRONG));
        reportWriter.append(reportOf(otherMemberId, ReportReason.HINT_WRONG));

        List<Report> mine = reportReader.findMine(memberId);

        assertThat(mine).hasSize(1);
        assertThat(mine.getFirst().getMemberId()).isEqualTo(memberId);
        assertThat(mine.getFirst().getReporterNickname()).isEqualTo("내신고보는사람");
        assertThat(mine.getFirst().getContext().contentId()).isEqualTo(CONTENT_ID);
    }

    @Test
    @DisplayName("남긴 답변이 신고와 함께 읽힌다.")
    void readsCommentsWithReport() {
        Long memberId = saveMember("답변받는사람");
        Long adminId = saveMember("답변하는관리자");
        Long reportId = reportWriter.append(reportOf(memberId, ReportReason.ANSWER_WRONG));

        reportWriter.appendComment(ReportComment.write(reportId, adminId, "정답 표기를 고쳤습니다."));

        Report read = reportReader.findById(reportId);
        assertThat(read.getComments()).hasSize(1);
        assertThat(read.getComments().getFirst().content()).isEqualTo("정답 표기를 고쳤습니다.");
        assertThat(read.getComments().getFirst().authorNickname()).isEqualTo("답변하는관리자");
        assertThat(read.getComments().getFirst().createdAt()).isNotNull();
    }

    @Test
    @DisplayName("바꾼 처리 상태가 DB 행에 남는다.")
    void persistsStatusChange() {
        Long memberId = saveMember("상태바뀌는신고");
        Long reportId = reportWriter.append(reportOf(memberId, ReportReason.CONTENT_WRONG));

        Report report = reportReader.findById(reportId);
        report.changeStatus(ReportStatus.RESOLVED);
        reportWriter.changeStatus(report);

        assertThat(reportRow(memberId).get("status")).isEqualTo("RESOLVED");
    }

    @Test
    @DisplayName("신고를 지우면 답변도 함께 지워진다.")
    void deletesCommentsWithReport() {
        Long memberId = saveMember("지워지는신고");
        Long adminId = saveMember("지워지는답변의관리자");
        Long reportId = reportWriter.append(reportOf(memberId, ReportReason.CONTENT_NOT_SHOWN));

        reportWriter.appendComment(ReportComment.write(reportId, adminId, "답변"));
        jdbcTemplate.update("delete from report where id = ?", reportId);

        Integer remaining = jdbcTemplate.queryForObject(
                "select count(*) from report_comment where report_id = ?", Integer.class, reportId);
        assertThat(remaining).isZero();
    }

    @Test
    @DisplayName("최근 접수 건수를 회원별로 센다.")
    void countsRecentReportsPerMember() {
        Long memberId = saveMember("연타한사람");
        Long otherMemberId = saveMember("가만히있던사람");

        reportWriter.append(reportOf(memberId, ReportReason.ANSWER_WRONG));
        reportWriter.append(reportOf(memberId, ReportReason.HINT_WRONG));

        LocalDateTime aMinuteAgo = LocalDateTime.now().minusMinutes(1);
        assertThat(reportReader.countSince(memberId, aMinuteAgo)).isEqualTo(2);
        assertThat(reportReader.countSince(otherMemberId, aMinuteAgo)).isZero();
    }
}
