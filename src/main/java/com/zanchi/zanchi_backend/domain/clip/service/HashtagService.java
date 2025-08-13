package com.zanchi.zanchi_backend.domain.clip.service;

import com.zanchi.zanchi_backend.domain.clip.Clip;
import com.zanchi.zanchi_backend.domain.clip.tag.ClipTag;
import com.zanchi.zanchi_backend.domain.clip.tag.Tag;
import com.zanchi.zanchi_backend.domain.clip.tag.repository.ClipTagRepository;
import com.zanchi.zanchi_backend.domain.clip.tag.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class HashtagService {

    // # 뒤에 글자, 숫자, 언더바 허용 (1~30글자)
    private static final Pattern TAG_PATTERN =
            Pattern.compile("\\B#([\\p{L}\\p{N}_]{1,30})");

    private final TagRepository tagRepository;
    private final ClipTagRepository clipTagRepository;

    /**
     * 태그 이름을 정규화 (소문자 + 특수문자 제거)
     */
    public static String normalize(String token) {
        return token.toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}_]", "");
    }

    /**
     * 캡션에서 해시태그 추출
     */
    public Set<String> extract(String text) {
        if (text == null) return Set.of();
        var m = TAG_PATTERN.matcher(text);
        Set<String> out = new LinkedHashSet<>();
        while (m.find()) {
            String raw = m.group(1);
            String normalized = normalize(raw);
            // 정규화 후에도 값이 있어야 태그로 인정
            if (!normalized.isBlank()) {
                out.add(raw);
            }
        }
        return out;
    }

    /**
     * Clip의 태그 동기화
     */
    @Transactional
    public void syncClipTags(Clip clip, String content) {
        Set<String> tokens = extract(content);

        // 기존 태그 삭제 후 새로 등록
        clipTagRepository.deleteByClip(clip);
        if (tokens.isEmpty()) return;

        for (String token : tokens) {
            String normalized = normalize(token);
            if (normalized.isBlank()) continue; // 안전망

            Tag tag = tagRepository.findByNormalizedName(normalized).orElseGet(() -> {
                try {
                    return tagRepository.save(Tag.builder()
                            .name(token)
                            .normalizedName(normalized)
                            .build());
                } catch (DataIntegrityViolationException e) {
                    // 이미 다른 트랜잭션에서 생성했을 수 있음 → 다시 조회
                    return tagRepository.findByNormalizedName(normalized).orElseThrow();
                }
            });

            clipTagRepository.save(ClipTag.builder()
                    .clip(clip)
                    .tag(tag)
                    .build());
        }
    }
}
