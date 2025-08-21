package com.zanchi.zanchi_backend.domain.ai;


import java.util.List;

public record DateRouteIdeas(
        List<PlacePick> singles,      // 개별 핫플 5~6개
        List<DateRoutePlan> plans     // 추천 코스 2개 (아래에서 DateRoutePlan에 explain 추가)
) {}
