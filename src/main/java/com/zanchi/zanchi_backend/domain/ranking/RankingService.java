package com.zanchi.zanchi_backend.domain.ranking;

import com.zanchi.zanchi_backend.domain.clip.repository.ClipRepository;
import com.zanchi.zanchi_backend.domain.ranking.dto.ClipRankView;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class RankingService {
    private final ClipRepository clipRepository;

    @Transactional(readOnly = true)
    public Page<ClipRankView> getRanking(Instant since, Pageable pageable) {
        return clipRepository.findRanking(since, pageable);
    }
}