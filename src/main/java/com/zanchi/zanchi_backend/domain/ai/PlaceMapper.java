package com.zanchi.zanchi_backend.domain.ai;



import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class PlaceMapper {

    public static PlaceCandidate from(KakaoLocalClient.KakaoDoc d) {
        String name = d.place_name();
        String address = (d.road_address_name() != null && !d.road_address_name().isBlank())
                ? d.road_address_name()
                : d.address_name();

        double lng = parseDoubleSafe(d.x());
        double lat = parseDoubleSafe(d.y());

        String mapLink = "https://map.kakao.com/link/to/"
                + URLEncoder.encode(name, StandardCharsets.UTF_8)
                + "," + lat + "," + lng;

        return new PlaceCandidate(
                name,
                address,
                lat,
                lng,
                d.place_url(),
                mapLink
        );
    }

    private static double parseDoubleSafe(String s) {
        try { return Double.parseDouble(s); }
        catch (Exception e) { return 0.0; }
    }
}

