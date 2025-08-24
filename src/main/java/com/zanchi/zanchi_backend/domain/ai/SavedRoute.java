package com.zanchi.zanchi_backend.domain.ai;


import java.time.Instant;
import java.util.List;

public record SavedRoute(
        String id,
        Instant createdAt,
        String title,
        String companion,
        String mobility,
        String startName,
        double startLat,
        double startLng,
        List<String> tags,
        Integer totalTravelMinutes,
        String explain,
        List<Step> steps
){
    public record Step(
            String label,  // A, B, C, D
            String id,
            String role, String name, String address,
            double lat, double lng,
            String externalUrl, String mapLink,
            Double rating, Integer ratingCount
    ){}
}
