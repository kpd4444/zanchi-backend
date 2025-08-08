package com.zanchi.zanchi_backend.domain.clip.service;

import com.zanchi.zanchi_backend.domain.clip.Clip;
import com.zanchi.zanchi_backend.domain.clip.ClipComment;
import com.zanchi.zanchi_backend.domain.clip.ClipLike;
import com.zanchi.zanchi_backend.domain.clip.repository.ClipCommentRepository;
import com.zanchi.zanchi_backend.domain.clip.repository.ClipLikeRepository;
import com.zanchi.zanchi_backend.domain.clip.repository.ClipRepository;
import com.zanchi.zanchi_backend.domain.member.Member;
import com.zanchi.zanchi_backend.domain.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional
public class ClipService {
    private final ClipRepository clipRepository;
    private final ClipLikeRepository likeRepository;
    private final ClipCommentRepository commentRepository;
    private final FileStorageService storage;
    private final MemberRepository memberRepository;

    public Clip upload(Long memberId, String caption, MultipartFile video) throws Exception {
        Member uploader = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("member not found"));
        String videoUrl = storage.saveVideo(video);
        Clip clip = Clip.builder()
                .uploader(uploader)
                .caption(caption)
                .videoUrl(videoUrl)
                .build();
        return clipRepository.save(clip);
    }

    @Transactional(readOnly = true)
    public Page<Clip> feed(Pageable pageable) {
        return clipRepository.findAllByOrderByIdDesc(pageable);
    }

    public void increaseView(Long clipId) {
        Clip c = clipRepository.findById(clipId).orElseThrow();
        c.setViewCount(c.getViewCount() + 1);
    }

    public boolean toggleLike(Long clipId, Long memberId) {
        var existing = likeRepository.findByClipIdAndMemberId(clipId, memberId);
        if (existing.isPresent()) {
            likeRepository.delete(existing.get());
        } else {
            Clip clip = clipRepository.findById(clipId).orElseThrow();
            Member member = memberRepository.findById(memberId).orElseThrow();
            likeRepository.save(ClipLike.builder().clip(clip).member(member).build());
        }
        long count = likeRepository.countByClipId(clipId);
        Clip clip = clipRepository.findById(clipId).orElseThrow();
        clip.setLikeCount(count);
        return existing.isEmpty(); // true: 좋아요됨, false: 취소됨
    }

    public ClipComment addComment(Long clipId, Long memberId, String content) {
        Clip clip = clipRepository.findById(clipId).orElseThrow();
        Member member = memberRepository.findById(memberId).orElseThrow();
        ClipComment c = commentRepository.save(ClipComment.builder()
                .clip(clip).member(member).content(content).build());
        clip.setCommentCount(commentRepository.countByClipId(clipId));
        return c;
    }
}