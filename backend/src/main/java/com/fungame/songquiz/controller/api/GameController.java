package com.fungame.songquiz.controller.api;

import com.fungame.songquiz.controller.request.CreateRoomRequest;
import com.fungame.songquiz.domain.GameAction;
import com.fungame.songquiz.domain.GameRoomService;
import com.fungame.songquiz.domain.GameService;
import com.fungame.songquiz.domain.PlayerScore;
import com.fungame.songquiz.domain.dto.PlayersInfo;
import com.fungame.songquiz.domain.dto.RoomInfo;
import com.fungame.songquiz.domain.member.MemberAdapter;
import com.fungame.songquiz.support.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping("/{roomId}/health")
    public ApiResponse<String> healthCheck(@PathVariable Long roomId) {
        gameRoomService.findUsers(roomId); // 방이 존재하지 않으면 예외 발생
        return ApiResponse.success("ok");
    }

    @GetMapping("/{roomId}/play/rank")
    public ApiResponse<List<PlayerScore>> findPlayingUsers(@PathVariable Long roomId) {
        List<PlayerScore> users = gameService.getPlayerRanks(roomId);
        return ApiResponse.success(users);
    }

    @PostMapping
    public ApiResponse<Long> createRoom(
            @RequestBody CreateRoomRequest request,
            @AuthenticationPrincipal MemberAdapter memberAdapter) {
        log.info("category: {}", request.getCategory());
        Long roomId = gameRoomService.createRoom(
                request.getGameType(),
                request.getTitle(),
                request.getMaxPlayers(),
                memberAdapter.getNickName(),
                request.toGameInfo()
        );
        return ApiResponse.success(roomId);
    }

    @PostMapping("/{roomId}/join")
    public ApiResponse<Integer> joinRoom(
            @PathVariable Long roomId,
            @AuthenticationPrincipal MemberAdapter memberAdapter) {
        int playerSequence = gameRoomService.joinRoom(roomId, memberAdapter.getNickName());
        return ApiResponse.success(playerSequence);
    }

    @PostMapping("/{roomId}/leave")
    public ApiResponse<Void> leaveRoom(
            @PathVariable Long roomId,
            @AuthenticationPrincipal MemberAdapter memberAdapter) {
        gameRoomService.leaveRoom(roomId, memberAdapter.getNickName());
        return ApiResponse.success();
    }

    @PostMapping("/{roomId}/start")
    public ApiResponse<Void> startGame(
            @PathVariable Long roomId,
            @AuthenticationPrincipal MemberAdapter memberAdapter) {
        gameService.startGame(roomId, memberAdapter.getNickName());
        return ApiResponse.success();
    }

    @PostMapping("/{roomId}/skip")
    public ApiResponse<Void> skipCurrentQuiz(
            @PathVariable Long roomId,
            @AuthenticationPrincipal MemberAdapter memberAdapter) {
        gameService.increaseSkipVote(roomId, memberAdapter.getNickName());
        return ApiResponse.success();
    }

    @PostMapping("/{roomId}/ready")
    public ApiResponse<Void> playerReady(
            @PathVariable Long roomId,
            @AuthenticationPrincipal MemberAdapter memberAdapter) {
        gameRoomService.readyPlayer(roomId, memberAdapter.getNickName());
        return ApiResponse.success();
    }

    @PostMapping("/{roomId}/action")
    public ApiResponse<Void> handleAction(@PathVariable Long roomId, @RequestBody GameAction action) {
        gameService.handleAction(roomId, action);
        return ApiResponse.success();
    }
}
