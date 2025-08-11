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

        return clipRepository.save(clip);
    }

    /**
     * 피드 (페이지네이션)
     * - N+1 방지 위해 가능하면 업로더 fetch join 메서드 사용 권장 (주석 참고)
     */
    public Page<Clip> feed(Pageable pageable) {

        // return clipRepository.findAllWithUploader(pageable);

        // 현재 메서드가 이미 존재한다면 일단 유지
        return clipRepository.findAllByOrderByIdDesc(pageable);
    }

    /**
     * 조회수 증가 (낙관적: 엔티티 읽고 +1)
     * - 동시성 더 강하게 원하면 레포에 update 쿼리 추가 권장
     */
    @Transactional
    public void increaseView(Long clipId) {
        final Clip c = clipRepository.findById(clipId).orElseThrow();
        c.setViewCount(c.getViewCount() + 1);
        // 강경한 동시성 필요 시:
        // clipRepository.incrementViewCount(clipId);
    }

    /**
     * 좋아요 토글
     * - 중복 좋아요는 유니크 제약으로 방지 (ClipLike uk: clip_id + member_id)
     * - 토글 후 count 재계산 -> 클립에 반영 (프론트에 카운트 보낼 때 일관성)
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
                // 동시 요청으로 인한 레이스: 이미 좋아요가 생긴 경우
                // 무시하고 아래 카운트 재계산으로 일관성 확보
            }
        }

        // 재계산 후 저장 (조회 쿼리 1회)
        final long count = likeRepository.countByClipId(clipId);
        final Clip clip = clipRepository.findById(clipId).orElseThrow();
        clip.setLikeCount(count);

        return !existed; // true: 좋아요됨, false: 좋아요 취소됨
    }

    /**
     * 댓글 작성
     * - 저장 후 총 카운트 재계산하여 클립에 반영
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
                        .author(member)   // 네 현재 엔티티가 author가 아니라 member 필드였음
                        .content(content.trim())
                        .build()
        );

        // 카운트 재계산하여 반영
        final long cnt = commentRepository.countByClipId(clipId);
        clip.setCommentCount(cnt);

        return saved;
    }

    public ClipComment addReply(Long parentCommentId, Long memberId, String content) {
        ClipComment parent = commentRepository.findById(parentCommentId).orElseThrow();
        Member author = memberRepository.findById(memberId).orElseThrow();

        ClipComment reply = ClipComment.builder()
                .clip(parent.getClip()) // 같은 영상
                .author(author)
                .content(content)
                .parent(parent)
                .build();

        parent.addReply(reply);
        return commentRepository.save(reply);
    }
}
