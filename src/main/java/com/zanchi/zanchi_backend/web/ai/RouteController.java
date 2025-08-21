package com.zanchi.zanchi_backend.web.ai;


import com.zanchi.zanchi_backend.domain.ai.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/date")
@RequiredArgsConstructor
public class RouteController {

    private final DateRoutePlanner planner;


    // 예) /date/plan?startName=떼아뜨르다락소극장&cuisine=양식&what=놀거리&finish=볼링&lat=...&lng=...
    @GetMapping("/plan")
    public ResponseEntity<DateRoutePlan> plan(
            @RequestParam String startName,
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam String cuisine,
            @RequestParam(defaultValue = "볼거리") String what,    // ★ 추가
            @RequestParam(defaultValue = "술집") String finish,    // ★ 추가
            @RequestParam(defaultValue = "1200") int radius,
            @RequestParam(name = "k", defaultValue = "8") int candidates
    ) {
        var plan = planner.planWalking(startName, lat, lng, cuisine, what, finish, radius, candidates);
        return ResponseEntity.ok(plan);
    }
    @GetMapping("/ideas")
    public ResponseEntity<DateRouteIdeas> ideas(
            @RequestParam String startName,
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam String cuisine,
            @RequestParam(required = false, defaultValue = "먹을거리") String what,
            @RequestParam(required = false, defaultValue = "술집") String finish,
            @RequestParam(defaultValue = "1200") int radius,
            @RequestParam(name = "k", defaultValue = "12") int candidates // 후보 더 넉넉히
    ) {
        var ideas = planner.ideasWalking(startName, lat, lng, cuisine, what, finish, radius, candidates);
        return ResponseEntity.ok(ideas);
    }

//    @GetMapping("/ideas")
//    public ResponseEntity<AiPlanner.AiPlanResponse> ideas(
//            @RequestParam String startName,
//            @RequestParam double lat,
//            @RequestParam double lng,
//            @RequestParam String cuisine,
//            @RequestParam String what,
//            @RequestParam String finish,
//            @RequestParam(defaultValue="연인") String companion,
//            @RequestParam(defaultValue="도보") String mobility,
//            @RequestParam(defaultValue="1200") int radius,
//            @RequestParam(name="k", defaultValue="12") int k
//    ){
//        var out = dateIdeasService.ideas(companion,mobility,cuisine,what,finish,startName,lat,lng,radius,k);
//        return ResponseEntity.ok(out);
//    }
}
