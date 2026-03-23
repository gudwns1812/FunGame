package com.fungame.songquiz.acceptance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fungame.songquiz.controller.ApiControllerAdvice;
import com.fungame.songquiz.controller.api.GameController;
import com.fungame.songquiz.domain.Category;
import com.fungame.songquiz.domain.GameRoomService;
import com.fungame.songquiz.domain.GameService;
import com.fungame.songquiz.domain.GameType;
import com.fungame.songquiz.domain.gamecreator.GameCreateInfo;
import com.fungame.songquiz.domain.gamecreator.SongGameCreateInfo;
import com.fungame.songquiz.domain.member.Member;
import com.fungame.songquiz.domain.member.MemberAdapter;
import com.fungame.songquiz.domain.member.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
                                .nickname("방장")
                                .role(Role.USER)
                                .build());
                    }
                })
                .build();
    }

    @Test
    @DisplayName("방 생성 요청이 올바르게 처리되는지 검증한다")
    void createRoomTest() throws Exception {
        // given
        Map<String, Object> request = new HashMap<>();
        request.put("gameType", "SONG");
        request.put("title", "테스트 방");
        request.put("maxPlayers", 5);
        request.put("category", "KPOP");
        request.put("totalRound", 10);

        given(gameRoomService.createRoom(eq(GameType.SONG), anyString(), anyInt(), anyString(), any(GameCreateInfo.class))).willReturn(
                1L);

        // when & then
        mockMvc.perform(post("/game/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(gameRoomService).createRoom(eq(GameType.SONG), eq("테스트 방"), eq(5), eq("방장"), any(SongGameCreateInfo.class));
    }

    @Test
    @DisplayName("방 입장 요청이 올바르게 처리되는지 검증한다")
    void joinRoomTest() throws Exception {
        // when & then
        mockMvc.perform(post("/game/rooms/1/join")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(gameRoomService).joinRoom(1L, "방장");
    }

    @Test
    @DisplayName("방 퇴장 요청이 올바르게 처리되는지 검증한다")
    void leaveRoomTest() throws Exception {
        // when & then
        mockMvc.perform(post("/game/rooms/1/leave")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(gameRoomService).leaveRoom(1L, "방장");
    }

    @Test
    @DisplayName("게임 시작 요청이 올바르게 처리되는지 검증한다")
    void startGameTest() throws Exception {
        // when & then
        mockMvc.perform(post("/game/rooms/1/start")
                        .header("playerName", "방장")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(gameService).startGame(1L, "방장");
    }
}
