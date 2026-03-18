package com.fungame.songquiz.controller.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fungame.songquiz.domain.SongService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AdminSongControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SongService songService;

    @Test
    @DisplayName("ADMIN 권한을 가진 사용자는 노래를 등록할 수 있다.")
    @WithMockUser(roles = "ADMIN")
    void createSong_Admin_Success() throws Exception {
        Map<String, Object> request = Map.of(
                "title", "테스트 노래",
                "singer", "테스트 가수",
                "categories", List.of("KPOP"),
                "answers", List.of("정답1", "정답2"),
                "releaseDate", "2024-03-16",
                "hint", "테스트 힌트"
        );

        mockMvc.perform(post("/api/admin/songs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("USER 권한을 가진 사용자는 노래를 등록할 수 없다 (403 Forbidden).")
    @WithMockUser(roles = "USER")
    void createSong_User_Forbidden() throws Exception {
        Map<String, Object> request = Map.of(
                "title", "테스트 노래",
                "singer", "테스트 가수"
        );

        mockMvc.perform(post("/api/admin/songs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("인증되지 않은 사용자는 노래를 등록할 수 없다 (401 Unauthorized).")
    void createSong_Anonymous_Unauthorized() throws Exception {
        Map<String, Object> request = Map.of(
                "title", "테스트 노래",
                "singer", "테스트 가수"
        );

        mockMvc.perform(post("/api/admin/songs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
