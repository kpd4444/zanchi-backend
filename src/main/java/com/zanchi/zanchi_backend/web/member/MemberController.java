package com.zanchi.zanchi_backend.web.member;

import com.zanchi.zanchi_backend.domain.member.Member;
import com.zanchi.zanchi_backend.domain.member.MemberRepository;
import com.zanchi.zanchi_backend.domain.member.MemberService;
import com.zanchi.zanchi_backend.domain.member.dto.ChangeNameRequest;
import com.zanchi.zanchi_backend.domain.member.dto.ChangeNameResponse;
import com.zanchi.zanchi_backend.web.member.form.MemberForm;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class MemberController {

    private final MemberRepository memberRepository;
    private final MemberService memberService;

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody @Valid MemberForm form, BindingResult bindingResult) {

        // 1. 유효성 검증 실패
        if (bindingResult.hasErrors()) {
            String errors = bindingResult.getFieldErrors().stream()
                    .map(error -> error.getField() + ": " + error.getDefaultMessage())
                    .collect(Collectors.joining(", "));
            return ResponseEntity.badRequest().body(Map.of("status", "fail", "message", errors));
        }

        try {
            // 2. 회원가입 처리
            memberService.signup(form);
            return ResponseEntity.ok(Map.of("status", "success", "message", "회원가입 성공"));
        } catch (IllegalArgumentException e) {
            // 3. 아이디 중복 등 로직 오류
            return ResponseEntity.badRequest().body(Map.of("status", "fail", "message", e.getMessage()));
        } catch (Exception e) {
            // 4. 서버 오류
            return ResponseEntity.status(500).body(Map.of("status", "fail", "message", "서버 오류 발생"));
        }
    }
    @GetMapping("/members")
    public ResponseEntity<?> findAllMembers() {
        List<Member> members = memberService.findAll();
        return ResponseEntity.ok(Map.of("status", "success", "members", members));
    }

    @DeleteMapping("/members/{id}")
    public ResponseEntity<?> deleteMember(@PathVariable Long id) {
        try {
            memberService.deleteById(id);
            return ResponseEntity.ok(Map.of("status", "success", "message", "회원 삭제 완료"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("status", "fail", "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("status", "fail", "message", "서버 오류 발생"));
        }
    }

    @PatchMapping("/name")
    public ResponseEntity<?> changeMyName(
            @Valid @RequestBody ChangeNameRequest req,
            @AuthenticationPrincipal(expression = "member.id") Long meId // ← 네 프로젝트에 맞춘 표현식
    ) {
        if (meId == null) {
            return ResponseEntity.status(401).body(java.util.Map.of("error", "UNAUTHORIZED"));
        }
        ChangeNameResponse res = memberService.changeName(meId, req.name());
        return ResponseEntity.ok(res);
    }

}
