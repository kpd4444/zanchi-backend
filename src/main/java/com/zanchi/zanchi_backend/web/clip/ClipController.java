package com.zanchi.zanchi_backend.web.clip;


import com.zanchi.zanchi_backend.config.security.MemberPrincipal;
import com.zanchi.zanchi_backend.domain.clip.Clip;
import com.zanchi.zanchi_backend.domain.clip.ClipComment;
import com.zanchi.zanchi_backend.domain.clip.repository.ClipCommentRepository;
import com.zanchi.zanchi_backend.domain.clip.service.ClipService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
            return ResponseEntity.status(401).body(Map.of("error", "UNAUTHORIZED", "msg", "로그인이 필요합니다."));
        }
        var saved = clipService.upload(memberId, caption, video);
        return ResponseEntity.ok(saved);
    }

    // 2) 피드 (페이지네이션)
    @GetMapping("/feed")
    public Page<Clip> feed(@RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "10") int size) {
        return clipService.feed(PageRequest.of(page, size));
    }

    // 3) 조회수 증가 (클라이언트가 플레이 시작 시 호출)
    @PostMapping("/{clipId}/view")
    public void view(@PathVariable Long clipId) {
        clipService.increaseView(clipId);
    }

    // 4) 좋아요 토글
    @PostMapping("/{clipId}/like")
    public ResponseEntity<?> like(@PathVariable Long clipId,
                                  @AuthenticationPrincipal MemberPrincipal principal) {
        boolean liked = clipService.toggleLike(clipId, principal.getId());
        return ResponseEntity.ok().body("{\"liked\":" + liked + "}");
    }

    // 5) 댓글 목록 / 작성
    @GetMapping("/{clipId}/comments")
    public Page<ClipComment> comments(@PathVariable Long clipId,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "20") int size) {
        return commentRepository.findByClipIdOrderByIdAsc(clipId, PageRequest.of(page, size));
    }

    @PostMapping("/{clipId}/comments")
    public ClipComment addComment(@PathVariable Long clipId,
                                  @AuthenticationPrincipal MemberPrincipal principal,
                                  @RequestBody CommentReq req) {
        return clipService.addComment(clipId, principal.getId(), req.content());
    }

    @GetMapping("/auth/whoami")
    public Map<String, Object> whoami(@AuthenticationPrincipal com.zanchi.zanchi_backend.config.security.MemberPrincipal p) {
        if (p == null) return Map.of("auth", false);
        return Map.of(
                "auth", true,
                "id", p.getId(),
                "loginId", p.getUsername(),
                "authorities", p.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList(),
                "roleRaw", p.getMember().getRole()
        );
    }

    public record CommentReq(@NotBlank String content) {}
}
