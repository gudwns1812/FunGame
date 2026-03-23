package com.fungame.songquiz.controller.api;

import com.fungame.songquiz.controller.request.CreateSongQuizRequest;
import com.fungame.songquiz.domain.SongService;
import com.fungame.songquiz.support.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

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
