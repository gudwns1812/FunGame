package com.fungame.songquiz.api.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fungame.songquiz.domain.member.MemberAdapter;
import com.fungame.songquiz.domain.record.GameRecordService;
import com.fungame.songquiz.domain.record.GameRecordSummary;
import com.fungame.songquiz.domain.record.GameTypeRecord;
import com.fungame.songquiz.enums.GameType;
import com.fungame.songquiz.support.MemberFixture;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.core.MethodParameter;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@ExtendWith(RestDocumentationExtension.class)
class GameRecordControllerDocsTest {

    private static final Long MEMBER_ID = 1L;

    private MockMvc mockMvc;
    private final GameRecordService gameRecordService = mock(GameRecordService.class);

    @BeforeEach
    void setUp(RestDocumentationContextProvider restDocumentation) {
        this.mockMvc = MockMvcBuilders.standaloneSetup(new GameRecordController(gameRecordService))
                .apply(documentationConfiguration(restDocumentation))
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.getParameterType().isAssignableFrom(MemberAdapter.class);
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                        return new MemberAdapter(MemberFixture.withId(MEMBER_ID, "전적보는사람"));
                    }
                })
                .build();
    }

    @Test
    @DisplayName("내 전적 조회 API - 게임 종류마다 따로 판수 · 승 · 패 · 승률을 준다.")
    void findMyRecords() throws Exception {
        given(gameRecordService.findMine(MEMBER_ID)).willReturn(GameRecordSummary.of(List.of(
                new GameTypeRecord(GameType.SONG, 3, 2),
                new GameTypeRecord(GameType.HANGMAN, 1, 0))));

        mockMvc.perform(get("/api/records/mine"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].gameType").value("SONG"))
                .andExpect(jsonPath("$.data[0].lost").value(1))
                .andExpect(jsonPath("$.data[0].winRate").value(67))
                .andExpect(jsonPath("$.data[1].gameType").value("CS"))
                .andExpect(jsonPath("$.data[1].played").value(0))
                .andExpect(jsonPath("$.data[1].winRate").doesNotExist())
                .andDo(document("game-record-mine",
                        responseFields(
                                fieldWithPath("result").description("응답 결과 (SUCCESS/FAIL)"),
                                fieldWithPath("data[]").description("노래 · CS · 행맨 순서의 세 줄. 한 판도 안 한 게임도 빠지지 않는다"),
                                fieldWithPath("data[].gameType").description("게임 종류 (SONG/CS/HANGMAN)"),
                                fieldWithPath("data[].played").description("끝까지 간 판 수 (탈주한 판 포함)"),
                                fieldWithPath("data[].won").description("이긴 판 수"),
                                fieldWithPath("data[].lost").description("진 판 수"),
                                fieldWithPath("data[].winRate").type(JsonFieldType.NUMBER).optional()
                                        .description("반올림한 승률(%). 한 판도 안 했으면 null"),
                                fieldWithPath("error").description("에러 정보 (성공 시 null)")
                        )
                ));
    }
}
