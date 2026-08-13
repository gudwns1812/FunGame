package com.fungame.songquiz.controller.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fungame.songquiz.controller.request.ChangeRoomSettingsRequest;
import com.fungame.songquiz.controller.request.CreateRoomRequest;
import com.fungame.songquiz.domain.GameRoomService;
import com.fungame.songquiz.domain.GameRoomStatus;
import com.fungame.songquiz.domain.GameService;
import com.fungame.songquiz.domain.GameType;
import com.fungame.songquiz.domain.Category;
import com.fungame.songquiz.domain.dto.PlayerReadyInfo;
import com.fungame.songquiz.domain.dto.RoomInfo;
import com.fungame.songquiz.domain.dto.RoomSettingsInfo;
import com.fungame.songquiz.domain.member.Member;
import com.fungame.songquiz.domain.member.MemberAdapter;
import com.fungame.songquiz.support.MemberFixture;
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
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.patch;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
                        return new MemberAdapter(MemberFixture.withId(1L, "테스트유저"));
                    }
                })
                .build();
    }

    @Test
    @DisplayName("방 목록을 조회한다.")
    void findAllRoom() throws Exception {
        // given
        given(gameRoomService.findAllRooms()).willReturn(List.of(
                new RoomInfo(1L, "K-POP 퀴즈방", 2L, "방장닉네임", GameRoomStatus.WAITING, 8, 3)
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

        given(gameRoomService.createRoom(any(), any())).willReturn(1L);

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
        given(gameRoomService.joinRoom(anyLong(), any())).willReturn(1);

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
    @DisplayName("플레이어가 준비 상태를 변경하면 바뀐 준비 상태를 돌려준다.")
    void playerReady() throws Exception {
        // given
        given(gameRoomService.readyPlayer(1L, 1L)).willReturn(new PlayerReadyInfo(1L, true, false));

        // when // then
        mockMvc.perform(post("/game/rooms/{roomId}/ready", 1L)
                        .header("playerName", "플레이어닉네임"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.memberId").value(1L))
                .andExpect(jsonPath("$.data.ready").value(true))
                .andExpect(jsonPath("$.data.isAllReady").value(false))
                .andDo(document("room-ready",
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("roomId").description("방 ID")
                        )
                ));
    }

    @Test
    @DisplayName("대기실에서 방 설정을 조회한다.")
    void findSettings() throws Exception {
        // given
        given(gameRoomService.findSettings(1L)).willReturn(
                new RoomSettingsInfo("K-POP 퀴즈방", GameType.SONG, 8, Category.KPOP, 10, 0, 2L, "방장닉네임"));

        // when // then
        mockMvc.perform(get("/game/rooms/{roomId}/settings", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("K-POP 퀴즈방"))
                .andExpect(jsonPath("$.data.gameType").value("SONG"))
                .andExpect(jsonPath("$.data.category").value("KPOP"))
                .andExpect(jsonPath("$.data.totalRound").value(10))
                .andDo(document("room-settings",
                        preprocessResponse(prettyPrint()),
                        pathParameters(parameterWithName("roomId").description("방 ID"))
                ));
    }

    @Test
    @DisplayName("방장이 대기실에서 방 설정을 변경한다.")
    void changeSettings() throws Exception {
        // given
        RoomSettingsInfo current =
                new RoomSettingsInfo("K-POP 퀴즈방", GameType.SONG, 8, Category.KPOP, 10, 0, 1L, "테스트유저");
        RoomSettingsInfo changed =
                new RoomSettingsInfo("K-POP 퀴즈방", GameType.SONG, 6, Category.BALLAD, 5, 0, 1L, "테스트유저");

        given(gameRoomService.findSettings(1L)).willReturn(current);
        given(gameRoomService.changeSettings(eq(1L), eq(1L), any())).willReturn(changed);

        ChangeRoomSettingsRequest request = ChangeRoomSettingsRequest.builder()
                .gameType(GameType.SONG)
                .maxPlayers(6)
                .category(Category.BALLAD)
                .totalRound(5)
                .build();

        // when // then
        mockMvc.perform(patch("/game/rooms/{roomId}/settings", 1L)
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("K-POP 퀴즈방"))
                .andExpect(jsonPath("$.data.maxPlayers").value(6))
                .andExpect(jsonPath("$.data.category").value("BALLAD"))
                .andDo(document("room-settings-change",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(parameterWithName("roomId").description("방 ID"))
                ));
    }
}
