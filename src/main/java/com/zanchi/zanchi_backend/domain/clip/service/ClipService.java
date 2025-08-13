package com.zanchi.zanchi_backend.domain.clip.service;

import com.zanchi.zanchi_backend.domain.clip.Clip;
import com.zanchi.zanchi_backend.domain.clip.ClipComment;
import com.zanchi.zanchi_backend.domain.clip.ClipLike;
import com.zanchi.zanchi_backend.domain.clip.ClipSave;
import com.zanchi.zanchi_backend.domain.clip.repository.ClipCommentRepository;
import com.zanchi.zanchi_backend.domain.clip.repository.ClipLikeRepository;
import com.zanchi.zanchi_backend.domain.clip.repository.ClipRepository;
import com.zanchi.zanchi_backend.domain.clip.repository.ClipSaveRepository;
import com.zanchi.zanchi_backend.domain.member.Member;
import com.zanchi.zanchi_backend.domain.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
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
    private final ClipSaveRepository clipSaveRepository;


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

        Clip clip = parent.getClip(); // 같은 클립
        Member author = memberRepository.findById(memberId).orElseThrow();

        ClipComment reply = ClipComment.builder()
                .clip(clip)
                .author(author)      // ← 필드명 author
                .parent(parent)      // ← 부모 연결
                .content(content.trim())
                .build();

        ClipComment saved = commentRepository.save(reply);

        // 총 댓글수 갱신(선택)
        long cnt = commentRepository.countByClipId(clipId);
        clip.setCommentCount(cnt);

        return saved;
    }

    @Transactional(readOnly = true)
    public Page<ClipComment> getReplies(Long parentCommentId, Pageable pageable) {
        return commentRepository.findByParentIdOrderByIdAsc(parentCommentId, pageable);
    }

    @Transactional
    public Clip updateCaption(Long clipId, Long memberId, String caption) {
        Clip clip = clipRepository.findById(clipId).orElseThrow();
        if (!clip.getUploader().getId().equals(memberId)) {
            throw new AccessDeniedException("FORBIDDEN");
        }
        clip.setCaption(caption);
        return clip;
    }

    @Transactional
    public Clip replaceVideo(Long clipId, Long memberId, MultipartFile video, String caption) throws Exception {
        if (video == null || video.isEmpty()) {
            throw new IllegalArgumentException("video file is required");
        }
        Clip clip = clipRepository.findById(clipId).orElseThrow();
        if (!clip.getUploader().getId().equals(memberId)) {
            throw new AccessDeniedException("FORBIDDEN");
        }

        // 1) 새 파일 저장
        String newUrl = storage.saveVideo(video);

        // 2) 엔티티 갱신
        String oldUrl = clip.getVideoUrl();
        clip.setVideoUrl(newUrl);
        if (caption != null) clip.setCaption(caption);

        // 3) 기존 파일 정리(실패해도 서비스 실패로 만들지 않음)
        try { storage.deleteByUrl(oldUrl); } catch (Exception ignore) {}

        return clip;
    }

    @Transactional
    public void deleteClip(Long clipId, Long memberId) {
        Clip clip = clipRepository.findById(clipId).orElseThrow();
        if (!clip.getUploader().getId().equals(memberId)) {
            throw new AccessDeniedException("FORBIDDEN");
        }
        String url = clip.getVideoUrl();

        // 댓글/좋아요는 Clip 엔티티의 orphanRemoval=true라 함께 삭제됨
        clipRepository.delete(clip);

        // 원본 파일 삭제(실패해도 무시)
        try { storage.deleteByUrl(url); } catch (Exception ignore) {}
    }

    @Transactional(readOnly = true)
    public Page<Clip> myClips(Long memberId, Pageable pageable) {
        return clipRepository.findByUploader_IdOrderByIdDesc(memberId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Clip> searchClips(String q, Pageable pageable) {
        String s = (q == null) ? "" : q.trim();
        return clipRepository.search(s, pageable);
    }

    //----------------------------------------
    // 저장 토글
    @Transactional
    public boolean toggleSave(Long clipId, Long memberId){
        if (clipSaveRepository.existsByMember_IdAndClip_Id(memberId, clipId)) {
            clipSaveRepository.deleteByMember_IdAndClip_Id(memberId, clipId);
            return false; // 해제됨
        }
        var member = memberRepository.getReferenceById(memberId);
        var clip   = clipRepository.getReferenceById(clipId);
        clipSaveRepository.save(ClipSave.builder().member(member).clip(clip).build());
        return true; // 저장됨
    }

    // 내가 저장한 클립 목록
    @Transactional(readOnly = true)
    public Page<Clip> savedClips(Long memberId, Pageable pageable){
        return clipSaveRepository
                .findByMember_IdOrderByIdDesc(memberId, pageable)
                .map(ClipSave::getClip);
    }

    @Transactional
    public void unsave(Long memberId, Long clipId){
        // 존재하지 않아도 에러 없이 무시
        clipSaveRepository.deleteByMemberIdAndClipId(memberId, clipId);
    }
    //--------------------------------

    @Transactional(readOnly = true)
    public Page<Clip> pickClips(Long memberId, Pageable pageable){
        return likeRepository.findByMemberIdOrderByIdDesc(memberId, pageable)
                .map(ClipLike::getClip);
    }
}
