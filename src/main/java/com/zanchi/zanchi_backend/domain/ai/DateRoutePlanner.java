package com.zanchi.zanchi_backend.domain.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import static java.util.Comparator.comparingDouble;

@Service
@RequiredArgsConstructor
public class DateRoutePlanner {

    private final PlaceService placeService;
    private final PlaceEnricher enricher;
    private final WalkEta walkEta;

    // 가중치(원하면 properties로 빼기)
    private static final double W_RATING = 5.0;   // 평점 1점당
    private static final double W_REVIEW = 1.2;   // ln(리뷰수+1)
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
        // mid(중간: 놀거리/볼거리/쉼자리) – 먹을거리면 스킵
        List<PlaceCandidateEnriched> midsEnriched = List.of();
        if (!"먹을거리".equals(what)) {
            var midQuery = mapWhatToQuery(what);
            if (midQuery != null) {
                var mids = placeService.searchCandidates(midQuery, startLat, startLng, radiusM, candidates);
                midsEnriched = enricher.enrichAll(mids);
            }
        }

        // restaurant
        var restaurants = placeService.searchCandidates(cuisine, startLat, startLng, radiusM, candidates);
        var rEnriched = enricher.enrichAll(restaurants);

        // finish
        var finQuery = mapFinishToQuery(finish);
        var fins = placeService.searchCandidates(finQuery, startLat, startLng, radiusM, candidates);
        var fEnriched = enricher.enrichAll(fins);

        if (rEnriched.isEmpty() || fEnriched.isEmpty()) {
            throw new IllegalStateException("주변에 후보가 부족합니다. 키워드를 바꾸거나 반경을 넓혀보세요.");
        }

        // ---------- 개별 핫플 5~6개 구성 ----------
        var singles = topSingles(startLat, startLng, midsEnriched, rEnriched, fEnriched, 6);

        // ---------- 코스 스코어링 (상위 2개) ----------
        var options = new ArrayList<RouteOption>();
        var limitedM = midsEnriched.stream().limit(8).toList();
        var limitedR = rEnriched.stream().limit(10).toList();
        var limitedF = fEnriched.stream().limit(10).toList();

        // mid 있으면 (start→mid→R→F), 없으면 (start→R→F)
        if (limitedM.isEmpty()) {
            for (var r : limitedR) {
                int s_r = walkEta.estimateMinutes(startLat, startLng, r.lat(), r.lng());
                for (var f : limitedF) {
                    int r_f = walkEta.estimateMinutes(r.lat(), r.lng(), f.lat(), f.lng());
                    int travel = s_r + r_f;
                    double score = scorePair(r, f, travel);
                    options.add(new RouteOption(null, r, f, travel, score,
                            s_r, 0, r_f));
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
                        options.add(new RouteOption(m, r, f, travel, score,
                                s_m, m_r, r_f));
                    }
                }
            }
        }

        options.sort(comparingDouble(RouteOption::score).reversed());

        // 다양성 확보: 레스토랑/피니시가 겹치지 않게 상위 2개
        var picked = new ArrayList<RouteOption>(2);
        var usedR = new HashSet<String>();
        var usedF = new HashSet<String>();
        for (var op : options) {
            if (picked.size() == 2) break;
            String rKey = op.r().name();
            String fKey = op.f().name();
            if (usedR.contains(rKey) || usedF.contains(fKey)) continue;
            picked.add(op);
            usedR.add(rKey);
            usedF.add(fKey);
        }
        if (picked.isEmpty() && !options.isEmpty()) picked.add(options.get(0)); // 최소 1개

        var plans = new ArrayList<DateRoutePlan>();
        for (int i = 0; i < picked.size(); i++) {
            var op = picked.get(i);
            var steps = new ArrayList<DateRoutePlan.Step>();
            steps.add(new DateRoutePlan.Step("start", startName, startAddr, startLat, startLng,
                    startUrl, mapLink(startName, startLat, startLng), null, null));
            if (op.m() != null) {
                steps.add(toStep("activity", op.m()));
            }
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
        return ideas.plans().isEmpty()
                ? new DateRoutePlan(List.of(), 0, "추천 코스를 찾지 못했습니다.")
                : ideas.plans().get(0);
    }

//    public DateRoutePlan planWalking(
//            String startName, double startLat, double startLng,
//            String cuisine, String what, String finish,
//            int radiusM, int candidates
//    ) {
//        // 0) 출발지 주소/URL 보강
//        var startResolved = placeService.lookupOneByNameNear(startName, startLat, startLng);
//        String startAddr = startResolved != null ? startResolved.address() : "";
//        String startUrl  = startResolved != null ? startResolved.kakaoPlaceUrl() : null;
//
//        var steps = new ArrayList<DateRoutePlan.Step>();
//        steps.add(new DateRoutePlan.Step("start", startName, startAddr, startLat, startLng,
//                startUrl, mapLink(startName, startLat, startLng), null, null));
//
//        // 1) 중간(what) 후보 (먹을거리면 스킵)
//        PlaceCandidateEnriched bestMid = null;
//        if (!"먹을거리".equals(what)) {
//            var midQuery = mapWhatToQuery(what);
//            if (midQuery != null) {
//                var mids = placeService.searchCandidates(midQuery, startLat, startLng, radiusM, candidates);
//                var midsEnriched = enricher.enrichAll(mids).stream().limit(10).toList();
//                bestMid = pickBestBy(startLat, startLng, midsEnriched);
//                if (bestMid != null) {
//                    steps.add(new DateRoutePlan.Step("activity", bestMid.name(), bestMid.address(),
//                            bestMid.lat(), bestMid.lng(),
//                            preferUrl(bestMid), bestMid.mapLink(), bestMid.rating(), bestMid.userRatingCount()));
//                    // 이후 기준점을 mid로 이동
//                    startLat = bestMid.lat();
//                    startLng = bestMid.lng();
//                }
//            }
//        }
//
//        // 2) 식당(사용자 cuisine)
//        var restaurants = placeService.searchCandidates(cuisine, startLat, startLng, radiusM, candidates);
//        var rEnriched = enricher.enrichAll(restaurants).stream().limit(10).toList();
//
//        // 3) 마무리 후보
//        var finQuery = mapFinishToQuery(finish); // 술집/카페/디저트/볼링장/공원
//        var fins = placeService.searchCandidates(finQuery, startLat, startLng, radiusM, candidates);
//        var fEnriched = enricher.enrichAll(fins).stream().limit(10).toList();
//
//        if (rEnriched.isEmpty() || fEnriched.isEmpty()) {
//            throw new IllegalStateException("주변에 후보가 부족합니다. 키워드를 바꾸거나 반경을 넓혀보세요.");
//        }
//
//        double bestScore = -1e9;
//        int bestTotalMin = Integer.MAX_VALUE;
//        PlaceCandidateEnriched bestR = null, bestF = null;
//
//        // 4) 조합 평가 (start→(mid)→R→F)
//        for (var r : rEnriched) {
//            int s_to_r = (bestMid == null)
//                    ? walkEta.estimateMinutes(steps.get(0).lat(), steps.get(0).lng(), r.lat(), r.lng())
//                    : walkEta.estimateMinutes(bestMid.lat(), bestMid.lng(), r.lat(), r.lng());
//
//            for (var f : fEnriched) {
//                int r_to_f = walkEta.estimateMinutes(r.lat(), r.lng(), f.lat(), f.lng());
//                int s_to_m = (bestMid == null) ? 0
//                        : walkEta.estimateMinutes(steps.get(0).lat(), steps.get(0).lng(), bestMid.lat(), bestMid.lng());
//                int travel = s_to_m + s_to_r + r_to_f;
//
//                double rRating = nz(r.rating(), DEFAULT_RATING);
//                double fRating = nz(f.rating(), DEFAULT_RATING);
//                int rCnt = nz(r.userRatingCount(), 0);
//                int fCnt = nz(f.userRatingCount(), 0);
//
//                double score = -travel
//                        + W_RATING * (rRating + fRating)
//                        + W_REVIEW * (Math.log(1 + rCnt) + Math.log(1 + fCnt));
//
//                if (score > bestScore) {
//                    bestScore = score;
//                    bestTotalMin = travel;
//                    bestR = r; bestF = f;
//                }
//            }
//        }
//
//        // 5) 스텝 구성
//        if (bestR != null) {
//            steps.add(new DateRoutePlan.Step("restaurant", bestR.name(), bestR.address(),
//                    bestR.lat(), bestR.lng(), preferUrl(bestR), bestR.mapLink(),
//                    bestR.rating(), bestR.userRatingCount()));
//        }
//        if (bestF != null) {
//            String role = mapFinishRole(finish); // bar | cafe | dessert | activity
//            steps.add(new DateRoutePlan.Step(role, bestF.name(), bestF.address(),
//                    bestF.lat(), bestF.lng(), preferUrl(bestF), bestF.mapLink(),
//                    bestF.rating(), bestF.userRatingCount()));
//        }
//
//        return new DateRoutePlan(steps, bestTotalMin);
//    }

    private List<PlacePick> topSingles(
            double baseLat, double baseLng,
            List<PlaceCandidateEnriched> mids,
            List<PlaceCandidateEnriched> rests,
            List<PlaceCandidateEnriched> fins,
            int take
    ) {
        var list = new ArrayList<PlacePick>();

        // 각 카테고리에서 2개씩 뽑아 섞기 (있으면)
        list.addAll(pickBest(mids, baseLat, baseLng, "mid", 2));
        list.addAll(pickBest(rests, baseLat, baseLng, "restaurant", 2));
        list.addAll(pickBest(fins, baseLat, baseLng, "finish", 2));

        // 중복 제거(이름 기준) 및 상위 N
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
        // mid 평점은 보너스(과하지 않게 0.5배)
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

    // 설명 문구 생성 (Spring-AI 붙이면 여기를 LLM 호출로 교체)
    private String makeExplain(String startName, String what, String cuisine, String finish, RouteOption op) {
        var sb = new StringBuilder();
        sb.append("출발지 ").append(startName).append("에서 ");
        if (op.m != null) {
            sb.append(op.s_m).append("분 이동해 ").append(op.m.name()).append("에서 ");
            sb.append(what).append(" 즐기고, ");
            sb.append(op.m_r).append("분 이동해 ").append(op.r.name()).append(" (").append(cuisine).append(")에서 식사, ");
        } else {
            sb.append(op.s_m).append("분 이동해 ").append(op.r.name()).append(" (").append(cuisine).append(")에서 식사 후, ");
        }
        sb.append(op.r_f).append("분 이동해 ").append(op.f.name()).append("에서 ").append(finish).append("로 마무리.");
        sb.append(" 총 도보 약 ").append(op.travel).append("분.");
        return sb.toString();
    }

    // 내부 옵션 구조체
    private record RouteOption(
            PlaceCandidateEnriched m,
            PlaceCandidateEnriched r,
            PlaceCandidateEnriched f,
            int travel,
            double score,
            int s_m, int m_r, int r_f
    ) {
        public PlaceCandidateEnriched m(){ return m; }
        public PlaceCandidateEnriched r(){ return r; }
        public PlaceCandidateEnriched f(){ return f; }
        public int travel(){ return travel; }
        public double score(){ return score; }
    }
}
