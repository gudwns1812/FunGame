package com.fungame.songquiz.controller.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fungame.songquiz.domain.member.MemberAdapter;
import com.fungame.songquiz.domain.report.Report;
import com.fungame.songquiz.domain.report.ReportComment;
import com.fungame.songquiz.domain.report.ReportContext;
import com.fungame.songquiz.domain.report.ReportService;
import com.fungame.songquiz.enums.GameType;
import com.fungame.songquiz.enums.ReportReason;
import com.fungame.songquiz.enums.ReportSource;
import com.fungame.songquiz.enums.ReportStatus;
import com.fungame.songquiz.support.MemberFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.patch;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(RestDocumentationExtension.class)
class AdminReportControllerDocsTest {

    private static final Long ADMIN_ID = 9L;
    private static final Long REPORTER_ID = 1L;
    private static final Long REPORT_ID = 7L;

    private MockMvc mockMvc;
    private final ReportService reportService = mock(ReportService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp(RestDocumentationContextProvider restDocumentation) {
        this.mockMvc = MockMvcBuilders.standaloneSetup(new AdminReportController(reportService))
                .apply(documentationConfiguration(restDocumentation))
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.getParameterType().isAssignableFrom(MemberAdapter.class);
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                        return new MemberAdapter(MemberFixture.withId(ADMIN_ID, "관리자"));
                    }
                })
                .build();
    }

    private static Report storedReport() {
        return Report.restore(REPORT_ID, REPORTER_ID, "신고한사람", ReportSource.IN_GAME, ReportReason.HINT_WRONG, null,
                new ReportContext(GameType.SONG, "KPOP", 777L, 42L, 2, 5,
                        "https://youtu.be/BzYnNdJhZQw", "아이유 - 밤편지", "아이유 - ㅂㅍㅈ"),
                ReportStatus.OPEN, LocalDateTime.of(2026, 8, 18, 0, 0),
                List.of(new ReportComment(1L, REPORT_ID, ADMIN_ID, "관리자", "힌트를 고쳤습니다.",
                        LocalDateTime.of(2026, 8, 18, 1, 0))));
    }

    @Test
    @DisplayName("신고 목록 조회 API - 관리자에게는 정답과 힌트까지 보여준다.")
    void findReports() throws Exception {
        given(reportService.findAllReports(ReportStatus.OPEN)).willReturn(List.of(storedReport()));

        mockMvc.perform(get("/api/admin/reports").param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].reporterNickname").value("신고한사람"))
                .andExpect(jsonPath("$.data[0].quizAnswer").value("아이유 - 밤편지"))
                .andExpect(jsonPath("$.data[0].quizHint").value("아이유 - ㅂㅍㅈ"))
                .andExpect(jsonPath("$.data[0].contentId").value(777))
                .andDo(document("admin-report-list",
                        queryParameters(
                                parameterWithName("status").optional()
                                        .description("걸러볼 처리 상태 (OPEN/RESOLVED). 없으면 전체")
                        ),
                        responseFields(
                                fieldWithPath("result").description("응답 결과 (SUCCESS/FAIL)"),
                                fieldWithPath("data[].id").description("신고 번호"),
                                fieldWithPath("data[].memberId").description("신고자 회원 번호"),
                                fieldWithPath("data[].reporterNickname").description("신고자 닉네임"),
                                fieldWithPath("data[].source").description("접수 위치 (IN_GAME/LOBBY)"),
                                fieldWithPath("data[].reason").description("사유 코드"),
                                fieldWithPath("data[].detail").type(JsonFieldType.STRING).optional()
                                        .description("직접 작성한 내용"),
                                fieldWithPath("data[].gameType").type(JsonFieldType.STRING).optional()
                                        .description("게임 종류"),
                                fieldWithPath("data[].quizCategory").type(JsonFieldType.STRING).optional()
                                        .description("카테고리"),
                                fieldWithPath("data[].contentId").type(JsonFieldType.NUMBER).optional()
                                        .description("신고 대상 행의 식별자"),
                                fieldWithPath("data[].roomId").type(JsonFieldType.NUMBER).optional()
                                        .description("방 번호"),
                                fieldWithPath("data[].currentRound").type(JsonFieldType.NUMBER).optional()
                                        .description("접수 시점의 라운드"),
                                fieldWithPath("data[].totalRound").type(JsonFieldType.NUMBER).optional()
                                        .description("전체 라운드"),
                                fieldWithPath("data[].quizContent").type(JsonFieldType.STRING).optional()
                                        .description("접수 시점의 문제"),
                                fieldWithPath("data[].quizAnswer").type(JsonFieldType.STRING).optional()
                                        .description("접수 시점의 정답"),
                                fieldWithPath("data[].quizHint").type(JsonFieldType.STRING).optional()
                                        .description("접수 시점의 힌트"),
                                fieldWithPath("data[].status").description("처리 상태 (OPEN/RESOLVED)"),
                                fieldWithPath("data[].createdAt").description("접수 시각"),
                                fieldWithPath("data[].comments[].id").description("답변 번호"),
                                fieldWithPath("data[].comments[].authorNickname").description("답변한 관리자"),
                                fieldWithPath("data[].comments[].content").description("답변 내용"),
                                fieldWithPath("data[].comments[].createdAt").description("답변 시각"),
                                fieldWithPath("error").description("에러 정보 (성공 시 null)")
                        )
                ));
    }

    @Test
    @DisplayName("상태를 주지 않으면 전체를 찾는다.")
    void findsEveryReportWithoutStatus() throws Exception {
        given(reportService.findAllReports(null)).willReturn(List.of(storedReport()));

        mockMvc.perform(get("/api/admin/reports"))
                .andExpect(status().isOk());

        verify(reportService).findAllReports(null);
    }

    @Test
    @DisplayName("신고에 답변을 남기는 API")
    void comment() throws Exception {
        mockMvc.perform(
                        post("/api/admin/reports/{reportId}/comments", REPORT_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of("content", "힌트를 고쳤습니다.")))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andDo(document("admin-report-comment",
                        pathParameters(
                                parameterWithName("reportId").description("답변할 신고 번호")
                        ),
                        requestFields(
                                fieldWithPath("content").description("답변 내용. 신고자에게 그대로 보인다")
                        ),
                        responseFields(
                                fieldWithPath("result").description("응답 결과 (SUCCESS/FAIL)"),
                                fieldWithPath("data").type(JsonFieldType.NULL).description("본문 없음"),
                                fieldWithPath("error").description("에러 정보 (성공 시 null)")
                        )
                ));

        verify(reportService).comment(ADMIN_ID, REPORT_ID, "힌트를 고쳤습니다.");
    }

    @Test
    @DisplayName("처리 상태를 바꾸는 API")
    void changeStatus() throws Exception {
        mockMvc.perform(
                        patch("/api/admin/reports/{reportId}/status", REPORT_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of("status", "RESOLVED")))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andDo(document("admin-report-status",
                        pathParameters(
                                parameterWithName("reportId").description("상태를 바꿀 신고 번호")
                        ),
                        requestFields(
                                fieldWithPath("status").description("바꿀 처리 상태 (OPEN/RESOLVED)")
                        ),
                        responseFields(
                                fieldWithPath("result").description("응답 결과 (SUCCESS/FAIL)"),
                                fieldWithPath("data").type(JsonFieldType.NULL).description("본문 없음"),
                                fieldWithPath("error").description("에러 정보 (성공 시 null)")
                        )
                ));

        verify(reportService).changeStatus(REPORT_ID, ReportStatus.RESOLVED);
    }
}
