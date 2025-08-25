package com.zanchi.zanchi_backend.domain.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Service
public class AiPlanner {

    private final ChatClient chat;         // 기본
    private final ChatClient explainChat;  // 설명용
    private final ObjectMapper om;

    public AiPlanner(@Qualifier("chatClient") ChatClient chat,
                     @Qualifier("explainChat") ChatClient explainChat,
                     ObjectMapper om) {
        this.chat = chat;
        this.explainChat = explainChat;
        this.om = om;
    }

    /** 후보들을 LLM에 넘겨서 singles(5~6) + plans(2) 를 JSON으로 받는다 */
    public DateRouteIdeas rankAndPlan(
            String companion, String mobility,
            String startName, double startLat, double startLng,
            String cuisine, String finish, String what,
            List<IdeaItem> restaurants,
            List<IdeaItem> mids,
            List<IdeaItem> finals
    ) {
        try {
            String payload = buildUserPayloadJson(
                    companion, mobility, startName, startLat, startLng,
                    cuisine, finish, what, restaurants, mids, finals
            );

            // ChatClient는 AiConfig에서 이미 JSON 모드 + 기본 system 세팅되어 있음
            String raw = chat.prompt()
                    .user(u -> u.text(
                            // 스키마/규칙은 user 프롬프트에 명시
                            """
                            스키마: {"singles":[{...}], "plans":[{"totalTravelMinutes":0,"explain":"","steps":[{...}]},{...}]}
                            규칙:
                              - singles: 5~6개 (role 다양하게 start/restaurant/cafe/bar/dessert/activity 중에서)
                              - plans: 정확히 2개, 도보 동선 짧게. finish(사용자 선호)는 최소 1코스에 강반영.
                              - step.mapLink는 "https://map.kakao.com/link/to/{URLEncoded name},{lat},{lng}" 형식.
                            입력:
                            """ + payload + "\n\n출력: JSON만."
                    ))
                    .call()
                    .content();

            String json = extractJson(raw);

            // LLM 응답은 우리 도메인 DTO(DateRouteIdeas) 구조와 동일하게 만들었으므로 그대로 매핑
            return om.readValue(json, DateRouteIdeas.class);
        } catch (Exception e) {
            throw new IllegalStateException("AI 추천 실패", e);
        }
    }

    private String buildUserPayloadJson(
            String companion, String mobility,
            String startName, double startLat, double startLng,
            String cuisine, String finish, String what,
            List<IdeaItem> restaurants,
            List<IdeaItem> mids,
            List<IdeaItem> finals
    ) throws Exception {
        boolean hasRestaurants = restaurants != null && !restaurants.isEmpty();

        var context = new java.util.LinkedHashMap<String, Object>();
        context.put("start", Map.of(
                "name", startName,
                "lat", startLat,
                "lng", startLng,
                "mapLink", kakaoToLink(startName, startLat, startLng)
        ));
        context.put("mobility", mobility);
        context.put("finish", finish);
        context.put("companion", companion);
        context.put("what", what);

        // 🍱 레스토랑 후보가 있을 때만 cuisine 포함
        if (hasRestaurants) {
            String cuisineSafe = (cuisine == null || cuisine.isBlank())
                    ? "맛집 OR 식당" : cuisine;
            context.put("cuisine", cuisineSafe);
        }

        var candidates = new java.util.LinkedHashMap<String, Object>();
        candidates.put("restaurants", restaurants != null ? restaurants : List.of());
        candidates.put("mid", mids != null ? mids : List.of());
        candidates.put("finals", finals != null ? finals : List.of());

        return om.writeValueAsString(Map.of("context", context, "candidates", candidates));
    }

    private static String extractJson(String s) {
        String t = s == null ? "" : s.trim();
        if (t.startsWith("```")) {
            int st = t.indexOf('{'), en = t.lastIndexOf('}');
            if (st >= 0 && en > st) return t.substring(st, en + 1);
        }
        int st = t.indexOf('{'), en = t.lastIndexOf('}');
        if (st >= 0 && en > st) return t.substring(st, en + 1);
        throw new IllegalArgumentException("모델 응답에서 JSON 블록을 찾지 못했습니다: " + s);
    }

    private static String[] parseIdeaId(String id) {
        try {
            byte[] dec = Base64.getUrlDecoder().decode(id);
            String raw = new String(dec, StandardCharsets.UTF_8);
            String[] parts = raw.split("\\|");
            return (parts.length == 3) ? parts : null; // [name, lat, lng]
        } catch (Exception e) {
            return null;
        }
    }

    private static String kakaoToLink(String name, double lat, double lng) {
        return "https://map.kakao.com/link/to/" +
                URLEncoder.encode(name, StandardCharsets.UTF_8) + "," + lat + "," + lng;
    }
    /** LLM에게 후보를 넘겨 singles(5~6) + plans(2) JSON을 받아온다. */
    public AiPlanResponse planTwoRoutes(
            String companion, String mobility,
            String startName, double startLat, double startLng,
            String cuisine, String finish, String what,
            List<IdeaItem> restaurants, List<IdeaItem> mid, List<IdeaItem> finals,
            List<String> tags, List<String> excludeIds, String seed
    ) {
        // 후보 존재 여부
        boolean hasRestaurants = restaurants != null && !restaurants.isEmpty();
        boolean hasFinals      = finals != null && !finals.isEmpty();

        // finish에 따른 최종 역할
        String finalRole = switch (finish == null ? "" : finish) {
            case "카페"   -> "cafe";
            case "디저트" -> "dessert";
            case "산책"   -> "activity"; // 산책은 activity로 내려오게
            default       -> "bar";      // 술집 등
        };

        // LLM에게 역할-버킷 제약을 아주 명확하게 박아둠
        String system = """
너는 데이트 코스 플래너다. 반드시 JSON만 출력.
스키마: {"singles":[{...}], "plans":[{"totalTravelMinutes":0,"explain":"","steps":[{...}]}]}
규칙(아주 중요):
  - 후보(candidates.*)만 사용. 새 장소 생성 금지. "id"는 반드시 candidates.*의 id 중 하나여야 한다.
  - 역할별 선택 제한:
      * activity 단계는 반드시 candidates.mid 안에서만 고른다.
      * restaurant 단계는 candidates.restaurants 안에서만 고른다. (restaurants가 비었으면 restaurant 단계 자체를 만들지 않는다)
      * 최종 단계는 candidates.finals 안에서만 고른다. 그리고 role은 context.finalRole과 일치해야 한다.
  - steps[].id는 필수. name/address/lat/lng는 비어 있어도 됨.
  - 이동수단(context.mobility)에 맞게 동선 최적화(도보/자전거: 근거리 위주; 대중교통/차: 중·장거리 허용하되 전체 시간 합리적).
  - finish 선호(context.finish)는 코스에 반드시 반영.
  - step.mapLink는 "https://map.kakao.com/link/to/{URLEncoded name},{lat},{lng}" 형식.
  - singles는 후보 중에서 5~6개, 역할 다양화. 단, 위 역할-버킷 제약은 singles에도 적용된다.
""";

        try {
            // 먹을거리 미선택 시 cuisine을 아예 제외 (500/NPE 예방 & 의도 보존)
            var ctx = new java.util.LinkedHashMap<String, Object>();
            ctx.put("start", Map.of(
                    "name", startName, "lat", startLat, "lng", startLng,
                    "mapLink", kakaoToLink(startName, startLat, startLng)
            ));
            ctx.put("mobility", mobility);
            ctx.put("finish", finish);
            ctx.put("finalRole", finalRole);  // 👈 LLM이 최종 단계 역할을 정확히 알도록
            ctx.put("companion", companion);
            ctx.put("what", what);
            ctx.put("tags", tags == null ? List.of() : tags);
            ctx.put("seed", seed == null ? "" : seed);
            ctx.put("excludeIds", excludeIds == null ? List.of() : excludeIds);
            if (hasRestaurants) {
                String cuisineSafe = (cuisine == null || cuisine.isBlank()) ? "맛집 OR 식당" : cuisine;
                ctx.put("cuisine", cuisineSafe);
            }

            Map<String, Object> payload = Map.of(
                    "context", ctx,
                    "candidates", Map.of(
                            "restaurants", restaurants != null ? restaurants : List.of(),
                            "mid",          mid != null         ? mid         : List.of(),
                            "finals",       finals != null      ? finals      : List.of()
                    )
            );

            String user = "입력:\n" + om.writeValueAsString(payload) + """
                
                작업:
                - 역할-버킷 제약을 반드시 지킨다(위 규칙 참조).
                - excludeIds 목록의 id는 절대 사용하지 않는다(steps/singles 모두).
                - steps는 3~4개로 간결하게: 활동 1~2개(activity from mid) + (선택)식사 1개(restaurant) + 최종 1개(finalRole from finals).
                - explain은 한국어 2~3문장.
                출력: JSON만.
                """;

            String raw  = chat.prompt().system(system).user(user).call().content();
            String json = extractJson(raw);

            // 1) 파싱
            AiPlanResponse parsed = om.readValue(json, AiPlanResponse.class);

            // 2) 후보 정보로 백필 (좌표/링크 보강)
            AiPlanResponse filled = backfillById(parsed, restaurants, mid, finals);

            // 3) excludeIds 강제 필터
            if (excludeIds != null && !excludeIds.isEmpty()) {
                var ex = new java.util.HashSet<>(excludeIds);
                var singles = filled.singles()==null ? List.<IdeaItem>of()
                        : filled.singles().stream().filter(s -> s.id()==null || !ex.contains(s.id())).toList();
                var plans = filled.plans()==null ? List.<AiPlanResponse.Plan>of()
                        : filled.plans().stream().map(p -> {
                    var steps = p.steps()==null ? List.<AiPlanResponse.Step>of()
                            : p.steps().stream().filter(st -> st.id()==null || !ex.contains(st.id())).toList();
                    return new AiPlanResponse.Plan(p.totalTravelMinutes(), p.explain(), steps);
                }).toList();
                filled = new AiPlanResponse(singles, plans);
            }

            // 4) 역할/버킷 강제 사후 교정(LLM이 가끔 어기더라도 최종 산출물은 우리가 보증)
            filled = coerceToCandidates(filled, restaurants, mid, finals, finalRole);

            return filled;
        } catch (Exception e) {
            throw new IllegalStateException("AI 플래너 호출/파싱 실패", e);
        }
    }
    private AiPlanResponse coerceToCandidates(
            AiPlanResponse in,
            List<IdeaItem> restaurants, List<IdeaItem> mids, List<IdeaItem> finals,
            String finalRole
    ) {
        var restIds = (restaurants==null?List.<IdeaItem>of():restaurants).stream().map(IdeaItem::id).collect(java.util.stream.Collectors.toSet());
        var midIds  = (mids==null?List.<IdeaItem>of():mids).stream().map(IdeaItem::id).collect(java.util.stream.Collectors.toSet());
        var finIds  = (finals==null?List.<IdeaItem>of():finals).stream().map(IdeaItem::id).collect(java.util.stream.Collectors.toSet());

        boolean hasRestaurants = !restIds.isEmpty();
        boolean hasFinals      = !finIds.isEmpty();

        // singles: 각 버킷에 속하는 것만 남기고, 역할도 버킷 기준으로 보정
        var fixedSingles = (in.singles()==null?List.<IdeaItem>of():in.singles()).stream()
                .filter(s -> {
                    String id = s.id();
                    if (id == null) return false;
                    if (midIds.contains(id)) return true;
                    if (restIds.contains(id)) return true;
                    if (finIds.contains(id)) return true;
                    return false;
                })
                .map(s -> {
                    String id = s.id();
                    String role = s.role();
                    if (midIds.contains(id)) role = "activity";
                    else if (restIds.contains(id)) role = "restaurant";
                    else if (finIds.contains(id)) role = finalRole; // finals는 finish에 맞춘 role 강제
                    return new IdeaItem(
                            s.id(), role, s.name(), s.address(), s.lat(), s.lng(),
                            s.externalUrl(), s.mapLink(), s.rating(), s.ratingCount()
                    );
                })
                .toList();

        // plans: 각 plan의 steps를 재구성
        var fixedPlans = (in.plans()==null?List.<AiPlanResponse.Plan>of():in.plans()).stream().map(p -> {
            var orig = p.steps()==null?List.<AiPlanResponse.Step>of():p.steps();

            // 1) 원본에서 규칙에 부합하는 스텝만 추려서 역할 보정
            java.util.List<AiPlanResponse.Step> activities = new java.util.ArrayList<>();
            java.util.List<AiPlanResponse.Step> restaurantsSteps = new java.util.ArrayList<>();
            AiPlanResponse.Step finalStep = null;

            for (var s : orig) {
                if (s.id()==null) continue;
                String id = s.id();
                if (midIds.contains(id)) {
                    activities.add(forceRole(s, "activity"));
                } else if (restIds.contains(id)) {
                    restaurantsSteps.add(forceRole(s, "restaurant"));
                } else if (finIds.contains(id)) {
                    var fs = forceRole(s, finalRole);
                    if (finalStep == null) finalStep = fs; // 첫 finals 1개만 채택
                }
            }

            // 2) 부족하면 후보에서 보충
            if (activities.isEmpty() && !midIds.isEmpty()) {
                IdeaItem first = mids.get(0);
                activities.add(toStepFromItem(first, "activity"));
            }
            if (hasRestaurants && restaurantsSteps.isEmpty()) {
                IdeaItem first = restaurants.get(0);
                restaurantsSteps.add(toStepFromItem(first, "restaurant"));
            }
            if (hasFinals && finalStep == null) {
                IdeaItem first = finals.get(0);
                finalStep = toStepFromItem(first, finalRole);
            }

            // 3) 순서 구성: 활동 1~2 + (식사 선택) + 최종 1
            java.util.List<AiPlanResponse.Step> out = new java.util.ArrayList<>();
            if (!activities.isEmpty()) out.add(activities.get(0));
            if (activities.size() > 1) out.add(activities.get(1));
            if (hasRestaurants && !restaurantsSteps.isEmpty()) out.add(restaurantsSteps.get(0));
            if (finalStep != null) out.add(finalStep);

            // 최대 4스텝로 제한
            if (out.size() > 4) out = out.subList(0, 4);

            return new AiPlanResponse.Plan(p.totalTravelMinutes(), p.explain(), out);
        }).toList();

        return new AiPlanResponse(fixedSingles, fixedPlans);
    }

    private static AiPlanResponse.Step forceRole(AiPlanResponse.Step s, String role) {
        return new AiPlanResponse.Step(
                s.id(), role, s.name(), s.address(),
                s.lat(), s.lng(), s.externalUrl(), s.mapLink(), s.rating(), s.ratingCount()
        );
    }

    private static AiPlanResponse.Step toStepFromItem(IdeaItem it, String role) {
        return new AiPlanResponse.Step(
                it.id(), role, it.name(), it.address(),
                it.lat(), it.lng(), it.externalUrl(), it.mapLink(), it.rating(), it.ratingCount()
        );
    }



    private AiPlanResponse backfillById(
            AiPlanResponse raw,
            List<IdeaItem> restaurants, List<IdeaItem> mids, List<IdeaItem> finals
    ) {
        var byId = new java.util.HashMap<String, IdeaItem>();
        java.util.function.Consumer<IdeaItem> put = it -> {
            if (it != null && it.id()!=null && !it.id().isBlank()) byId.put(it.id(), it);
        };
        (restaurants!=null?restaurants:List.<IdeaItem>of()).forEach(put);
        (mids!=null?mids:List.<IdeaItem>of()).forEach(put);
        (finals!=null?finals:List.<IdeaItem>of()).forEach(put);

        // singles 백필
        var singles = raw.singles()==null ? List.<IdeaItem>of()
                : raw.singles().stream().map(s -> fillIdea(s, byId)).toList();

        // plans 백필
        var plans = raw.plans()==null ? List.<AiPlanResponse.Plan>of()
                : raw.plans().stream().map(p -> {
            var steps = p.steps()==null ? List.<AiPlanResponse.Step>of()
                    : p.steps().stream().map(st -> fillStep(st, byId)).toList();
            return new AiPlanResponse.Plan(p.totalTravelMinutes(), p.explain(), steps);
        }).toList();

        return new AiPlanResponse(singles, plans);
    }

    private IdeaItem fillIdea(IdeaItem s, java.util.Map<String, IdeaItem> byId) {
        if (s == null) return null;
        var base = s.id()!=null ? byId.get(s.id()) : null;
        if (base == null) return s;
        return new IdeaItem(
                s.id(),
                nvl(s.role(), base.role()),
                nvl(s.name(), base.name()),
                nvl(s.address(), base.address()),
                (s.lat()!=0 ? s.lat() : base.lat()),
                (s.lng()!=0 ? s.lng() : base.lng()),
                nvl(s.externalUrl(), base.externalUrl()),
                nvl(s.mapLink(), base.mapLink()),
                s.rating()!=null ? s.rating() : base.rating(),
                s.ratingCount()!=null ? s.ratingCount() : base.ratingCount()
        );
    }

    private AiPlanResponse.Step fillStep(AiPlanResponse.Step s, java.util.Map<String, IdeaItem> byId) {
        if (s == null) return null;
        var base = (s.id()!=null) ? byId.get(s.id()) : null;

        if (base == null) {
            // 후보(byId)에 없으면: id 디코드해서 보강
            String[] parts = (s.id()!=null) ? parseIdeaId(s.id()) : null;
            if (parts != null) {
                String name = nvl(s.name(), parts[0]);
                double lat  = (s.lat()!=0 ? s.lat() : Double.parseDouble(parts[1]));
                double lng  = (s.lng()!=0 ? s.lng() : Double.parseDouble(parts[2]));
                String map  = (s.mapLink()!=null && !s.mapLink().isBlank()) ? s.mapLink() : kakaoToLink(name, lat, lng);
                return new AiPlanResponse.Step(
                        s.id(), nvl(s.role(), "activity"), name, nvl(s.address(), ""),
                        lat, lng, s.externalUrl(), map, s.rating(), s.ratingCount()
                );
            }
            // 최후 폴백: 링크만이라도 생성
            if (s.mapLink()==null || s.mapLink().isBlank()) {
                String link = kakaoToLink(nvl(s.name(), "장소"), s.lat(), s.lng());
                return new AiPlanResponse.Step(s.id(), s.role(), s.name(), s.address(),
                        s.lat(), s.lng(), s.externalUrl(), link, s.rating(), s.ratingCount());
            }
            return s;
        }

        // 기존 backfill 경로 (후보 정보로 보강)
        String name = nvl(s.name(), base.name());
        double lat  = (s.lat()!=0 ? s.lat() : base.lat());
        double lng  = (s.lng()!=0 ? s.lng() : base.lng());
        String map  = nvl(s.mapLink(), base.mapLink());
        if (map==null || map.isBlank()) map = kakaoToLink(name, lat, lng);

        return new AiPlanResponse.Step(
                s.id(),
                nvl(s.role(), base.role()),
                name,
                nvl(s.address(), base.address()),
                lat, lng,
                nvl(s.externalUrl(), base.externalUrl()),
                map,
                s.rating()!=null ? s.rating() : base.rating(),
                s.ratingCount()!=null ? s.ratingCount() : base.ratingCount()
        );
    }



    private static <T> T nvl(T a, T b){
        if (a instanceof String sa) return (sa!=null && !sa.isBlank()) ? a : b;
        return a!=null ? a : b;
    }

}

