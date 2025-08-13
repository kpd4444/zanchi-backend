package com.zanchi.zanchi_backend.domain.report.dto;

import com.zanchi.zanchi_backend.domain.report.ReportReason;

public record ReportCreateReq(ReportReason reason, String detail) {}