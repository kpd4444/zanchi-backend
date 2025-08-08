package com.zanchi.zanchi_backend.web.login;

import com.zanchi.zanchi_backend.domain.login.LoginService;
import com.zanchi.zanchi_backend.web.login.form.LoginForm;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@ResponseBody
@RequiredArgsConstructor
@RequestMapping("/api/login")
public class LoginController {

    private final LoginService loginService;

    @PostMapping
    public ResponseEntity<?> login(@RequestBody LoginForm form) {
        String token = loginService.login(form.getLoginId(), form.getPassword());


        // 로그인 실패 시
        if (token == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("로그인 실패: 아이디 또는 비밀번호를 확인하세요.");
        }

        // 로그인 성공 후 정상적으로 토큰이 발급
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);

        return ResponseEntity
                .ok()
                .headers(headers)
                .body("로그인 성공: 토큰이 발급되었습니다.");
    }
}
