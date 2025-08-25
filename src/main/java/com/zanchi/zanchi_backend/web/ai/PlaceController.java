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

        KakaoResp resp = kakaoClient.get()
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
                .block(java.time.Duration.ofSeconds(5));

        if (resp == null || resp.documents == null) return List.of();

        // ✅ 여기서 “필수값 보장 + 불량건 제외”
        return resp.documents.stream()
                .map(this::mapKakaoDocSafely)     // Optional<PlaceDto>
                .flatMap(Optional::stream)        // 유효한 것만
                .collect(Collectors.toList());
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