package com.zanchi.zanchi_backend.web.ai;

import com.zanchi.zanchi_backend.domain.ai.SavedRoute;
import com.zanchi.zanchi_backend.domain.ai.RouteStore;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/routes")
@RequiredArgsConstructor
public class RoutesController {

    private final RouteStore store;

    /** 저장 */
    @PostMapping
    public ResponseEntity<SavedRoute> save(@RequestBody SaveRouteRequest req){
        var saved = store.save(new SavedRoute(
                null, Instant.now(),
                (req.getTitle()==null || req.getTitle().isBlank()) ? "나의 루트" : req.getTitle(),
                req.getCompanion(), req.getMobility(),
                req.getStartName(), req.getStartLat(), req.getStartLng(),
                req.getTags(),
                req.getTotalTravelMinutes(), req.getExplain(),
                req.getSteps()==null? List.of() : req.getSteps().stream()
                        .map(s -> new SavedRoute.Step(s.getLabel(), s.getId(), s.getRole(), s.getName(), s.getAddress(),
                                s.getLat(), s.getLng(), s.getExternalUrl(), s.getMapLink(), s.getRating(), s.getRatingCount()))
                        .toList()
        ));
        return ResponseEntity.ok(saved);
    }

    /** 목록 */
    @GetMapping
    public List<SavedRoute> list(){ return store.list(); }

    /** 단건 조회 */
    @GetMapping("/{id}")
    public ResponseEntity<SavedRoute> get(@PathVariable String id){
        return store.get(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    /** 삭제 */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id){
        store.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ---------- 요청 DTO ----------
    @Data
    public static class SaveRouteRequest {
        private String title;
        private String companion;
        private String mobility;
        private String startName;
        private double startLat;
        private double startLng;
        private List<String> tags;
        private Integer totalTravelMinutes;
        private String explain;
        private List<Step> steps;

        @Data
        public static class Step {
            private String label;
            private String id;
            private String role, name, address;
            private double lat, lng;
            private String externalUrl, mapLink;
            private Double rating; private Integer ratingCount;
        }
    }
}
