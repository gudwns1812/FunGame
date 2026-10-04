package com.fungame.songquiz.api.controller;

import com.fungame.songquiz.api.controller.request.CreateSongQuizRequest;
import com.fungame.songquiz.api.controller.response.ApiResponse;
import com.fungame.songquiz.domain.quiz.SongService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/songs")
@RequiredArgsConstructor
public class AdminSongController {

    private final SongService songService;

    @PostMapping
    public ApiResponse<Void> createSongQuiz(@RequestBody CreateSongQuizRequest request) {
        songService.createSongQuiz(request.toSong());

        return ApiResponse.success();
    }

    @GetMapping
    public ApiResponse<Boolean> existsSongQuiz(@RequestParam String title, @RequestParam LocalDate releaseDate) {
        boolean exists = songService.existSongQuiz(title, releaseDate);

        return ApiResponse.success(exists);
    }
}
