package com.zanchi.zanchi_backend.web.login;


import com.zanchi.zanchi_backend.domain.login.LoginService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class LoginController {

    private final LoginService loginService;
}
