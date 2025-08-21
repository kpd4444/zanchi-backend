package com.zanchi.zanchi_backend.domain.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Component
public class KakaoLocalClient {

    private final WebClient kakaoWebClient;

    // ✅ 명시적 생성자 + @Qualifier 로 주입
    public KakaoLocalClient(@Qualifier("kakaoWebClient") WebClient kakaoWebClient) {
        this.kakaoWebClient = kakaoWebClient;
    }

    /** 좌표 기반 키워드 검색(가까운 순) */
    public KakaoResponse searchKeyword(String query, double lat, double lng, int radiusM, int size) {
        int sizeClamped = Math.max(1, Math.min(size, 15));
        int radiusClamped = Math.max(1, Math.min(radiusM, 20000));
        return kakaoWebClient.get()
                .uri(u -> u.path("/v2/local/search/keyword.json")
                        .queryParam("query", query)
                        .queryParam("y", lat)
                        .queryParam("x", lng)
                        .queryParam("radius", radiusClamped)
                        .queryParam("size", sizeClamped)
                        .queryParam("sort", "distance")
                        .build())
                .retrieve()
                .bodyToMono(KakaoResponse.class)
                .block();
    }

    /** 좌표 없이 이름만으로 전역 검색(장소명 → 대략 좌표 얻을 때) */
    public KakaoResponse searchKeywordNoBias(String query, int size) {
        int sizeClamped = Math.max(1, Math.min(size, 15));
        return kakaoWebClient.get()
                .uri(u -> u.path("/v2/local/search/keyword.json")
                        .queryParam("query", query)
                        .queryParam("size", sizeClamped)
                        .build())
                .retrieve()
                .bodyToMono(KakaoResponse.class)
                .block();
    }

    // --- DTO ---
    public record KakaoResponse(List<KakaoDoc> documents, Meta meta) {}
    public record KakaoDoc(
            String place_name,
            String address_name,
            String road_address_name,
            String x,            // lng
            String y,            // lat
            String place_url,
            String distance      // 좌표검색일 때만 제공(문자열, 미터)
    ) {}
    public record Meta(Integer total_count, Boolean is_end) {}
}
