package com.zanchi.zanchi_backend.domain.report.dto;

import com.zanchi.zanchi_backend.domain.report.ReportReason;

import java.time.LocalDateTime;

public record ReportRes(
        Long id,
        Long clipId,
        Long reporterId,
        ReportReason reason,
        String detail,
        LocalDateTime createdAt
) {}