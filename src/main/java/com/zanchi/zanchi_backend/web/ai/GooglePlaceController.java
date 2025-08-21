package com.zanchi.zanchi_backend.web.ai;

import com.zanchi.zanchi_backend.domain.ai.GooglePlacesClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/places")
@RequiredArgsConstructor
public class GooglePlaceController {

    private final GooglePlacesClient google;

    // 예) /places/google?query=홍대 양식집&lat=37.5563&lng=126.9236&radius=1200&size=5
    @GetMapping("/google")
    public ResponseEntity<?> searchGoogle(
            @RequestParam String query,
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "1200") int radius,
            @RequestParam(name = "size", defaultValue = "5") int size
    ) {
        var resp = google.searchText(query, lat, lng, radius, size);
        return ResponseEntity.ok(resp);
    }
}
