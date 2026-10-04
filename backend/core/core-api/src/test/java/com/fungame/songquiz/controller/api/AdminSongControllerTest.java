package com.fungame.songquiz.controller.api;

import com.fungame.songquiz.enums.Role;
import com.fungame.songquiz.storage.MemberEntity;
import com.fungame.songquiz.storage.MemberRepository;
import com.fungame.songquiz.support.ApiIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 권한 검사를 {@code @WithMockUser} 가 아니라 실제 로그인으로 확인한다. MockMvc 전용 장치를
 * 걷어내면 다른 통합 테스트와 같은 컨텍스트를 쓸 수 있고, 세션과 보안 필터 체인까지 함께 지난다.
 */
public class AdminSongControllerTest extends ApiIntegrationTest {

    private static final String PASSWORD = "password1";

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("ADMIN 권한을 가진 사용자는 노래를 등록할 수 있다.")
    void createSongAsAdmin() {
        ResponseEntity<String> response = createSong(loginAs("adminuser", Role.ADMIN));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("USER 권한을 가진 사용자는 노래를 등록할 수 없다 (403 Forbidden).")
    void createSongAsUserIsForbidden() {
        ResponseEntity<String> response = createSong(loginAs("normaluser", Role.USER));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("인증되지 않은 사용자는 노래를 등록할 수 없다 (401 Unauthorized).")
    void createSongAnonymousIsUnauthorized() {
        ResponseEntity<String> response = createSong(null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private ResponseEntity<String> createSong(String sessionCookie) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (sessionCookie != null) {
            headers.add(HttpHeaders.COOKIE, sessionCookie);
        }

        Map<String, Object> request = Map.of(
                "title", "테스트 노래",
                "singer", "테스트 가수",
                "categories", List.of("KPOP"),
                "answers", List.of("정답1", "정답2"),
                "releaseDate", "2024-03-16",
                "hint", "테스트 힌트"
        );

        return restTemplate.postForEntity("/api/admin/songs", new HttpEntity<>(request, headers), String.class);
    }

    private String loginAs(String loginId, Role role) {
        memberRepository.save(MemberEntity.builder()
                .loginId(loginId)
                .password(passwordEncoder.encode(PASSWORD))
                .nickname(loginId)
                .email(loginId + "@fun-game.club")
                .role(role)
                .build());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/auth/login",
                new HttpEntity<>(Map.of("loginId", loginId, "password", PASSWORD), headers),
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        String setCookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).isNotNull();

        return setCookie.split(";", 2)[0];
    }
}
