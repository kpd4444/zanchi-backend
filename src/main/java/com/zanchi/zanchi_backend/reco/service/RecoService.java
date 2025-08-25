package com.zanchi.zanchi_backend.reco.service;

import com.zanchi.zanchi_backend.domain.clip.Clip;
import com.zanchi.zanchi_backend.domain.clip.dto.ClipFeedRes;
import com.zanchi.zanchi_backend.domain.clip.repository.ClipLikeRepository;
import com.zanchi.zanchi_backend.domain.clip.repository.ClipRepository;
import com.zanchi.zanchi_backend.domain.clip.repository.ClipSaveRepository;
import com.zanchi.zanchi_backend.domain.member.MemberRepository;
import com.zanchi.zanchi_backend.reco.PersonalizeItemMap;
import com.zanchi.zanchi_backend.reco.repository.PersonalizeItemMapRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.personalizeruntime.PersonalizeRuntimeClient;
import software.amazon.awssdk.services.personalizeruntime.model.GetRecommendationsRequest;
import software.amazon.awssdk.services.personalizeruntime.model.GetRecommendationsResponse;
import software.amazon.awssdk.services.personalizeruntime.model.PersonalizeRuntimeException;
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

    private final PersonalizeRuntimeClient personalizeRuntime;   // AWS SDK v2
    private final MemberRepository memberRepository;
    private final ClipRepository clipRepository;
    private final ClipLikeRepository clipLikeRepository;
    private final ClipSaveRepository clipSaveRepository;
    private final PersonalizeItemMapRepository itemMapRepository; // ★ 추가

    @Value("${reco.personalize.campaign-arn:}")
    private String campaignArn;

    @Value("${reco.personalize.enabled:true}")
    private boolean personalizeEnabled;

    // 추천 결과가 DB와 불일치할 수 있으니 넉넉히 받아서 건질 확률을 올림
    private static final int WANT_MULTIPLIER = 10;

    public Page<ClipFeedRes> getMyFeed(String loginId, int page, int size) {
        Long userId = memberRepository.findIdByLoginId(loginId)
                .orElseThrow(() -> new IllegalStateException("로그인 사용자 id를 찾을 수 없습니다: " + loginId));

        final Pageable pageable = PageRequest.of(page, size);

        // 기능 토글: 비활성화면 곧장 폴백
        if (!personalizeEnabled) {
            log.warn("Personalize disabled by config. Falling back to trending.");
            return trendingFeed(pageable, userId);
        }
        if (!StringUtils.hasText(campaignArn)) {
            log.error("reco.personalize.campaign-arn 설정이 비어있습니다. Falling back to trending.");
            return trendingFeed(pageable, userId);
        }

        try {
            final int want = Math.max(size * WANT_MULTIPLIER, size);

            GetRecommendationsResponse resp = personalizeRuntime.getRecommendations(
                    GetRecommendationsRequest.builder()
                            .campaignArn(campaignArn)
                            .userId(String.valueOf(userId))
                            .numResults(want)
                            .build()
            );

            List<String> itemIds = resp.itemList().stream().map(PredictedItem::itemId).toList();
            log.info("Personalize itemIds userId={} count={} sample(<=20)={}",
                    userId, itemIds.size(), itemIds.stream().limit(20).toList());

            if (itemIds.isEmpty()) {
                log.warn("Personalize returned empty list. Falling back to trending.");
                return trendingFeed(pageable, userId);
            }

            // 1) 숫자 파싱 + 2) 매핑테이블 조회 (원본 순서 보존)
            //    - 순서를 지키기 위해 itemIds를 순회하며 clipId를 결정
            Map<String, Long> tableMap = Map.of();
            List<String> unmapped = new ArrayList<>();

            // 1차: 숫자 파싱 시도
            List<Long> clipIdsOrdered = new ArrayList<>(itemIds.size());
            for (String iid : itemIds) {
                Optional<Long> parsed = parseClipId(iid);
                if (parsed.isPresent()) {
                    clipIdsOrdered.add(parsed.get());
                } else {
                    unmapped.add(iid);
                }
            }

            // 2차: 테이블 매핑 (있을 때만 조회)
            if (!unmapped.isEmpty()) {
                var rows = itemMapRepository.findByItemIdIn(unmapped);
                if (!rows.isEmpty()) {
                    tableMap = rows.stream().collect(Collectors.toMap(PersonalizeItemMap::getItemId, PersonalizeItemMap::getClipId));
                }
            }

            if (!tableMap.isEmpty()) {
                // 원본 순서를 유지하며 남은 itemId를 테이블 매핑으로 보충
                for (String iid : itemIds) {
                    if (clipIdsOrdered.size() >= itemIds.size()) break;
                    if (tableMap.containsKey(iid)) {
                        clipIdsOrdered.add(tableMap.get(iid));
                    }
                }
            }

            // 중복 제거 (앞쪽 우선순위 유지)
            LinkedHashSet<Long> dedup = new LinkedHashSet<>(clipIdsOrdered);
            clipIdsOrdered = new ArrayList<>(dedup);

            if (clipIdsOrdered.isEmpty()) {
                log.warn("추천 결과에 매핑 가능한 Clip ID가 없습니다. rawItemIds={}", itemIds);
                return trendingFeed(pageable, userId);
            }

            // DB 조회 후 추천 순서 유지
            Map<Long, Clip> byId = clipRepository.findAllById(clipIdsOrdered).stream()
                    .collect(Collectors.toMap(Clip::getId, c -> c));

            List<Clip> ordered = clipIdsOrdered.stream()
                    .map(byId::get)
                    .filter(Objects::nonNull)
                    .toList();

            // 추천이 전부 DB에 없으면 폴백
            if (ordered.isEmpty()) {
                log.warn("Mapped IDs not found in DB. rawItemIds={}", itemIds);
                return trendingFeed(pageable, userId);
            }

            // 페이지 슬라이스
            int from = Math.min(page * size, ordered.size());
            int to = Math.min(from + size, ordered.size());
            List<Clip> pageList = new ArrayList<>(ordered.subList(from, to));

            // 추천으로 채운 수가 부족하면 폴백으로 보강 (중복 제외)
            if (pageList.size() < size) {
                int need = size - pageList.size();
                log.info("Fill with fallback: need={}", need);
                List<Long> excludes = pageList.stream().map(Clip::getId).toList();
                pageList.addAll(trendingFill(excludes, need));
            }

            // 나의 좋아요/저장 표시
            Set<Long> idsForMark = pageList.stream().map(Clip::getId).collect(Collectors.toSet());
            Set<Long> liked = idsForMark.isEmpty() ? Set.of()
                    : new HashSet<>(clipLikeRepository.findLikedClipIds(userId, new ArrayList<>(idsForMark)));
            Set<Long> saved = idsForMark.isEmpty() ? Set.of()
                    : new HashSet<>(clipSaveRepository.findSavedClipIds(userId, new ArrayList<>(idsForMark)));

            List<ClipFeedRes> res = pageList.stream()
                    .map(c -> ClipFeedRes.of(c, liked.contains(c.getId()), saved.contains(c.getId())))
                    .toList();

            return new PageImpl<>(res, pageable, Math.max(ordered.size(), page * size + res.size()));

        } catch (PersonalizeRuntimeException e) {
            String msg = (e.awsErrorDetails() != null && e.awsErrorDetails().errorMessage() != null)
                    ? e.awsErrorDetails().errorMessage()
                    : e.getMessage();
            log.error("Personalize GetRecommendations 실패: {}", msg, e);
            return trendingFeed(pageable, userId); // 장애 시 폴백
        } catch (Exception e) {
            log.error("추천 피드 처리 중 예외: {}", e.getMessage(), e);
            return trendingFeed(pageable, userId); // 예외 시 폴백
        }
    }

    /** 폴백 피드: 최신 업로드(또는 트렌딩) */
    private Page<ClipFeedRes> trendingFeed(Pageable pageable, Long userId) {
        Page<Clip> pageClips = clipRepository.findAll(
                PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                        Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        Set<Long> ids = pageClips.getContent().stream().map(Clip::getId).collect(Collectors.toSet());
        Set<Long> liked = ids.isEmpty() ? Set.of()
                : new HashSet<>(clipLikeRepository.findLikedClipIds(userId, new ArrayList<>(ids)));
        Set<Long> saved = ids.isEmpty() ? Set.of()
                : new HashSet<>(clipSaveRepository.findSavedClipIds(userId, new ArrayList<>(ids)));

        List<ClipFeedRes> res = pageClips.getContent().stream()
                .map(c -> ClipFeedRes.of(c, liked.contains(c.getId()), saved.contains(c.getId())))
                .toList();

        return new PageImpl<>(res, pageable, pageClips.getTotalElements());
    }

    /** 보강용 폴백 아이템: 최신 업로드에서 excludes를 제외하고 need만큼 채움 */
    private List<Clip> trendingFill(List<Long> excludes, int need) {
        int fetch = Math.max(need * 3, need);
        Page<Clip> recent = clipRepository.findAll(
                PageRequest.of(0, fetch, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        Set<Long> excludeSet = (excludes == null) ? Set.of() : new HashSet<>(excludes);

        List<Clip> out = new ArrayList<>(need);
        for (Clip c : recent.getContent()) {
            if (excludeSet.contains(c.getId())) continue;
            out.add(c);
            if (out.size() >= need) break;
        }
        return out;
    }

    // ========= ITEM_ID → Clip.id 파싱 =========
    private static final Pattern CLIP_PREFIX_NUM = Pattern.compile("^clip[_-]?0*(\\d+)$");

    private Optional<Long> parseClipId(String itemId) {
        if (itemId == null || itemId.isBlank()) return Optional.empty();

        // 순수 숫자
        boolean allDigits = true;
        for (int i = 0; i < itemId.length(); i++) {
            if (!Character.isDigit(itemId.charAt(i))) { allDigits = false; break; }
        }
        if (allDigits) {
            try { return Optional.of(Long.parseLong(itemId)); }
            catch (NumberFormatException ignore) { return Optional.empty(); }
        }

        // clip_XXXX / clip-XXXX (선행 0 허용)
        Matcher m = CLIP_PREFIX_NUM.matcher(itemId);
        if (m.matches()) {
            try { return Optional.of(Long.parseLong(m.group(1))); }
            catch (NumberFormatException ignore) { return Optional.empty(); }
        }

        // 매핑 불가
        return Optional.empty();
    }
}
