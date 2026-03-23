package com.fungame.songquiz.controller.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fungame.songquiz.domain.member.AuthService;
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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(RestDocumentationExtension.class)
class AuthControllerDocsTest {

    private MockMvc mockMvc;
    private final AuthService authService = mock(AuthService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp(RestDocumentationContextProvider restDocumentation) {
        this.mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService))
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
}
