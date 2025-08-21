package com.zanchi.zanchi_backend.domain.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WalkEta {

    private final GoogleRoutesClient routes;

    public int estimateMinutes(double fromLat, double fromLng, double toLat, double toLng) {
        try {
            int m = routes.walkMinutes(fromLat, fromLng, toLat, toLng);
            if (m > 0) return m;
        } catch (Exception ignored) { }
        // 실패/쿼터초과/빈 응답 시 Haversine 근사(보행 80m/분)
        double dist = haversine(fromLat, fromLng, toLat, toLng); // meters
        return (int) Math.ceil(dist / 80.0);
    }

    private static double haversine(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371000;
        double dLat = Math.toRadians(lat2 - lat1), dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2)*Math.sin(dLat/2)
                + Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon/2)*Math.sin(dLon/2);
        return 2*R*Math.asin(Math.sqrt(a));
    }
}
