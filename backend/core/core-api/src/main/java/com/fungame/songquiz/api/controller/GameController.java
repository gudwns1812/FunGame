package com.fungame.songquiz.api.controller;

import com.fungame.songquiz.api.controller.request.ChangeRoomSettingsRequest;
import com.fungame.songquiz.api.controller.request.CreateRoomRequest;
import com.fungame.songquiz.api.controller.request.GameActionRequest;
import com.fungame.songquiz.api.controller.request.KickPlayerRequest;
import com.fungame.songquiz.api.controller.response.ApiResponse;
import com.fungame.songquiz.api.controller.response.GameStateResponse;
import com.fungame.songquiz.api.controller.response.PlayerReadyResponse;
import com.fungame.songquiz.api.controller.response.PlayerScoreResponse;
import com.fungame.songquiz.api.controller.response.RoomResponse;
import com.fungame.songquiz.api.controller.response.RoomSettingsResponse;
import com.fungame.songquiz.api.controller.response.RoomStateResponse;
import com.fungame.songquiz.domain.member.MemberAdapter;
import com.fungame.songquiz.domain.member.MemberProfiles;
import com.fungame.songquiz.domain.room.GamePlayer;
import com.fungame.songquiz.domain.room.GameRoomService;
import com.fungame.songquiz.domain.room.RoomSettings;
import com.fungame.songquiz.domain.session.GameService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
    private final MemberProfiles memberProfiles;

    @GetMapping
    public ApiResponse<List<RoomResponse>> findAllRoom() {
        return ApiResponse.success(RoomResponse.listFrom(gameRoomService.findAllRooms(), memberProfiles));
    }

    @GetMapping("/{roomId}/users")
    public ApiResponse<RoomStateResponse> findUsers(@PathVariable Long roomId) {
        return ApiResponse.success(RoomStateResponse.from(gameRoomService.findRoomState(roomId), memberProfiles));
    }

    @GetMapping("/{roomId}/settings")
    public ApiResponse<RoomSettingsResponse> findSettings(@PathVariable Long roomId) {
        return ApiResponse.success(RoomSettingsResponse.from(gameRoomService.findRoomState(roomId), memberProfiles));
    }

    @PatchMapping("/{roomId}/settings")
    public ApiResponse<RoomSettingsResponse> changeSettings(
            @PathVariable Long roomId,
            @RequestBody ChangeRoomSettingsRequest request,
            @AuthenticationPrincipal MemberAdapter memberAdapter) {
        RoomSettings current = gameRoomService.findRoomState(roomId).settings();
        return ApiResponse.success(RoomSettingsResponse.from(gameRoomService.changeSettings(
                roomId, memberAdapter.getId(), request.applyTo(current)), memberProfiles));
    }

    @GetMapping("/{roomId}/health")
    public ApiResponse<String> healthCheck(@PathVariable Long roomId) {
        gameRoomService.healthCheck(roomId);
        return ApiResponse.success("ok");
    }

    @GetMapping("/{roomId}/play/state")
    public ApiResponse<GameStateResponse> findPlayState(@PathVariable Long roomId) {
        return ApiResponse.success(GameStateResponse.from(gameService.getPlayState(roomId)));
    }

    @GetMapping("/{roomId}/play/rank")
    public ApiResponse<List<PlayerScoreResponse>> findPlayingUsers(@PathVariable Long roomId) {
        List<PlayerScoreResponse> users = PlayerScoreResponse.listFrom(gameService.getPlayerRanks(roomId),
                memberProfiles);
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

    @PostMapping("/{roomId}/kick")
    public ApiResponse<Void> kickPlayer(
            @PathVariable Long roomId,
            @RequestBody KickPlayerRequest request,
            @AuthenticationPrincipal MemberAdapter memberAdapter) {
        gameRoomService.kickPlayer(roomId, memberAdapter.getId(), request.targetMemberId());
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

    private static GamePlayer toPlayer(MemberAdapter memberAdapter) {
        return GamePlayer.createNewPlayer(memberAdapter.getId());
    }
}
