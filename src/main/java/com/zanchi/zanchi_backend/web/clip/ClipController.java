package com.zanchi.zanchi_backend.web.clip;

import com.zanchi.zanchi_backend.domain.clip.Clip;
import com.zanchi.zanchi_backend.domain.clip.ClipComment;
import com.zanchi.zanchi_backend.domain.clip.dto.*;
import com.zanchi.zanchi_backend.domain.clip.repository.ClipCommentRepository;
import com.zanchi.zanchi_backend.domain.clip.service.ClipService;
import com.zanchi.zanchi_backend.domain.member.MemberRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Validated
public class ClipController {

    private final ClipService clipService;
    private final ClipCommentRepository commentRepository;
    private final MemberRepository memberRepository;

    // 1) 업로드 (multipart/form-data: video, caption)
    @PostMapping(path = "/api/clips", consumes = "multipart/form-data")
    public ResponseEntity<?> upload(
            @RequestPart("video") MultipartFile video,
            @RequestPart(value = "caption", required = false) String caption,
            @AuthenticationPrincipal(expression = "member.id") Long memberId
    ) throws Exception {
        if (memberId == null) {
            return ResponseEntity.status(401).body(Map.of("error","UNAUTHORIZED","msg","로그인이 필요합니다."));
        }
        Clip saved = clipService.upload(memberId, caption, video);
        return ResponseEntity.ok(ClipUploadRes.of(saved));
    }

    // 2) 피드 (페이지네이션)
    @GetMapping("/api/clips/feed")
    public ResponseEntity<Page<ClipFeedRes>> feed(@RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "10") int size) {
        Page<Clip> p = clipService.feed(PageRequest.of(page, size));
        Page<ClipFeedRes> mapped = p.map(ClipFeedRes::of);
        return ResponseEntity.ok(mapped);
    }

    // 3) 조회수 증가
    @PostMapping("/api/clips/{clipId}/view")
    public ResponseEntity<?> view(@PathVariable Long clipId) {
        clipService.increaseView(clipId);
        return ResponseEntity.ok().build();
    }

    // 4) 좋아요 토글
    @PostMapping("/api/clips/{clipId}/like")
    public ResponseEntity<?> like(@PathVariable Long clipId,
                                  @AuthenticationPrincipal(expression = "id") Long memberId) {
        if (memberId == null) {
            return ResponseEntity.status(401).body(Map.of("error","UNAUTHORIZED"));
        }
        boolean liked = clipService.toggleLike(clipId, memberId);
        return ResponseEntity.ok(new LikeToggleRes(liked));
    }

    // 5) 댓글 목록
    @GetMapping("/api/clips/{clipId}/comments")
    public ResponseEntity<Page<ClipCommentRes>> comments(@PathVariable Long clipId,
                                                         @RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "20") int size) {
        Page<ClipComment> p = commentRepository.findByClipIdOrderByIdAsc(clipId, PageRequest.of(page, size));
        List<ClipCommentRes> list = p.getContent().stream().map(ClipCommentRes::of).toList();
        return ResponseEntity.ok(new PageImpl<>(list, p.getPageable(), p.getTotalElements()));
    }

    // 5-1) 댓글 작성
    @PostMapping("/api/clips/{clipId}/comments")
    public ResponseEntity<?> addComment(@PathVariable Long clipId,
                                        @AuthenticationPrincipal(expression = "id") Long memberId,
                                        @Valid @RequestBody CommentCreateReq req) {
        if (memberId == null) {
            return ResponseEntity.status(401).body(Map.of("error","UNAUTHORIZED"));
        }
        ClipComment saved = clipService.addComment(clipId, memberId, req.content());
        return ResponseEntity.ok(ClipCommentRes.of(saved));
    }

    // 대댓글 목록
    @GetMapping("/api/clips/{clipId}/comments/{commentId}/replies")
    public ResponseEntity<Page<ClipCommentRes>> replies(
            @PathVariable Long clipId,
            @PathVariable Long commentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        var p = clipService.getReplies(commentId, PageRequest.of(page, size));
        var list = p.getContent().stream().map(ClipCommentRes::of).toList();
        return ResponseEntity.ok(new PageImpl<>(list, p.getPageable(), p.getTotalElements()));
    }

    // 대댓글 작성
    @PostMapping("/api/clips/{clipId}/comments/{commentId}/replies")
    public ResponseEntity<?> addReply(
            @PathVariable Long clipId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal(expression = "id") Long memberId,
            @Valid @RequestBody CommentCreateReq req) {
        if (memberId == null) {
            return ResponseEntity.status(401).body(Map.of("error","UNAUTHORIZED"));
        }
        var saved = clipService.addReply(clipId, commentId, memberId, req.content());
        return ResponseEntity.ok(ClipCommentRes.of(saved));
    }

    // 6) 캡션만 수정 (JSON PATCH)
    @PatchMapping("/api/clips/{clipId}")
    public ResponseEntity<?> updateCaption(
            @PathVariable Long clipId,
            @AuthenticationPrincipal(expression = "member.id") Long memberId,
            @Valid @RequestBody ClipUpdateReq req) {
        if (memberId == null) return ResponseEntity.status(401).body(Map.of("error","UNAUTHORIZED"));
        Clip updated = clipService.updateCaption(clipId, memberId, req.caption());
        return ResponseEntity.ok(ClipUploadRes.of(updated));
    }

    // 7) 비디오 교체(+캡션 옵션) (multipart/form-data)
    @PutMapping(path = "/api/clips/{clipId}", consumes = "multipart/form-data")
    public ResponseEntity<?> replaceVideo(
            @PathVariable Long clipId,
            @RequestPart("video") MultipartFile video,
            @RequestPart(value = "caption", required = false) String caption,
            @AuthenticationPrincipal(expression = "member.id") Long memberId) throws Exception {
        if (memberId == null) return ResponseEntity.status(401).body(Map.of("error","UNAUTHORIZED"));
        Clip updated = clipService.replaceVideo(clipId, memberId, video, caption);
        return ResponseEntity.ok(ClipUploadRes.of(updated));
    }

    // 8) 삭제
    @DeleteMapping("/api/clips/{clipId}")
    public ResponseEntity<?> deleteClip(
            @PathVariable Long clipId,
            @AuthenticationPrincipal(expression = "member.id") Long memberId) {
        if (memberId == null) return ResponseEntity.status(401).body(Map.of("error","UNAUTHORIZED"));
        clipService.deleteClip(clipId, memberId);
        return ResponseEntity.noContent().build();
    }

    // 내 페이지용(내 계정 기준)
    @GetMapping("/api/me/clips")
    public ResponseEntity<Page<ClipFeedRes>> myClips(
            @AuthenticationPrincipal(expression = "member.id") Long meId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        if (meId == null) return ResponseEntity.status(401).build();
        var p = clipService.myClips(meId, PageRequest.of(page, size)).map(ClipFeedRes::of);
        return ResponseEntity.ok(p);
    }

    // 다른 유저 페이지용(프로필 방문)
    @GetMapping("/api/members/{userId}/clips")
    public ResponseEntity<Page<ClipFeedRes>> userClips(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        var p = clipService.myClips(userId, PageRequest.of(page, size)).map(ClipFeedRes::of);
        return ResponseEntity.ok(p);
    }

    // 검색
    @GetMapping("/api/clips/search")
    public ResponseEntity<Page<ClipFeedRes>> searchClips(
            @RequestParam String q,
            @RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="20") int size) {
        var p = clipService.searchClips(q, PageRequest.of(page, size)).map(ClipFeedRes::of);
        return ResponseEntity.ok(p);
    }

    // 저장 토글
    @PostMapping("/api/clips/{clipId}/save")
    public ResponseEntity<SaveToggleRes> toggleSave(
            @PathVariable Long clipId,
            @AuthenticationPrincipal(expression="member.id") Long memberId) {
        if (memberId == null) return ResponseEntity.status(401).build();
        boolean saved = clipService.toggleSave(clipId, memberId);
        return ResponseEntity.ok(new SaveToggleRes(saved));
    }

    // 내가 저장한 클립 목록
    @GetMapping("/api/me/saved")
    public ResponseEntity<Page<ClipFeedRes>> mySaved(
            @AuthenticationPrincipal(expression="member.id") Long meId,
            @RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="50") int size){
        if (meId == null) return ResponseEntity.status(401).build();
        var p = clipService.savedClips(meId, PageRequest.of(page, size))
                .map(ClipFeedRes::of);
        return ResponseEntity.ok(p);
    }

    // 내가 좋아요한(픽) 클립 목록
    @GetMapping("/api/me/picks")
    public ResponseEntity<Page<ClipFeedRes>> myPicks(
            @AuthenticationPrincipal(expression="member.id") Long meId,
            @RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="50") int size){
        if (meId == null) return ResponseEntity.status(401).build();
        var p = clipService.pickClips(meId, PageRequest.of(page, size))
                .map(ClipFeedRes::of);
        return ResponseEntity.ok(p);
    }

    // 저장 해제 (id 기반)
    @DeleteMapping("/api/clips/{clipId}/save")
    public ResponseEntity<?> unsave(@PathVariable Long clipId,
                                    @AuthenticationPrincipal String loginId) {
        if (loginId == null) {
            return ResponseEntity.status(401).body(Map.of("error","UNAUTHORIZED"));
        }
        Long meId = memberRepository.findIdByLoginId(loginId)
                .orElseThrow(() -> new IllegalStateException("user not found: " + loginId));
        clipService.unsave(meId, clipId);
        return ResponseEntity.noContent().build();
    }
}
