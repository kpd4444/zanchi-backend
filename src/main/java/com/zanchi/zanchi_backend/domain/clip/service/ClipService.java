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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClipService {

    private final ClipRepository clipRepository;
    private final ClipLikeRepository likeRepository;
    private final ClipCommentRepository commentRepository;
    private final FileStorageService storage;
    private final MemberRepository memberRepository;

    // 추가: 해시태그 동기화 서비스
    private final HashtagService hashtagService;

    /**
     * 클립 업로드
     */
    @Transactional
    public Clip upload(Long memberId, String caption, MultipartFile video) throws Exception {
        if (video == null || video.isEmpty()) {
            throw new IllegalArgumentException("video file is required");
        }
        final Member uploader = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("member not found"));

        final String videoUrl = storage.saveVideo(video);

        final Clip clip = Clip.builder()
                .uploader(uploader)
                .caption(caption)
                .videoUrl(videoUrl)
                .build();

        Clip saved = clipRepository.save(clip);

        // 본문에서 #태그 추출하여 연결
        hashtagService.syncClipTags(saved, caption);

        return saved;
    }

    /**
     * 캡션 수정(선택): 캡션 바뀌면 태그도 재동기화
     */
    @Transactional
    public Clip updateCaption(Long clipId, Long memberId, String newCaption) {
        Clip clip = clipRepository.findById(clipId).orElseThrow();
        if (!clip.getUploader().getId().equals(memberId)) {
            throw new IllegalArgumentException("forbidden");
        }
        clip.setCaption(newCaption);
        hashtagService.syncClipTags(clip, newCaption);
        return clip;
    }

    /**
     * 피드
     */
    public Page<Clip> feed(Pageable pageable) {
        return clipRepository.findAllByOrderByIdDesc(pageable);
    }

    /**
     * 조회수 증가
     */
    @Transactional
    public void increaseView(Long clipId) {
        final Clip c = clipRepository.findById(clipId).orElseThrow();
        c.setViewCount(c.getViewCount() + 1);
    }

    /**
     * 좋아요 토글
     */
    @Transactional
    public boolean toggleLike(Long clipId, Long memberId) {
        final boolean existed = likeRepository.existsByClipIdAndMemberId(clipId, memberId);

        if (existed) {
            likeRepository.deleteByClipIdAndMemberId(clipId, memberId);
        } else {
            final Clip clip = clipRepository.findById(clipId).orElseThrow();
            final Member member = memberRepository.findById(memberId).orElseThrow();
            try {
                likeRepository.save(ClipLike.builder().clip(clip).member(member).build());
            } catch (DataIntegrityViolationException dup) {
            }
        }

        final long count = likeRepository.countByClipId(clipId);
        final Clip clip = clipRepository.findById(clipId).orElseThrow();
        clip.setLikeCount(count);

        return !existed;
    }

    /**
     * 댓글 작성
     */
    @Transactional
    public ClipComment addComment(Long clipId, Long memberId, String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("content is required");
        }
        final Clip clip = clipRepository.findById(clipId).orElseThrow();
        final Member member = memberRepository.findById(memberId).orElseThrow();

        final ClipComment saved = commentRepository.save(
                ClipComment.builder()
                        .clip(clip)
                        .author(member)
                        .content(content.trim())
                        .build()
        );

        final long cnt = commentRepository.countByClipId(clipId);
        clip.setCommentCount(cnt);

        return saved;
    }

    @Transactional
    public ClipComment addReply(Long clipId, Long parentCommentId, Long memberId, String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("content is required");
        }
        ClipComment parent = commentRepository.findById(parentCommentId)
                .orElseThrow(() -> new IllegalArgumentException("parent comment not found"));

        if (!parent.getClip().getId().equals(clipId)) {
            throw new IllegalArgumentException("parent comment not in this clip");
        }

        Clip clip = parent.getClip();
        Member author = memberRepository.findById(memberId).orElseThrow();

        ClipComment reply = ClipComment.builder()
                .clip(clip)
                .author(author)
                .parent(parent)
                .content(content.trim())
                .build();

        ClipComment saved = commentRepository.save(reply);

        long cnt = commentRepository.countByClipId(clipId);
        clip.setCommentCount(cnt);

        return saved;
    }

    public Page<ClipComment> getReplies(Long parentCommentId, Pageable pageable) {
        return commentRepository.findByParentIdOrderByIdAsc(parentCommentId, pageable);
    }
}
