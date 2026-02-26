package com.fungame.songquiz.controller;

import com.fungame.songquiz.domain.GameRoomService;
import com.fungame.songquiz.support.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/game")
@RequiredArgsConstructor
@Slf4j
public class GameController {

    private final GameRoomService gameRoomService;

    @PostMapping("/room")
    public ApiResponse<String> createRoom(@RequestBody CreateRoomRequest request) {
        log.info("max player: {}", request.getMaxPlayers());
        String roomId = gameRoomService.createRoom(request.getTitle(), request.getMaxPlayers());
        return ApiResponse.success(roomId);
    }
}
