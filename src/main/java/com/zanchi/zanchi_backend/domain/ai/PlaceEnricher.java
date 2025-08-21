package com.zanchi.zanchi_backend.domain.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PlaceEnricher {

    private final GooglePlacesClient google;

    public PlaceCandidateEnriched enrichOne(PlaceCandidate base) {
        // 후보를 base 좌표 반경 150m로 이름 검색
        var resp = google.searchText(base.name(), base.lat(), base.lng(), 150, 6);
        if (resp == null || resp.places() == null || resp.places().isEmpty()) {
            return PlaceCandidateEnriched.of(base, null, null, null);
        }

        GooglePlacesClient.Place best = null;
        double bestScore = -1;

        for (var p : resp.places()) {
            if (p.location() == null) continue;
            double meters = haversine(base.lat(), base.lng(),
                    p.location().latitude(), p.location().longitude());
            // 이름 유사도 + 거리 가중치(200m 이내일수록 높음)
            double sim = nameSim(base.name(), p.displayName() != null ? p.displayName().text() : "");
            double distScore = 1.0 - Math.min(meters, 200.0) / 200.0; // 0~1
            double score = 0.6 * distScore + 0.4 * sim;
            if (score > bestScore) { bestScore = score; best = p; }
        }

        // 임계치 미달이면 매칭 안 함
        if (best == null || bestScore < 0.45) {
            return PlaceCandidateEnriched.of(base, null, null, null);
        }

        return new PlaceCandidateEnriched(
                base.name(), base.address(), base.lat(), base.lng(),
                base.kakaoPlaceUrl(), base.mapLink(),
                best.rating(), best.userRatingCount(), best.googleMapsUri()
        );
    }

    public List<PlaceCandidateEnriched> enrichAll(List<PlaceCandidate> bases) {
        return bases.stream().map(this::enrichOne).toList();
    }

    // --- utils ---
    private static double haversine(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371000;
        double dLat = Math.toRadians(lat2 - lat1), dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2)*Math.sin(dLat/2)
                + Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon/2)*Math.sin(dLon/2);
        return 2*R*Math.asin(Math.sqrt(a));
    }

    private static double nameSim(String a, String b) {
        // 간단 토큰 유사도(Jaccard). 한글/영문 혼용 대비해서 정규화
        var A = Normalizer.normalize(a, Normalizer.Form.NFKD)
                .toLowerCase().replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}\\s]", "")
                .replaceAll("\\s+", " ").trim().split(" ");
        var B = Normalizer.normalize(b, Normalizer.Form.NFKD)
                .toLowerCase().replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}\\s]", "")
                .replaceAll("\\s+", " ").trim().split(" ");

        java.util.Set<String> sA = new java.util.HashSet<>(java.util.Arrays.asList(A));
        java.util.Set<String> sB = new java.util.HashSet<>(java.util.Arrays.asList(B));
        if (sA.isEmpty() || sB.isEmpty()) return 0.0;
        java.util.Set<String> inter = new java.util.HashSet<>(sA); inter.retainAll(sB);
        java.util.Set<String> union = new java.util.HashSet<>(sA); union.addAll(sB);
        return inter.size() / (double) union.size(); // 0~1
    }
}