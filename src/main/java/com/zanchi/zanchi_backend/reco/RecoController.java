package com.zanchi.zanchi_backend.reco;

import com.zanchi.zanchi_backend.domain.clip.Clip;
import com.zanchi.zanchi_backend.domain.clip.dto.ClipFeedRes;
import com.zanchi.zanchi_backend.domain.clip.repository.ClipLikeRepository;
import com.zanchi.zanchi_backend.domain.clip.repository.ClipRepository;
import com.zanchi.zanchi_backend.domain.clip.repository.ClipSaveRepository;
import com.zanchi.zanchi_backend.domain.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.services.personalizeruntime.PersonalizeRuntimeClient;
import software.amazon.awssdk.services.personalizeruntime.model.GetRecommendationsRequest;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reco")
public class RecoController {

    private final PersonalizeRuntimeClient runtime;
    private final ClipRepository clipRepository;
    private final ClipLikeRepository clipLikeRepository;
    private final ClipSaveRepository clipSaveRepository;
    private final MemberRepository memberRepository;

    @Value("${reco.personalize.campaign-arn}")
    private String campaignArn;

    /**
     * 개인화 추천 피드 (clip-feed와 같은 응답 구조 + liked/saved 반영)
     */
    @GetMapping("/my-feed")
    public ResponseEntity<Page<ClipFeedRes>> myFeed(Authentication auth,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        if (auth == null) return ResponseEntity.status(401).build();

        String loginId = auth.getName();
        Long meId = memberRepository.findIdByLoginId(loginId).orElse(null);

        // Personalize는 서버 페이징이 없어서 넉넉히 받아와서 슬라이싱
        int want = Math.min(100, (page + 1) * size);

        List<String> recItemIds;
        try {
            var res = runtime.getRecommendations(GetRecommendationsRequest.builder()
                    .campaignArn(campaignArn)
                    .userId(loginId)           // ★ interactions.users에 저장한 user-id 형식과 동일해야 함 (확실)
                    .numResults(want)
                    .build());
            recItemIds = res.itemList().stream().map(i -> i.itemId()).toList();
        } catch (Exception e) {
            // Personalize 호출 실패 시 폴백
            return ResponseEntity.ok(fallbackFeed(meId, page, size));
        }

        // === 아이템 ID → Clip ID 파싱 ===
        List<Long> predictedIds = recItemIds.stream()
                .map(RecoController::toClipId)      // 아래 파서 참고
                .filter(Objects::nonNull)
                .distinct()                          // 중복 제거
                .toList();

        if (predictedIds.isEmpty()) {
            // 추천은 왔지만 우리 DB와 매칭이 0개면 폴백
            return ResponseEntity.ok(fallbackFeed(meId, page, size));
        }

        // DB 조회 및 원래 추천 순서 보존
        Map<Long, Clip> found = clipRepository.findAllById(predictedIds).stream()
                .collect(Collectors.toMap(Clip::getId, c -> c));

        List<Long> ordered = predictedIds.stream()
                .filter(found::containsKey)
                .toList();

        // 페이지 슬라이스
        int from = Math.min(page * size, ordered.size());
        int to = Math.min(from + size, ordered.size());
        List<Long> pageIds = ordered.subList(from, to);
        List<Clip> pageClips = pageIds.stream().map(found::get).toList();

        // liked/saved 플래그 (clip-feed와 동일 로직)
        Set<Long> likedSet = (meId != null && !pageIds.isEmpty())
                ? new HashSet<>(clipLikeRepository.findLikedClipIds(meId, pageIds))
                : Set.of();

        Set<Long> savedSet = (meId != null && !pageIds.isEmpty())
                ? new HashSet<>(clipSaveRepository.findSavedClipIds(meId, pageIds))
                : Set.of();

        var content = pageClips.stream()
                .map(c -> ClipFeedRes.of(c,
                        likedSet.contains(c.getId()),
                        savedSet.contains(c.getId())))
                .toList();

        return ResponseEntity.ok(new PageImpl<>(content, PageRequest.of(page, size), ordered.size()));
    }

    /** 실패/공백 시 기본 피드로 폴백 (clip-feed와 동일 응답) */
    private Page<ClipFeedRes> fallbackFeed(Long meId, int page, int size) {
        Page<Clip> p = clipRepository.findAllByOrderByIdDesc(PageRequest.of(page, size));
        var ids = p.getContent().stream().map(Clip::getId).toList();

        Set<Long> likedSet = (meId != null && !ids.isEmpty())
                ? new HashSet<>(clipLikeRepository.findLikedClipIds(meId, ids))
                : Set.of();

        Set<Long> savedSet = (meId != null && !ids.isEmpty())
                ? new HashSet<>(clipSaveRepository.findSavedClipIds(meId, ids))
                : Set.of();

        return p.map(c -> ClipFeedRes.of(c,
                likedSet.contains(c.getId()),
                savedSet.contains(c.getId())));
    }

    /** itemId 파서: "123" 또는 "...123" (접미 숫자) */
    private static Long toClipId(String itemId) {
        if (itemId == null || itemId.isBlank()) return null;

        // 1) 순수 숫자
        try { return Long.valueOf(itemId); }
        catch (NumberFormatException ignore) {}

        // 2) 문자열 끝의 숫자 (예: "clip_0012", "clip-42") → 12, 42
        Matcher m = Pattern.compile("(\\d+)$").matcher(itemId);
        if (m.find()) {
            try { return Long.valueOf(m.group(1)); }
            catch (NumberFormatException ignore) {}
        }
        return null; // 매칭 실패 시 스킵
    }
}
