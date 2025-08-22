package com.zanchi.zanchi_backend.web.ai;

import com.zanchi.zanchi_backend.domain.ai.PlaceCandidate;
import com.zanchi.zanchi_backend.domain.ai.PlaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/places")
@RequiredArgsConstructor
public class PlaceController {

    private final PlaceService placeService;

    /**
     * 예) /places/kakao?query=떼아뜨르다락소극장&size=1
     * 예) /places/kakao?query=홍대 양식&lat=37.55&lng=126.92&radius=1200&size=5
     *
     * lat/lng 생략 가능: 생략 시 Kakao 전역 검색으로 좌표 1건을 얻는 용도로도 사용 가능
     */
    @GetMapping("/kakao")
    public ResponseEntity<List<PlaceCandidate>> search(
            @RequestParam String query,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(defaultValue = "1200") int radius,
            @RequestParam(defaultValue = "5") int size
    ) {
        return ResponseEntity.ok(
                placeService.searchCandidatesAdaptive(query, lat, lng, radius, size)
        );
    }
}