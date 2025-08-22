package com.zanchi.zanchi_backend.domain.ai;

public record PlacePick(
        String role,               // "mid" | "restaurant" | "finish"
        String name,
        String address,
        double lat, double lng,
        String externalUrl,        // googleMapsUri or kakao place url
        String mapLink,            // kakao map deep link
        Double rating,
        Integer ratingCount
) {}