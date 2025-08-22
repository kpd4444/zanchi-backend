package com.zanchi.zanchi_backend.domain.ai;

public record PlaceCandidateEnriched(
        String name,
        String address,
        double lat, double lng,
        String kakaoPlaceUrl,
        String mapLink,
        Double rating,              // Google
        Integer userRatingCount,    // Google
        String googleMapsUri        // Google
) {
    public static PlaceCandidateEnriched of(PlaceCandidate b,
                                            Double rating, Integer count, String gUri) {
        return new PlaceCandidateEnriched(
                b.name(), b.address(), b.lat(), b.lng(),
                b.kakaoPlaceUrl(), b.mapLink(),
                rating, count, gUri
        );
    }
}