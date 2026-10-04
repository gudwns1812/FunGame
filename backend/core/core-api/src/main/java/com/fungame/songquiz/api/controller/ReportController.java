package com.fungame.songquiz.api.controller;

import com.fungame.songquiz.api.controller.request.ReportRequest;
import com.fungame.songquiz.api.controller.response.ApiResponse;
import com.fungame.songquiz.api.controller.response.MyReportResponse;
import com.fungame.songquiz.domain.member.MemberAdapter;
import com.fungame.songquiz.domain.report.ReportService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping
    public ApiResponse<Void> receiveReport(@AuthenticationPrincipal MemberAdapter user,
                                           @RequestBody ReportRequest request) {
        reportService.receive(user.getId(), request.toCommand());

        return ApiResponse.success();
    }

    @GetMapping("/mine")
    public ApiResponse<List<MyReportResponse>> findMyReports(@AuthenticationPrincipal MemberAdapter user) {
        return ApiResponse.success(MyReportResponse.listFrom(reportService.findMyReports(user.getId())));
    }
}
