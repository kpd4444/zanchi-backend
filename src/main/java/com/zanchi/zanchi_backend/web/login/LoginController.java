package com.zanchi.zanchi_backend.web.login;

import com.zanchi.zanchi_backend.domain.login.LoginService;
import com.zanchi.zanchi_backend.web.login.form.LoginForm;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

@Controller
@ResponseBody
@RequiredArgsConstructor
@RequestMapping("/api/login")
public class LoginController {

    private final LoginService loginService;

    @PostMapping
    public ResponseEntity<?> login(@RequestBody LoginForm form, HttpServletResponse response) {
        String token = loginService.login(form.getLoginId(), form.getPassword());

        // 로그인 실패 시
        if (token == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("로그인 실패: 아이디 또는 비밀번호를 확인하세요.");
        }

        // 1. Authorization 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);

        // 2. HttpOnly 쿠키 설정 (테스트/운영 둘 다 가능)
        Cookie cookie = new Cookie("accessToken", token);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        response.addCookie(cookie);

        return ResponseEntity
                .ok()
                .headers(headers)
                .body("로그인 성공: 토큰이 발급되었습니다.");
    }
}
