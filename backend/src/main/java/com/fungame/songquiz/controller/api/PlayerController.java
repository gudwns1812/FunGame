package com.fungame.songquiz.controller.api;

import com.fungame.songquiz.controller.request.NicknameRequest;
import com.fungame.songquiz.domain.PlayerService;
import com.fungame.songquiz.support.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/game/player")
public class PlayerController {
    private static final String DELIMITER = "#";

    private final PlayerService service;

    @PostMapping
    public ApiResponse<String> getUniqueNickName(@RequestBody NicknameRequest request) {
        Long key = service.getAutoKey();
        return ApiResponse.success(request.getNickName() + DELIMITER + key);
    }
}
