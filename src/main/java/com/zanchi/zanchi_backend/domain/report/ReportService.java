package com.zanchi.zanchi_backend.domain.report;

import com.zanchi.zanchi_backend.domain.clip.Clip;
import com.zanchi.zanchi_backend.domain.clip.repository.ClipRepository;
import com.zanchi.zanchi_backend.domain.member.Member;
import com.zanchi.zanchi_backend.domain.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ClipReportRepository clipReportRepository;
    private final ClipRepository clipRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public ClipReport reportClip(Long clipId, Long reporterId, ReportReason reason, String detail) {
        if (clipReportRepository.existsByClip_IdAndReporter_Id(clipId, reporterId)) {
            throw new IllegalStateException("이미 신고를 접수했습니다.");
        }
        Clip clip = clipRepository.findById(clipId).orElseThrow();
        Member reporter = memberRepository.findById(reporterId).orElseThrow();

        // (선택) 본인 게시물 신고 방지
        if (clip.getUploader() != null && clip.getUploader().getId().equals(reporterId)) {
            throw new IllegalArgumentException("본인 게시물은 신고할 수 없습니다.");
        }

        ClipReport r = new ClipReport();
        r.setClip(clip);
        r.setReporter(reporter);
        r.setReason(reason);
        r.setDetail(detail != null && !detail.isBlank() ? detail.trim() : null);
        return clipReportRepository.save(r);
    }

    /* 관리자/운영자용 조회 (필터 optional) */
    @Transactional(readOnly = true)
    public Page<ClipReport> list(Long clipId, Long reporterId, Pageable pageable) {
        if (clipId != null) return clipReportRepository.findByClip_Id(clipId, pageable);
        if (reporterId != null) return clipReportRepository.findByReporter_Id(reporterId, pageable);
        return clipReportRepository.findAll(pageable);
    }

    @Transactional
    public void delete(Long reportId) {
        clipReportRepository.deleteById(reportId);
    }
}
