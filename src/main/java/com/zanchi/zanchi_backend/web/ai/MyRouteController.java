package com.zanchi.zanchi_backend.web.ai;

import com.zanchi.zanchi_backend.domain.ai.RoutePlan;
import com.zanchi.zanchi_backend.domain.ai.RoutePlanRepository;
import com.zanchi.zanchi_backend.domain.ai.RouteStep;
import com.zanchi.zanchi_backend.domain.member.Member;
import com.zanchi.zanchi_backend.domain.member.MemberRepository;


import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;


import java.util.*;

@RestController
@RequestMapping("/api/me/routes") // 내 루트만 다룬다
@RequiredArgsConstructor
public class MyRouteController {

    private final RoutePlanRepository repo;
    private final MemberRepository memberRepo;

    // ✅ 헬퍼: Jwt만 받도록, sub 숫자/문자 모두 처리
    private Long currentUserId(Jwt jwt){
        if (jwt == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);

        String sub = jwt.getClaimAsString("sub");
        if (sub == null || sub.isBlank())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "JWT sub missing");

        // sub이 숫자면 그대로 사용
        try { return Long.parseLong(sub); } catch (NumberFormatException ignore) {}

        // 숫자가 아니면 loginId로 Member 조회
        return memberRepo.findByLoginId(sub)
                .map(Member::getId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Member not found for loginId="+sub));
    }

    @GetMapping
    public List<RouteDto> list(@AuthenticationPrincipal(expression = "username") String loginId){
        Long uid = memberRepo.findByLoginId(loginId)
                .map(Member::getId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return repo.findByOwnerIdOrderByCreatedAtDesc(uid).stream().map(RouteDto::from).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<RouteDto> get(@PathVariable Long id,
                                        @AuthenticationPrincipal(expression = "username") String loginId){
        Long uid = memberRepo.findByLoginId(loginId)
                .map(Member::getId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return repo.findByIdAndOwnerId(id, uid).map(RouteDto::from).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Transactional
    public RouteDto create(@RequestBody SaveRequest req,
                           @AuthenticationPrincipal(expression = "username") String loginId){
        Long uid = memberRepo.findByLoginId(loginId)
                .map(Member::getId).orElseThrow();
        Member owner = memberRepo.findById(uid).orElseThrow();

        RoutePlan p = RoutePlan.builder()
                .owner(owner)
                .title(nz(req.title, new java.text.SimpleDateFormat("yyyy.MM.dd").format(new java.util.Date())))
                .companion(req.companion)
                .mobility(req.mobility)
                .startName(req.startName)
                .startLat(req.startLat)
                .startLng(req.startLng)
                .totalTravelMinutes(req.totalTravelMinutes)
                .planExplain(req.explain)    // ✅ 여기 변경
                .build();

        p.setTags(req.tags==null? List.of() : req.tags);
        p.setSteps(req.toSteps());

        return RouteDto.from(repo.save(p));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt){
        Long uid = currentUserId(jwt);
        int n = repo.deleteByIdAndOwnerId(id, uid);
        return (n>0) ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    private static String nz(String v, String def){ return (v!=null && !v.isBlank())? v : def; }

    // ---------- DTOs ----------
    @Data
    public static class SaveRequest{
        public String title;
        public String companion;
        public String mobility;
        public String startName;
        public double startLat;
        public double startLng;
        public List<String> tags;
        public Integer totalTravelMinutes;
        public String explain;
        public List<StepDto> steps;

        public List<RouteStep> toSteps(){
            if (steps == null) return List.of();
            List<RouteStep> out = new ArrayList<>();
            for (StepDto s : steps){
                out.add(RouteStep.builder()
                        .label(s.label)
                        .ideaId(s.id)
                        .role(s.role)
                        .name(s.name)
                        .address(s.address)
                        .lat(s.lat)
                        .lng(s.lng)
                        .externalUrl(s.externalUrl)
                        .mapLink(s.mapLink)
                        .rating(s.rating)
                        .ratingCount(s.ratingCount)
                        .build());
            }
            return out;
        }
    }

    @Data
    public static class StepDto{
        public String label;
        public String id;
        public String role;
        public String name;
        public String address;
        public double lat;
        public double lng;
        public String externalUrl;
        public String mapLink;
        public Double rating;
        public Integer ratingCount;
    }

    @Data
    public static class RouteDto{
        public Long id;
        public String title;
        public Date createdAt;
        public String companion;
        public String mobility;
        public String startName;
        public double startLat;
        public double startLng;
        public List<String> tags;
        public Integer totalTravelMinutes;
        public String explain;
        public List<StepDto> steps;

        public static RouteDto from(RoutePlan p){
            RouteDto d = new RouteDto();
            d.id = p.getId();
            d.title = p.getTitle();
            d.createdAt = Date.from(p.getCreatedAt());
            d.companion = p.getCompanion();
            d.mobility = p.getMobility();
            d.startName = p.getStartName();
            d.startLat = p.getStartLat();
            d.startLng = p.getStartLng();
            d.tags = p.getTags();
            d.totalTravelMinutes = p.getTotalTravelMinutes();
            d.explain = p.getPlanExplain();
            d.steps = p.getSteps().stream().map(s -> {
                StepDto x = new StepDto();
                x.label = s.getLabel();
                x.id = s.getIdeaId();
                x.role = s.getRole();
                x.name = s.getName();
                x.address = s.getAddress();
                x.lat = s.getLat();
                x.lng = s.getLng();
                x.externalUrl = s.getExternalUrl();
                x.mapLink = s.getMapLink();
                x.rating = s.getRating();
                x.ratingCount = s.getRatingCount();
                return x;
            }).toList();
            return d;
        }
    }
}
