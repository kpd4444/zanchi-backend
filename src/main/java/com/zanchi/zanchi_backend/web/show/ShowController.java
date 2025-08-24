// com.zanchi.zanchi_backend.web.show.ShowController

package com.zanchi.zanchi_backend.web.show;

import com.zanchi.zanchi_backend.domain.ranking.RankingService;
import com.zanchi.zanchi_backend.domain.ranking.dto.ClipRankItem;
import com.zanchi.zanchi_backend.domain.show.Show;
import com.zanchi.zanchi_backend.domain.show.ShowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.*;

@RestController
@RequestMapping("/api/shows")
@RequiredArgsConstructor
public class ShowController {

    private final ShowRepository showRepository;
    private final RankingService rankingService;

    @GetMapping("/{showId}")
    public Map<String, Object> detail(
            @PathVariable Integer showId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime since
    ) {
        Show s = showRepository.findById(showId)
                .orElseThrow(() -> new NoSuchElementException("show not found"));

        var page = rankingService.getRankingByShow(showId, since, PageRequest.of(0, 10));

        List<Map<String, Object>> topClips = page.getContent().stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("clipId", r.getClipId());
            m.put("uploaderId", r.getUploaderId());
            m.put("uploaderName", r.getUploaderName());
            m.put("likeCount", r.getLikeCount());
            m.put("caption", r.getCaption());
            m.put("videoUrl", r.getVideoUrl());
            m.put("createdAt", r.getCreatedAt());
            return m;
        }).toList();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("showId", s.getId());
        body.put("title", s.getTitle());
        body.put("date", s.getDate());
        body.put("venue", s.getVenue());
        body.put("posterUrl", s.getPosterUrl());
        body.put("price", s.getPrice());
        body.put("remainingQty", s.getRemainingQty());
        body.put("salesStartAt", s.getSalesStartAt());
        body.put("salesEndAt", s.getSalesEndAt());
        body.put("description", s.getDescription());
        body.put("topClips", topClips);
        return body;
    }
}
