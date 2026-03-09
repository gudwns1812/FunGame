package com.fungame.songquiz.controller.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fungame.songquiz.controller.config.argumentresolver.NickNameDecodeResolver;
import com.fungame.songquiz.controller.config.interceptor.PlayerInterceptor;
import com.fungame.songquiz.controller.request.CreateRoomRequest;
import com.fungame.songquiz.domain.GameRoomService;
import com.fungame.songquiz.domain.GameService;
import com.fungame.songquiz.domain.GameRoomStatus;
import com.fungame.songquiz.domain.GameType;
import com.fungame.songquiz.domain.dto.RoomInfo;
import com.fungame.songquiz.support.RestDocsSupport;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(GameController.class)
class GameControllerTest extends RestDocsSupport {

    @MockitoBean
    private GameRoomService gameRoomService;

    @MockitoBean
    private GameService gameService;

    @MockitoBean
    private NickNameDecodeResolver nickNameDecodeResolver;

    @MockitoBean
    private PlayerInterceptor playerInterceptor;

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
                .hostName("방장닉네임")
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
