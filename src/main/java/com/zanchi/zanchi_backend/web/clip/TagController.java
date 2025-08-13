package com.zanchi.zanchi_backend.web.clip;

import com.zanchi.zanchi_backend.domain.clip.Clip;
import com.zanchi.zanchi_backend.domain.clip.tag.dto.ClipByTagRes;
import com.zanchi.zanchi_backend.domain.clip.tag.dto.TagDto;
import com.zanchi.zanchi_backend.domain.clip.repository.ClipRepository;
import com.zanchi.zanchi_backend.domain.clip.service.HashtagService;
import com.zanchi.zanchi_backend.domain.clip.tag.Tag;
import com.zanchi.zanchi_backend.domain.clip.tag.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class TagController {

    private final TagRepository tagRepository;
    private final ClipRepository clipRepository;

    /** GET /api/tags : 태그 목록(사용량 내림차순) */
    @GetMapping("/tags")
    public ResponseEntity<List<TagDto>> listTags() {
        var rows = tagRepository.findAllWithUsageCount(); // (Tag, Long usageCount)
        var body = rows.stream().map(row -> {
            Tag t = (Tag) row[0];
            long cnt = (long) row[1];
            return TagDto.of(t, cnt);
        }).toList();
        return ResponseEntity.ok(body);
    }

    /** GET /api/tags/clips?tag=xxx&page=0&size=20 : 태그로 클립 검색 */
    @GetMapping("/tags/clips")
    public ResponseEntity<Page<ClipByTagRes>> listClipsByTag(
            @RequestParam("tag") String tag,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        String token = tag.startsWith("#") ? tag.substring(1) : tag;
        String normalized = HashtagService.normalize(token);

        Page<Clip> result = clipRepository.findByTagNormalized(normalized, PageRequest.of(page, size));
        return ResponseEntity.ok(result.map(ClipByTagRes::of));
    }
}
