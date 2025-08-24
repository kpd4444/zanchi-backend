package com.zanchi.zanchi_backend.domain.ranking;

import com.zanchi.zanchi_backend.domain.clip.repository.ClipRepository;
import com.zanchi.zanchi_backend.domain.ranking.dto.ClipRankItem;
import com.zanchi.zanchi_backend.domain.ranking.dto.ClipRankView;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;        // 전체 랭킹용
import java.time.LocalDateTime; // 공연별 랭킹용

@Service
@RequiredArgsConstructor
public class RankingService {
    private final ClipRepository clipRepository;

    /** 전체 랭킹 (RankingController가 호출) — 기존 API 호환 */
    @Transactional(readOnly = true)
    public Page<ClipRankView> getRanking(Instant since, Pageable pageable) {
        return clipRepository.findRanking(since, pageable);
    }

    /** 공연별 Top10 (ShowController가 호출) */
    @Transactional(readOnly = true)
    public Page<ClipRankItem> getRankingByShow(Integer showId, LocalDateTime since, Pageable pageable) {
        return clipRepository.findRankingByShowId(showId, since, pageable);
    }
}
