package com.fungame.songquiz.domain.report;

import com.fungame.songquiz.domain.quiz.Song;
import com.fungame.songquiz.domain.quiz.SongQuiz;
import com.fungame.songquiz.domain.room.GamePlayer;
import com.fungame.songquiz.domain.room.GameRoomManager;
import com.fungame.songquiz.domain.session.GameSession;
import com.fungame.songquiz.domain.session.GameSessionManager;
import com.fungame.songquiz.enums.Category;
import com.fungame.songquiz.enums.GameType;
import com.fungame.songquiz.enums.ReportReason;
import com.fungame.songquiz.enums.ReportSource;
import com.fungame.songquiz.enums.ReportStatus;
import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.error.ErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long ADMIN_ID = 9L;
    private static final Long REPORT_ID = 100L;
    private static final Long ROOM_ID = 42L;
    private static final long SONG_ID = 777L;
    private static final String SONG_TITLE = "밤편지";
    private static final String SINGER = "아이유";
    private static final String VIDEO_LINK = "https://youtu.be/BzYnNdJhZQw";
    private static final String SONG_HINT = "ㅂㅍㅈ";

    @Mock
    private GameRoomManager gameRoomManager;

    @Mock
    private GameSessionManager gameSessionManager;

    @Mock
    private ReportReader reportReader;

    @Mock
    private ReportWriter reportWriter;

    @Mock
    private ReportNotifier reportNotifier;

    private final Clock clock = Clock.fixed(Instant.parse("2026-08-18T00:00:00Z"), ZoneOffset.UTC);

    private ReportService reportService;

    @BeforeEach
    void setUp() {
        reportService = new ReportService(gameRoomManager, gameSessionManager, reportReader, reportWriter,
                reportNotifier, clock);
    }

    private static GameSession songSession() {
        SongQuiz quiz = new SongQuiz(List.of(Song.stored(SONG_ID, SONG_TITLE, SINGER, List.of(Category.KPOP),
                LocalDate.of(2017, 3, 24), VIDEO_LINK, 30, List.of(), SONG_HINT)), Category.KPOP);

        return new GameSession(quiz, List.of(GamePlayer.createNewPlayer(MEMBER_ID, "신고자")));
    }

    private void givenMemberIsInRoom() {
        given(gameRoomManager.hasPlayer(ROOM_ID, MEMBER_ID)).willReturn(true);
    }

    private Report savedReport() {
        ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
        verify(reportWriter).append(captor.capture());

        return captor.getValue();
    }

    private Report notifiedReport() {
        ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
        verify(reportNotifier).notifyReport(captor.capture());

        return captor.getValue();
    }

    @Test
    @DisplayName("라운드가 진행 중이면 문제 식별자와 정답·힌트까지 스냅샷으로 담는다.")
    void snapshotsWholeContextWhileRoundRunning() {
        givenMemberIsInRoom();
        GameSession session = songSession();
        session.startRound();
        given(gameSessionManager.getGameSession(ROOM_ID)).willReturn(session);

        reportService.receive(MEMBER_ID, inGameCommand(ReportReason.HINT_WRONG, null));

        ReportContext context = savedReport().getContext();
        assertThat(context.gameType()).isEqualTo(GameType.SONG);
        assertThat(context.contentId()).isEqualTo(SONG_ID);
        assertThat(context.roomId()).isEqualTo(ROOM_ID);
        assertThat(context.currentRound()).isEqualTo(1);
        assertThat(context.totalRound()).isEqualTo(1);
        assertThat(context.content()).isEqualTo(VIDEO_LINK);
        assertThat(context.answer()).contains(SONG_TITLE);
        assertThat(context.hint()).contains(SONG_HINT);
    }

    @Test
    @DisplayName("신고는 접수 시점에 열린 상태로 남는다.")
    void opensReport() {
        givenMemberIsInRoom();
        GameSession session = songSession();
        session.startRound();
        given(gameSessionManager.getGameSession(ROOM_ID)).willReturn(session);

        reportService.receive(MEMBER_ID, inGameCommand(ReportReason.ANSWER_WRONG, null));

        Report report = savedReport();
        assertThat(report.getMemberId()).isEqualTo(MEMBER_ID);
        assertThat(report.getReason()).isEqualTo(ReportReason.ANSWER_WRONG);
        assertThat(report.getStatus()).isEqualTo(ReportStatus.OPEN);
    }

    @Test
    @DisplayName("게임 중이지만 라운드가 시작되지 않았으면 게임 종류까지만 담는다.")
    void snapshotsOnlyGameTypeBeforeRoundStarts() {
        givenMemberIsInRoom();
        given(gameSessionManager.getGameSession(ROOM_ID)).willReturn(songSession());

        reportService.receive(MEMBER_ID, inGameCommand(ReportReason.CONTENT_NOT_SHOWN, null));

        ReportContext context = savedReport().getContext();
        assertThat(context.gameType()).isEqualTo(GameType.SONG);
        assertThat(context.roomId()).isEqualTo(ROOM_ID);
        assertThat(context.contentId()).isNull();
        assertThat(context.currentRound()).isNull();
        assertThat(context.content()).isNull();
        assertThat(context.answer()).isNull();
        assertThat(context.hint()).isNull();
    }

    @Test
    @DisplayName("대기실에서 신고하면 방의 게임 종류까지만 담는다.")
    void snapshotsRoomGameTypeInWaitingRoom() {
        givenMemberIsInRoom();
        given(gameSessionManager.getGameSession(ROOM_ID)).willReturn(null);
        given(gameRoomManager.getGameType(ROOM_ID)).willReturn(GameType.CS);

        reportService.receive(MEMBER_ID, inGameCommand(ReportReason.CONTENT_WRONG, null));

        ReportContext context = savedReport().getContext();
        assertThat(context.gameType()).isEqualTo(GameType.CS);
        assertThat(context.roomId()).isEqualTo(ROOM_ID);
        assertThat(context.contentId()).isNull();
    }

    @Test
    @DisplayName("로비에서 신고하면 방 정보 없이 사용자가 고른 게임 종류만 담는다.")
    void snapshotsNothingFromLobby() {
        reportService.receive(MEMBER_ID,
                new ReportCommand(ReportSource.LOBBY, null, ReportReason.ETC, "로그인하면 가끔 튕겨요", GameType.HANGMAN));

        ReportContext context = savedReport().getContext();
        assertThat(context.roomId()).isNull();
        assertThat(context.gameType()).isEqualTo(GameType.HANGMAN);
        assertThat(context.contentId()).isNull();
    }

    @Test
    @DisplayName("참여하지 않은 방을 가리키는 신고는 거절한다.")
    void rejectsReportAboutRoomTheMemberIsNotIn() {
        given(gameRoomManager.hasPlayer(ROOM_ID, MEMBER_ID)).willReturn(false);

        assertThatThrownBy(() -> reportService.receive(MEMBER_ID, inGameCommand(ReportReason.ANSWER_WRONG, null)))
                .isInstanceOf(CoreException.class)
                .hasFieldOrPropertyWithValue("type", ErrorType.REPORT_NOT_IN_ROOM);
        verify(reportWriter, never()).append(any());
    }

    @Test
    @DisplayName("분당 접수 한도를 채우면 더 받지 않는다.")
    void rejectsTooFrequentReports() {
        given(reportReader.countSince(eq(MEMBER_ID), any())).willReturn((long) ReportService.MAX_REPORTS_PER_MINUTE);

        assertThatThrownBy(() -> reportService.receive(MEMBER_ID, lobbyCommand("자꾸 눌렀어요")))
                .isInstanceOf(CoreException.class)
                .hasFieldOrPropertyWithValue("type", ErrorType.REPORT_RATE_LIMIT_EXCEEDED);
        verify(reportWriter, never()).append(any());
    }

    @Test
    @DisplayName("한도에 한 건 못 미치면 아직 받는다.")
    void acceptsReportUnderRateLimit() {
        given(reportReader.countSince(eq(MEMBER_ID), any())).willReturn((long) ReportService.MAX_REPORTS_PER_MINUTE - 1);

        reportService.receive(MEMBER_ID, lobbyCommand("마지막 한 건"));

        verify(reportWriter).append(any());
    }

    @Test
    @DisplayName("기타 사유인데 내용이 비어 있으면 거절한다.")
    void rejectsEtcWithoutDetail() {
        assertThatThrownBy(() -> reportService.receive(MEMBER_ID, lobbyCommand("  ")))
                .isInstanceOf(CoreException.class)
                .hasFieldOrPropertyWithValue("type", ErrorType.REPORT_DETAIL_REQUIRED);
        verify(reportWriter, never()).append(any());
    }

    @Test
    @DisplayName("게임 중 신고인데 방 번호가 없으면 만들 수 없다.")
    void rejectsInGameCommandWithoutRoom() {
        assertThatThrownBy(() ->
                new ReportCommand(ReportSource.IN_GAME, null, ReportReason.ANSWER_WRONG, null, null))
                .isInstanceOf(CoreException.class)
                .hasFieldOrPropertyWithValue("type", ErrorType.INVALID_INPUT_VALUE);
    }

    @Test
    @DisplayName("로비 신고인데 방 번호가 붙어 있으면 만들 수 없다.")
    void rejectsLobbyCommandWithRoom() {
        assertThatThrownBy(() ->
                new ReportCommand(ReportSource.LOBBY, ROOM_ID, ReportReason.ETC, "내용", null))
                .isInstanceOf(CoreException.class)
                .hasFieldOrPropertyWithValue("type", ErrorType.INVALID_INPUT_VALUE);
    }

    @Test
    @DisplayName("게임 중 신고는 사용자가 보낸 게임 종류를 믿지 않고 서버가 아는 것을 담는다.")
    void ignoresDeclaredGameTypeWhileInGame() {
        givenMemberIsInRoom();
        GameSession session = songSession();
        session.startRound();
        given(gameSessionManager.getGameSession(ROOM_ID)).willReturn(session);

        reportService.receive(MEMBER_ID,
                new ReportCommand(ReportSource.IN_GAME, ROOM_ID, ReportReason.HINT_WRONG, null, GameType.HANGMAN));

        assertThat(savedReport().getContext().gameType()).isEqualTo(GameType.SONG);
    }

    @Test
    @DisplayName("접수한 신고는 곧바로 알린다.")
    void notifiesReceivedReport() {
        givenMemberIsInRoom();
        GameSession session = songSession();
        session.startRound();
        given(gameSessionManager.getGameSession(ROOM_ID)).willReturn(session);

        reportService.receive(MEMBER_ID, inGameCommand(ReportReason.HINT_WRONG, null));

        assertThat(notifiedReport().getReason()).isEqualTo(ReportReason.HINT_WRONG);
    }

    @Test
    @DisplayName("알림에는 저장된 신고 번호가 실린다.")
    void notifiesWithSavedReportId() {
        givenMemberIsInRoom();
        GameSession session = songSession();
        session.startRound();
        given(gameSessionManager.getGameSession(ROOM_ID)).willReturn(session);
        given(reportWriter.append(any())).willReturn(REPORT_ID);

        reportService.receive(MEMBER_ID, inGameCommand(ReportReason.HINT_WRONG, null));

        assertThat(notifiedReport().getId()).isEqualTo(REPORT_ID);
    }

    @Test
    @DisplayName("같은 문제를 같은 사유로 다시 신고하면 저장은 하고 알리지는 않는다.")
    void savesButDoesNotNotifyDuplicateReport() {
        givenMemberIsInRoom();
        GameSession session = songSession();
        session.startRound();
        given(gameSessionManager.getGameSession(ROOM_ID)).willReturn(session);
        given(reportReader.existsSameReport(MEMBER_ID, SONG_ID, ReportReason.HINT_WRONG)).willReturn(true);

        reportService.receive(MEMBER_ID, inGameCommand(ReportReason.HINT_WRONG, null));

        verify(reportWriter).append(any());
        verify(reportNotifier, never()).notifyReport(any());
    }

    @Test
    @DisplayName("가리키는 문제가 없는 신고는 중복을 따지지 않고 알린다.")
    void notifiesEveryReportWithoutContent() {
        reportService.receive(MEMBER_ID, lobbyCommand("또 튕겨요"));

        verify(reportReader, never()).existsSameReport(any(), any(), any());
        verify(reportNotifier).notifyReport(any());
    }

    @Test
    @DisplayName("내가 접수한 신고만 돌려준다.")
    void findsOnlyMyReports() {
        given(reportReader.findMine(MEMBER_ID)).willReturn(List.of(storedReport()));

        assertThat(reportService.findMyReports(MEMBER_ID)).hasSize(1);
        verify(reportReader).findMine(MEMBER_ID);
    }

    @Test
    @DisplayName("상태를 주면 그 상태의 신고만 찾는다.")
    void findsReportsByStatus() {
        given(reportReader.findAll(ReportStatus.OPEN)).willReturn(List.of(storedReport()));

        assertThat(reportService.findAllReports(ReportStatus.OPEN)).hasSize(1);
    }

    @Test
    @DisplayName("답변을 남기면 신고에 붙는다.")
    void appendsComment() {
        given(reportReader.findById(REPORT_ID)).willReturn(storedReport());

        reportService.comment(ADMIN_ID, REPORT_ID, "힌트를 고쳤습니다.");

        ArgumentCaptor<ReportComment> captor = ArgumentCaptor.forClass(ReportComment.class);
        verify(reportWriter).appendComment(captor.capture());
        assertThat(captor.getValue().reportId()).isEqualTo(REPORT_ID);
        assertThat(captor.getValue().authorId()).isEqualTo(ADMIN_ID);
        assertThat(captor.getValue().content()).isEqualTo("힌트를 고쳤습니다.");
    }

    @Test
    @DisplayName("빈 답변은 남기지 않는다.")
    void rejectsBlankComment() {
        assertThatThrownBy(() -> reportService.comment(ADMIN_ID, REPORT_ID, "   "))
                .isInstanceOf(CoreException.class)
                .hasFieldOrPropertyWithValue("type", ErrorType.REPORT_COMMENT_REQUIRED);
        verify(reportWriter, never()).appendComment(any());
    }

    @Test
    @DisplayName("처리 상태를 바꾼다.")
    void changesStatus() {
        given(reportReader.findById(REPORT_ID)).willReturn(storedReport());

        reportService.changeStatus(REPORT_ID, ReportStatus.RESOLVED);

        ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
        verify(reportWriter).changeStatus(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ReportStatus.RESOLVED);
    }

    private static Report storedReport() {
        return Report.restore(REPORT_ID, MEMBER_ID, "신고자", ReportSource.IN_GAME, ReportReason.HINT_WRONG, null,
                new ReportContext(GameType.SONG, "KPOP", SONG_ID, ROOM_ID, 1, 1, VIDEO_LINK, "정답", "힌트"),
                ReportStatus.OPEN, LocalDateTime.of(2026, 8, 18, 0, 0), List.of());
    }

    private static ReportCommand inGameCommand(ReportReason reason, String detail) {
        return new ReportCommand(ReportSource.IN_GAME, ROOM_ID, reason, detail, null);
    }

    private static ReportCommand lobbyCommand(String detail) {
        return new ReportCommand(ReportSource.LOBBY, null, ReportReason.ETC, detail, null);
    }
}
