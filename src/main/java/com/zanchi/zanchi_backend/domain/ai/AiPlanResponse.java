package com.zanchi.zanchi_backend.domain.ai;


import java.util.List;

/** LLM이 반환하는 JSON을 매핑하는 DTO */
public record AiPlanResponse(
        List<IdeaItem> singles,   // 개별 추천 5~6
        List<Plan> plans          // 코스 2개
) {
    public static AiPlanResponse empty() {
        return new AiPlanResponse(List.of(), List.of());
    }

    public record Plan(
            Integer totalTravelMinutes,   // 총 이동시간(분)
            String explain,               // 코스 설명(선택)
            List<Step> steps              // 코스 구성 스텝
    ) {}

    public record Step(
            String id,
            String role, String name, String address,
            double lat, double lng,
            String externalUrl, String mapLink,
            Double rating, Integer ratingCount
    ) {}
}
