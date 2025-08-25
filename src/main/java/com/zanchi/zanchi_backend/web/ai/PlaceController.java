package com.zanchi.zanchi_backend.web.ai;

import com.zanchi.zanchi_backend.web.ai.dto.PlaceDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono; // Mono import

import java.util.*;
import java.util.stream.Collectors;

@RestController
public class PlaceController {

    private final WebClient kakaoClient;

    public PlaceController(@Qualifier("kakaoWebClient") WebClient kakaoClient) {
        this.kakaoClient = kakaoClient;
    }

    @GetMapping("/places/kakao")
    public List<PlaceDto> kakao(
            @RequestParam String query,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(defaultValue = "1200") int radius,
            @RequestParam(defaultValue = "8") int size,
            @RequestParam(required = false) String category
    ) {
        List<String> terms = Arrays.stream(query.split("(?i)\\s*OR\\s*"))
                .map(String::trim).filter(s -> !s.isEmpty()).toList();

        Set<String> dedup = new LinkedHashSet<>();
        List<PlaceDto> out = new ArrayList<>();

        for (String term : terms.isEmpty() ? List.of(query) : terms) {
            List<PlaceDto> page = callKakao(term, lat, lng, radius, size, category);

            for (PlaceDto p : page) {
                // ✅ 중복키 안정화(좌표 6자리 반올림) + name은 절대 null 아님
                String key = (p.getName().toLowerCase() + "|" +
                        round6(p.getLat()) + "|" + round6(p.getLng()));
                if (dedup.add(key)) out.add(p);
                if (out.size() >= size) break;
            }
            if (out.size() >= size) break;
        }
        return out;
    }

    private List<PlaceDto> callKakao(String term,
                                     Double lat, Double lng,
                                     int radius, int size,
                                     String category) {

        int reqSize   = Math.max(1, Math.min(size, 15));
        int reqRadius = Math.max(1, Math.min(radius, 20000));

        Mono<KakaoResp> mono = kakaoClient.get()
                .uri(uri -> {
                    var b = uri.path("/v2/local/search/keyword.json")
                            .queryParam("query", term)
                            .queryParam("size", reqSize);
                    if (lat != null && lng != null) {
                        b.queryParam("y", lat)
                                .queryParam("x", lng)
                                .queryParam("radius", reqRadius);
                    }
                    return b.build();
                })
                .retrieve()
                .bodyToMono(KakaoResp.class)
                // Netty(8s)보다 살짝 짧게 걸어 우리가 먼저 제어
                .timeout(java.time.Duration.ofSeconds(7))
                // 타임아웃/네트워크/역직렬화/HTTP 오류 → 빈 결과로 폴백
                .onErrorResume(ex -> Mono.just(new KakaoResp(java.util.List.of())));

        KakaoResp resp;
        try {
            // 체인에서 이미 timeout/fallback 했으므로 block()에 시간 제한 불필요
            resp = mono.block();
        } catch (Exception e) {
            // 최후 방어: 어떤 예외든 빈 리스트 반환
            return java.util.List.of();
        }

        if (resp == null || resp.documents == null) return java.util.List.of();

        return resp.documents.stream().map(d ->
                PlaceDto.builder()
                        .name(nz(d.place_name, "?"))
                        .address(nz(d.road_address_name, d.address_name))
                        .lat(parseD(d.y))   // Kakao: y=lat
                        .lng(parseD(d.x))   // Kakao: x=lng
                        .externalUrl(d.place_url)
                        .rating(null)
                        .ratingCount(null)
                        .category(d.category_name)
                        .build()
        ).collect(java.util.stream.Collectors.toList());
    }

    private static double parseD(Object v) {
        if (v == null) return 0.0;
        try {
            if (v instanceof Number n) return n.doubleValue();
            String s = v.toString().trim();
            if (s.isEmpty()) return 0.0;
            return Double.parseDouble(s);
        } catch (Exception ignore) {
            return 0.0;
        }
    }

    // a가 비어있으면 b로 대체 (b도 null이면 빈 문자열)
    private static String nz(String a, String b) {
        return (a != null && !a.isBlank()) ? a : (b != null ? b : "");
    }

    // 필요하면 단항 버전도 추가
    private static String nz(String a) {
        return a != null ? a : "";
    }


    /** 좌표 없거나 파싱 실패 → 제외, 나머지는 안전한 기본값으로 채움 */
    private Optional<PlaceDto> mapKakaoDocSafely(KakaoDoc d) {
        Double lat = tryParseDouble(d.y);
        Double lng = tryParseDouble(d.x);
        if (lat == null || lng == null) return Optional.empty();   // ❌ 좌표 없으면 스킵

        String name = firstNonBlank(d.place_name, d.road_address_name, d.address_name, "장소");
        String addr = firstNonBlank(d.road_address_name, d.address_name, "");
        String ext  = blankToNull(d.place_url);

        return Optional.of(
                PlaceDto.builder()
                        .name(name)          // ✅ 절대 null 아님
                        .address(addr)       // ✅ 절대 null 아님(없으면 "")
                        .lat(lat)
                        .lng(lng)
                        .externalUrl(ext)    // 없으면 null
                        .rating(null)        // Kakao Local에는 평점 없음
                        .ratingCount(null)
                        .category(blankToNull(d.category_name))
                        .build()
        );
    }

    private static Double tryParseDouble(String s) {
        try {
            if (s == null || s.isBlank()) return null;
            double v = Double.parseDouble(s);
            if (Double.isFinite(v)) return v;
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private static String firstNonBlank(String... arr) {
        for (String s : arr) if (s != null && !s.isBlank()) return s;
        return ""; // 모든게 비었으면 빈문자열
    }

    private static String blankToNull(String s) { return (s == null || s.isBlank()) ? null : s; }

    private static String round6(double v) { return String.format(java.util.Locale.US, "%.6f", v); }

    /* Kakao 응답 DTO */
    private record KakaoResp(List<KakaoDoc> documents) {}
    private record KakaoDoc(String place_name, String road_address_name, String address_name,
                            String x, String y, String place_url, String category_name) {}
}