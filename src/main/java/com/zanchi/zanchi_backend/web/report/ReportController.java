package com.zanchi.zanchi_backend.web.report;

import com.zanchi.zanchi_backend.domain.report.ClipReport;
import com.zanchi.zanchi_backend.domain.report.ReportService;
import com.zanchi.zanchi_backend.domain.report.dto.ReportCreateReq;
import com.zanchi.zanchi_backend.domain.report.dto.ReportRes;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    /** 신고 접수 */
    @PostMapping("/clips/{clipId}")
    public ResponseEntity<?> reportClip(@PathVariable Long clipId,
                                        @AuthenticationPrincipal(expression = "member.id") Long meId,
                                        @RequestBody ReportCreateReq req) {
        if (meId == null) return ResponseEntity.status(401).body(java.util.Map.of("error","UNAUTHORIZED"));
        ClipReport saved = reportService.reportClip(clipId, meId, req.reason(), req.detail());
        return ResponseEntity.status(201).body(
                new ReportRes(saved.getId(), clipId, meId, saved.getReason(), saved.getDetail(), saved.getCreatedAt())
        );
    }

    /** (선택) 관리자/운영자: 신고 목록 조회 */
    @GetMapping
    public Page<ReportRes> list(@RequestParam(required=false) Long clipId,
                                @RequestParam(required=false) Long reporterId,
                                @RequestParam(defaultValue="0") int page,
                                @RequestParam(defaultValue="20") int size) {
        var p = reportService.list(clipId, reporterId, PageRequest.of(page, size));
        List<ReportRes> rows = p.getContent().stream()
                .map(r -> new ReportRes(r.getId(), r.getClip().getId(), r.getReporter().getId(),
                        r.getReason(), r.getDetail(), r.getCreatedAt()))
                .toList();
        return new PageImpl<>(rows, p.getPageable(), p.getTotalElements());
    }

    /** (선택) 관리자/운영자: 처리 완료 후 삭제 */
    @DeleteMapping("/{reportId}")
    public ResponseEntity<?> delete(@PathVariable Long reportId) {
        reportService.delete(reportId);
        return ResponseEntity.noContent().build();
    }
}
