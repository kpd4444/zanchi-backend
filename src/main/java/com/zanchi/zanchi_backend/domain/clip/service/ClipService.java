package com.zanchi.zanchi_backend.domain.clip.service;

import com.zanchi.zanchi_backend.domain.clip.*;
import com.zanchi.zanchi_backend.domain.clip.dto.ClipSummary;
import com.zanchi.zanchi_backend.domain.clip.repository.*;
import com.zanchi.zanchi_backend.domain.member.Member;
import com.zanchi.zanchi_backend.domain.member.MemberRepository;
import com.zanchi.zanchi_backend.domain.notification.event.LikeCreatedEvent;
import com.zanchi.zanchi_backend.domain.notification.event.CommentCreatedEvent;
import com.zanchi.zanchi_backend.domain.notification.event.MentionCreatedEvent;
import com.zanchi.zanchi_backend.domain.notification.event.ReplyCreatedEvent;
import com.zanchi.zanchi_backend.reco.PersonalizeCatalogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.ArrayList;

@Slf4j
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
    private final HashtagService hashtagService;                    // 해시태그 동기화
    private final ClipMentionRepository clipMentionRepository;      // 클립 멘션
    private final ClipCommentMentionRepository clipCommentRepo;     // 댓글 멘션
    private final ApplicationEventPublisher publisher;              // 알림 이벤트 발행
    private final PersonalizeCatalogService personalizeCatalogService;

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

        // #태그 동기화
        hashtagService.syncClipTags(saved, caption);

        // @멘션 동기화(캡션)
        upsertMentions(saved.getId(), caption);

        // === Personalize 카탈로그 업서트 ===
        try {
            String genres = extractGenresForPersonalize(caption); // "rock|ballad" 형식
            personalizeCatalogService.upsertClip(saved, genres);
        } catch (Exception e) {
            log.warn("Personalize upsert 실패: {}", e.getMessage());
        }

        return saved;
    }

    /**
     * 피드 (페이지네이션)
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
        // 필요 시 강경 동시성: clipRepository.incrementViewCount(clipId);
    }

    /**
     * 좋아요 토글 (+ 새 좋아요 시 알림)
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

                // 새 좋아요 알림 (actor = memberId, receiver = 업로더)
                Long receiverId = clip.getUploader().getId();
                publisher.publishEvent(new LikeCreatedEvent(memberId, receiverId, clipId));
            } catch (DataIntegrityViolationException dup) {
                // 동시 요청 레이스 케이스 무시
            }
        }

        final long count = likeRepository.countByClipId(clipId);
        final Clip clip = clipRepository.findById(clipId).orElseThrow();
        clip.setLikeCount(count);

        return !existed;
    }

    /**
     * 댓글 작성 (+ 알림, 댓글 멘션 동기화)
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

        // [ADD - 중요] 부모(댓글) INSERT를 DB에 먼저 반영시켜 FK 통과 보장
        commentRepository.flush();

        final long cnt = commentRepository.countByClipId(clipId);
        clip.setCommentCount(cnt);

        // 댓글 멘션 동기화
        upsertCommentMentions(saved);

        // 댓글 알림 (receiver = 클립 업로더)
        Long receiverId = clip.getUploader().getId();
        publisher.publishEvent(new CommentCreatedEvent(memberId, receiverId, clipId, saved.getId()));

        return saved;
    }

    /**
     * 대댓글 작성 (+ 알림, 멘션 동기화)
     */
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

        // [ADD - 중요]
        commentRepository.flush();

        long cnt = commentRepository.countByClipId(clipId);
        clip.setCommentCount(cnt);

        // 대댓글 멘션 동기화
        upsertCommentMentions(saved);

        // 대댓글 알림 (receiver = 부모 댓글 작성자)
        Long receiverId = parent.getAuthor().getId();
        publisher.publishEvent(new ReplyCreatedEvent(memberId, receiverId, clipId, saved.getId()));

        return saved;
    }

    @Transactional(readOnly = true)
    public Page<ClipComment> getReplies(Long parentCommentId, Pageable pageable) {
        return commentRepository.findByParentIdOrderByIdAsc(parentCommentId, pageable);
    }

    /**
     * 캡션 수정 → 해시태그/멘션 재동기화
     */
    @Transactional
    public Clip updateCaption(Long clipId, Long memberId, String caption) {
        Clip clip = clipRepository.findById(clipId).orElseThrow();
        if (!clip.getUploader().getId().equals(memberId)) {
            throw new AccessDeniedException("FORBIDDEN");
        }
        clip.setCaption(caption);

        // #태그 재동기화
        hashtagService.syncClipTags(clip, caption);

        // @멘션 재동기화
        upsertMentions(clipId, caption);

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

        String newUrl = storage.saveVideo(video);
        String oldUrl = clip.getVideoUrl();
        clip.setVideoUrl(newUrl);

        if (caption != null) {
            clip.setCaption(caption);
            // 캡션이 변경된 경우 동기화
            hashtagService.syncClipTags(clip, caption);
            upsertMentions(clipId, caption);
        }

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
        clipRepository.delete(clip);
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

    // 저장 토글
    @Transactional
    public boolean toggleSave(Long clipId, Long memberId) {
        if (clipSaveRepository.existsByMember_IdAndClip_Id(memberId, clipId)) {
            clipSaveRepository.deleteByMember_IdAndClip_Id(memberId, clipId);
            return false;
        }
        var member = memberRepository.getReferenceById(memberId);
        var clip = clipRepository.getReferenceById(clipId);
        clipSaveRepository.save(ClipSave.builder().member(member).clip(clip).build());
        return true;
    }

    @Transactional(readOnly = true)
    public Page<Clip> savedClips(Long memberId, Pageable pageable) {
        return clipSaveRepository
                .findByMember_IdOrderByIdDesc(memberId, pageable)
                .map(ClipSave::getClip);
    }

    @Transactional
    public void unsave(Long memberId, Long clipId) {
        clipSaveRepository.deleteByMember_IdAndClip_Id(memberId, clipId);
    }

    // 내가 좋아요한 클립 목록
    @Transactional(readOnly = true)
    public Page<Clip> pickClips(Long memberId, Pageable pageable) {
        return likeRepository.findByMemberId(memberId, pageable)
                .map(ClipLike::getClip);
    }

    /* =========================
       멘션 처리 (클립/댓글)
       ========================= */

    private void upsertMentions(Long clipId, String caption) {
        clipMentionRepository.deleteByClipId(clipId);

        // 프로젝트 내 MentionParser를 사용해 캡션에서 멘션된 memberId 추출
        var ids = MentionParser.parseIdsFromCaption(caption);
        if (ids == null || ids.isEmpty()) return;

        var entities = ids.stream()
                .distinct()
                .map(mid -> ClipMention.builder()
                        .clipId(clipId)
                        .mentionedMemberId(mid)
                        .build())
                .toList();

        clipMentionRepository.saveAll(entities);
    }

    private static final Pattern AT = Pattern.compile("(^|[^A-Za-z0-9_])@([A-Za-z0-9._-]{2,32})");

    // 해시태그 파싱용 (#kpop 같은 태그 뽑기)
    private static final Pattern HASH = Pattern.compile("#([\\p{L}0-9_\\-]{1,50})");

    // Personalize Items.GENRES 채우기용 ("dance|kpop" 형식)
    private String extractGenresForPersonalize(String caption) {
        if (caption == null || caption.isBlank()) return "default";
        var m = HASH.matcher(caption);
        var list = new ArrayList<String>();
        while (m.find()) list.add(m.group(1).toLowerCase());
        return list.isEmpty() ? "default" : String.join("|", list);
    }

    private Set<String> extractHandles(String text) {
        if (text == null || text.isBlank()) return Set.of();
        Matcher m = AT.matcher(text);
        Set<String> out = new HashSet<>();
        while (m.find()) out.add(m.group(2)); // @ 뒤의 핸들
        return out;
    }

    private void upsertCommentMentions(ClipComment comment) {
        clipCommentRepo.deleteByCommentId(comment.getId());

        Set<String> handles = extractHandles(comment.getContent());
        if (handles.isEmpty()) return;

        List<Member> users = memberRepository.findAllByLoginIdIn(handles);
        if (users.isEmpty()) return;

        var rows = users.stream()
                .map(u -> ClipCommentMention.builder()
                        .comment(comment)
                        .mentioned(u)
                        .handleSnapshot(u.getLoginId())
                        .build())
                .collect(Collectors.toList());

        clipCommentRepo.saveAll(rows);

        // 🚀 여기서 멘션 알림 이벤트 발행
        for (Member u : users) {
            if (!u.getId().equals(comment.getAuthor().getId())) {
                publisher.publishEvent(new MentionCreatedEvent(
                        comment.getAuthor().getId(),
                        u.getId(),
                        comment.getClip().getId(),
                        comment.getId()
                ));
            }
        }
    }

    @Transactional
    public void unlike(Long clipId, Long memberId) {
        // 존재하지 않아도 예외 없이 통과 → 멱등
        likeRepository.deleteByClipIdAndMemberId(clipId, memberId);

        // 최신 좋아요 수 반영
        long cnt = likeRepository.countByClipId(clipId);
        Clip clip = clipRepository.findById(clipId).orElseThrow();
        clip.setLikeCount(cnt);
    }

    @Transactional(readOnly = true)
    public Page<ClipSummary> followingClips(Long userId, Pageable pageable, String q) {
        return clipRepository.findFollowingClips(userId, q == null ? "" : q.trim(), pageable);
    }

}
