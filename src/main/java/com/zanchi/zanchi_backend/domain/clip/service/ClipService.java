package com.zanchi.zanchi_backend.domain.clip.service;

import com.zanchi.zanchi_backend.domain.clip.*;
import com.zanchi.zanchi_backend.domain.clip.repository.*;
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
    private final HashtagService hashtagService; // 해시태그 동기화
    private final ClipMentionRepository clipMentionRepository;
    private final ClipCommentMentionRepository clipCommentMentionRepository;

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

        //언급
        upsertMentions(saved.getId(), caption);
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
                // 동시 요청 레이스 케이스 무시
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

        upsertCommentMentions(saved);

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

        upsertCommentMentions(saved);
        return saved;
    }

    @Transactional(readOnly = true)
    public Page<ClipComment> getReplies(Long parentCommentId, Pageable pageable) {
        return commentRepository.findByParentIdOrderByIdAsc(parentCommentId, pageable);
    }

    /**
     * 캡션 수정 → 해시태그 재동기화
     */
    @Transactional
    public Clip updateCaption(Long clipId, Long memberId, String caption) {
        Clip clip = clipRepository.findById(clipId).orElseThrow();
        if (!clip.getUploader().getId().equals(memberId)) {
            throw new AccessDeniedException("FORBIDDEN");
        }
        clip.setCaption(caption);
        hashtagService.syncClipTags(clip, caption);
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
        if (caption != null){
            clip.setCaption(caption);
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
        clipSaveRepository.deleteByMemberIdAndClipId(memberId, clipId);
    }

    @Transactional(readOnly = true)
    public Page<Clip> pickClips(Long memberId, Pageable pageable) {
        return likeRepository.findByMemberIdOrderByIdDesc(memberId, pageable)
                .map(ClipLike::getClip);
    }

    private void upsertMentions(Long clipId, String caption) {
        clipMentionRepository.deleteByClipId(clipId);
        var ids = MentionParser.parseIdsFromCaption(caption);
        if (ids.isEmpty()) return;
        var entities = ids.stream()
                .distinct()
                .map(mid -> ClipMention.builder().clipId(clipId).mentionedMemberId(mid).build())
                .toList();
        clipMentionRepository.saveAll(entities);
    }

    private static final Pattern AT = Pattern.compile("(^|[^A-Za-z0-9_])@([A-Za-z0-9._-]{2,32})");

    private Set<String> extractHandles(String text){
        if (text == null || text.isBlank()) return Set.of();
        Matcher m = AT.matcher(text);
        Set<String> out = new HashSet<>();
        while (m.find()) out.add(m.group(2)); // @ 뒤의 핸들
        return out;
    }

    private void upsertCommentMentions(ClipComment comment){
        clipCommentMentionRepository.deleteByCommentId(comment.getId());

        Set<String> handles = extractHandles(comment.getContent());
        if (handles.isEmpty()) return;

        // @handle → Member 매핑
        List<Member> users = memberRepository.findAllByLoginIdIn(handles);
        if (users.isEmpty()) return;

        var rows = users.stream()
                .map(u -> ClipCommentMention.builder()
                        .comment(comment)
                        .mentioned(u)
                        .handleSnapshot(u.getLoginId())
                        .build())
                .collect(Collectors.toList());

        clipCommentMentionRepository.saveAll(rows);

        // TODO: 여기서 알림 이벤트 publish 하고 싶으면 추가
    }


}
