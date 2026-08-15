package com.fungame.songquiz.controller.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fungame.songquiz.domain.member.AuthService;
import com.fungame.songquiz.domain.member.PasswordResetService;
import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.error.ErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(RestDocumentationExtension.class)
class AuthControllerDocsTest {

    private MockMvc mockMvc;
    private final AuthService authService = mock(AuthService.class);
    private final PasswordResetService passwordResetService = mock(PasswordResetService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp(RestDocumentationContextProvider restDocumentation) {
        this.mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService, passwordResetService))
                .apply(documentationConfiguration(restDocumentation))
                .build();
    }

    @Test
    @DisplayName("닉네임 중복 확인 API - 중복되지 않은 경우")
    void checkNickname_notDuplicated() throws Exception {
        given(authService.checkNicknameDuplicate(anyString()))
                .willReturn(false);

        mockMvc.perform(
                        get("/api/auth/check-nickname")
                                .param("nickname", "uniqueNickname")
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data").value(false))
                .andDo(document("auth/check-nickname-not-duplicated",
                        queryParameters(
                                parameterWithName("nickname").description("중복 확인할 닉네임")
                        ),
                        responseFields(
                                fieldWithPath("result").description("응답 결과 (SUCCESS/FAIL)"),
                                fieldWithPath("data").type(JsonFieldType.BOOLEAN).description("중복 여부 (false: 중복 아님)"),
                                fieldWithPath("error").description("에러 정보 (성공 시 null)")
                        )
                ));
    }

    @Test
    @DisplayName("닉네임 중복 확인 API - 중복된 경우")
    void checkNickname_duplicated() throws Exception {
        given(authService.checkNicknameDuplicate(anyString()))
                .willReturn(true);

        mockMvc.perform(
                        get("/api/auth/check-nickname")
                                .param("nickname", "existingNickname")
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data").value(true))
                .andDo(document("auth/check-nickname-duplicated",
                        queryParameters(
                                parameterWithName("nickname").description("중복 확인할 닉네임")
                        ),
                        responseFields(
                                fieldWithPath("result").description("응답 결과 (SUCCESS/FAIL)"),
                                fieldWithPath("data").type(JsonFieldType.BOOLEAN).description("중복 여부 (true: 중복됨)"),
                                fieldWithPath("error").description("에러 정보 (성공 시 null)")
                        )
                ));
    }

    @Test
    @DisplayName("비밀번호 재설정 요청 API")
    void requestPasswordReset() throws Exception {
        mockMvc.perform(
                        post("/api/auth/password-reset-request")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        Map.of("loginId", "tester", "email", "tester@fun-game.club")))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andDo(document("auth/password-reset-request",
                        requestFields(
                                fieldWithPath("loginId").description("계정 아이디"),
                                fieldWithPath("email").description("가입할 때 등록한 이메일")
                        ),
                        responseFields(
                                fieldWithPath("result").description("응답 결과 (SUCCESS/FAIL)"),
                                fieldWithPath("data").type(JsonFieldType.NULL)
                                        .description("계정 열거를 막기 위해 일치 여부와 무관하게 항상 성공한다"),
                                fieldWithPath("error").description("에러 정보 (성공 시 null)")
                        )
                ));
    }

    @Test
    @DisplayName("비밀번호 재설정 API")
    void resetPassword() throws Exception {
        mockMvc.perform(
                        post("/api/auth/password-reset")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        Map.of("token", "메일 링크의 token 쿼리 파라미터", "newPassword", "new1234")))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andDo(document("auth/password-reset",
                        requestFields(
                                fieldWithPath("token").description("메일로 받은 일회용 토큰"),
                                fieldWithPath("newPassword").description("새 비밀번호")
                        ),
                        responseFields(
                                fieldWithPath("result").description("응답 결과 (SUCCESS/FAIL)"),
                                fieldWithPath("data").type(JsonFieldType.NULL).description("본문 없음"),
                                fieldWithPath("error").description("에러 정보 (성공 시 null)")
                        )
                ));
    }
}
