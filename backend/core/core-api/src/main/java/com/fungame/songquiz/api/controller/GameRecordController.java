package com.fungame.songquiz.api.controller;

import com.fungame.songquiz.api.controller.response.ApiResponse;
import com.fungame.songquiz.api.controller.response.MyGameRecordResponse;
import com.fungame.songquiz.domain.member.MemberAdapter;
import com.fungame.songquiz.domain.record.GameRecordService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/records")
@RequiredArgsConstructor
public class GameRecordController {

    private final GameRecordService gameRecordService;

    @GetMapping("/mine")
    public ApiResponse<List<MyGameRecordResponse>> findMyRecords(@AuthenticationPrincipal MemberAdapter user) {
        return ApiResponse.success(MyGameRecordResponse.listFrom(gameRecordService.findMine(user.getId())));
    }
}
