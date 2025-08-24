package com.zanchi.zanchi_backend.reco;

import com.zanchi.zanchi_backend.domain.clip.Clip;
import com.zanchi.zanchi_backend.domain.clip.dto.ClipFeedRes;
import com.zanchi.zanchi_backend.domain.clip.repository.ClipLikeRepository;
import com.zanchi.zanchi_backend.domain.clip.repository.ClipRepository;
import com.zanchi.zanchi_backend.domain.clip.repository.ClipSaveRepository;
import com.zanchi.zanchi_backend.domain.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.personalizeruntime.PersonalizeRuntimeClient;
import software.amazon.awssdk.services.personalizeruntime.model.GetRecommendationsRequest;
import software.amazon.awssdk.services.personalizeruntime.model.GetRecommendationsResponse;
import software.amazon.awssdk.services.personalizeruntime.model.PredictedItem;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RecoService {

    private final PersonalizeRuntimeClient personalizeRuntime;   // sdk v2
    private final MemberRepository memberRepository;
    private final ClipRepository clipRepository;
    private final ClipLikeRepository clipLikeRepository;
    private final ClipSaveRepository clipSaveRepository;

    @Value("${reco.personalize.campaign-arn}")
    private String campaignArn; // 예: arn:aws:personalize:ap-northeast-2:...:campaign/zanchi-up-campaign

    /**
     * 개인화 추천 피드 (페이지네이션)
     * - Personalize는 오프셋 기반 페이지 기능이 없으므로 요청 시점에 page*size + size 만큼 받아 slice.
     * - 정렬은 Personalize가 반환한 순서를 유지.
     */
    public Page<ClipFeedRes> getMyFeed(String loginId, int page, int size) {
        // 0) userId
        Long userId = memberRepository.findIdByLoginId(loginId)
                .orElseThrow(() -> new IllegalStateException("로그인 사용자 id를 찾을 수 없습니다: " + loginId));
        String personalizeUserId = String.valueOf(userId); // [확정] USERS 스키마 USER_ID=string

        int want = Math.max(size * (page + 1), size);
        // 1) 호출
        GetRecommendationsResponse resp = personalizeRuntime.getRecommendations(
                GetRecommendationsRequest.builder()
                        .campaignArn(campaignArn)
                        .userId(personalizeUserId)
                        .numResults(want)
                        .build()
        );

        List<String> itemIds = resp.itemList().stream()
                .map(PredictedItem::itemId)
                .collect(Collectors.toList());

        if (itemIds.isEmpty()) {
            return Page.empty(PageRequest.of(page, size));
        }

        // 2) Personalize ITEM_ID → Clip.id 매핑
        // [확실하지 않음] 규칙: 숫자면 그대로 Long, "clip_123" 형태면 숫자만 추출
        List<Long> clipIdsOrdered = new ArrayList<>();
        for (String iid : itemIds) {
            parseClipId(iid).ifPresent(clipIdsOrdered::add);
        }

        if (clipIdsOrdered.isEmpty()) {
            log.warn("추천 결과에 매핑 가능한 Clip ID가 없습니다. itemIds={}", itemIds);
            return Page.empty(PageRequest.of(page, size));
        }

        // 3) DB 조회 후 추천 순서 유지
        Map<Long, Clip> byId = clipRepository.findAllById(clipIdsOrdered).stream()
                .collect(Collectors.toMap(Clip::getId, c -> c));
        List<Clip> ordered = clipIdsOrdered.stream()
                .map(byId::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // 4) 페이지 슬라이스
        int from = Math.min(page * size, ordered.size());
        int to = Math.min(from + size, ordered.size());
        List<Clip> pageList = ordered.subList(from, to);

        // 5) 나의 좋아요/저장 표시
        Set<Long> idsForMark = pageList.stream().map(Clip::getId).collect(Collectors.toSet());
        Set<Long> liked = idsForMark.isEmpty() ? Set.of()
                : new HashSet<>(clipLikeRepository.findLikedClipIds(userId, new ArrayList<>(idsForMark)));
        Set<Long> saved = idsForMark.isEmpty() ? Set.of()
                : new HashSet<>(clipSaveRepository.findSavedClipIds(userId, new ArrayList<>(idsForMark)));

        List<ClipFeedRes> res = pageList.stream()
                .map(c -> ClipFeedRes.of(c, liked.contains(c.getId()), saved.contains(c.getId())))
                .collect(Collectors.toList());

        // totalElements: Personalize가 반환한 결과 길이를 총합으로 사용
        Pageable pageable = PageRequest.of(page, size);
        return new PageImpl<>(res, pageable, ordered.size());
    }

    // ======================
    // ITEM_ID → Clip.id 파싱
    // ======================

    private static final Pattern CLIP_PREFIX_NUM = Pattern.compile("^clip[_-]?(\\d+)$");

    private Optional<Long> parseClipId(String itemId) {
        if (itemId == null || itemId.isBlank()) return Optional.empty();

        // pure number
        if (itemId.chars().allMatch(Character::isDigit)) {
            try { return Optional.of(Long.parseLong(itemId)); }
            catch (NumberFormatException ignore) { return Optional.empty(); }
        }

        // clip_123, clip-123
        Matcher m = CLIP_PREFIX_NUM.matcher(itemId);
        if (m.matches()) {
            try { return Optional.of(Long.parseLong(m.group(1))); }
            catch (NumberFormatException ignore) { return Optional.empty(); }
        }

        // [확실하지 않음] 프로젝트에서 다른 규칙을 쓰면 여기서 확장
        // 예: "clip_seed1" 같은 seed는 매핑 불가 → 제외
        return Optional.empty();
    }
}
