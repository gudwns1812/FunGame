package com.fungame.songquiz.controller.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fungame.songquiz.controller.request.CreateRoomRequest;
import com.fungame.songquiz.domain.GameRoomService;
import com.fungame.songquiz.domain.GameRoomStatus;
import com.fungame.songquiz.domain.GameService;
import com.fungame.songquiz.domain.GameType;
import com.fungame.songquiz.domain.dto.RoomInfo;
import com.fungame.songquiz.domain.member.Member;
import com.fungame.songquiz.domain.member.MemberAdapter;
import com.fungame.songquiz.domain.member.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(RestDocumentationExtension.class)
class GameControllerTest {

    private MockMvc mockMvc;
    private final GameRoomService gameRoomService = mock(GameRoomService.class);
    private final GameService gameService = mock(GameService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp(RestDocumentationContextProvider restDocumentation) {
        this.mockMvc = MockMvcBuilders.standaloneSetup(new GameController(gameRoomService, gameService))
                .apply(documentationConfiguration(restDocumentation))
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.getParameterType().isAssignableFrom(MemberAdapter.class);
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                        return new MemberAdapter(Member.builder()
                                .loginId("testUser")
                                .password("password")
                                .nickname("테스트유저")
                                .role(Role.USER)
                                .build());
                    }
                })
                .build();
    }

    @Test
    @DisplayName("방 목록을 조회한다.")
    void findAllRoom() throws Exception {
        // given
        given(gameRoomService.findAllRooms()).willReturn(List.of(
                new RoomInfo(1L, "K-POP 퀴즈방", "방장닉네임", GameRoomStatus.WAITING, 8, 3)
        ));

        // when // then
        mockMvc.perform(get("/game/rooms"))
                .andExpect(status().isOk())
                .andDo(document("room-list",
                        preprocessResponse(prettyPrint())
                ));
    }

    @Test
    @DisplayName("새로운 방을 생성한다.")
    void createRoom() throws Exception {
        // given
        CreateRoomRequest request = CreateRoomRequest.builder()
                .title("방 제목")
                .maxPlayers(8)
                .gameType(GameType.SONG)
                .totalRound(10)
                .build();

        given(gameRoomService.createRoom(any(), anyString(), anyInt(), anyString(), any()))
                .willReturn(1L);

        // when // then
        mockMvc.perform(post("/game/rooms")
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(document("room-create",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint())
                ));
    }

    @Test
    @DisplayName("방에 입장한다.")
    void joinRoom() throws Exception {
        // given
        given(gameRoomService.joinRoom(anyLong(), anyString())).willReturn(1);

        // when // then
        mockMvc.perform(post("/game/rooms/{roomId}/join", 1L)
                        .header("playerName", "플레이어닉네임"))
                .andExpect(status().isOk())
                .andDo(document("room-join",
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("roomId").description("방 ID")
                        )
                ));
    }

    @Test
    @DisplayName("게임을 시작한다.")
    void startGame() throws Exception {
        // when // then
        mockMvc.perform(post("/game/rooms/{roomId}/start", 1L)
                        .header("playerName", "방장닉네임"))
                .andExpect(status().isOk())
                .andDo(document("game-start",
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("roomId").description("방 ID")
                        )
                ));
    }

    @Test
    @DisplayName("플레이어가 준비 상태를 변경한다.")
    void playerReady() throws Exception {
        // when // then
        mockMvc.perform(post("/game/rooms/{roomId}/ready", 1L)
                        .header("playerName", "플레이어닉네임"))
                .andExpect(status().isOk())
                .andDo(document("room-ready",
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("roomId").description("방 ID")
                        )
                ));
    }
}
