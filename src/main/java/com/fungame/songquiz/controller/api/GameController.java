package com.fungame.songquiz.controller.api;

import com.fungame.songquiz.controller.request.CreateRoomRequest;
import com.fungame.songquiz.domain.GameRoomService;
import com.fungame.songquiz.domain.GameService;
import com.fungame.songquiz.support.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/game")
@RequiredArgsConstructor
@Slf4j
public class GameController {

    private final GameRoomService gameRoomService;
    private final GameService gameService;

    @PostMapping("/room")
    public ApiResponse<String> createRoom(@RequestBody CreateRoomRequest request) {
        log.info("max player: {}", request.getMaxPlayers());
        String roomId = gameRoomService.createRoom(request.getTitle(), request.getMaxPlayers(), request.getName());
        return ApiResponse.success(roomId);
    }

    @PostMapping("/room/{roomId}/join")
    public ApiResponse<Void> joinRoom(@PathVariable String roomId, @RequestHeader("nickname") String playerName) {
        gameRoomService.joinRoom(roomId, playerName);
        return ApiResponse.success();
    }

    @PostMapping("/room/{roomId}/leave")
    public ApiResponse<Void> leaveRoom(@PathVariable String roomId , @RequestHeader("nickname") String nickName) {
        gameRoomService.leaveRoom(roomId, nickName);
        return ApiResponse.success();
    }

    @PostMapping("/room/{roomId}/start")
    public ApiResponse<Void> startGame(@PathVariable String roomId, @RequestHeader("nickname") String nickname) {
        gameService.startGame(roomId, nickname);
        return ApiResponse.success();
    }
}
