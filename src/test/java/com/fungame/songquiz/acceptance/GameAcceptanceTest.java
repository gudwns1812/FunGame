package com.fungame.songquiz.acceptance;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fungame.songquiz.controller.ApiControllerAdvice;
import com.fungame.songquiz.controller.api.GameController;
import com.fungame.songquiz.domain.GameRoomService;
import com.fungame.songquiz.domain.GameService;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
public class GameAcceptanceTest {

    private MockMvc mockMvc;

    @Mock
    private GameRoomService gameRoomService;

    @Mock
    private GameService gameService;

    @InjectMocks
    private GameController gameController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(gameController)
                .setControllerAdvice(new ApiControllerAdvice())
                .build();
    }

    @Test
    @DisplayName("방 생성 요청이 올바르게 처리되는지 검증한다")
    void createRoomTest() throws Exception {
        // given
        Map<String, Object> request = new HashMap<>();
        request.put("title", "테스트 방");
        request.put("maxPlayers", 5);
        request.put("name", "방장");

        given(gameRoomService.createRoom(anyString(), anyInt(), anyString())).willReturn("1");

        // when & then
        mockMvc.perform(post("/game/room")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(gameRoomService).createRoom("테스트 방", 5, "방장");
    }

    @Test
    @DisplayName("방 입장 요청이 올바르게 처리되는지 검증한다")
    void joinRoomTest() throws Exception {
        // when & then
        mockMvc.perform(post("/game/room/1/join")
                        .header("nickname", "플레이어2")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(gameRoomService).joinRoom("1", "플레이어2");
    }

    @Test
    @DisplayName("방 퇴장 요청이 올바르게 처리되는지 검증한다")
    void leaveRoomTest() throws Exception {
        // when & then
        mockMvc.perform(post("/game/room/1/leave")
                        .header("nickname", "플레이어2")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(gameRoomService).leaveRoom("1", "플레이어2");
    }

    @Test
    @DisplayName("게임 시작 요청이 올바르게 처리되는지 검증한다")
    void startGameTest() throws Exception {
        // when & then
        mockMvc.perform(post("/game/room/1/start")
                        .header("nickname", "방장")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(gameService).startGame("1", "방장");
    }
}
