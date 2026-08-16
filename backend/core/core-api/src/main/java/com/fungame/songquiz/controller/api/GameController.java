package com.fungame.songquiz.controller.api;

import com.fungame.songquiz.domain.member.MemberAdapter;
import com.fungame.songquiz.domain.room.GamePlayer;
import com.fungame.songquiz.domain.room.GameRoomService;
import com.fungame.songquiz.domain.room.RoomSettings;
import com.fungame.songquiz.domain.session.GameService;
import com.fungame.songquiz.controller.room.RoomListReader;
import com.fungame.songquiz.controller.request.ChangeRoomSettingsRequest;
import com.fungame.songquiz.controller.request.CreateRoomRequest;
import com.fungame.songquiz.controller.request.GameActionRequest;
import com.fungame.songquiz.controller.response.ApiResponse;
import com.fungame.songquiz.controller.response.GameStateResponse;
import com.fungame.songquiz.controller.response.PlayerReadyResponse;
import com.fungame.songquiz.controller.response.PlayerScoreResponse;
import com.fungame.songquiz.controller.response.RoomPlayersResponse;
import com.fungame.songquiz.controller.response.RoomResponse;
import com.fungame.songquiz.controller.response.RoomSettingsResponse;
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
    private final RoomListReader roomListReader;

    @GetMapping
    public ApiResponse<List<RoomResponse>> findAllRoom() {
        return ApiResponse.success(RoomResponse.listFrom(roomListReader.findAllRooms()));
    }

    @GetMapping("/{roomId}/users")
    public ApiResponse<RoomPlayersResponse> findUsers(@PathVariable Long roomId) {
        return ApiResponse.success(RoomPlayersResponse.from(gameRoomService.findUsers(roomId)));
    }

    @GetMapping("/{roomId}/settings")
    public ApiResponse<RoomSettingsResponse> findSettings(@PathVariable Long roomId) {
        return ApiResponse.success(RoomSettingsResponse.from(gameRoomService.findSettings(roomId)));
    }

    @PatchMapping("/{roomId}/settings")
    public ApiResponse<RoomSettingsResponse> changeSettings(
            @PathVariable Long roomId,
            @RequestBody ChangeRoomSettingsRequest request,
            @AuthenticationPrincipal MemberAdapter memberAdapter) {
        RoomSettings current = gameRoomService.findSettings(roomId).toSettings();
        return ApiResponse.success(RoomSettingsResponse.from(gameRoomService.changeSettings(
                roomId, memberAdapter.getId(), request.applyTo(current))));
    }

    @GetMapping("/{roomId}/health")
    public ApiResponse<String> healthCheck(@PathVariable Long roomId) {
        gameRoomService.findUsers(roomId); // 방이 존재하지 않으면 예외 발생
        return ApiResponse.success("ok");
    }

    @GetMapping("/{roomId}/play/state")
    public ApiResponse<GameStateResponse> findPlayState(@PathVariable Long roomId) {
        return ApiResponse.success(GameStateResponse.from(gameService.getPlayState(roomId)));
    }

    @GetMapping("/{roomId}/play/rank")
    public ApiResponse<List<PlayerScoreResponse>> findPlayingUsers(@PathVariable Long roomId) {
        List<PlayerScoreResponse> users = PlayerScoreResponse.listFrom(gameService.getPlayerRanks(roomId));
        return ApiResponse.success(users);
    }

    @PostMapping
    public ApiResponse<Long> createRoom(
            @RequestBody CreateRoomRequest request,
            @AuthenticationPrincipal MemberAdapter memberAdapter) {
        Long roomId = gameRoomService.createRoom(request.toRoomSettings(), toPlayer(memberAdapter));
        return ApiResponse.success(roomId);
    }

    @PostMapping("/{roomId}/join")
    public ApiResponse<Integer> joinRoom(
            @PathVariable Long roomId,
            @AuthenticationPrincipal MemberAdapter memberAdapter) {
        int playerSequence = gameRoomService.joinRoom(roomId, toPlayer(memberAdapter));
        return ApiResponse.success(playerSequence);
    }

    @PostMapping("/{roomId}/leave")
    public ApiResponse<Void> leaveRoom(
            @PathVariable Long roomId,
            @AuthenticationPrincipal MemberAdapter memberAdapter) {
        gameRoomService.leaveRoom(roomId, memberAdapter.getId());
        return ApiResponse.success();
    }

    @PostMapping("/{roomId}/start")
    public ApiResponse<Void> startGame(
            @PathVariable Long roomId,
            @AuthenticationPrincipal MemberAdapter memberAdapter) {
        gameService.startGame(roomId, memberAdapter.getId());
        return ApiResponse.success();
    }

    @PostMapping("/{roomId}/skip")
    public ApiResponse<Void> skipCurrentQuiz(
            @PathVariable Long roomId,
            @AuthenticationPrincipal MemberAdapter memberAdapter) {
        gameService.increaseSkipVote(roomId, memberAdapter.getId());
        return ApiResponse.success();
    }

    @PostMapping("/{roomId}/ready")
    public ApiResponse<PlayerReadyResponse> playerReady(
            @PathVariable Long roomId,
            @AuthenticationPrincipal MemberAdapter memberAdapter) {
        return ApiResponse.success(
                PlayerReadyResponse.from(gameRoomService.readyPlayer(roomId, memberAdapter.getId())));
    }

    @PostMapping("/{roomId}/action")
    public ApiResponse<Void> handleAction(
            @PathVariable Long roomId,
            @RequestBody GameActionRequest request,
            @AuthenticationPrincipal MemberAdapter memberAdapter) {
        gameService.handleAction(roomId, request.toAction(memberAdapter.getId()));
        return ApiResponse.success();
    }

    private GamePlayer toPlayer(MemberAdapter memberAdapter) {
        return GamePlayer.createNewPlayer(memberAdapter.getId(), memberAdapter.getNickName());
    }
}
