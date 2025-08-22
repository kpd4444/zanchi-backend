package com.zanchi.zanchi_backend.domain.ai;

public record PlaceCandidate(
        String name,
        String address,
        double lat,
        double lng,
        String kakaoPlaceUrl,
        String mapLink
) {}