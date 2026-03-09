package com.fungame.songquiz.controller.api;

import com.fungame.songquiz.controller.config.argumentresolver.NickNameDecoder;
import com.fungame.songquiz.controller.request.CreateRoomRequest;
import com.fungame.songquiz.domain.GameRoomService;
import com.fungame.songquiz.domain.GameService;
import com.fungame.songquiz.domain.PlayerScore;
import com.fungame.songquiz.domain.dto.PlayersInfo;
import com.fungame.songquiz.domain.dto.RoomInfo;
import com.fungame.songquiz.support.response.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/game/rooms")
@RequiredArgsConstructor
@Slf4j
public class GameController {

    private final GameRoomService gameRoomService;
    private final GameService gameService;

    @GetMapping
    public ApiResponse<List<RoomInfo>> findAllRoom() {
        List<RoomInfo> rooms = gameRoomService.findAllRooms();
        return ApiResponse.success(rooms);
    }

    @GetMapping("/{roomId}/users")
    public ApiResponse<PlayersInfo> findUsers(@PathVariable Long roomId) {
        PlayersInfo users = gameRoomService.findUsers(roomId);
        return ApiResponse.success(users);
    }

    @GetMapping("/{roomId}/play/rank")
    public ApiResponse<List<PlayerScore>> findPlayingUsers(@PathVariable Long roomId) {
        List<PlayerScore> users = gameService.getPlayerRanks(roomId);
        return ApiResponse.success(users);
    }

    @PostMapping
    public ApiResponse<Long> createRoom(@RequestBody CreateRoomRequest request) {
        log.info("category: {}", request.getCategory());
        Long roomId = gameRoomService.createRoom(request.getGameType(),request.getTitle(), request.getMaxPlayers(), request.getHostName(),
                request.toGameInfo());
        return ApiResponse.success(roomId);
    }

    @PostMapping("/{roomId}/join")
    public ApiResponse<Integer> joinRoom(@PathVariable Long roomId, @NickNameDecoder String playerName) {
        int playerSequence = gameRoomService.joinRoom(roomId, playerName);
        return ApiResponse.success(playerSequence);
    }

    @PostMapping("/{roomId}/leave")
    public ApiResponse<Void> leaveRoom(@PathVariable Long roomId, @NickNameDecoder String playerName) {
        gameRoomService.leaveRoom(roomId, playerName);
        return ApiResponse.success();
    }

    @PostMapping("/{roomId}/start")
    public ApiResponse<Void> startGame(@PathVariable Long roomId, @NickNameDecoder String playerName) {
        gameService.startGame(roomId, playerName);
        return ApiResponse.success();
    }

    @PostMapping("/{roomId}/skip")
    public ApiResponse<Void> skipCurrentQuiz(@PathVariable Long roomId, @NickNameDecoder String playerName) {
        gameService.increaseSkipVote(roomId, playerName);
        return ApiResponse.success();
    }
}
