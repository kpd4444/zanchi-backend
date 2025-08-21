package com.zanchi.zanchi_backend.domain.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Component
public class GooglePlacesClient {

    private final WebClient places;

    // ✨ Lombok 대신 직접 생성자 + 파라미터에 @Qualifier
    public GooglePlacesClient(@Qualifier("googlePlacesWebClient") WebClient places) {
        this.places = places;
    }

    public SearchTextResponse searchText(String query, double lat, double lng, int radiusMeters, int maxResults) {
        var body = Map.of(
                "textQuery", query,
                "locationBias", Map.of(
                        "circle", Map.of(
                                "center", Map.of("latitude", lat, "longitude", lng),
                                "radius", radiusMeters
                        )
                ),
                "maxResultCount", maxResults
        );

        String fieldMask = String.join(",",
                "places.id","places.displayName","places.formattedAddress",
                "places.location","places.rating","places.userRatingCount",
                "places.priceLevel","places.googleMapsUri","places.primaryType"
        );

        return places.post()
                .uri("/v1/places:searchText")
                .header("X-Goog-FieldMask", fieldMask)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(SearchTextResponse.class)
                .block();
    }



    // --- DTO ---
    public record SearchTextResponse(java.util.List<Place> places) {}
    public record Place(
            String id,
            DisplayName displayName,
            String formattedAddress,
            Location location,
            Double rating,
            Integer userRatingCount,
            String priceLevel,
            String googleMapsUri,
            String primaryType
    ) {}
    public record DisplayName(String text, String languageCode) {}
    public record Location(Double latitude, Double longitude) {}
}
