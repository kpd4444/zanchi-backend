package com.zanchi.zanchi_backend.domain.ai;

import java.util.List;

public record DateRoutePlan(
        java.util.List<Step> steps,
        int totalTravelMinutes,
        String explain                 // 코스 설명 (nullable)
) {
    public record Step(
            String role, String name, String address,
            double lat, double lng,
            String externalUrl, String mapLink,
            Double rating, Integer ratingCount
    ) {}
}
