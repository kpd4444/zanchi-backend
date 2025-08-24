package com.zanchi.zanchi_backend.domain.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;

import static java.util.Comparator.comparingDouble;

@Service
@RequiredArgsConstructor
public class DateRoutePlanner {

    private final PlaceService placeService;
    private final PlaceEnricher enricher;
    private final WalkEta walkEta;
    private final AiPlanner aiPlanner;   // ✅ LLM 사용

    // 가중치(폴백용)
    private static final double W_RATING = 5.0;
    private static final double W_REVIEW = 1.2;
    private static final double DEFAULT_RATING = 3.9;

    /** 개별 핫플 5~6 + 추천코스 2개 */
    public DateRouteIdeas ideasWalking(
            String startName, double startLat, double startLng,
            String cuisine, String what, String finish,
            int radiusM, int candidates
    ) {

        // 출발지 카드 정보(주소/URL) 보강
        var startResolved = placeService.lookupOneByNameNear(startName, startLat, startLng);
        String startAddr = startResolved != null ? startResolved.address() : "";
        String startUrl  = startResolved != null ? startResolved.kakaoPlaceUrl() : null;

        // ---------- 후보 수집 ----------
        List<PlaceCandidateEnriched> midsEnriched = List.of();
        if (!"먹을거리".equals(what)) {
            var midQuery = mapWhatToQuery(what);
            if (midQuery != null) {
                var mids = placeService.searchCandidates(midQuery, startLat, startLng, radiusM, candidates);
                midsEnriched = enricher.enrichAll(mids);
            }
        }

        var restaurants = placeService.searchCandidates(cuisine, startLat, startLng, radiusM, candidates);
        var rEnriched   = enricher.enrichAll(restaurants);

        var finQuery = mapFinishToQuery(finish);
        var fins     = placeService.searchCandidates(finQuery, startLat, startLng, radiusM, candidates);
        var fEnriched = enricher.enrichAll(fins);

        if (rEnriched.isEmpty() || fEnriched.isEmpty()) {
            throw new IllegalStateException("주변에 후보가 부족합니다. 키워드를 바꾸거나 반경을 넓혀보세요.");
        }

        // ============== ① LLM에게 재랭킹 + 코스 생성 + 설명 위임 ==============
        try {
            // 프론트가 companion/mobility를 아직 안 넘기니 기본값으로 고정
            String companion = "연인";
            String mobility  = "도보";

            // 후보를 AiPlanner가 먹는 형태로 변환
            var ideaRestaurants = toIdeas(rEnriched, "restaurant");
            var ideaMids        = toIdeas(midsEnriched, "activity");
            var ideaFinals      = toIdeas(fEnriched, mapFinishRole(finish));

            AiPlanResponse ai = aiPlanner.planTwoRoutes(
                    companion, mobility,
                    startName, startLat, startLng,
                    cuisine, finish, what,
                    ideaRestaurants, ideaMids, ideaFinals,
                    null, null, null   // ▲ 추가: tags, excludeIds, seed
            );

            // AI 응답을 우리 응답 타입으로 변환
            var singles = ai.singles() == null ? List.<PlacePick>of()
                    : ai.singles().stream().map(this::fromIdea).toList();

            var plans = ai.plans() == null ? List.<DateRoutePlan>of()
                    : ai.plans().stream().map(this::fromAiPlan).toList();

            // AI가 뭔가라도 만들었다면 바로 반환
            if (!plans.isEmpty() || !singles.isEmpty()) {
                return new DateRouteIdeas(singles, plans);
            }
            // 비어 있으면 폴백으로 넘어감
        } catch (Exception ignore) {
            // 모델 오류/JSON 파싱 실패 등은 조용히 폴백
        }

        // ============== ② 폴백: 기존 점수식 로직 유지 ==============
        var singles = topSingles(startLat, startLng, midsEnriched, rEnriched, fEnriched, 6);

        var options  = new ArrayList<RouteOption>();
        var limitedM = midsEnriched.stream().limit(8).toList();
        var limitedR = rEnriched.stream().limit(10).toList();
        var limitedF = fEnriched.stream().limit(10).toList();

        if (limitedM.isEmpty()) {
            for (var r : limitedR) {
                int s_r = walkEta.estimateMinutes(startLat, startLng, r.lat(), r.lng());
                for (var f : limitedF) {
                    int r_f = walkEta.estimateMinutes(r.lat(), r.lng(), f.lat(), f.lng());
                    int travel = s_r + r_f;
                    double score = scorePair(r, f, travel);
                    options.add(new RouteOption(null, r, f, travel, score, s_r, 0, r_f));
                }
            }
        } else {
            for (var m : limitedM) {
                int s_m = walkEta.estimateMinutes(startLat, startLng, m.lat(), m.lng());
                for (var r : limitedR) {
                    int m_r = walkEta.estimateMinutes(m.lat(), m.lng(), r.lat(), r.lng());
                    for (var f : limitedF) {
                        int r_f = walkEta.estimateMinutes(r.lat(), r.lng(), f.lat(), f.lng());
                        int travel = s_m + m_r + r_f;
                        double score = scoreTriple(m, r, f, travel);
                        options.add(new RouteOption(m, r, f, travel, score, s_m, m_r, r_f));
                    }
                }
            }
        }

        options.sort(comparingDouble(RouteOption::score).reversed());

        var picked = new ArrayList<RouteOption>(2);
        var usedR  = new HashSet<String>();
        var usedF  = new HashSet<String>();
        for (var op : options) {
            if (picked.size() == 2) break;
            if (usedR.contains(op.r().name()) || usedF.contains(op.f().name())) continue;
            picked.add(op);
            usedR.add(op.r().name());
            usedF.add(op.f().name());
        }
        if (picked.isEmpty() && !options.isEmpty()) picked.add(options.get(0));

        var plans = new ArrayList<DateRoutePlan>();
        for (var op : picked) {
            var steps = new ArrayList<DateRoutePlan.Step>();
            steps.add(new DateRoutePlan.Step("start", startName, startAddr, startLat, startLng,
                    startUrl, mapLink(startName, startLat, startLng), null, null));
            if (op.m() != null) steps.add(toStep("activity", op.m()));
            steps.add(toStep("restaurant", op.r()));
            steps.add(toStep(mapFinishRole(finish), op.f()));

            String explain = makeExplain(startName, what, cuisine, finish, op);
            plans.add(new DateRoutePlan(steps, op.travel(), explain));
        }

        return new DateRouteIdeas(singles, plans);
    }

    public DateRoutePlan planWalking(
            String startName, double startLat, double startLng,
            String cuisine, String what, String finish,
            int radiusM, int candidates
    ) {
        var ideas = ideasWalking(startName, startLat, startLng, cuisine, what, finish, radiusM, candidates);
        if (ideas.plans() == null || ideas.plans().isEmpty()) {
            return new DateRoutePlan(List.of(), 0, "추천 코스를 찾지 못했습니다.");
        }
        return ideas.plans().get(0);
    }

    // ----------------- AI ↔ Our DTO 변환 -----------------

    private static String ideaId(String name, double lat, double lng) {
        // name|lat|lng 를 url-safe base64로
        String raw = name + "|" + String.format("%.6f", lat) + "|" + String.format("%.6f", lng);
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private List<IdeaItem> toIdeas(List<PlaceCandidateEnriched> src, String role) {
        return src == null ? List.of() : src.stream()
                .map(p -> new IdeaItem(
                        ideaId(p.name(), p.lat(), p.lng()),   // ★ id
                        role,
                        p.name(), p.address(),
                        p.lat(), p.lng(),
                        preferUrl(p),
                        p.mapLink(),
                        p.rating(), p.userRatingCount()
                ))
                .toList();
    }

    private PlacePick fromIdea(IdeaItem x) {
        return new PlacePick(
                x.role(), x.name(), x.address(),
                x.lat(), x.lng(),
                x.externalUrl(), x.mapLink(),
                x.rating(), x.ratingCount()
        );
    }

    private DateRoutePlan fromAiPlan(AiPlanResponse.Plan p) {
        var steps = p.steps() == null ? List.<DateRoutePlan.Step>of()
                : p.steps().stream()
                .map(s -> new DateRoutePlan.Step(
                        s.role(), s.name(), s.address(),
                        s.lat(), s.lng(),
                        s.externalUrl(), s.mapLink(),
                        s.rating(), s.ratingCount()))
                .toList();

        return new DateRoutePlan(steps,
                p.totalTravelMinutes() == null ? 0 : p.totalTravelMinutes(),
                p.explain());
    }

    // ----------------- 이하 폴백(기존 로직) 유틸 -----------------

    private List<PlacePick> topSingles(
            double baseLat, double baseLng,
            List<PlaceCandidateEnriched> mids,
            List<PlaceCandidateEnriched> rests,
            List<PlaceCandidateEnriched> fins,
            int take
    ) {
        var list = new ArrayList<PlacePick>();
        list.addAll(pickBest(mids,  baseLat, baseLng, "mid",        2));
        list.addAll(pickBest(rests, baseLat, baseLng, "restaurant", 2));
        list.addAll(pickBest(fins,  baseLat, baseLng, "finish",     2));

        var seen = new HashSet<String>();
        var result = new ArrayList<PlacePick>();
        for (var p : list) {
            if (seen.add(p.name())) result.add(p);
            if (result.size() == take) break;
        }
        return result;
    }

    private List<PlacePick> pickBest(List<PlaceCandidateEnriched> src, double baseLat, double baseLng, String role, int n) {
        if (src == null || src.isEmpty()) return List.of();
        return src.stream()
                .sorted( comparingDouble((PlaceCandidateEnriched p) -> -nz(p.rating(), DEFAULT_RATING))
                        .thenComparingDouble(p -> dist(baseLat, baseLng, p.lat(), p.lng())) )
                .limit(n)
                .map(p -> new PlacePick(role, p.name(), p.address(), p.lat(), p.lng(),
                        preferUrl(p), p.mapLink(), p.rating(), p.userRatingCount()))
                .toList();
    }

    private DateRoutePlan.Step toStep(String role, PlaceCandidateEnriched p) {
        return new DateRoutePlan.Step(role, p.name(), p.address(), p.lat(), p.lng(),
                preferUrl(p), p.mapLink(), p.rating(), p.userRatingCount());
    }

    private double scorePair(PlaceCandidateEnriched r, PlaceCandidateEnriched f, int travel) {
        double rr = nz(r.rating(), DEFAULT_RATING);
        double fr = nz(f.rating(), DEFAULT_RATING);
        int rc = nz(r.userRatingCount(), 0);
        int fc = nz(f.userRatingCount(), 0);
        return -travel + W_RATING*(rr+fr) + W_REVIEW*(Math.log(1+rc)+Math.log(1+fc));
    }

    private double scoreTriple(PlaceCandidateEnriched m, PlaceCandidateEnriched r, PlaceCandidateEnriched f, int travel) {
        double mr = nz(m.rating(), DEFAULT_RATING);
        double rr = nz(r.rating(), DEFAULT_RATING);
        double fr = nz(f.rating(), DEFAULT_RATING);
        int mc = nz(m.userRatingCount(), 0);
        int rc = nz(r.userRatingCount(), 0);
        int fc = nz(f.userRatingCount(), 0);
        return -travel + W_RATING*(0.5*mr + rr + fr)
                + W_REVIEW*(0.5*Math.log(1+mc) + Math.log(1+rc) + Math.log(1+fc));
    }

    private static double dist(double lat1, double lng1, double lat2, double lng2) {
        double R = 6371000;
        double dLat = Math.toRadians(lat2 - lat1), dLon = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat/2)*Math.sin(dLat/2)
                + Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon/2)*Math.sin(dLon/2);
        return 2*R*Math.asin(Math.sqrt(a));
    }
    private static double nz(Double v, double def){ return v==null?def:v; }
    private static int nz(Integer v, int def){ return v==null?def:v; }

    private static String mapWhatToQuery(String what) {
        return switch (what) {
            case "볼거리" -> "전시 OR 미술관 OR 박물관 OR 관광명소";
            case "놀거리" -> "오락실 OR 보드게임 OR 룸카페 OR 체험";
            case "쉼자리" -> "공원 OR 산책로 OR 전망대";
            default -> null;
        };
    }
    private static String mapFinishToQuery(String finish) {
        return switch (finish) {
            case "카페" -> "카페";
            case "디저트" -> "디저트 카페 OR 베이커리";
            case "볼링" -> "볼링장";
            case "산책" -> "공원 OR 산책로";
            default -> "술집";
        };
    }
    private static String mapFinishRole(String finish) {
        return switch (finish) {
            case "카페" -> "cafe";
            case "디저트" -> "dessert";
            case "볼링", "산책" -> "activity";
            default -> "bar";
        };
    }
    private static String mapLink(String name, double lat, double lng) {
        return "https://map.kakao.com/link/to/"
                + java.net.URLEncoder.encode(name, java.nio.charset.StandardCharsets.UTF_8)
                + "," + lat + "," + lng;
    }
    private static String preferUrl(PlaceCandidateEnriched p) {
        return p.googleMapsUri() != null ? p.googleMapsUri() : p.kakaoPlaceUrl();
    }

    // 폴백 설명 생성(LLM 실패 시)
    private String makeExplain(String startName, String what, String cuisine, String finish, RouteOption op) {
        var sb = new StringBuilder();
        sb.append("출발지 ").append(startName).append("에서 ");
        if (op.m != null) {
            sb.append(op.s_m).append("분 이동해 ").append(op.m.name()).append("에서 ")
                    .append(what).append(" 즐기고, ")
                    .append(op.m_r).append("분 이동해 ").append(op.r.name()).append(" (").append(cuisine).append(")에서 식사, ");
        } else {
            sb.append(op.s_m).append("분 이동해 ").append(op.r.name()).append(" (").append(cuisine).append(")에서 식사 후, ");
        }
        sb.append(op.r_f).append("분 이동해 ").append(op.f.name()).append("에서 ").append(finish).append("로 마무리. ")
                .append("총 도보 약 ").append(op.travel).append("분.");
        return sb.toString();
    }

    private record RouteOption(
            PlaceCandidateEnriched m,
            PlaceCandidateEnriched r,
            PlaceCandidateEnriched f,
            int travel,
            double score,
            int s_m, int m_r, int r_f
    ) { }
}
