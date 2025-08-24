package com.zanchi.zanchi_backend.web.ranking;

import com.zanchi.zanchi_backend.domain.ranking.RankingService;
import com.zanchi.zanchi_backend.domain.ranking.dto.ClipRankView;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequiredArgsConstructor
public class RankingController {
    private final RankingService rankingService;

    @GetMapping("/api/ranking/clips")
    public Page<ClipRankView> ranking(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant since,
            @PageableDefault(size = 30, sort = "likeCount", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return rankingService.getRanking(since, pageable);
    }
}
