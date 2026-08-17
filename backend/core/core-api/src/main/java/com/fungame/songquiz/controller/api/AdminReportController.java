package com.fungame.songquiz.controller.api;

import com.fungame.songquiz.controller.request.ReportCommentRequest;
import com.fungame.songquiz.controller.request.ReportStatusRequest;
import com.fungame.songquiz.controller.response.AdminReportResponse;
import com.fungame.songquiz.controller.response.ApiResponse;
import com.fungame.songquiz.domain.member.MemberAdapter;
import com.fungame.songquiz.domain.report.ReportService;
import com.fungame.songquiz.enums.ReportStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
public class AdminReportController {

    private final ReportService reportService;

    @GetMapping
    public ApiResponse<List<AdminReportResponse>> findReports(
            @RequestParam(required = false) ReportStatus status) {
        return ApiResponse.success(AdminReportResponse.listFrom(reportService.findAllReports(status)));
    }

    @PostMapping("/{reportId}/comments")
    public ApiResponse<Void> comment(@AuthenticationPrincipal MemberAdapter user,
                                     @PathVariable Long reportId,
                                     @RequestBody ReportCommentRequest request) {
        reportService.comment(user.getId(), reportId, request.content());

        return ApiResponse.success();
    }

    @PatchMapping("/{reportId}/status")
    public ApiResponse<Void> changeStatus(@PathVariable Long reportId,
                                          @RequestBody ReportStatusRequest request) {
        reportService.changeStatus(reportId, request.status());

        return ApiResponse.success();
    }
}
