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

    private static final Pattern TAG_PATTERN =
            Pattern.compile("\\B#([\\p{L}\\p{N}_]{1,30})");

    private final TagRepository tagRepository;
    private final ClipTagRepository clipTagRepository;

    public static String normalize(String token) {
        return token.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}_]", "");
    }

    public Set<String> extract(String text) {
        if (text == null) return Set.of();
        var m = TAG_PATTERN.matcher(text);
        Set<String> out = new LinkedHashSet<>();
        while (m.find()) out.add(m.group(1));
        return out;
    }

    @Transactional
    public void syncClipTags(Clip clip, String content) {
        Set<String> tokens = extract(content);
        clipTagRepository.deleteByClip(clip);
        if (tokens.isEmpty()) return;

        for (String token : tokens) {
            String normalized = normalize(token);
            if (normalized.isBlank()) continue;

            Tag tag = tagRepository.findByNormalizedName(normalized).orElseGet(() -> {
                try {
                    return tagRepository.save(Tag.builder()
                            .name(token)
                            .normalizedName(normalized)
                            .build());
                } catch (DataIntegrityViolationException e) {
                    return tagRepository.findByNormalizedName(normalized).orElseThrow();
                }
            });

            clipTagRepository.save(ClipTag.builder().clip(clip).tag(tag).build());
        }
    }
}
