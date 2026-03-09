package com.fungame.songquiz.controller.config.interceptor;

import com.fungame.songquiz.domain.GameSessionManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlayerInterceptor implements HandlerInterceptor {

    private final GameSessionManager sessionManager;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        String playerName = request.getHeader("playerName");
        if (playerName == null) {
            return true;
        }

        Long roomId = sessionManager.getGameRoomIdByPlayer(playerName);
        if (roomId == null) {
            return true;
        }

        log.info("현재 게임중인 방으로 이동합니다.");

        response.setStatus(HttpServletResponse.SC_CONFLICT);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String jsonResponse = objectMapper.writeValueAsString(
                Map.of("message", "이미 진행 중인 게임이 있습니다.", "redirectRoomId", roomId)
        );
        response.getWriter().write(jsonResponse);

        return false;
    }
}
