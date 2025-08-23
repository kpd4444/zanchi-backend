package com.zanchi.zanchi_backend.domain.ai;

public record IdeaItem(
        String id,          // ← 추가: name+lat+lng 해시 등
        String role, String name, String address,
        double lat, double lng,
        String externalUrl, String mapLink,
        Double rating, Integer ratingCount) {

    public static IdeaItem from(PlaceCandidateEnriched p, String role) {
        var id = java.util.Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString((p.name()+"|"+p.lat()+"|"+p.lng()).getBytes());
        return new IdeaItem(
                id, role, p.name(), p.address(), p.lat(), p.lng(),
                p.googleMapsUri()!=null ? p.googleMapsUri() : p.kakaoPlaceUrl(),
                p.mapLink(), p.rating(), p.userRatingCount()
        );
    }
}