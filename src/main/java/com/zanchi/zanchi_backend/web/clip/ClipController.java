package com.zanchi.zanchi_backend.web.clip;

import com.zanchi.zanchi_backend.domain.clip.Clip;
import com.zanchi.zanchi_backend.domain.clip.ClipComment;
import com.zanchi.zanchi_backend.domain.clip.dto.*;
import com.zanchi.zanchi_backend.domain.clip.repository.ClipCommentRepository;
import com.zanchi.zanchi_backend.domain.clip.service.ClipService;
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
@RequestMapping("/api/clips")
@Validated
public class ClipController {

    private final ClipService clipService;
    private final ClipCommentRepository commentRepository;

    // 1) 업로드 (multipart/form-data: video, caption)
    @PostMapping(consumes = "multipart/form-data")
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

    // 2) 피드 (페이지네이션) - 항상 작성자 이름 포함해서 반환
    @GetMapping("/feed")
    public ResponseEntity<Page<ClipFeedRes>> feed(@RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "10") int size) {
        Page<Clip> p = clipService.feed(PageRequest.of(page, size)); // Page<Clip>
        Page<ClipFeedRes> mapped = p.map(ClipFeedRes::of);
        return ResponseEntity.ok(mapped);
    }

    // 3) 조회수 증가
    @PostMapping("/{clipId}/view")
    public ResponseEntity<?> view(@PathVariable Long clipId) {
        clipService.increaseView(clipId);
        return ResponseEntity.ok().build();
    }

    // 4) 좋아요 토글 (인증 필수, NPE 방지)
    @PostMapping("/{clipId}/like")
    public ResponseEntity<?> like(@PathVariable Long clipId,
                                  @AuthenticationPrincipal(expression = "id") Long memberId) {
        if (memberId == null) {
            return ResponseEntity.status(401).body(Map.of("error","UNAUTHORIZED"));
        }
        boolean liked = clipService.toggleLike(clipId, memberId);
        return ResponseEntity.ok(new LikeToggleRes(liked));
    }

    // 5) 댓글 목록/작성
    @GetMapping("/{clipId}/comments")
    public ResponseEntity<Page<ClipCommentRes>> comments(@PathVariable Long clipId,
                                                         @RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "20") int size) {
        Page<ClipComment> p = commentRepository.findByClipIdOrderByIdAsc(clipId, PageRequest.of(page, size));
        List<ClipCommentRes> list = p.getContent().stream().map(ClipCommentRes::of).toList();
        return ResponseEntity.ok(new PageImpl<>(list, p.getPageable(), p.getTotalElements()));
    }

    @PostMapping("/{clipId}/comments")
    public ResponseEntity<?> addComment(@PathVariable Long clipId,
                                        @AuthenticationPrincipal(expression = "id") Long memberId,
                                        @Valid @RequestBody CommentCreateReq req) {
        if (memberId == null) {
            return ResponseEntity.status(401).body(Map.of("error","UNAUTHORIZED"));
        }
        ClipComment saved = clipService.addComment(clipId, memberId, req.content());
        return ResponseEntity.ok(ClipCommentRes.of(saved));
    }

}
