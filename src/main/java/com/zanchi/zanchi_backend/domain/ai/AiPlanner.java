package com.zanchi.zanchi_backend.domain.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

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
        Map<String, Object> context = Map.of(
                "start", Map.of(
                        "name", startName,
                        "lat", startLat,
                        "lng", startLng,
                        "mapLink", kakaoToLink(startName, startLat, startLng)
                ),
                "mobility", mobility,
                "cuisine", cuisine,
                "finish", finish,
                "companion", companion,
                "what", what
        );
        Map<String, Object> candidates = Map.of(
                "restaurants", restaurants,
                "mid", mids == null ? List.of() : mids,
                "finals", finals == null ? List.of() : finals
        );
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

    private static String kakaoToLink(String name, double lat, double lng) {
        return "https://map.kakao.com/link/to/" +
                URLEncoder.encode(name, StandardCharsets.UTF_8) + "," + lat + "," + lng;
    }
    /** LLM에게 후보를 넘겨 singles(5~6) + plans(2) JSON을 받아온다. */
    public AiPlanResponse planTwoRoutes(
            String companion, String mobility,
            String startName, double startLat, double startLng,
            String cuisine, String finish, String what,
            List<IdeaItem> restaurants, List<IdeaItem> mid, List<IdeaItem> finals
    ) {
        String system = """
        너는 데이트 코스 플래너다. 반드시 JSON만 출력.
        스키마: {"singles":[{...}], "plans":[{"totalTravelMinutes":0,"explain":"","steps":[{...}]},{...}]}
        규칙:
          - 후보(candidates.*)만 사용하고, 새 장소를 만들지 않는다.
          - 장소는 반드시 후보의 'id'로 참조한다.
          - steps[].id 를 반드시 포함한다(나머지 필드는 비어도 됨).
          - singles는 후보 중에서 5~6개 (역할 다양화).
          - plans는 2개, 도보 동선 짧게, finish 선호는 최소 1코스에 강반영.
          - step.mapLink는 "https://map.kakao.com/link/to/{URLEncoded name},{lat},{lng}" 형식.
        """;

        try {
            Map<String, Object> payload = Map.of(
                    "context", Map.of(
                            "start", Map.of(
                                    "name", startName, "lat", startLat, "lng", startLng,
                                    "mapLink", kakaoToLink(startName, startLat, startLng)
                            ),
                            "mobility", mobility, "cuisine", cuisine,
                            "finish", finish, "companion", companion, "what", what
                    ),
                    "candidates", Map.of(
                            "restaurants", restaurants != null ? restaurants : List.of(),
                            "mid",          mid != null ? mid : List.of(),
                            "finals",       finals != null ? finals : List.of()
                    )
            );

            String user = "입력:\n" + om.writeValueAsString(payload) + """
                
                작업:
                - candidates.mid/restaurants/finals 중에서 'id'로만 선택한다. 새 장소 추가 금지.
                - steps[].id 반드시 포함. name/address/lat/lng가 비어도 된다(서버가 보강).
                - explain은 한국어 2~3문장.
                출력: JSON만.
                """;

            String raw  = chat.prompt().system(system).user(user).call().content();
            String json = extractJson(raw);

            AiPlanResponse parsed = om.readValue(json, AiPlanResponse.class);

            // ✅ 응답을 후보로 백필하여 ? 방지 + 링크/좌표 보장
            return backfillById(parsed, restaurants, mid, finals);

        } catch (Exception e) {
            throw new IllegalStateException("AI 플래너 호출/파싱 실패", e);
        }
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
            // id가 없으면 그대로 두되, mapLink라도 만들어 줌
            if (s.mapLink()==null || s.mapLink().isBlank()) {
                String link = kakaoToLink(nvl(s.name(), "장소"), s.lat(), s.lng());
                return new AiPlanResponse.Step(s.id(), s.role(), s.name(), s.address(),
                        s.lat(), s.lng(), s.externalUrl(), link, s.rating(), s.ratingCount());
            }
            return s;
        }
        // base로 보강
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

