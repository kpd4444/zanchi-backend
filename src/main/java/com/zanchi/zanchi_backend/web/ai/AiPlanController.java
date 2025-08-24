package com.zanchi.zanchi_backend.web.ai;

import com.zanchi.zanchi_backend.domain.ai.AiPlanResponse;
import com.zanchi.zanchi_backend.domain.ai.AiPlanner;
import com.zanchi.zanchi_backend.domain.ai.IdeaItem;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiPlanController {

    private final AiPlanner aiPlanner;

    /**
     * AI에게 후보들을 넘겨 5~6개의 singles와 2개의 plans를 생성한다.
     * 엔드포인트: POST /ai/plan
     */
    @PostMapping("/plan")
    public ResponseEntity<AiPlanResponse> plan(@RequestBody PlanRequest req) {
        List<CandidateDto> restaurants = nz(req.getRestaurants());
        List<CandidateDto> mid         = nz(req.getMid());
        List<CandidateDto> finals      = nz(req.getFinals());

        List<IdeaItem> ideaRestaurants = restaurants.stream().map(this::toIdea).toList();
        List<IdeaItem> ideaMids        = mid.stream().map(this::toIdea).toList();
        List<IdeaItem> ideaFinals      = finals.stream().map(this::toIdea).toList();

        // ✅ tags / excludeIds / seed까지 넘김
        AiPlanResponse ai = aiPlanner.planTwoRoutes(
                req.getCompanion(),
                req.getMobility(),
                req.getStartName(),
                req.getStartLat(),
                req.getStartLng(),
                req.getCuisine(),
                req.getFinish(),
                req.getWhat(),
                ideaRestaurants,
                ideaMids,
                ideaFinals,
                nz(req.getTags()),
                nz(req.getExcludeIds()),
                req.getSeed()
        );

        return ResponseEntity.ok(ai);
    }

    // ---------- 내부 유틸 ----------

    private static <T> List<T> nz(List<T> v) {
        return CollectionUtils.isEmpty(v) ? Collections.emptyList() : v;
    }

    private IdeaItem toIdea(CandidateDto c) {
        String link = kakaoToLink(c.getName(), c.getLat(), c.getLng());
        String id   = (c.getId() != null && !c.getId().isBlank())
                ? c.getId()
                : ideaId(c.getName(), c.getLat(), c.getLng());  // ✅ 서버에서 생성

        return new IdeaItem(
                id,
                c.getRole(),
                c.getName(),
                c.getAddress(),
                c.getLat(),
                c.getLng(),
                (c.getExternalUrl() != null && !c.getExternalUrl().isBlank()) ? c.getExternalUrl() : link,
                link,
                c.getRating(),
                c.getRatingCount()
        );
    }

    // name|lat|lng 기반으로 url-safe base64 ID 생성
    private static String ideaId(String name, double lat, double lng) {
        String raw = name + "|" + String.format("%.6f", lat) + "|" + String.format("%.6f", lng);
        return java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
    private static String kakaoToLink(String name, double lat, double lng) {
        String enc = URLEncoder.encode(name, StandardCharsets.UTF_8);
        return "https://map.kakao.com/link/to/" + enc + "," + lat + "," + lng;
    }

    // ---------- 요청 DTO ----------

    @Data
    public static class PlanRequest {
        // 컨텍스트
        private String companion;
        private String mobility;
        private String startName;
        private double startLat;
        private double startLng;

        // 선호
        private String cuisine;
        private String finish;
        private String what;

        // 후보들
        private List<CandidateDto> restaurants;
        private List<CandidateDto> mid;
        private List<CandidateDto> finals;

        // ▼ 새 필드
        private List<String> tags;        // 선택 태그(최대 4)
        private String seed;              // 리롤 시드
        private List<String> excludeIds;  // 직전 루트에서 쓴 장소 id
    }

    /** 후보 아이템 DTO (프론트에서 보내주는 형태) */
    @Data
    public static class CandidateDto {
        private String id;
        private String role;          // "restaurant"/"activity"/"bar"/"cafe"/"dessert" 등 (없어도됨)
        private String name;
        private String address;
        private double lat;
        private double lng;
        private String externalUrl;   // kakao/google url 중 아무거나 (없어도됨)
        private Double rating;        // 평점 (없어도됨)
        private Integer ratingCount;  // 리뷰 수 (없어도됨)
    }
}