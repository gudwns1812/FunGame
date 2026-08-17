package com.fungame.songquiz.controller.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fungame.songquiz.domain.member.MemberAdapter;
import com.fungame.songquiz.domain.report.ReportCommand;
import com.fungame.songquiz.domain.report.ReportService;
import com.fungame.songquiz.enums.GameType;
import com.fungame.songquiz.enums.ReportReason;
import com.fungame.songquiz.enums.ReportSource;
import com.fungame.songquiz.support.MemberFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(RestDocumentationExtension.class)
class ReportControllerDocsTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long ROOM_ID = 42L;

    private MockMvc mockMvc;
    private final ReportService reportService = mock(ReportService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp(RestDocumentationContextProvider restDocumentation) {
        this.mockMvc = MockMvcBuilders.standaloneSetup(new ReportController(reportService))
                .apply(documentationConfiguration(restDocumentation))
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.getParameterType().isAssignableFrom(MemberAdapter.class);
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                        return new MemberAdapter(MemberFixture.withId(MEMBER_ID, "신고한사람"));
                    }
                })
                .build();
    }

    private static Map<String, Object> body(Object... keyValues) {
        Map<String, Object> body = new HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            body.put((String) keyValues[i], keyValues[i + 1]);
        }

        return body;
    }

    private ReportCommand receivedCommand() {
        ArgumentCaptor<ReportCommand> captor = ArgumentCaptor.forClass(ReportCommand.class);
        verify(reportService).receive(eq(MEMBER_ID), captor.capture());

        return captor.getValue();
    }

    @Test
    @DisplayName("게임 중 신고 API - 게임 정보는 보내지 않는다.")
    void reportInGame() throws Exception {
        mockMvc.perform(
                        post("/api/reports")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(body(
                                        "source", "IN_GAME",
                                        "roomId", ROOM_ID,
                                        "reason", "HINT_WRONG",
                                        "detail", null,
                                        "gameType", null)))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andDo(document("report-in-game",
                        requestFields(
                                fieldWithPath("source").description("신고를 보낸 자리 (IN_GAME/LOBBY)"),
                                fieldWithPath("roomId").description("신고할 방 번호. IN_GAME 이면 필수, LOBBY 면 null"),
                                fieldWithPath("reason").description(
                                        "사유 코드 (CONTENT_NOT_SHOWN/CONTENT_WRONG/HINT_WRONG/ANSWER_WRONG/ETC)"),
                                fieldWithPath("detail").type(JsonFieldType.STRING).optional()
                                        .description("직접 작성한 내용. ETC 이면 필수"),
                                fieldWithPath("gameType").type(JsonFieldType.STRING).optional()
                                        .description("LOBBY 신고에서 사용자가 고른 게임 종류. IN_GAME 이면 서버가 아는 값을 쓴다")
                        ),
                        responseFields(
                                fieldWithPath("result").description("응답 결과 (SUCCESS/FAIL)"),
                                fieldWithPath("data").type(JsonFieldType.NULL).description("본문 없음"),
                                fieldWithPath("error").description("에러 정보 (성공 시 null)")
                        )
                ));

        ReportCommand command = receivedCommand();
        assertThat(command.source()).isEqualTo(ReportSource.IN_GAME);
        assertThat(command.roomId()).isEqualTo(ROOM_ID);
        assertThat(command.reason()).isEqualTo(ReportReason.HINT_WRONG);
    }

    @Test
    @DisplayName("로비 신고 API - 사용자가 직접 쓴다.")
    void reportFromLobby() throws Exception {
        mockMvc.perform(
                        post("/api/reports")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(body(
                                        "source", "LOBBY",
                                        "roomId", null,
                                        "reason", "ETC",
                                        "detail", "로그인하면 가끔 튕겨요",
                                        "gameType", "SONG")))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andDo(document("report-lobby",
                        requestFields(
                                fieldWithPath("source").description("신고를 보낸 자리 (IN_GAME/LOBBY)"),
                                fieldWithPath("roomId").type(JsonFieldType.NUMBER).optional()
                                        .description("LOBBY 신고에는 방이 없다"),
                                fieldWithPath("reason").description("사유 코드"),
                                fieldWithPath("detail").description("직접 작성한 내용"),
                                fieldWithPath("gameType").description("사용자가 고른 게임 종류 (SONG/CS/HANGMAN)")
                        ),
                        responseFields(
                                fieldWithPath("result").description("응답 결과 (SUCCESS/FAIL)"),
                                fieldWithPath("data").type(JsonFieldType.NULL).description("본문 없음"),
                                fieldWithPath("error").description("에러 정보 (성공 시 null)")
                        )
                ));

        ReportCommand command = receivedCommand();
        assertThat(command.source()).isEqualTo(ReportSource.LOBBY);
        assertThat(command.roomId()).isNull();
        assertThat(command.detail()).isEqualTo("로그인하면 가끔 튕겨요");
        assertThat(command.gameType()).isEqualTo(GameType.SONG);
    }
}
