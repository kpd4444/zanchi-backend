package com.zanchi.zanchi_backend.web.member;

import com.zanchi.zanchi_backend.domain.clip.service.FileStorageService;
import com.zanchi.zanchi_backend.domain.member.MemberRepository;
import com.zanchi.zanchi_backend.domain.member_follow.dto.MemberSummary;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/me")
public class ProfileController {

    private final MemberRepository memberRepository;
    private final FileStorageService storage;

    // 내 프로필 요약(아바타 포함)
    @GetMapping("/summary")
    public ResponseEntity<MemberSummary> me(@AuthenticationPrincipal(expression="member.id") Long meId) {
        if (meId == null) return ResponseEntity.status(401).build();
        var me = memberRepository.findById(meId).orElseThrow();
        return ResponseEntity.ok(MemberSummary.of(me));
    }

    // 아바타 업로드/교체
    @PostMapping(path="/avatar", consumes = "multipart/form-data")
    public ResponseEntity<?> uploadAvatar(@RequestPart("image") @NotNull MultipartFile image,
                                          @AuthenticationPrincipal(expression="member.id") Long meId) throws Exception {
        if (meId == null) return ResponseEntity.status(401).body(Map.of("error","UNAUTHORIZED"));
        if (image.isEmpty() || image.getContentType() == null || !image.getContentType().startsWith("image/")) {
            return ResponseEntity.badRequest().body(Map.of("error","INVALID_IMAGE"));
        }

        var me = memberRepository.findById(meId).orElseThrow();
        String old = me.getAvatarUrl();
        String url = storage.saveAvatar(image);
        me.setAvatarUrl(url);
        memberRepository.saveAndFlush(me);
        try { storage.deleteByUrl(old); } catch (Exception ignore) {}

        return ResponseEntity.ok(Map.of("avatarUrl", url));
    }

    // 아바타 제거(기본 이미지로)
    @DeleteMapping("/avatar")
    public ResponseEntity<?> deleteAvatar(@AuthenticationPrincipal(expression="member.id") Long meId) {
        if (meId == null) return ResponseEntity.status(401).body(Map.of("error","UNAUTHORIZED"));
        var me = memberRepository.findById(meId).orElseThrow();
        String old = me.getAvatarUrl();
        me.setAvatarUrl(null);
        try { storage.deleteByUrl(old); } catch (Exception ignore) {}
        return ResponseEntity.noContent().build();
    }
}