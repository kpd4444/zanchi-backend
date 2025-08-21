package com.zanchi.zanchi_backend.domain.ai;

import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Optional;

@RequiredArgsConstructor
public enum Spot {
    BOWLING("볼링", "볼링장", "bowling_alley"),
    BOARD_GAME("보드게임", "보드게임 카페", null),     // primaryType 없음 → 텍스트검색만
    ARCADE("오락실", "오락실", "amusement_center"),
    PUB("펍", "펍", "bar"),
    PARK("산책", "공원", "park");

    public final String tag;         // 프론트에서 오는 한글 태그
    public final String query;       // Text Search용 질의
    public final String primaryType; // Google Places primary type(있으면 사용)

    public static Optional<Spot> fromTag(String t) {
        return Arrays.stream(values())
                .filter(s -> s.tag.equalsIgnoreCase(t))
                .findFirst();
    }
}