package com.zanchi.zanchi_backend.domain.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Component
public class GoogleRoutesClient {

    private final WebClient routes;

    // ✅ 직접 생성자에 @Qualifier 지정
    public GoogleRoutesClient(@Qualifier("googleRoutesWebClient") WebClient routes) {
        this.routes = routes;
    }
    /** Google Routes로 도보 ETA(분) 계산 */
    public int walkMinutes(double oLat, double oLng, double dLat, double dLng) {
        var body = Map.of(
                "origin", Map.of("location", Map.of("latLng", Map.of("latitude", oLat, "longitude", oLng))),
                "destination", Map.of("location", Map.of("latLng", Map.of("latitude", dLat, "longitude", dLng))),
                "travelMode", "WALK",
                "routingPreference", "ROUTING_PREFERENCE_UNSPECIFIED",
                "computeAlternativeRoutes", false
        );

        var resp = routes.post()
                .uri("/directions/v2:computeRoutes")
                .header("X-Goog-FieldMask", "routes.duration,routes.distanceMeters")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        var routesList = (List<Map<String, Object>>) resp.get("routes");
        if (routesList == null || routesList.isEmpty()) return -1;

        var r0 = routesList.get(0);
        String duration = (String) r0.get("duration"); // 예: "247s"
        return secondsToMinutes(parseSeconds(duration));
    }

    private static int parseSeconds(String s) {
        if (s == null) return -1;
        s = s.trim();
        if (s.endsWith("s")) s = s.substring(0, s.length() - 1);
        try { return (int) Math.round(Double.parseDouble(s)); } catch (Exception e) { return -1; }
    }
    private static int secondsToMinutes(int sec) {
        return sec <= 0 ? -1 : (int) Math.round(sec / 60.0);
    }
}