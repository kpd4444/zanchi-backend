package com.zanchi.zanchi_backend.web.member.form;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MemberForm {

    @NotBlank(message = "이름은 필수입니다.")
    private String name;

    @NotBlank(message = "로그인 ID는 필수입니다.")
    private String loginId;

    @NotBlank(message = "비밀번호는 필수입니다.")
    private String password;
}
