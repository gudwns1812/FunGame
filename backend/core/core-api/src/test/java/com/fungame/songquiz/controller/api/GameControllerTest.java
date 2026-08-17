package com.fungame.songquiz.controller.api;

import com.fungame.songquiz.domain.member.MemberAdapter;
import com.fungame.songquiz.domain.room.GamePlayer;
import com.fungame.songquiz.domain.room.RoomSettings;
import com.fungame.songquiz.domain.room.GameRoomService;
import com.fungame.songquiz.domain.room.PlayerReadyInfo;
import com.fungame.songquiz.domain.room.RoomInfo;
import com.fungame.songquiz.domain.room.RoomStateInfo;
import com.fungame.songquiz.domain.session.GameService;
import com.fungame.songquiz.controller.request.ChangeRoomSettingsRequest;
import com.fungame.songquiz.controller.request.CreateRoomRequest;
import com.fungame.songquiz.controller.request.KickPlayerRequest;
import com.fungame.songquiz.enums.CSQuizDifficulty;
import com.fungame.songquiz.enums.Category;
import com.fungame.songquiz.enums.GameRoomStatus;
import com.fungame.songquiz.enums.GameType;
import com.fungame.songquiz.support.MemberFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.mockito.Mockito.verify;
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
                new RoomInfo(1L,
                        new RoomSettings(GameType.SONG, "K-POP 퀴즈방", 8, Category.KPOP, 10, 0, CSQuizDifficulty.HARD),
                        GamePlayer.createNewPlayer(2L, "방장닉네임"), GameRoomStatus.WAITING, 3)
        ));

        // when // then
        mockMvc.perform(get("/game/rooms"))
                .andExpect(status().isOk())
                .andDo(document("room-list",
                        preprocessResponse(prettyPrint())
                ));
    }

    @Test
    @DisplayName("방 참가자 목록의 각 참가자는 memberId, nickname, isReady 로 내려간다.")
    void findUsers() throws Exception {
        // given
        given(gameRoomService.findRoomState(1L)).willReturn(new RoomStateInfo(1L, 4, GameRoomStatus.WAITING,
                new RoomSettings(GameType.SONG, "K-POP 퀴즈방", 8, Category.KPOP, 10, 0, CSQuizDifficulty.HARD),
                List.of(new GamePlayer(2L, "방장닉네임", true), new GamePlayer(3L, "참가자닉네임", false)),
                new GamePlayer(2L, "방장닉네임", true)));

        // when // then
        mockMvc.perform(get("/game/rooms/1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value(4))
                .andExpect(jsonPath("$.data.hostMemberId").value(2))
                .andExpect(jsonPath("$.data.hostNickname").value("방장닉네임"))
                .andExpect(jsonPath("$.data.players[0].memberId").value(2))
                .andExpect(jsonPath("$.data.players[0].nickname").value("방장닉네임"))
                .andExpect(jsonPath("$.data.players[0].isReady").value(true))
                .andExpect(jsonPath("$.data.players[1].isReady").value(false))
                .andDo(document("room-users",
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
    @DisplayName("방장이 대기실에서 다른 플레이어를 내보낸다.")
    void kickPlayer() throws Exception {
        // when // then
        mockMvc.perform(post("/game/rooms/{roomId}/kick", 1L)
                        .content(objectMapper.writeValueAsString(new KickPlayerRequest(2L)))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(document("room-kick",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("roomId").description("방 ID")
                        )
                ));

        verify(gameRoomService).kickPlayer(1L, 1L, 2L);
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
        given(gameRoomService.findRoomState(1L)).willReturn(
                roomState(new RoomSettings(GameType.SONG, "K-POP 퀴즈방", 8, Category.KPOP, 10, 0, CSQuizDifficulty.HARD),
                        GamePlayer.createNewPlayer(2L, "방장닉네임")));

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
        RoomStateInfo current =
                roomState(new RoomSettings(GameType.SONG, "K-POP 퀴즈방", 8, Category.KPOP, 10, 0, CSQuizDifficulty.HARD),
                        GamePlayer.createNewPlayer(1L, "테스트유저"));
        RoomStateInfo changed =
                roomState(new RoomSettings(GameType.SONG, "K-POP 퀴즈방", 6, Category.BALLAD, 5, 0, CSQuizDifficulty.HARD),
                        GamePlayer.createNewPlayer(1L, "테스트유저"));

        given(gameRoomService.findRoomState(1L)).willReturn(current);
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

    private static RoomStateInfo roomState(RoomSettings settings, GamePlayer host) {
        return new RoomStateInfo(1L, 1, GameRoomStatus.WAITING, settings, List.of(host), host);
    }
}
